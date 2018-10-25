package org.craftercms.engine.store.s3;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.craftercms.core.store.impl.File;
import com.amazonaws.services.s3.model.S3Object;

public class S3File implements File {

    protected String name;
    protected String path;
    protected long lastModified;
    protected long length;

    protected byte[] content;

    public S3File(final S3Object s3Object) {
        name = FilenameUtils.getName(s3Object.getKey());
        path = FilenameUtils.getPath(s3Object.getKey());
        lastModified = s3Object.getObjectMetadata().getLastModified().getTime();
        length = s3Object.getObjectMetadata().getContentLength();
        content = new byte[(int)length];
        try(InputStream is = s3Object.getObjectContent()) {
            IOUtils.readFully(is, content);
        } catch (Exception e) {
            // do something?
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public String toString() {
        return "S3File{" + "name='" + name + '\'' + ", path='" + path + '\'' + ", lastModified=" + lastModified + ", "
            + "length=" + length + '}';
    }

}
