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
package org.openremote.beehive.configuration;

import org.glassfish.jersey.server.ResourceConfig;
import org.openremote.beehive.configuration.www.AccountsAPI;
import org.openremote.beehive.configuration.www.CommandsAPI;
import org.openremote.beehive.configuration.www.ControllerConfigurationsAPI;
import org.openremote.beehive.configuration.www.DevicesAPI;
import org.openremote.beehive.configuration.www.SensorsAPI;
import org.openremote.beehive.configuration.www.UsersAPI;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        packages("org.openremote.beehive.configuration");
        register(DevicesAPI.class);
        register(AccountsAPI.class);
        register(CommandsAPI.class);
        register(SensorsAPI.class);
        register(ControllerConfigurationsAPI.class);
        register(UsersAPI.class);
    }
}
