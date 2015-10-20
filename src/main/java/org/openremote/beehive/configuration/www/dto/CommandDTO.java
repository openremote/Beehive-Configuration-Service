package org.openremote.beehive.configuration.www.dto;

import org.openremote.beehive.configuration.model.Command;
import org.openremote.beehive.configuration.model.ProtocolAttribute;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
@XmlRootElement
public class CommandDTO {

    private Long id;
    private String name;
    private String protocol;

    private Map<String, String> properties = new HashMap<String, String>();
    private Collection<String> tags = new HashSet<String>();

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    public Map<String,String> getProperties() {
        return properties;
    }

    public void addProperty(String propertyName, String propertyValue) {
        this.properties.put(propertyName, propertyValue);
    }

    public Collection<String> getTags() {
        return tags;
    }

    public void addTag(String tagValue) {
        this.tags.add(tagValue);
    }

    public CommandDTO(Command command)
    {
        setId(command.getId());
        setName(command.getName());
        setProtocol(command.getProtocol().getType());

        if (command.getSectionId() != null && !"".equals(command.getSectionId())) {
            addProperty("urn:openremote:device-command:lirc:section-id", command.getSectionId());
        }

        Collection<ProtocolAttribute> attributes = command.getProtocol().getAttributes();

        attributes.forEach(attribute -> {
            if ("urn:openremote:device-command:tag".equals(attribute.getName())) {
                addTag(attribute.getValue());
            } else {
                addProperty(attribute.getName(), attribute.getValue());
            }
        });

    }
}
