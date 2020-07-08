/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.engine.navigation.impl;

import org.apache.commons.io.FilenameUtils;
import org.craftercms.commons.locale.LocaleUtils;
import org.craftercms.core.exception.ItemProcessingException;
import org.craftercms.core.processors.ItemProcessor;
import org.craftercms.core.service.CachingOptions;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.craftercms.engine.service.context.SiteContext;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * @author joseross
 * @since
 */
public class LocaleItemProcessor implements ItemProcessor {

    protected ContentStoreService storeService;

    public LocaleItemProcessor(ContentStoreService storeService) {
        this.storeService = storeService;
    }

    @Override
    public Item process(Context context, CachingOptions cachingOptions, Item item) throws ItemProcessingException {

        if (item != null) {
            Locale locale = LocaleContextHolder.getLocale();
            String url = item.getDescriptorUrl();
            String localeUrl = FilenameUtils.getPath(url) + FilenameUtils.getBaseName(url) + "_" + LocaleUtils.toString(locale) + "." + FilenameUtils.getExtension(url);

            if (storeService.exists(SiteContext.getCurrent().getContext(), localeUrl)) {
                return storeService.getItem(SiteContext.getCurrent().getContext(), localeUrl);
            }

        }

        return item;
    }

}
