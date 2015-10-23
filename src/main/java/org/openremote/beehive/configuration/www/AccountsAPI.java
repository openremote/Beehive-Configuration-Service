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
import org.openremote.beehive.configuration.model.ControllerConfiguration;
import org.openremote.beehive.configuration.model.persistence.jpa.MinimalPersistentUser;
import org.openremote.beehive.configuration.repository.AccountRepository;
import org.openremote.beehive.configuration.repository.MinimalPersistentUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

@Component
@Path("/accounts")
@Scope("prototype")
public class AccountsAPI {
    @Context
    private ResourceContext resourceContext;

    @Context
    private SecurityContext security;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    MinimalPersistentUserRepository userRepository;

    private Account getAccountOrThrow(@PathParam("accountId") Long accountId) {
        Account account = accountRepository.findOne(accountId);
        if (account == null) {
            throw new NotFoundException();
        }
        return account;
    }

    @Path("/{accountId}/devices")
    public DevicesAPI getDevices(@PathParam("accountId") Long accountId) {
        validateAccountAccess(accountId);

        DevicesAPI resource = resourceContext.getResource(DevicesAPI.class);
        Account account = getAccountOrThrow(accountId);
        resource.setAccount(account);
        return resource;
    }

    @Path("/{accountId}/controllerConfigurations")
    public ControllerConfigurationsAPI getControllerConfigurations(@PathParam("accountId") Long accountId)
    {
        validateAccountAccess(accountId);

        ControllerConfigurationsAPI resource = resourceContext.getResource(ControllerConfigurationsAPI.class);
        Account account = getAccountOrThrow(accountId);
        resource.setAccount(account);
        return resource;
    }

    private void validateAccountAccess(Long accountId) {
        String username = security.getUserPrincipal().getName();

        MinimalPersistentUser user = null;
        user = userRepository.findByUsername(username);

        if (user == null) {
            throw new NotAuthorizedException("Basic realm=\"Beehive Configuration Service\"");
        }
        if (user.getAccountId() != accountId) {
            throw new ForbiddenException();
        }
    }
}
