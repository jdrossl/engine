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
package org.craftercms.engine.util.cache;

import org.craftercms.core.exception.CrafterException;
import org.craftercms.core.service.CacheService;
import org.craftercms.core.service.Context;
import org.craftercms.engine.service.context.SiteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;

/**
 * Default implementation for {@link SiteCacheWarmerImpl}.
 *
 * @author avasquez
 * @since 3.1.4
 */
public class SiteCacheWarmerImpl implements SiteCacheWarmer {

    private static final Logger logger = LoggerFactory.getLogger(SiteCacheWarmerImpl.class);

    protected CacheService cacheService;
    protected List<ContextCacheWarmer> contextCacheWarmers;

    @Required
    public void setCacheService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Required
    public void setContextCacheWarmers(List<ContextCacheWarmer> contextCacheWarmers) {
        this.contextCacheWarmers = contextCacheWarmers;
    }

    @Override
    public void warmUpCache(SiteContext siteContext, boolean switchCache) {
        if (switchCache) {
            Context oldContext = siteContext.getContext();

            Context newContext = oldContext.clone();
            newContext.setCacheVersion(System.nanoTime());

            cacheService.addScope(newContext);

            try {
                logger.info("Warm up for new cache of site '{}' started", siteContext.getSiteName());

                doCacheWarmUp(newContext);
                if (siteContext.isValid()) {
                    siteContext.setContext(newContext);
                } else {
                    throw new CrafterException("The site context has become invalid");
                }

                logger.info("Warm up for new cache of site '{}' finished (switched with old cache)",
                            siteContext.getSiteName());
            } catch (Exception e) {
                cacheService.removeScope(newContext);

                logger.error("Cache warm up failed", e);
            }

            cacheService.removeScope(oldContext);
        } else {
            logger.info("Warm up for cache of site '{}' started", siteContext.getSiteName());

            doCacheWarmUp(siteContext.getContext());

            logger.info("Warm up for cache of site '{}' finished", siteContext.getSiteName());
        }
    }

    private void doCacheWarmUp(Context cacheContext) {
        for (ContextCacheWarmer cacheWarmer : contextCacheWarmers) {
            cacheWarmer.warmUpCache(cacheContext);
        }
    }

}
