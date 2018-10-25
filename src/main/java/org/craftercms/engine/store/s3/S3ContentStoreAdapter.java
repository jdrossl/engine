package org.craftercms.engine.store.s3;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.exception.AuthenticationException;
import org.craftercms.core.exception.InvalidContextException;
import org.craftercms.core.exception.RootFolderNotFoundException;
import org.craftercms.core.exception.StoreException;
import org.craftercms.core.service.Context;
import org.craftercms.core.store.impl.AbstractFileBasedContentStoreAdapter;
import org.craftercms.core.store.impl.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;

public class S3ContentStoreAdapter extends AbstractFileBasedContentStoreAdapter {

    private static final Logger logger = LoggerFactory.getLogger(org.craftercms.engine.store.s3.S3ContentStoreAdapter.class);

    public static final String DELIMITER = "/";

    protected AmazonS3 getClient() {
        return AmazonS3ClientBuilder.defaultClient();
    }

    protected boolean isResultEmpty(ListObjectsV2Result result) {
        return (result.getCommonPrefixes() == null || result.getCommonPrefixes().isEmpty()) && (result.getObjectSummaries() == null || result.getObjectSummaries().isEmpty());
    }

    @Override
    public Context createContext(final String id, final String storeServerUrl, final String username,
                                 final String password, final String rootFolderPath, final boolean mergingOn,
                                 final boolean cacheOn, final int maxAllowedItemsInCache, final boolean ignoreHiddenFiles) throws RootFolderNotFoundException, StoreException, AuthenticationException {

        AmazonS3URI uri = new AmazonS3URI(rootFolderPath);

        ListObjectsV2Request request =
            new ListObjectsV2Request().withBucketName(uri.getBucket()).withPrefix(uri.getKey()).withDelimiter(DELIMITER);
        ListObjectsV2Result result = getClient().listObjectsV2(request);

        if(isResultEmpty(result)) {
            throw new RootFolderNotFoundException("Root folder " + rootFolderPath + " not found");
        }

        return new S3Context(id, this, storeServerUrl, rootFolderPath, mergingOn, cacheOn, maxAllowedItemsInCache,
            ignoreHiddenFiles, uri);
    }

    @Override
    protected File findFile(final Context context, final String path) throws InvalidContextException, StoreException {
        S3Context s3Context = (S3Context) context;
        String key = StringUtils.appendIfMissing(s3Context.getKey(), path);

        logger.debug("Looking item for key {}", key);
        AmazonS3 client = getClient();
        if(StringUtils.isEmpty(FilenameUtils.getExtension(key))) {
            // If it is a folder, check if there are objects with the prefix
            try {
                ListObjectsV2Request request =
                    new ListObjectsV2Request().withBucketName(s3Context.getBucket()).withPrefix(key).withDelimiter(DELIMITER);
                ListObjectsV2Result result = client.listObjectsV2(request);
                if (!isResultEmpty(result)) {
                    return new S3Prefix(StringUtils.appendIfMissing(key, DELIMITER));
                }
            } catch (AmazonS3Exception e) {
                if(e.getStatusCode() == 404) {
                    logger.debug("no item found for key {}", key);
                } else {
                    logger.error("Error looking for key " + key, e);
                }
            }
        } else {
            // If it is a file, check if the key exist
            try {
                GetObjectRequest request = new GetObjectRequest(s3Context.getBucket(), key);
                S3Object object = client.getObject(request);
                return new S3File(object);
            } catch (AmazonS3Exception e) {
                if(e.getStatusCode() == 404) {
                    logger.debug("no item found for key {}", key);
                } else {
                    logger.error("Error looking for key " + key, e);
                }
            }
        }
        return null;
    }

    @Override
    protected List<File> getChildren(final Context context, final File dir) throws InvalidContextException, StoreException {

        logger.debug("Looking for children of key {}", dir);

        S3Context s3Context = (S3Context) context;

        List<File> children = new LinkedList<>();
        AmazonS3 client = getClient();

        ListObjectsV2Request request =
            new ListObjectsV2Request().withBucketName(s3Context.getBucket()).withPrefix(((S3Prefix)dir).getPrefix()).withDelimiter(
                DELIMITER);
        ListObjectsV2Result result = client.listObjectsV2(request);
        if(isResultEmpty(result)) {
            return null;
        } else {
            result.getCommonPrefixes().forEach(prefix -> children.add(new S3Prefix(StringUtils.appendIfMissing(prefix, DELIMITER))));
            result.getObjectSummaries().forEach(summary -> children.add(new S3File(client.getObject(s3Context.getBucket(), summary.getKey()))));

            return children;
        }
    }

    @Override
    public boolean validate(final Context context) throws StoreException, AuthenticationException {
        S3Context s3Context = (S3Context) context;
        ListObjectsV2Request request =
            new ListObjectsV2Request().withBucketName(s3Context.getBucket()).withPrefix(s3Context.getKey()).withDelimiter(DELIMITER);
        ListObjectsV2Result result = getClient().listObjectsV2(request);

        return !isResultEmpty(result);
    }

    @Override
    public void destroyContext(final Context context) throws StoreException, AuthenticationException {
        // Nothing to do ...
    }

}
