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

import java.util.List;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * @author joseross
 */
public class EngineSecurityFilterChain implements SecurityFilterChain {

    protected boolean securityEnabled;

    protected String[] urlsToExclude;

    protected List<Filter> filters;

    public EngineSecurityFilterChain(final boolean securityEnabled, final String[] urlsToExclude,
                                     final List<Filter> filters) {
        this.securityEnabled = securityEnabled;
        this.urlsToExclude = urlsToExclude;
        this.filters = filters;
    }

    @Override
    public boolean matches(final HttpServletRequest request) {
        if (!securityEnabled) {
            return false;
        }
        for (String url : urlsToExclude) {
            AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
            if (matcher.matches(request)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Filter> getFilters() {
        return filters;
    }

}
