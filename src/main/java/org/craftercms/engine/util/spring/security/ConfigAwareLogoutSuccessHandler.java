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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.craftercms.engine.util.ConfigUtils;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

/**
 * @author joseross
 */
public class ConfigAwareLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    public static final String LOGOUT_SUCCESS_URL_KEY = "security.logout.successUrl";

    @Override
    protected String determineTargetUrl(final HttpServletRequest request, final HttpServletResponse response) {
        HierarchicalConfiguration siteConfig = ConfigUtils.getCurrentConfig();
        if (siteConfig != null && siteConfig.containsKey(LOGOUT_SUCCESS_URL_KEY)) {
            return siteConfig.getString(LOGOUT_SUCCESS_URL_KEY);
        }
        return super.determineTargetUrl(request, response);
    }

}
