package org.openremote.beehive.configuration.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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
@Entity
@Table(name = "protocol")
public class Protocol extends AbstractEntity {
    @Column(name = "type")
    private String type;

    @OneToMany
    private Collection<ProtocolAttribute> attributes = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Collection<ProtocolAttribute> getAttributes() {
        return Collections.unmodifiableCollection(attributes);
    }

    public void setAttributes(Collection<ProtocolAttribute> attributes) {
        if (this.attributes != attributes) {
            this.attributes.clear();
            if (attributes != null) {
                this.attributes.addAll(attributes);
            }
        }
    }
}
