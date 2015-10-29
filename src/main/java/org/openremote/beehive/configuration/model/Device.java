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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

@Entity
@Table(name = "device")
public class Device extends AbstractEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "vendor")
    private String vendor;

    @Column(name = "model")
    private String model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_oid")
    private Account account;

    @OneToMany(mappedBy = "device", fetch = FetchType.LAZY, orphanRemoval = true)
    private Collection<Command> commands = new ArrayList<>();

    @OneToMany(mappedBy = "device", fetch = FetchType.LAZY, orphanRemoval = true)
    private Collection<Sensor> sensors = new ArrayList<>();

    public Collection<Command> getCommands() {
        return commands;
    }

    public void setCommands(Collection<Command> commands) {
        if (this.commands != commands) {
            this.commands.clear();
            if (commands != null) {
                this.commands.addAll(commands);
            }
        }
    }

    public void addCommand(Command command) {
        this.commands.add(command);
        command.setDevice(this);
    }

    public void removeCommand(Command command) {
        this.commands.remove(command);
        command.setDevice(null);
    }

    public Collection<Sensor> getSensors()
    {
        return sensors;
    }

    public void setSensors(Collection<Sensor> sensors)
    {
        if (this.sensors != sensors) {
            this.sensors.clear();
            if (sensors != null) {
                this.sensors.addAll(sensors);
            }
        }
    }

    public void addSensor(Sensor sensor) {
        this.sensors.add(sensor);
        sensor.setDevice(this);
    }

    public void removeSensor(Sensor sensor) {
        this.sensors.remove(sensor);
        sensor.setDevice(null);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Command getCommandById(Long commandId)
    {
        Collection<Command> commands = this.getCommands();
        for (Command command : commands) {
            if (commandId.equals(command.getId())) {
                return command;
            }
        }
        throw new NotFoundException();
    }

    public Command getCommandByName(String name) {
        if (name == null) {
            return null;
        }
        Collection<Command> commands = this.getCommands();
        for (Command command : commands) {
            if (name.equals(command.getName())) {
                return command;
            }
        }
        return null;
    }

    public Sensor getSensorById(Long sensorId)
    {
        Collection<Sensor> sensors = this.getSensors();
        for (Sensor sensor : sensors) {
            if (sensorId.equals(sensor.getId())) {
                return sensor;
            }
        }
        throw new NotFoundException();
    }

    public Sensor getSensorByName(String name) {
        if (name == null) {
            return null;
        }
        Collection<Sensor> sensors = this.getSensors();
        for (Sensor sensor : sensors) {
            if (name.equals(sensor.getName())) {
                return sensor;
            }
        }
        return null;
    }

    public Collection<Sensor> getSensorsReferencingCommand(Command command) {
        Collection<Sensor> sensors = this.getSensors();
        Collection<Sensor> referencingSensors = new HashSet<Sensor>();
        for (Sensor sensor : sensors) {
            if (sensor.getSensorCommandReference() != null) {
                if (command.getId().equals(sensor.getSensorCommandReference().getCommand().getId())) {
                    referencingSensors.add(sensor);
                }
            }
        }
        return referencingSensors;
    }
}
