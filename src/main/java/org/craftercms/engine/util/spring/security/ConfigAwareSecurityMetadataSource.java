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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.engine.util.ConfigUtils;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * @author joseross
 */
public class ConfigAwareSecurityMetadataSource implements FilterInvocationSecurityMetadataSource {

    public static final String URL_RESTRICTION_KEY = "security.urlRestrictions.restriction";
    public static final String URL_RESTRICTION_URL_KEY = "url";
    public static final String URL_RESTRICTION_EXPRESSION_KEY = "expression";

    @Override
    public Collection<ConfigAttribute> getAttributes(final Object object) throws IllegalArgumentException {
        FilterInvocation invocation = (FilterInvocation) object;
        //TODO: Cache config
        HierarchicalConfiguration siteConfig = ConfigUtils.getCurrentConfig();
        if (siteConfig != null) {
            List<HierarchicalConfiguration> restrictionsConfig = siteConfig.configurationsAt(URL_RESTRICTION_KEY);
            if (CollectionUtils.isNotEmpty(restrictionsConfig)) {
                for (HierarchicalConfiguration restrictionConfig : restrictionsConfig) {
                    String url = restrictionConfig.getString(URL_RESTRICTION_URL_KEY);
                    String expression = restrictionConfig.getString(URL_RESTRICTION_EXPRESSION_KEY);
                    if (StringUtils.isNotEmpty(url) && StringUtils.isNotEmpty(expression)) {
                        AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
                        if (matcher.matches(invocation.getRequest())) {
                            return Collections.singleton(new SecurityConfig(expression));
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return null;
    }

    @Override
    public boolean supports(final Class<?> clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }
}
