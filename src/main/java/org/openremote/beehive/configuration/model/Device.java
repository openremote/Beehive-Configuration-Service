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

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @OneToMany(mappedBy = "device", fetch = FetchType.LAZY)
    private Collection<Command> commands = new ArrayList<>();

    @OneToMany(mappedBy = "device", fetch = FetchType.LAZY)
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
        Optional<Command> commandOptional = commands
                .stream()
                .filter(command -> command.getId().equals(commandId))
                .findFirst();
        if (!commandOptional.isPresent())
        {
            throw new NotFoundException();
        }
        return commandOptional.get();
    }

    public Optional<Command> getCommandByName(String name) {
        if (name == null) {
            return null;
        }
        Collection<Command> commands = this.getCommands();
        Optional<Command> commandOptional = commands
                .stream()
                .filter(command -> name.equals(command.getName()))
                .findFirst();
        return commandOptional;
    }

    public Sensor getSensorById(Long sensorId)
    {
        Collection<Sensor> sensors = this.getSensors();
        Optional<Sensor> sensorOptional = sensors
                .stream()
                .filter(sensor -> sensor.getId().equals(sensorId))
                .findFirst();
        if (!sensorOptional.isPresent())
        {
            throw new NotFoundException();
        }
        return sensorOptional.get();
    }

    public Optional<Sensor> getSensorByName(String name) {
        if (name == null) {
            return null;
        }
        Collection<Sensor> sensors = this.getSensors();
        Optional<Sensor> sensorOptional = sensors
                .stream()
                .filter(sensor -> name.equals(sensor.getName()))
                .findFirst();
        return sensorOptional;
    }

    public Collection<Sensor> getSensorsReferencingCommand(Command command) {
        Collection<Sensor> sensors = this.getSensors();
        return sensors
                .stream()
                .filter(sensor -> {
                    if (sensor.getSensorCommandReference() == null) {
                      return false;
                    }
                    return (command.getId().equals(sensor.getSensorCommandReference().getCommand().getId()));
                }).collect(Collectors.toList());
    }
}
