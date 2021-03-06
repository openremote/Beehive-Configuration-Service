/*
 *
 *  * OpenRemote, the Home of the Digital Home.
 *  * Copyright 2008-2015, OpenRemote Inc.
 *  *
 *  * See the contributors.txt file in the distribution for a
 *  * full listing of individual contributors.
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as
 *  * published by the Free Software Foundation, either version 3 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openremote.beehive.configuration.www;

import org.apache.commons.io.FileUtils;

import org.apache.commons.io.IOUtils;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieModuleModel;
import org.openremote.beehive.configuration.exception.NotFoundException;
import org.openremote.beehive.configuration.model.Account;
import org.openremote.beehive.configuration.model.Command;
import org.openremote.beehive.configuration.model.ControllerConfiguration;
import org.openremote.beehive.configuration.model.Device;
import org.openremote.beehive.configuration.model.ProtocolAttribute;
import org.openremote.beehive.configuration.model.RangeSensor;
import org.openremote.beehive.configuration.model.Sensor;
import org.openremote.beehive.configuration.model.SensorState;
import org.openremote.beehive.configuration.model.persistence.jpa.MinimalPersistentUser;
import org.openremote.beehive.configuration.repository.AccountRepository;
import org.openremote.beehive.configuration.repository.MinimalPersistentUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@Path("/user")
@Scope("prototype")
public class UsersAPI
{
  private final static String CONTROLLER_XSD_PATH = "/controller-2.0-M7.xsd";

  private final static String OPENREMOTE_NAMESPACE = "http://www.openremote.org";

  private static final Logger log = LoggerFactory.getLogger(UsersAPI.class);

  @Context
  private ResourceContext resourceContext;

  @Context
  private SecurityContext security;

  @Autowired
  AccountRepository accountRepository;

  @Autowired
  MinimalPersistentUserRepository userRepository;

  @GET
  @Produces("application/octet-stream")
  @Path("/{username}/openremote.zip")
  public Response getConfigurationFile(@PathParam("username") String username)
  {
    log.info("Get configuration for user " + username);

    MinimalPersistentUser user = userRepository.findByUsername(username);
    if (user == null) {
      log.error("Configuration requested for unknown user " + username);
      throw new NotFoundException();
    }

    Account account = accountRepository.findOne(user.getAccountId());
    if (account == null) {
      log.error("Account not found for user " + username);
      throw new NotFoundException();
    }

    java.nio.file.Path temporaryFolderForFinalCleanup = null;
    try
    {
      // Create temporary folder
      final java.nio.file.Path temporaryFolder = Files.createTempDirectory("OR");
      temporaryFolderForFinalCleanup = temporaryFolder;

      // Create panel.xml file
      final File panelXmlFile = createPanelXmlFile(temporaryFolder);

      // Create controller.xml file
      final File controllerXmlFile = createControllerXmlFile(temporaryFolder, account);

      // Create drools folder and rules file (rules/modeler_rules.drl)
      final File droolsFile = createRules(temporaryFolder, account);

      // Create and return openremote.zip file
      StreamingOutput stream = new StreamingOutput() {
        public void write(OutputStream output) throws IOException, WebApplicationException
        {
          try {
            ZipOutputStream zipOutput = new ZipOutputStream(output);
            writeZipEntry(zipOutput, panelXmlFile, temporaryFolder);
            writeZipEntry(zipOutput, controllerXmlFile, temporaryFolder);
            if (droolsFile != null)
            {
              writeZipEntry(zipOutput, droolsFile, temporaryFolder);
            }
            zipOutput.close();
          } catch (Exception e) {
            log.error("Impossible to stream openremote.zip file" ,e);
            throw new WebApplicationException(e);
          } finally
          {
            removeTemporaryFiles(temporaryFolder);
          }
        }
      };

      // We've been able to build everything we need, set this to null so the cleanup is done
      // after the streaming has been done and not before (in final block of this method)
      temporaryFolderForFinalCleanup = null;
      return Response.ok(stream).header("content-disposition", "attachment; filename = \"openremote.zip\"").build();
    } catch (IOException e)
    {
      log.error("Issue creating openremote.zip file", e);
    }
    finally
    {
      removeTemporaryFiles(temporaryFolderForFinalCleanup);
    }

    return Response.serverError().build();
  }

  private File createRules(java.nio.file.Path temporaryFolder, Account account) throws IOException
  {
    ControllerConfiguration rulesConfiguration = account.getControllerConfigurationByName("rules.editor");
    File rulesFolder = new File(temporaryFolder.toFile(), "rules");
    rulesFolder.mkdir(); // TODO test return value
    final File droolsFile = new File(rulesFolder, "modeler_rules.drl");

    FileOutputStream fos = new FileOutputStream(droolsFile);

    if (rulesConfiguration != null) {
      PrintWriter pw = new PrintWriter(fos);
      pw.print(rulesConfiguration.getValue());
      pw.close();
    }
    fos.close();

    KieServices ks = KieServices.Factory.get();

    KieFileSystem kfs = ks.newKieFileSystem();

    KieModuleModel kproj = ks.newKieModuleModel();
    kfs.writeKModuleXML(kproj.toXML());

    ReleaseId releaseId = ks.newReleaseId("org.openremote.controller", "rules", "1.0");
    kfs.generateAndWritePomXML(releaseId);

    kfs.write("src/main/resources/modeler_rules.drl", rulesConfiguration.getValue());

    KieBuilder kb = ks.newKieBuilder(kfs).buildAll();

    // TODO: check for errors
        if( kb.getResults().hasMessages( org.kie.api.builder.Message.Level.ERROR ) ) {
            for( org.kie.api.builder.Message result : kb.getResults().getMessages() ) {
                System.out.println(result.getText());
              return null;
            }
        }


    final File kjarFile = new File(rulesFolder, "modeler_rules.kjar");

    InternalKieModule kieModule = (InternalKieModule) ks.getRepository().getKieModule(releaseId);
    FileUtils.writeByteArrayToFile(kjarFile, kieModule.getBytes());

    return kjarFile;
  }

  private void writeZipEntry(ZipOutputStream zipOutput, File file, java.nio.file.Path basePath) throws IOException
  {
    ZipEntry entry = new ZipEntry(basePath.relativize(file.toPath()).toString());
    entry.setSize(file.length());
    entry.setTime(file.lastModified());
    zipOutput.putNextEntry(entry);

    IOUtils.copy(new FileInputStream(file), zipOutput);

    zipOutput.flush();
    zipOutput.closeEntry();
  }

  // We just require an empty panel.xml file
  // Write bare minimum to it
  private File createPanelXmlFile(java.nio.file.Path temporaryFolder) throws IOException
  {
    File panelXmlFile = new File(temporaryFolder.toFile(), "panel.xml");

    FileOutputStream fos = new FileOutputStream(panelXmlFile);
    PrintWriter pw = new PrintWriter(fos);
    pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    pw.println("<openremote xmlns=\"http://www.openremote.org\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openremote.org http://www.openremote.org/schemas/panel.xsd\">");
    pw.println("</openremote>");
    pw.close();
    fos.close();

    return panelXmlFile;
  }

  private File createControllerXmlFile(java.nio.file.Path temporaryFolder, Account account)
  {
    File controllerXmlFile = new File(temporaryFolder.toFile(), "controller.xml");

    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    try
    {
      documentBuilderFactory.setNamespaceAware(true);
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      DOMImplementation domImplementation = documentBuilder.getDOMImplementation();
      Document document = domImplementation.createDocument(OPENREMOTE_NAMESPACE, "openremote", null);
      document.getDocumentElement().setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
              "xsi:schemaLocation", "http://www.openremote.org http://www.openremote.org/schemas/controller.xsd");

      Element componentsElement = document.createElementNS(OPENREMOTE_NAMESPACE, "components");
      document.getDocumentElement().appendChild(componentsElement);
      writeSensors(document, document.getDocumentElement(), account, findHighestCommandId(account));
      writeCommands(document, document.getDocumentElement(), account);
      writeConfig(document, document.getDocumentElement(), account);

      // Document is fully built, validate against schema before writing to file
      URL xsdResource = UsersAPI.class.getResource(CONTROLLER_XSD_PATH);
      if (xsdResource == null)
      {
        log.error("Cannot find XSD schema ''{0}''. Disabling validation...", CONTROLLER_XSD_PATH);
      }
      else
      {
        String language = XMLConstants.W3C_XML_SCHEMA_NS_URI;
        SchemaFactory factory = SchemaFactory.newInstance(language);
        Schema schema = factory.newSchema(xsdResource);
        Validator validator = schema.newValidator();
        validator.validate(new DOMSource(document));
      }

      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      Result output = new StreamResult(controllerXmlFile);
      Source input = new DOMSource(document);
      transformer.transform(input, output);
    } catch (ParserConfigurationException e)
    {
      log.error("Error generating controller.xml file", e);
      throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
    } catch (TransformerConfigurationException e)
    {
      log.error("Error generating controller.xml file", e);
      throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
    } catch (TransformerException e)
    {
      log.error("Error generating controller.xml file", e);
      throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
    } catch (SAXException e)
    {
      log.error("Error generating controller.xml file", e);
      throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
    } catch (IOException e)
    {
      log.error("Error generating controller.xml file", e);
      throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
    }

    return controllerXmlFile;
  }

  private Long findHighestCommandId(Account account)
  {
    Long maxId = -1l;
    Collection<Device> devices = account.getDevices();

    for (Device device : devices)
    {
      Collection<Command> commands = device.getCommands();
      for (Command command : commands)
      {
        if (command.getId() > maxId)
        {
          maxId = command.getId();
        }
      }
    }
    return  maxId;
  }

  private void writeSensors(Document document, Element rootElement, Account account, Long offset)
  {
    Element sensorsElement = document.createElementNS(OPENREMOTE_NAMESPACE, "sensors");
    rootElement.appendChild(sensorsElement);

    Collection<Device> devices = account.getDevices();

    for (Device device : devices)
    {
      Collection<Sensor> sensors = device.getSensors();
      for (Sensor sensor : sensors)
      {
        Element sensorElement = document.createElementNS(OPENREMOTE_NAMESPACE, "sensor");
        sensorsElement.appendChild(sensorElement);
        sensorElement.setAttribute("id", Long.toString(sensor.getId() + offset));
        sensorElement.setAttribute("name", sensor.getName());
        sensorElement.setAttribute("type", sensor.getSensorType().toString().toLowerCase());

        Element includeElement = document.createElementNS(OPENREMOTE_NAMESPACE, "include");
        sensorElement.appendChild(includeElement);
        includeElement.setAttribute("type", "command");
        includeElement.setAttribute("ref", Long.toString(sensor.getSensorCommandReference().getCommand().getId()));

        switch (sensor.getSensorType())
        {
          case RANGE:
          {
            Element minElement = document.createElementNS(OPENREMOTE_NAMESPACE, "min");
            sensorElement.appendChild(minElement);
            minElement.setAttribute("value", Integer.toString(((RangeSensor)sensor).getMinValue()));

            Element maxElement = document.createElementNS(OPENREMOTE_NAMESPACE, "max");
            sensorElement.appendChild(maxElement);
            maxElement.setAttribute("value", Integer.toString(((RangeSensor)sensor).getMaxValue()));
            break;
          }
          case SWITCH:
          {
            Element stateElement = document.createElementNS(OPENREMOTE_NAMESPACE, "state");
            sensorElement.appendChild(stateElement);
            stateElement.setAttribute("name", "on");

            stateElement = document.createElementNS(OPENREMOTE_NAMESPACE, "state");
            sensorElement.appendChild(stateElement);
            stateElement.setAttribute("name", "off");
            break;
          }
          case CUSTOM:
          {
            Collection<SensorState> states = sensor.getStates();
            for (SensorState state : states)
            {
              Element stateElement = document.createElementNS(OPENREMOTE_NAMESPACE, "state");
              sensorElement.appendChild(stateElement);
              stateElement.setAttribute("name" , state.getName());
              stateElement.setAttribute("value", state.getValue());
            }
            break;
          }
        }
      }
    }
  }

  private void writeCommands(Document document, Element rootElement, Account account)
  {
    Element commandsElement = document.createElementNS(OPENREMOTE_NAMESPACE, "commands");
    rootElement.appendChild(commandsElement);

    Collection<Device> devices = account.getDevices();

    for (Device device : devices) {
      Collection<Command> commands = device.getCommands();
      for (Command command : commands) {
        Element commandElement = document.createElementNS(OPENREMOTE_NAMESPACE, "command");
        commandsElement.appendChild(commandElement);
        commandElement.setAttribute("id", Long.toString(command.getId()));
        commandElement.setAttribute("protocol", command.getProtocol().getType());

        Collection<ProtocolAttribute> attributes = command.getProtocol().getAttributes();
        for (ProtocolAttribute attribute : attributes) {
          Element propertyElement = document.createElementNS(OPENREMOTE_NAMESPACE, "property");
          commandElement.appendChild(propertyElement);
          propertyElement.setAttribute("name", attribute.getName());
          propertyElement.setAttribute("value", attribute.getValue());
        }
        Element propertyElement = document.createElementNS(OPENREMOTE_NAMESPACE, "property");
        commandElement.appendChild(propertyElement);
        propertyElement.setAttribute("name", "name");
        propertyElement.setAttribute("value", command.getName());

        propertyElement = document.createElementNS(OPENREMOTE_NAMESPACE, "property");
        commandElement.appendChild(propertyElement);
        propertyElement.setAttribute("name", "urn:openremote:device-command:device-name");
        propertyElement.setAttribute("value", command.getDevice().getName());

        propertyElement = document.createElementNS(OPENREMOTE_NAMESPACE, "property");
        commandElement.appendChild(propertyElement);
        propertyElement.setAttribute("name", "urn:openremote:device-command:device-id");
        propertyElement.setAttribute("value", Long.toString(command.getDevice().getId()));
      }
    }
  }

  private void writeConfig(Document document, Element rootElement, Account account)
  {
    Element configElement = document.createElementNS(OPENREMOTE_NAMESPACE, "config");
    rootElement.appendChild(configElement);

    Collection<ControllerConfiguration> configurations = account.getControllerConfigurations();
    for (ControllerConfiguration configuration : configurations) {
      if (!"rules.editor".equals(configuration.getName()))
      {
        Element propertyElement = document.createElementNS(OPENREMOTE_NAMESPACE, "property");
        configElement.appendChild(propertyElement);
        propertyElement.setAttribute("name", configuration.getName());
        propertyElement.setAttribute("value", configuration.getValue());
      }
    }

  }

  private void removeTemporaryFiles(java.nio.file.Path directory)
  {
    if (directory == null) {
      return;
    }
    try
    {
      Files.walkFileTree(directory, new SimpleFileVisitor<java.nio.file.Path>() {
        @Override
        public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(java.nio.file.Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e)
    {
      log.error("Could not clean-up temporary folder used to create openremote.zip file", e);
    }
  }

}