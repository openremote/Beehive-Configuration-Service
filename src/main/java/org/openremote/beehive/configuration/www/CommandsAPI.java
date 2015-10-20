/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2014, OpenRemote Inc.
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
import org.openremote.beehive.configuration.www.dto.CommandDTO;
import org.openremote.beehive.configuration.www.dto.DeviceDTOOut;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class CommandsAPI {

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
  public Collection<CommandDTO> list() {
    Collection<Command> commands = device.getCommands();
    return commands
            .stream()
            .map(command -> new CommandDTO(command))
            .collect(Collectors.toList());
  }

}
