package org.openremote.beehive.configuration.www.dto;

import org.openremote.beehive.configuration.model.Device;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.function.Function;

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
public class DeviceDTO {
    private final Device device;

    public DeviceDTO(Device device) {
        this.device = device;
    }

    @XmlElement
    public Long getId() {
        return 1L;
    }

    @XmlElement
    public String getName() {
        return "";
    }

    @XmlElement
    public String getVendor() {
        return "";
    }

    @XmlElement
    public String getModel() {
        return "";
    }


}
