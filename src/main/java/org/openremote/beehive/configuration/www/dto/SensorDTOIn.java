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

import java.util.Map;

public class SensorDTOIn implements SensorDTO
{
  private Long id;
  private String name;
  private String type;
  private Long commandId;
  private Integer minValue;
  private Integer maxValue;
  private Map<String, String> states;

  @Override
  public Long getId()
  {
    return id;
  }

  public void setId(Long id)
  {
    this.id = id;
  }

  @Override
  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  @Override
  public String getType()
  {
    return type;
  }

  public void setType(String type)
  {
    this.type = type;
  }

  @Override
  public Long getCommandId()
  {
    return commandId;
  }

  public void setCommandId(Long commandId)
  {
    this.commandId = commandId;
  }

  @Override
  public Integer getMinValue()
  {
    return minValue;
  }

  public void setMinValue(Integer minValue)
  {
    this.minValue = minValue;
  }

  @Override
  public Integer getMaxValue()
  {
    return maxValue;
  }

  public void setMaxValue(Integer maxValue)
  {
    this.maxValue = maxValue;
  }

  @Override
  public Map<String, String> getStates()
  {
    return states;
  }

  public void setStates(Map<String, String> states)
  {
    this.states = states;
  }
}
