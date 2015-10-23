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

import org.openremote.beehive.configuration.model.Account;
import org.openremote.beehive.configuration.model.ControllerConfiguration;
import org.openremote.beehive.configuration.repository.ControllerConfigurationRepository;
import org.openremote.beehive.configuration.www.dto.ControllerConfigurationDTOIn;
import org.openremote.beehive.configuration.www.dto.ControllerConfigurationDTOOut;
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
import java.util.stream.Collectors;

@Component
@Scope("prototype")
@Path("/controllerConfigurations")
public class ControllerConfigurationsAPI
{

  @Context
  private ResourceContext resourceContext;

  @Autowired
  PlatformTransactionManager platformTransactionManager;

  @Autowired
  ControllerConfigurationRepository controllerConfigurationRepository;

  private Account account;

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  @GET
  public Collection<ControllerConfigurationDTOOut> list() {
    Collection<ControllerConfiguration> configurations = account.getControllerConfigurations();
    return configurations
            .stream()
            .map(configuration -> new ControllerConfigurationDTOOut(configuration))
            .collect(Collectors.toList());
  }

  @GET
  @Path("/{configurationId}")
  public ControllerConfigurationDTOOut getById(@PathParam("configurationId")Long configurationId) {
    return new ControllerConfigurationDTOOut(account.getControllerConfigurationById(configurationId));
  }

  @POST
  public Response createControllerConfiguration(ControllerConfigurationDTOIn configurationDTO) {

    /* TODO
    if (deviceRepository.findByName(deviceDTO.getName()) != null)
    {
      return Response.status(Response.Status.CONFLICT).entity(new ErrorDTO(409, "A device with the same name already exists")).build();
    }
    */

    return Response.ok(new ControllerConfigurationDTOOut(new TransactionTemplate(platformTransactionManager).execute(new TransactionCallback<ControllerConfiguration>()
    {
      @Override
      public ControllerConfiguration doInTransaction(TransactionStatus transactionStatus)
      {
        ControllerConfiguration newConfiguration = new ControllerConfiguration();
        newConfiguration.setCategory(configurationDTO.getCategory());
        newConfiguration.setName(configurationDTO.getName());
        newConfiguration.setValue(configurationDTO.getValue());
        account.addControllerConfiguration(newConfiguration);
        controllerConfigurationRepository.save(newConfiguration);
        return newConfiguration;
      }
    }))).build();
  }

  @PUT
  @Path("/{configurationId}")
  public Response updateControllerConfiguration(@PathParam("configurationId")Long configurationId, ControllerConfigurationDTOIn configurationDTO) {
    ControllerConfiguration existingConfiguration = account.getControllerConfigurationById(configurationId);

    /*
    Device deviceWithSameName = deviceRepository.findByName(configurationDTOIn.getName());

    if (deviceWithSameName != null && !deviceWithSameName.getId().equals(existingConfiguration.getId()))
    {
      return Response.status(Response.Status.CONFLICT).entity(new ErrorDTO(409, "A device with the same name already exists")).build();
    }
    */

    return Response.ok(new ControllerConfigurationDTOOut(new TransactionTemplate(platformTransactionManager).execute(new TransactionCallback<ControllerConfiguration>()
    {
      @Override
      public ControllerConfiguration doInTransaction(TransactionStatus transactionStatus)
      {
        existingConfiguration.setCategory(configurationDTO.getCategory());
        existingConfiguration.setName(configurationDTO.getName());
        existingConfiguration.setValue(configurationDTO.getValue());
        controllerConfigurationRepository.save(existingConfiguration);
        return existingConfiguration;
      }
    }))).build();
  }

  @DELETE
  @Path("/{configurationId}")
  public Response deleteControllerConfiguration(@PathParam("configurationId")Long configurationid) {
    ControllerConfiguration existingConfiguration = account.getControllerConfigurationById(configurationid);

    new TransactionTemplate(platformTransactionManager).execute(new TransactionCallback<Object>()
    {
      @Override
      public Object doInTransaction(TransactionStatus transactionStatus)
      {
        account.removeControllerConfigurations(existingConfiguration);
        controllerConfigurationRepository.delete(existingConfiguration);
        return null;
      }
    });

    return Response.ok().build();

  }

}