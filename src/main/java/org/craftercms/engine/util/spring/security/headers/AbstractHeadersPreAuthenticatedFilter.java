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

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.engine.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

/**
 * @author joseross
 */
public abstract class AbstractHeadersPreAuthenticatedFilter extends AbstractPreAuthenticatedProcessingFilter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHeadersPreAuthenticatedFilter.class);

    public static final String DEFAULT_HEADER_PREFIX = "MELLON_";
    public static final String DEFAULT_USERNAME_HEADER_NAME = DEFAULT_HEADER_PREFIX + "username";
    public static final String DEFAULT_EMAIL_HEADER_NAME = DEFAULT_HEADER_PREFIX + "email";
    public static final String DEFAULT_GROUPS_HEADER_NAME = DEFAULT_HEADER_PREFIX + "groups";
    public static final String DEFAULT_TOKEN_HEADER_NAME = DEFAULT_HEADER_PREFIX + "secure_key";

    public static final String HEADERS_CONFIG_KEY = "security.headers";
    public static final String HEADERS_TOKEN_CONFIG_KEY = HEADERS_CONFIG_KEY + ".token";
    public static final String HEADERS_ATTRS_CONFIG_KEY = HEADERS_CONFIG_KEY + ".attributes";
    public static final String HEADERS_GROUPS_CONFIG_KEY = HEADERS_CONFIG_KEY + ".groups";

    protected String headerPrefix = DEFAULT_HEADER_PREFIX;
    protected String usernameHeaderName = DEFAULT_USERNAME_HEADER_NAME;
    protected String emailHeaderName = DEFAULT_EMAIL_HEADER_NAME;
    protected String groupsHeaderName = DEFAULT_GROUPS_HEADER_NAME;
    protected String tokenHeaderName = DEFAULT_TOKEN_HEADER_NAME;
    protected String tokenExpectedValue;

    public AbstractHeadersPreAuthenticatedFilter() {
        setCheckForPrincipalChanges(true);
    }

    public void setHeaderPrefix(final String headerPrefix) {
        this.headerPrefix = headerPrefix;
    }

    public void setUsernameHeaderName(final String usernameHeaderName) {
        this.usernameHeaderName = usernameHeaderName;
    }

    public void setEmailHeaderName(final String emailHeaderName) {
        this.emailHeaderName = emailHeaderName;
    }

    public void setGroupsHeaderName(final String groupsHeaderName) {
        this.groupsHeaderName = groupsHeaderName;
    }

    public void setTokenHeaderName(final String tokenHeaderName) {
        this.tokenHeaderName = tokenHeaderName;
    }

    public void setTokenExpectedValue(final String tokenExpectedValue) {
        this.tokenExpectedValue = tokenExpectedValue;
    }

    protected String getTokenExpectedValue() {
        HierarchicalConfiguration config = ConfigUtils.getCurrentConfig();
        if (config != null && config.containsKey(HEADERS_TOKEN_CONFIG_KEY)) {
            return config.getString(HEADERS_TOKEN_CONFIG_KEY);
        }
        return tokenExpectedValue;
    }

    protected boolean hasValidToken(HttpServletRequest request) {
        String tokenHeaderValue = request.getHeader(tokenHeaderName);
        if (StringUtils.equals(tokenHeaderValue, getTokenExpectedValue())) {
            return true;
        } else {
            logger.warn("Token mismatch during authentication from '{}'", request.getRemoteAddr());
            return false;
        }
    }

    protected abstract boolean isEnabled();

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        if (isEnabled() && hasValidToken((HttpServletRequest) request)) {
            logger.debug("Filter '{}' is enabled, processing request", getClass().getSimpleName());
            super.doFilter(request, response, chain);
        } else {
            logger.debug("Filter '{}' is disabled", getClass().getSimpleName());
            chain.doFilter(request, response);
        }
    }

}
