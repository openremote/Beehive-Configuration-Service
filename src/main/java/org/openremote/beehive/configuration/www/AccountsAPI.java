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
import org.openremote.beehive.configuration.repository.AccountRepository;
import org.openremote.beehive.configuration.www.dto.AccountDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Component
@Path("/accounts")
@Scope("prototype")
public class AccountsAPI {
    @Context
    private ResourceContext resourceContext;
    @Autowired
    AccountRepository accountRepository;


    private Account getAccountOrThrow(@PathParam("accountId") Long accountId) {
        Account account = accountRepository.findOne(accountId);
        if (account == null) {
            throw new NotFoundException();
        }
        return account;
    }

    @GET
    public List<AccountDTO> list() {
        List<AccountDTO> result = new ArrayList<>();
        Iterable<Account> all = accountRepository.findAll();
        for (Account account : all) {
            AccountDTO accountDTO = new AccountDTO();
            accountDTO.setId(account.getId());
            result.add(accountDTO);
        }
        return result;
    }


    @GET
    @Path("/{accountId}")
    public AccountDTO getAccountById(@PathParam("accountId") Long accountId) {
        Account account = getAccountOrThrow(accountId);
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId(account.getId());
        return accountDTO;
    }

    @POST
    @Path("/{accountId}")
    public AccountDTO createAccountById(@PathParam("accountId") Long accountId, AccountDTO accountDTO) {
        Account account = new Account();
        accountRepository.save(account);
        return new AccountDTO();
    }

    @Path("/{accountId}/devices")
    public DevicesAPI getDevices(@PathParam("accountId") Long accountId) {
        DevicesAPI resource = resourceContext.getResource(DevicesAPI.class);
        Account account = getAccountOrThrow(accountId);
        resource.setAccount(account);
        return resource;
    }

}
