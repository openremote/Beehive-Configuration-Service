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
import org.openremote.beehive.configuration.model.Account;
import org.openremote.beehive.configuration.model.Device;
import org.openremote.beehive.configuration.repository.DeviceRepository;
import org.openremote.beehive.configuration.www.dto.DeviceDTOIn;
import org.openremote.beehive.configuration.www.dto.DeviceDTOOut;
import org.openremote.beehive.configuration.www.dto.ErrorDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
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
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
@Path("/devices")
public class DevicesAPI {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    PlatformTransactionManager platformTransactionManager;

    @Autowired
    DeviceRepository deviceRepository;

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
        return new DeviceDTOOut(account.getDeviceById(deviceId));
    }

    @POST
    public Response createDevice(DeviceDTOIn deviceDTO) {
        if (account.getDeviceByName(deviceDTO.getName()).isPresent())
        {
            return Response.status(Response.Status.CONFLICT).entity(new ErrorDTO(409, "A device with the same name already exists")).build();
        }

        return Response.ok(new DeviceDTOOut(new TransactionTemplate(platformTransactionManager).execute(new TransactionCallback<Device>()
        {
            @Override
            public Device doInTransaction(TransactionStatus transactionStatus)
            {
                Device newDevice = new Device();
                newDevice.setName(deviceDTO.getName());
                newDevice.setModel(deviceDTO.getModel());
                newDevice.setVendor(deviceDTO.getVendor());
                account.addDevice(newDevice);
                deviceRepository.save(newDevice);
                return newDevice;
            }
        }))).build();
    }

    @PUT
    @Path("/{deviceId}")
    public Response udpateDevice(@PathParam("deviceId")Long deviceId, DeviceDTOIn deviceDTO) {
        Device existingDevice = account.getDeviceById(deviceId);

        Optional<Device> optionalDeviceWithSameName = account.getDeviceByName(deviceDTO.getName());

        if (optionalDeviceWithSameName.isPresent() && !optionalDeviceWithSameName.get().getId().equals(existingDevice.getId()))
        {
            return Response.status(Response.Status.CONFLICT).entity(new ErrorDTO(409, "A device with the same name already exists")).build();
        }

        return Response.ok(new DeviceDTOOut(new TransactionTemplate(platformTransactionManager).execute(new TransactionCallback<Device>()
        {
            @Override
            public Device doInTransaction(TransactionStatus transactionStatus)
            {
                existingDevice.setName(deviceDTO.getName());
                existingDevice.setModel(deviceDTO.getModel());
                existingDevice.setVendor(deviceDTO.getVendor());
                deviceRepository.save(existingDevice);
                return existingDevice;
            }
        }))).build();
    }

    @DELETE
    @Path("/{deviceId}")
    public Response deleteDevice(@PathParam("deviceId")Long deviceId) {
        Device existingDevice = account.getDeviceById(deviceId);

        new TransactionTemplate(platformTransactionManager).execute(new TransactionCallback<Object>()
        {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus)
            {
                account.removeDevice(existingDevice);
                deviceRepository.delete(existingDevice);
                return null;
            }
        });

        return Response.ok().build();

    }

    @Path("/{deviceId}/commands")
    public CommandsAPI getCommands(@PathParam("deviceId") Long deviceId) {
        CommandsAPI resource = resourceContext.getResource(CommandsAPI.class);
        resource.setDevice(account.getDeviceById(deviceId));
        return resource;
    }

    @Path("/{deviceId}/sensors")
    public SensorsAPI getSensors(@PathParam("deviceId") Long deviceId) {
        SensorsAPI resource = resourceContext.getResource(SensorsAPI.class);
        resource.setDevice(account.getDeviceById(deviceId));
        return resource;
    }
}
