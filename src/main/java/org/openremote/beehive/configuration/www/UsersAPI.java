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
import org.openremote.beehive.configuration.model.SensorType;
import org.openremote.beehive.configuration.model.persistence.jpa.MinimalPersistentUser;
import org.openremote.beehive.configuration.repository.AccountRepository;
import org.openremote.beehive.configuration.repository.MinimalPersistentUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.openremote.beehive.configuration.model.SensorType.*;

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

    FileOutputStream fos = new FileOutputStream(controllerXmlFile);
    PrintWriter pw = new PrintWriter(fos);
    pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    pw.println("<openremote xmlns=\"http://www.openremote.org\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openremote.org http://www.openremote.org/schemas/controller.xsd\">");
    pw.println("<components/>");
    writeSensors(pw, account);
    writeCommands(pw, account);
    writeConfig(pw, account);
    pw.println("</openremote>");

    pw.close();
    fos.close();

    return controllerXmlFile;
  }

  private void writeSensors(PrintWriter pw, Account account)
  {
    // TODO: escaping

    pw.println("<sensors>");
    Collection<Device> devices = account.getDevices();

    for (Device device : devices) {
      Collection<Sensor> sensors = device.getSensors();
      for (Sensor sensor : sensors) {
        pw.println("<sensor id=\"" + sensor.getId() + "\" name=\"" + sensor.getName() + "\" type=\"" + sensor.getSensorType() + "\">");
        pw.println("<include type=\"command\" ref=\"" + sensor.getSensorCommandReference().getCommand().getId() + "\"/>");
        switch (sensor.getSensorType())
        {
          case RANGE:
          {
            pw.println("<min value=\"" + ((RangeSensor)sensor).getMinValue() + "\"/>");
            pw.println("<max value=\"" + ((RangeSensor)sensor).getMaxValue() + "\"/>");
            break;
          }
          case SWITCH:
          {
            pw.println("<state name=\"on\"/>");
            pw.println("<state name=\"off\"/>");
            break;
          }
          case CUSTOM:
          {
            Collection<SensorState> states = sensor.getStates();
            for (SensorState state : states)
            {
              pw.println("<state name=\"" + state.getName() + "\" value=\"" + state.getValue() + "\"/>");
            }
            break;
          }
        }
        pw.println("</sensor>");
      }
    }
    pw.println("</sensors>");
  }

  private void writeCommands(PrintWriter pw, Account account)
  {
    pw.println("<commands>");
    Collection<Device> devices = account.getDevices();

    for (Device device : devices) {
      Collection<Command> commands = device.getCommands();
      for (Command command : commands) {
        pw.println("<command id=\"" + command.getId() + "\" protocol=\"" + command.getProtocol().getType() + "\">");
        Collection<ProtocolAttribute> attributes = command.getProtocol().getAttributes();
        for (ProtocolAttribute attribute : attributes) {
          pw.println("<property name=\"" + attribute.getName() + "\" value=\"" + attribute.getValue() + "\"/>");
        }
        pw.println("<property name=\"name\" value=\"" + command.getName() + "\"/>");
        pw.println("<property name=\"urn:openremote:device-command:device-name\" value=\"" + command.getDevice().getName() + "\"/>");
        pw.println("<property name=\"urn:openremote:device-command:device-id\" value=\"" + command.getDevice().getId() + "\"/>");
        pw.println("</command>");
      }
    }
    pw.println("</commands>");
  }

  private void writeConfig(PrintWriter pw, Account account)
  {
    pw.println("<config>");
    Collection<ControllerConfiguration> configurations = account.getControllerConfigurations();
    for (ControllerConfiguration configuration : configurations) {
      if (!"rules.editor".equals(configuration.getName()))
      {
        pw.println("<property name=\"" + configuration.getName() + "\" value=\"" + configuration.getValue() + "\"/>");
      }
    }
    pw.println("</config>");
  }

}