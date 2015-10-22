/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2014-2015, OpenRemote Inc.
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
package org.openremote.beehive.configuration.model.persistence.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Minimalistic mapping to the user table providing access to the basic information required in this service.
 * The sole usage of this is to retrieve the account id from the user name.
 */
@Entity
@Table(name = "user")
public class MinimalPersistentUser
{
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "oid")
  @Id
  private Long id;

  @Column(name="account_oid")
  private Long accountId;

  @Column(name="username")
  private String username;

  public Long getAccountId()
  {
    return accountId;
  }
}
