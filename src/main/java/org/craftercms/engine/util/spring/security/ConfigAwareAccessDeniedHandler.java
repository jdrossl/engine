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
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.craftercms.engine.util.ConfigUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;

/**
 * @author joseross
 */
public class ConfigAwareAccessDeniedHandler extends AccessDeniedHandlerImpl {

    public static final String ACCESS_DENIED_ERROR_PAGE_URL_KEY = "security.accessDenied.errorPageUrl";

    protected String errorPage;

    public ConfigAwareAccessDeniedHandler(final String errorPage) {
        this.errorPage = errorPage;
    }

    public String getErrorPage() {
        HierarchicalConfiguration siteConfig = ConfigUtils.getCurrentConfig();
        if (siteConfig != null && siteConfig.containsKey(ACCESS_DENIED_ERROR_PAGE_URL_KEY)) {
            return siteConfig.getString(ACCESS_DENIED_ERROR_PAGE_URL_KEY);
        }
        return errorPage;
    }

    // Copied because the super class doesn't use a getter :(
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException,
        ServletException {
        if (!response.isCommitted()) {
            String errorPage = getErrorPage();
            if (errorPage != null) {
                // Put exception into request scope (perhaps of use to a view)
                request.setAttribute(WebAttributes.ACCESS_DENIED_403,
                    accessDeniedException);

                // Set the 403 status code.
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                // forward to error page.
                RequestDispatcher dispatcher = request.getRequestDispatcher(errorPage);
                dispatcher.forward(request, response);
            }
            else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    accessDeniedException.getMessage());
            }
        }
    }

}