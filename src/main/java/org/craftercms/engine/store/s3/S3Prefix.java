package org.craftercms.engine.store.s3;

import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;
import org.craftercms.core.store.impl.File;

public class S3Prefix implements File {

    protected String prefix;

    public S3Prefix(final String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public String getName() {
        return FilenameUtils.getName(prefix);
    }

    @Override
    public String getPath() {
        return FilenameUtils.getPath(prefix);
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public String toString() {
        return "S3Prefix{" + "prefix='" + prefix + '\'' + '}';
    }
}
