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
package org.openremote.beehive.configuration.www.dto;

import org.openremote.beehive.configuration.model.Command;
import org.openremote.beehive.configuration.model.Protocol;
import org.openremote.beehive.configuration.model.ProtocolAttribute;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CommandDTOOut implements CommandDTO
{

  private final Command command;

  private Map<String, String> properties = new HashMap<String, String>();
  private Collection<String> tags = new HashSet<String>();

  public CommandDTOOut(Command command)
  {
    this.command = command;

    if (command.getSectionId() != null && !"".equals(command.getSectionId())) {
      addProperty(URN_OPENREMOTE_DEVICE_COMMAND_LIRC_SECTION_ID, command.getSectionId());
    }

    Collection<ProtocolAttribute> attributes = command.getProtocol().getAttributes();

    for (ProtocolAttribute attribute : attributes) {
      if (URN_OPENREMOTE_DEVICE_COMMAND_TAG.equals(attribute.getName())) {
        addTag(attribute.getValue());
      } else {
        addProperty(attribute.getName(), attribute.getValue());
      }
    }
  }

  @Override
  public Long getId()
  {
    return command.getId();
  }

  @Override
  public String getName()
  {
    return command.getName();
  }

  @Override
  public String getProtocol()
  {
    return command.getProtocol().getType();
  }

  @Override
  public Map<String, String> getProperties()
  {
    return properties;
  }

  @Override
  public Collection<String> getTags()
  {
    return tags;
  }

  private void addProperty(String propertyName, String propertyValue) {
    this.properties.put(propertyName, propertyValue);
  }

  private void addTag(String tagValue) {
    this.tags.add(tagValue);
  }

}
