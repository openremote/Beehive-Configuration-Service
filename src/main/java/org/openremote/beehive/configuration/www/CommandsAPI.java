/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2015, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.beehive.configuration.www;

import org.openremote.beehive.configuration.model.Command;
import org.openremote.beehive.configuration.model.Device;
import org.openremote.beehive.configuration.model.Protocol;
import org.openremote.beehive.configuration.model.ProtocolAttribute;
import org.openremote.beehive.configuration.repository.CommandRepository;
import org.openremote.beehive.configuration.repository.ProtocolRepository;
import org.openremote.beehive.configuration.www.dto.CommandDTO;
import org.openremote.beehive.configuration.www.dto.CommandDTOIn;
import org.openremote.beehive.configuration.www.dto.CommandDTOOut;
import org.openremote.beehive.configuration.www.dto.ErrorDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class CommandsAPI
{

  @Autowired
  PlatformTransactionManager platformTransactionManager;

  @Autowired
  CommandRepository commandRepository;

  @Autowired
  ProtocolRepository protocolRepository;

  private Device device;

  public Device getDevice()
  {
    return device;
  }

  public void setDevice(Device device)
  {
    this.device = device;
  }

  @GET
  public Collection<CommandDTOOut> list()
  {
    Collection<Command> commands = device.getCommands();
    Collection<CommandDTOOut> commandDTOs = new ArrayList<CommandDTOOut>();
    for (Command command : commands) {
      commandDTOs.add(new CommandDTOOut(command));
    }
    return commandDTOs;
  }

  @GET
  @Path("/{commandId}")
  public CommandDTOOut getById(@PathParam("commandId") Long commandId)
  {
    return new CommandDTOOut(device.getCommandById(commandId));
  }

  @POST
  public Response createCommand(final CommandDTOIn commandDTO)
  {
    if (device.getCommandByName(commandDTO.getName()) != null) {
      return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.CONFLICT).entity(new ErrorDTO(409, "A command with the same name already exists")).build();
    }

    return Response.ok(new CommandDTOOut(new TransactionTemplate(platformTransactionManager).execute(new TransactionCallback<Command>()
    {
      @Override
      public Command doInTransaction(TransactionStatus transactionStatus)
      {
        Command newCommand = new Command();
        populateCommandFromDTO(newCommand, commandDTO);
        commandRepository.save(newCommand);
        return newCommand;
      }
    }))).build();
  }

  private void populateCommandFromDTO(Command command, CommandDTOIn commandDTO)
  {
    command.setName(commandDTO.getName());
    Protocol protocol = new Protocol();
    protocol.setType(commandDTO.getProtocol());
    command.setProtocol(protocol);

    List<ProtocolAttribute> attributes = new ArrayList<ProtocolAttribute>();
    for (Map.Entry<String, String> e : commandDTO.getProperties().entrySet()) {
      if (CommandDTO.URN_OPENREMOTE_DEVICE_COMMAND_LIRC_SECTION_ID.equals(e.getKey()))
      {
        command.setSectionId(e.getValue());
      } else
      {
        ProtocolAttribute attribute = new ProtocolAttribute(e.getKey(), e.getValue());
        attribute.setProtocol(protocol);
        attributes.add(attribute);
      }
    }
    for (String t : commandDTO.getTags()) {
      ProtocolAttribute attribute = new ProtocolAttribute(CommandDTO.URN_OPENREMOTE_DEVICE_COMMAND_TAG, t);
      attribute.setProtocol(protocol);
      attributes.add(attribute);
    }
    protocol.setAttributes(attributes);

    device.addCommand(command);
  }

  @PUT
  @Path("/{commandId}")
  public Response udpateCommand(@PathParam("commandId")Long commandId, final CommandDTOIn commandDTO) {
    final Command existingCommand = device.getCommandById(commandId);

    Command optionalCommandWithSameName = device.getCommandByName(commandDTO.getName());
    if (optionalCommandWithSameName != null && !optionalCommandWithSameName.getId().equals(existingCommand.getId()))
    {
      return Response.status(Response.Status.CONFLICT).entity(new ErrorDTO(409, "A command with the same name already exists")).build();
    }

    return Response.ok(new CommandDTOOut(new TransactionTemplate(platformTransactionManager).execute(new TransactionCallback<Command>()
    {
      @Override
      public Command doInTransaction(TransactionStatus transactionStatus)
      {
        // TODO: check for more optimal solution, e.g. go over existing attributes and update individually
        // and add/delete as required
        // -> currently delete and re-creates protocol / protocol_attr records

        protocolRepository.delete(existingCommand.getProtocol());

        populateCommandFromDTO(existingCommand, commandDTO);
        commandRepository.save(existingCommand);
        return existingCommand;
      }
    }))).build();
  }

  @DELETE
  @Path("/{commandId}")
  public Response deleteCommand(@PathParam("commandId") Long commandId)
  {
    final Command existingCommand = device.getCommandById(commandId);

    if (!device.getSensorsReferencingCommand(existingCommand).isEmpty()) {
      return Response.status(Response.Status.CONFLICT).entity(new ErrorDTO(409, "This command is used by a sensor")).build();
    }

    new TransactionTemplate(platformTransactionManager).execute(new TransactionCallback<Object>()
    {
      @Override
      public Object doInTransaction(TransactionStatus transactionStatus)
      {
        device.removeCommand(existingCommand);
        commandRepository.delete(existingCommand);
        return null;
      }
    });

    return Response.ok().build();
  }

}