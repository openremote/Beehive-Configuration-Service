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

import org.apache.tomcat.util.http.fileupload.IOUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@Path("/user")
@Scope("prototype")
public class UsersAPI
{
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
    System.out.println("Get configuration for user " + username);

    MinimalPersistentUser user = userRepository.findByUsername(username);

    Account account = accountRepository.findOne(user.getAccountId());
    try
    {
      // Create temporary folder
      final java.nio.file.Path temporaryFolder = Files.createTempDirectory("OR");


      // Create panel.xml file
      final File panelXmlFile = createPanelXmlFile(temporaryFolder);

      // Create controller.xml file
      final File controllerXmlFile = createControllerXmlFile(temporaryFolder, account);


      // Create drools folder and rules file (rules/modeler_rules.drl)
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


    // Create openremote.zip
    // Return openremote.zip

    StreamingOutput stream = new StreamingOutput() {
      public void write(OutputStream output) throws IOException, WebApplicationException
      {
        try {

          ZipOutputStream zipOutput = new ZipOutputStream(output);
          writeZipEntry(zipOutput, panelXmlFile, temporaryFolder);
          writeZipEntry(zipOutput, controllerXmlFile, temporaryFolder);
          writeZipEntry(zipOutput, droolsFile, temporaryFolder);
          zipOutput.close();
        } catch (Exception e) {
          throw new WebApplicationException(e);
        }
      }
    };

      // TODO: when can the temporary folder be deleted ?

      return Response.ok(stream).header("content-disposition", "attachment; filename = \"openremote.zip\"").build();

    } catch (IOException e)
    {
      e.printStackTrace();
    }

    return null;

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

  private File createControllerXmlFile(java.nio.file.Path temporaryFolder, Account account) throws IOException
  {
    File controllerXmlFile = new File(temporaryFolder.toFile(), "controller.xml");

    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    try
    {
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      DOMImplementation domImplementation = documentBuilder.getDOMImplementation();
      Document document = domImplementation.createDocument("http://www.openremote.org", "openremote", null);
      document.getDocumentElement().setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
              "xsi:schemaLocation", "http://www.openremote.org http://www.openremote.org/schemas/controller.xsd");

      Element componentsElement = document.createElement("components");
      document.getDocumentElement().appendChild(componentsElement);
      writeSensors(document, componentsElement, account);
      writeCommands(document, componentsElement, account);
      writeConfig(document, componentsElement, account);

      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      Result output = new StreamResult(controllerXmlFile);
      Source input = new DOMSource(document);
      transformer.transform(input, output);
    } catch (ParserConfigurationException e)
    {
      e.printStackTrace();
    } catch (TransformerConfigurationException e)
    {
      e.printStackTrace();
    } catch (TransformerException e)
    {
      e.printStackTrace();
    }

    return controllerXmlFile;
  }

  private void writeSensors(Document document, Element rootElement, Account account)
  {
    Element sensorsElement = document.createElement("sensors");
    rootElement.appendChild(sensorsElement);

    Collection<Device> devices = account.getDevices();

    for (Device device : devices)
    {
      Collection<Sensor> sensors = device.getSensors();
      for (Sensor sensor : sensors)
      {
        Element sensorElement = document.createElement("sensor");
        sensorsElement.appendChild(sensorElement);
        sensorElement.setAttribute("id", Long.toString(sensor.getId()));
        sensorElement.setAttribute("name", sensor.getName());
        sensorElement.setAttribute("type", sensor.getSensorType().toString().toLowerCase());

        Element includeElement = document.createElement("include");
        sensorElement.appendChild(includeElement);
        includeElement.setAttribute("type", "command");
        includeElement.setAttribute("ref", Long.toString(sensor.getSensorCommandReference().getCommand().getId()));

        switch (sensor.getSensorType())
        {
          case RANGE:
          {
            Element minElement = document.createElement("min");
            sensorElement.appendChild(minElement);
            minElement.setAttribute("value", Integer.toString(((RangeSensor)sensor).getMinValue()));

            Element maxElement = document.createElement("max");
            sensorElement.appendChild(maxElement);
            maxElement.setAttribute("value", Integer.toString(((RangeSensor)sensor).getMaxValue()));
            break;
          }
          case SWITCH:
          {
            Element stateElement = document.createElement("state");
            sensorElement.appendChild(stateElement);
            stateElement.setAttribute("name", "on");

            stateElement = document.createElement("state");
            sensorElement.appendChild(stateElement);
            stateElement.setAttribute("name", "off");
            break;
          }
          case CUSTOM:
          {
            Collection<SensorState> states = sensor.getStates();
            for (SensorState state : states)
            {
              Element stateElement = document.createElement("state");
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
    Element commandsElement = document.createElement("commands");
    rootElement.appendChild(commandsElement);

    Collection<Device> devices = account.getDevices();

    for (Device device : devices) {
      Collection<Command> commands = device.getCommands();
      for (Command command : commands) {
        Element commandElement = document.createElement("command");
        commandsElement.appendChild(commandElement);
        commandElement.setAttribute("id", Long.toString(command.getId()));
        commandElement.setAttribute("protocol", command.getProtocol().getType());

        Collection<ProtocolAttribute> attributes = command.getProtocol().getAttributes();
        for (ProtocolAttribute attribute : attributes) {
          Element propertyElement = document.createElement("property");
          commandElement.appendChild(propertyElement);
          propertyElement.setAttribute("name", attribute.getName());
          propertyElement.setAttribute("value", attribute.getValue());
        }
        Element propertyElement = document.createElement("property");
        commandElement.appendChild(propertyElement);
        propertyElement.setAttribute("name", "name");
        propertyElement.setAttribute("value", command.getName());

        propertyElement = document.createElement("property");
        commandElement.appendChild(propertyElement);
        propertyElement.setAttribute("name", "urn:openremote:device-command:device-name");
        propertyElement.setAttribute("value", command.getDevice().getName());

        propertyElement = document.createElement("property");
        commandElement.appendChild(propertyElement);
        propertyElement.setAttribute("name", "urn:openremote:device-command:device-id");
        propertyElement.setAttribute("value", Long.toString(command.getDevice().getId()));
      }
    }
  }

  private void writeConfig(Document document, Element rootElement, Account account)
  {
    Element configElement = document.createElement("config");
    rootElement.appendChild(configElement);

    Collection<ControllerConfiguration> configurations = account.getControllerConfigurations();
    for (ControllerConfiguration configuration : configurations) {
      if (!"rules.editor".equals(configuration.getName()))
      {
        Element propertyElement = document.createElement("property");
        configElement.appendChild(propertyElement);
        propertyElement.setAttribute("name", configuration.getName());
        propertyElement.setAttribute("value", configuration.getValue());
      }
    }

  }

}