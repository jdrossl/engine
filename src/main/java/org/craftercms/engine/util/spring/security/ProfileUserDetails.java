/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.engine.util.spring.security;

import org.craftercms.security.authentication.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import static java.util.stream.Collectors.toSet;

/**
 * @author joseross
 */
public class ProfileUserDetails extends User {

    protected Authentication authentication;

    public ProfileUserDetails(final Authentication authentication, final UsernamePasswordAuthenticationToken token) {
        // TODO: Encrypt password!
        super(authentication.getProfile().getUsername(), token.getCredentials().toString(),
            authentication.getProfile().isEnabled(), true, true, true,
            authentication.getProfile().getRoles().stream().map(SimpleGrantedAuthority::new).collect(toSet()));
        this.authentication = authentication;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

}
