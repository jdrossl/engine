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

package org.craftercms.engine.util.spring.security.targeting;

import java.util.Collection;

import org.craftercms.engine.util.spring.security.profile.ProfileUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesUserDetailsService;

/**
 * @author joseross
 */
public class TargetingUserDetailsService extends PreAuthenticatedGrantedAuthoritiesUserDetailsService {

    @Override
    protected UserDetails createUserDetails(final Authentication token,
                                            final Collection<? extends GrantedAuthority> authorities) {
        return (ProfileUser) token.getPrincipal();
    }

}
