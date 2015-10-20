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
package org.openremote.beehive.configuration.www;

import org.openremote.beehive.configuration.exception.NotFoundException;
import org.openremote.beehive.configuration.model.Account;
import org.openremote.beehive.configuration.model.Device;
import org.openremote.beehive.configuration.www.dto.DeviceDTO;
import org.openremote.beehive.configuration.www.dto.DeviceDTOIn;
import org.openremote.beehive.configuration.www.dto.DeviceDTOOut;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
@Path("/devices")
public class DevicesAPI {

    @Context
    private ResourceContext resourceContext;

    private Account account;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    @GET
    public Collection<DeviceDTOOut> list() {
        Collection<Device> devices = account.getDevices();
        return devices
                .stream()
                .map(device -> new DeviceDTOOut(device))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{deviceId}")
    public DeviceDTOOut getById(@PathParam("deviceId")Long deviceId) {
        return new DeviceDTOOut(getDeviceById(deviceId));

    }


    @POST
    public DeviceDTOOut createDevice(DeviceDTOIn deviceDTO) {
        Device newDevice = new Device();
        newDevice.setName(deviceDTO.getName());
        account.addDevice(newDevice);
        return new DeviceDTOOut(newDevice);
    }

    @Path("/{deviceId}/commands")
    public CommandsAPI getCommands(@PathParam("deviceId") Long deviceId) {
        CommandsAPI resource = resourceContext.getResource(CommandsAPI.class);
        resource.setDevice(getDeviceById(deviceId));
        return resource;
    }

    private Device getDeviceById(Long deviceId) {
        Collection<Device> devices = account.getDevices();
        Optional<Device> deviceOptional = devices
                .stream()
                .filter(device -> device.getId().equals(deviceId))
                .findFirst();
        if (!deviceOptional.isPresent()) {
            throw new NotFoundException();
        }
        return deviceOptional.get();
    }

}
