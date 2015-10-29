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
package org.openremote.beehive.configuration.model;

import org.openremote.beehive.configuration.exception.NotFoundException;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;

@Entity
@Table(name = "account")
public class Account extends AbstractEntity {

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private Collection<Device> devices = new ArrayList<>();

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private Collection<ControllerConfiguration> controllerConfigurations = new ArrayList<>();

    public Collection<Device> getDevices() {
        return devices;
    }

    public void setDevices(Collection<Device> devices) {
        if (this.devices != devices) {
            this.devices.clear();
            if (devices != null) {
                this.devices.addAll(devices);
            }
        }
    }

    public void addDevice(Device device) {
        this.devices.add(device);
        device.setAccount(this);
    }

    public void removeDevice(Device device) {
        this.devices.remove(device);
        device.setAccount(null);
    }

    public Device getDeviceById(Long deviceId) {
        Collection<Device> devices = this.getDevices();
        for (Device device : devices) {
            if (deviceId.equals(device.getId())) {
                return device;
            }
        }
        throw new NotFoundException();
    }

    public Device getDeviceByName(String name) {
        if (name == null) {
            return null;
        }
        Collection<Device> devices = this.getDevices();
        for (Device device : devices) {
            if (name.equals(device.getName())) {
                return device;
            }
        }
        return null;
    }

    public Collection<ControllerConfiguration> getControllerConfigurations()
    {
        return controllerConfigurations;
    }

    public void setControllerConfigurations(Collection<ControllerConfiguration> controllerConfigurations)
    {
        if (this.controllerConfigurations != controllerConfigurations) {
            this.controllerConfigurations.clear();
            if (controllerConfigurations != null) {
                this.controllerConfigurations.addAll(controllerConfigurations);
            }
        }
    }

    public void addControllerConfiguration(ControllerConfiguration configuration) {
        this.controllerConfigurations.add(configuration);
        configuration.setAccount(this);
    }

    public void removeControllerConfigurations(ControllerConfiguration configuration) {
        this.controllerConfigurations.remove(configuration);
        configuration.setAccount(null);
    }

    public ControllerConfiguration getControllerConfigurationById(Long configurationId) {
        Collection<ControllerConfiguration> configurations = this.getControllerConfigurations();
        for (ControllerConfiguration configuration : configurations) {
            if (configurationId.equals(configuration.getId())) {
                return configuration;
            }
        }
        throw new NotFoundException();
    }

    public ControllerConfiguration getControllerConfigurationByName(String name) {
        if (name == null) {
            return null;
        }
        Collection<ControllerConfiguration> configurations = this.getControllerConfigurations();
        for (ControllerConfiguration configuration : configurations) {
            if (name.equals(configuration.getName())) {
                return configuration;
            }
        }
        return null;
    }

}
