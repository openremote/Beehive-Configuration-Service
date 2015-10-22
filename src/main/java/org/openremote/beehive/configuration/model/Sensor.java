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

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

@Entity
@Table(name = "sensor")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@DiscriminatorValue("SIMPLE_SENSOR")
public class Sensor extends AbstractEntity {

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type")
    private SensorType sensorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_oid")
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_oid")
    private Account account;

    @OneToMany(mappedBy = "sensor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Collection<SensorState> states = new ArrayList<>();

    @OneToOne(mappedBy = "sensor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SensorCommandReference sensorCommandReference;

    public Sensor()
    {
    }

    public Sensor(SensorType sensorType)
    {
        this.sensorType = sensorType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SensorType getSensorType() {
        return sensorType;
    }

    public void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Collection<SensorState> getStates()
    {
        return states;
    }

    public void setStates(Collection<SensorState> states)
    {
        if (this.states != states) {
            this.states.clear();
            if (states != null) {
                this.states.addAll(states);
            }
        }
    }

    public void addState(SensorState state) {
        this.states.add(state);
        state.setSensor(this);
    }

    public void removeState(SensorState state) {
        this.states.remove(state);
        state.setSensor(null);
    }
    public SensorCommandReference getSensorCommandReference()
    {
        return sensorCommandReference;
    }

    public void setSensorCommandReference(SensorCommandReference sensorCommandReference)
    {
        this.sensorCommandReference = sensorCommandReference;
    }
}
