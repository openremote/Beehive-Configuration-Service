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

import org.openremote.beehive.configuration.model.RangeSensor;
import org.openremote.beehive.configuration.model.Sensor;
import org.openremote.beehive.configuration.model.SensorType;

import java.util.HashMap;
import java.util.Map;

public class SensorDTOOut implements SensorDTO
{
  private Sensor sensor;

  public SensorDTOOut(Sensor sensor)
  {
    this.sensor = sensor;
  }

  @Override
  public Long getId()
  {
    return this.sensor.getId();
  }

  @Override
  public String getName()
  {
    return this.sensor.getName();
  }

  @Override
  public String getType()
  {
    return this.sensor.getSensorType().toString();
  }

  @Override
  public Long getCommandId()
  {
    if (this.sensor.getSensorCommandReference() != null) {
      return this.sensor.getSensorCommandReference().getCommand().getId();
    }
    return null;
  }

  @Override
  public Integer getMinValue()
  {
    if (this.sensor.getSensorType().equals(SensorType.RANGE)) {
      return ((RangeSensor)sensor).getMinValue();
    }
    return null;
  }

  @Override
  public Integer getMaxValue()
  {
    if (this.sensor.getSensorType().equals(SensorType.RANGE)) {
      return ((RangeSensor)sensor).getMaxValue();
    }
    return null;
  }

  @Override
  public Map<String, String> getStates()
  {
    Map<String, String> states = new HashMap<String, String>();
    this.sensor.getStates().forEach(s -> states.put(s.getName(), s.getValue()));
    return states;
  }
}
