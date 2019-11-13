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

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.craftercms.engine.util.ConfigUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

/**
 *
 *
 * @author joseross
 * @since 3.1.5
 */
public abstract class ConfigAwarePreAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

    protected boolean alwaysEnabled = false;
    protected String enabledConfigKey;
    protected Class<? extends UserDetails> supportedPrincipalClass;

    public ConfigAwarePreAuthenticationFilter(final String enabledConfigKey) {
        this.enabledConfigKey = enabledConfigKey;
    }

    public void setAlwaysEnabled(final boolean alwaysEnabled) {
        this.alwaysEnabled = alwaysEnabled;
    }

    public void setSupportedPrincipalClass(final Class<? extends UserDetails> supportedPrincipalClass) {
        this.supportedPrincipalClass = supportedPrincipalClass;
    }

    public boolean isEnabled() {
        HierarchicalConfiguration siteConfig = ConfigUtils.getCurrentConfig();
        return alwaysEnabled || siteConfig != null && siteConfig.getBoolean(enabledConfigKey, false);
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        if (isEnabled()) {
            logger.debug("Filter is enabled, processing request");
            super.doFilter(request, response, chain);
        } else {
            logger.debug("Filter is disabled, skipping execution");
            chain.doFilter(request, response);
        }
    }

    @Override
    protected boolean principalChanged(final HttpServletRequest request, final Authentication currentAuthentication) {
        logger.debug("Current principal class:" + currentAuthentication.getPrincipal().getClass().getSimpleName());
        if (supportedPrincipalClass == null ||
            currentAuthentication.getPrincipal().getClass().equals(supportedPrincipalClass)) {
            logger.debug("Current principal class is supported, proceeding with the checks");
            return super.principalChanged(request, currentAuthentication);
        } else {
            logger.debug("Current principal class is not supported, skipping checks");
            return false;
        }
    }

    @Override
    protected Object getPreAuthenticatedCredentials(final HttpServletRequest request) {
        return "N/A";
    }

}
