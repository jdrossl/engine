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

package org.craftercms.engine.util.spring.security.profile;

import org.apache.commons.lang3.ArrayUtils;
import org.craftercms.profile.api.Profile;
import org.craftercms.profile.api.exceptions.ProfileException;
import org.craftercms.profile.api.services.ProfileService;
import org.craftercms.security.utils.tenant.TenantsResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @author joseross
 */
public class ProfileUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileUserDetailsService.class);

    protected TenantsResolver tenantsResolver;

    protected ProfileService profileService;

    public ProfileUserDetailsService(final TenantsResolver tenantsResolver, final ProfileService profileService) {
        this.tenantsResolver = tenantsResolver;
        this.profileService = profileService;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        String[] tenants = tenantsResolver.getTenants();
        if (ArrayUtils.isEmpty(tenants)) {
            throw new IllegalStateException("No tenants resolved for authentication");
        }
        for (String tenant : tenants) {
            try {
                Profile profile = profileService.getProfileByUsername(tenant, username);
                return new ProfileUser(profile);
            } catch (ProfileException e) {
                logger.debug("Profile not found for '{}' in tenant '{}', will try next tenant", username, tenant);
            }
        }
        logger.error("Profile not found for '{}' in any tenant", username);
        return null;
    }

}
