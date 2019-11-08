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

import java.util.Objects;

import org.craftercms.profile.api.Profile;
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

    protected Profile profile;

    public ProfileUserDetails(final Authentication auth) {
        this(auth.getProfile(), null);
        this.authentication = auth;
    }

    public ProfileUserDetails(final Authentication auth, final UsernamePasswordAuthenticationToken token) {
        this(auth.getProfile(), token);
        this.authentication = auth;
    }

    public ProfileUserDetails(final Profile profile) {
        this(profile, null);
    }

    public ProfileUserDetails(final Profile profile, final UsernamePasswordAuthenticationToken token) {
        // TODO: Encrypt password?
        super(profile.getUsername(), token != null? token.getCredentials().toString() : profile.getUsername(),
            profile.isEnabled(), true, true, true,
            profile.getRoles().stream().map(SimpleGrantedAuthority::new).collect(toSet()));
        this.profile = profile;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public Profile getProfile() {
        return profile;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProfileUserDetails)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final ProfileUserDetails details = (ProfileUserDetails)o;
        return Objects.equals(profile, details.profile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), profile);
    }

}
