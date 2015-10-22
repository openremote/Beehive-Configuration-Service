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

import org.openremote.beehive.configuration.exception.NotFoundException;
import org.openremote.beehive.configuration.model.Command;
import org.openremote.beehive.configuration.model.Device;
import org.openremote.beehive.configuration.model.Sensor;
import org.openremote.beehive.configuration.repository.CommandRepository;
import org.openremote.beehive.configuration.repository.ProtocolRepository;
import org.openremote.beehive.configuration.repository.SensorRepository;
import org.openremote.beehive.configuration.www.dto.CommandDTOOut;
import org.openremote.beehive.configuration.www.dto.SensorDTO;
import org.openremote.beehive.configuration.www.dto.SensorDTOOut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SensorsAPI {

  @Autowired
  PlatformTransactionManager platformTransactionManager;

  @Autowired
  SensorRepository sensorRepository;

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
  public Collection<SensorDTOOut> list()
  {
    Collection<Sensor> sensors = device.getSensors();
    return sensors
            .stream()
            .map(sensor -> new SensorDTOOut(sensor))
            .collect(Collectors.toList());
  }

  @GET
  @Path("/{sensorId}")
  public SensorDTOOut getById(@PathParam("sensorId") Long sensorId)
  {
    return new SensorDTOOut(getSensorById(sensorId));
  }

  private Sensor getSensorById(Long sensorId)
  {
    Collection<Sensor> sensors = device.getSensors();
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

}
