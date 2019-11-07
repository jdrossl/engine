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

import org.apache.commons.lang3.ArrayUtils;
import org.craftercms.security.authentication.Authentication;
import org.craftercms.security.authentication.AuthenticationManager;
import org.craftercms.security.utils.tenant.TenantsResolver;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author joseross
 */
public class ProfileAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    protected TenantsResolver tenantsResolver;

    protected AuthenticationManager authenticationManager;

    public ProfileAuthenticationProvider(final TenantsResolver tenantsResolver,
                                         final AuthenticationManager authenticationManager) {
        this.tenantsResolver = tenantsResolver;
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void additionalAuthenticationChecks(final UserDetails userDetails,
                                                  final UsernamePasswordAuthenticationToken authentication)
        throws AuthenticationException {
        // do nothing
    }

    @Override
    protected UserDetails retrieveUser(final String username,
                                       final UsernamePasswordAuthenticationToken authentication)
        throws AuthenticationException {
        String[] tenants = tenantsResolver.getTenants();
        if (ArrayUtils.isEmpty(tenants)) {
            throw new AuthenticationServiceException("No tenants resolved for authentication");
        }
        try {
            Authentication profileAuth =
                authenticationManager.authenticateUser(tenants, username, authentication.getCredentials().toString());

            return new ProfileUserDetails(profileAuth, authentication);
        } catch (Exception e) {
            throw new AuthenticationServiceException("Error authenticating user " + username, e);
        }
    }
}
