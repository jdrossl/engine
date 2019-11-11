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

package org.craftercms.engine.util.spring.security.headers;

import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.craftercms.core.util.cache.CacheTemplate;
import org.craftercms.engine.service.context.SiteContext;
import org.craftercms.engine.util.spring.security.CustomUser;
import org.springframework.security.core.userdetails.User;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * @author joseross
 */
public class StandaloneHeadersPreAuthenticatedFilter extends AbstractHeadersPreAuthenticatedFilter {

    public static final String STANDALONE_CONFIG_KEY = HEADERS_CONFIG_KEY + ".standalone";

    protected CacheTemplate cacheTemplate;

    @Override
    protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
        String username = request.getHeader(usernameHeaderName);

        if (isNotEmpty(username)) {
            //TODO: Get authorities from headers
            CustomUser user = new CustomUser(username, username, authorities);
            //TODO: Set attributes from headers
            return user;
        }

        return null;
    }

    @Override
    protected Object getPreAuthenticatedCredentials(final HttpServletRequest request) {
        // TODO: ok to return a random value?
        return UUID.randomUUID().toString();
    }

    @Override
    protected boolean isEnabled() {
        SiteContext siteContext = SiteContext.getCurrent();
        if (siteContext != null) {
            return cacheTemplate.getObject(siteContext.getContext(), () -> {
                HierarchicalConfiguration siteConfig = siteContext.getConfig();
                if (siteConfig != null && siteConfig.containsKey(STANDALONE_CONFIG_KEY)) {
                    return siteConfig.getBoolean(STANDALONE_CONFIG_KEY);
                }
                return false;
            }, STANDALONE_CONFIG_KEY);
        }
        return false;
    }

}
