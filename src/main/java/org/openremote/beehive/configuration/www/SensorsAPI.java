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
import org.openremote.beehive.configuration.model.CustomSensor;
import org.openremote.beehive.configuration.model.Device;
import org.openremote.beehive.configuration.model.Protocol;
import org.openremote.beehive.configuration.model.ProtocolAttribute;
import org.openremote.beehive.configuration.model.RangeSensor;
import org.openremote.beehive.configuration.model.Sensor;
import org.openremote.beehive.configuration.model.SensorCommandReference;
import org.openremote.beehive.configuration.model.SensorState;
import org.openremote.beehive.configuration.model.SensorType;
import org.openremote.beehive.configuration.repository.SensorRepository;
import org.openremote.beehive.configuration.www.dto.CommandDTO;
import org.openremote.beehive.configuration.www.dto.CommandDTOIn;
import org.openremote.beehive.configuration.www.dto.CommandDTOOut;
import org.openremote.beehive.configuration.www.dto.ErrorDTO;
import org.openremote.beehive.configuration.www.dto.SensorDTOIn;
import org.openremote.beehive.configuration.www.dto.SensorDTOOut;
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
import java.util.HashMap;
import java.util.List;
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
    return new SensorDTOOut(device.getSensorById(sensorId));
  }

  @POST
  public Response createSensor(SensorDTOIn sensorDTO)
  {
    if (device.getSensorByName(sensorDTO.getName()).isPresent()) {
      return Response.status(Response.Status.CONFLICT).entity(new ErrorDTO(409, "A sensor with the same name already exists")).build();
    }

    try {
      Command command = device.getCommandById(sensorDTO.getCommandId());
    } catch (NotFoundException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDTO(400, "Referenced command does not exist")).build();
    }

    return Response.ok(new SensorDTOOut(new TransactionTemplate(platformTransactionManager).execute(new TransactionCallback<Sensor>()
    {
      @Override
      public Sensor doInTransaction(TransactionStatus transactionStatus)
      {
        Sensor newSensor = createSensorFromDTO(sensorDTO);
        sensorRepository.save(newSensor);
        return newSensor;
      }
    }))).build();
  }

  private Sensor createSensorFromDTO(SensorDTOIn sensorDTO)
  {
    SensorType type = SensorType.valueOf(sensorDTO.getType());
    Sensor newSensor;
    switch (type) {
      case CUSTOM:
      {
        newSensor = new CustomSensor();
        break;
      }
      case RANGE:
      {
        newSensor = new RangeSensor();
        break;
      }
      default:
      {
        newSensor = new Sensor();
      }
    }
    populateSensorFromDTO(newSensor, sensorDTO);
    device.addSensor(newSensor);
    return newSensor;
  }

  private void populateSensorFromDTO(Sensor sensor, SensorDTOIn sensorDTO)
  {
    sensor.setName(sensorDTO.getName());
    SensorType type = SensorType.valueOf(sensorDTO.getType());
    sensor.setSensorType(type);
    switch (type) {
      case CUSTOM:
      {
        // Makes code below easier if we ensure the DTO states is non null
        if (sensorDTO.getStates() == null) {
          sensorDTO.setStates(new HashMap<String, String>());
        }

        CustomSensor customSensor = (CustomSensor)sensor;
        // Deal with new and updates
        sensorDTO.getStates().entrySet().forEach(e -> {
          Optional<SensorState> stateOptional = sensor.getStateByName(e.getKey());
          if (stateOptional.isPresent())
          {
            stateOptional.get().setValue(e.getValue());
          } else
          {
            SensorState state = new SensorState();
            state.setName(e.getKey());
            state.setValue(e.getValue());
            state.setSensor(sensor);
            customSensor.addState(state);
          }
        });

        // Go over existing states and get rid of ones that do not exist anymore
        sensor.getStates().forEach(state -> {
          if (!sensorDTO.getStates().containsKey(state.getName())) {
            customSensor.removeState(state);
          }
        });

        break;
      }
      case SWITCH:
        break;
      case LEVEL:
        break;
      case RANGE:
      {
        RangeSensor rangeSensor = (RangeSensor)sensor;
        rangeSensor.setMinValue(sensorDTO.getMinValue());
        rangeSensor.setMaxValue(sensorDTO.getMaxValue());
        break;
      }
      case COLOR:
        break;
    }

    SensorCommandReference commandReference = sensor.getSensorCommandReference();
    if (commandReference == null) {
      commandReference = new SensorCommandReference();
      commandReference.setSensor(sensor);
      sensor.setSensorCommandReference(commandReference);
    }
    Command command = device.getCommandById(sensorDTO.getCommandId());
    commandReference.setCommand(command);

    sensor.setAccount(device.getAccount());
  }

  @PUT
  @Path("/{sensorId}")
  public Response updateSensor(@PathParam("sensorId")Long sensorId, SensorDTOIn sensorDTO) {
    Sensor existingSensor = device.getSensorById(sensorId);

    Optional<Sensor> optionalSensorWithSameName = device.getSensorByName(sensorDTO.getName());
    if (optionalSensorWithSameName.isPresent() && !optionalSensorWithSameName.get().getId().equals(existingSensor.getId())) {
      return Response.status(Response.Status.CONFLICT).entity(new ErrorDTO(409, "A sensor with the same name already exists")).build();
    }

    try {
      Command command = device.getCommandById(sensorDTO.getCommandId());
    } catch (NotFoundException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorDTO(400, "Referenced command does not exist")).build();
    }

    return Response.ok(new SensorDTOOut(new TransactionTemplate(platformTransactionManager).execute(new TransactionCallback<Sensor>()
    {
      @Override
      public Sensor doInTransaction(TransactionStatus transactionStatus)
      {
        // TODO: check for more optimal solution for update of sensor states

        SensorType newType = SensorType.valueOf(sensorDTO.getType());
        Sensor sensor;

        if (existingSensor.getSensorType() == newType) {
          populateSensorFromDTO(existingSensor, sensorDTO);
          sensor = existingSensor;
        } else {
          device.removeSensor(existingSensor);
          sensorRepository.delete(existingSensor);
          sensor = createSensorFromDTO(sensorDTO);
        }
        sensorRepository.save(sensor);
        return sensor;
      }
    }))).build();
  }

  @DELETE
  @Path("/{sensorId}")
  public Response deleteSensor(@PathParam("sensorId") Long sensorId)
  {
    Sensor existingSensor = device.getSensorById(sensorId);

    new TransactionTemplate(platformTransactionManager).execute(new TransactionCallback<Object>()
    {
      @Override
      public Object doInTransaction(TransactionStatus transactionStatus)
      {
        device.removeSensor(existingSensor);
        sensorRepository.delete(existingSensor);
        return null;
      }
    });

    return Response.ok().build();
  }

}
