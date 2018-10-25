package org.craftercms.engine.store.s3;

import org.craftercms.core.service.ContextImpl;
import org.craftercms.core.store.ContentStoreAdapter;
import com.amazonaws.services.s3.AmazonS3URI;

public class S3Context extends ContextImpl {

    protected AmazonS3URI rootFolderUri;

    public S3Context(final String id, final ContentStoreAdapter storeAdapter, final String storeServerUrl,
                     final String rootFolderPath, final boolean mergingOn, final boolean cacheOn,
                     final int maxAllowedItemsInCache, final boolean ignoreHiddenFiles,
                     final AmazonS3URI rootFolderUri) {
        super(id, storeAdapter, storeServerUrl, rootFolderPath, mergingOn, cacheOn, maxAllowedItemsInCache,
            ignoreHiddenFiles);
        this.rootFolderUri = rootFolderUri;
    }

    public String getBucket() {
        return rootFolderUri.getBucket();
    }

    public String getKey() {
        return rootFolderUri.getKey();
    }

    @Override
    public String toString() {
        return "S3Context{" + "id='" + id + '\'' + ", storeAdapter=" + storeAdapter + ", storeServerUrl='" + storeServerUrl + '\'' + ", rootFolderPath='" + rootFolderPath + '\'' + ", mergingOn=" + mergingOn + ", cacheOn=" + cacheOn + ", maxAllowedItemsInCache=" + maxAllowedItemsInCache + ", ignoreHiddenFiles=" + ignoreHiddenFiles + '}';
    }

}
