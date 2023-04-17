
package org.openrefine.importing;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a file to import in an importing job. Multiple files can be imported to form the same project.
 * 
 *
 */
public class ImportingFileRecord {

    private final static Logger logger = LoggerFactory.getLogger(ImportingFileRecord.class);

    private String _location;
    private String _fileName;
    private long _size;
    private final String _origin;
    private String _declaredMimeType;
    private final String _mimeType;
    private final String _url;
    private String _encoding;
    private String _declaredEncoding;
    private String _format;
    private final String _archiveFileName;

    @JsonCreator
    public ImportingFileRecord(
            @JsonProperty("location") String location,
            @JsonProperty("fileName") String fileName,
            @JsonProperty("size") long size,
            @JsonProperty("origin") String origin,
            @JsonProperty("declaredMimeType") String declaredMimeType,
            @JsonProperty("mimeType") String mimeType,
            @JsonProperty("url") String url,
            @JsonProperty("encoding") String encoding,
            @JsonProperty("declaredEncoding") String declaredEncoding,
            @JsonProperty("format") String format,
            @JsonProperty("archiveFileName") String archiveFileName) {
        _location = location == null ? "" : location;
        _fileName = fileName;
        _size = size;
        _origin = origin;
        _declaredMimeType = declaredMimeType;
        _mimeType = mimeType;
        _url = url;
        _encoding = encoding;
        _declaredEncoding = declaredEncoding;
        _format = format;
        _archiveFileName = archiveFileName;
    }

    @JsonProperty("location")
    public String getLocation() {
        return _location;
    }

    @JsonProperty("fileName")
    public String getFileName() {
        return _fileName;
    }

    @JsonProperty("size")
    public long getCachedSize() {
        return _size;
    }

    @JsonProperty("origin")
    public String getOrigin() {
        return _origin;
    }

    @JsonProperty("declaredMimeType")
    public String getDeclaredMimeType() {
        return _declaredMimeType;
    }

    @JsonProperty("mimeType")
    public String getMimeType() {
        return _mimeType;
    }

    @JsonProperty("url")
    public String getUrl() {
        return _url;
    }

    @JsonProperty("encoding")
    public String getEncoding() {
        return _encoding;
    }

    @JsonProperty("declaredEncoding")
    public String getDeclaredEncoding() {
        return _declaredEncoding;
    }

    @JsonProperty("format")
    public String getFormat() {
        return _format;
    }

    @JsonProperty("archiveFileName")
    public String getArchiveFileName() {
        return _archiveFileName;
    }

    /**
     * Returns a string which represents where the file came from (URL, spark URI or filename).
     */
    @JsonIgnore
    public String getFileSource() {
        if (_url != null) {
            return _url;
        } else {
            return _fileName != null ? _fileName : "unknown";
        }
    }

    /**
     * Returns a path to the file stored in the raw data directory corresponding to this file record.
     * 
     * @param rawDataDir
     *            the directory where the files pertaining to the corresponding importing job are stored.
     */
    @JsonIgnore
    public File getFile(File rawDataDir) {
        return new File(rawDataDir, _location);
    }

    /**
     * Returns the number of bytes in this file. If this is cached in this record, the cached value will be returned.
     * 
     * @param rawDataDir
     *            the directory where the files pertaining to the corresponding importing
     * @return the length of the file in bytes
     */
    public long getSize(File rawDataDir) {
        if (_size > 0) {
            return _size;
        }
        File localFile = getFile(rawDataDir);
        _size = localFile.length();
        return _size;
    }

    @JsonIgnore
    public String getDerivedEncoding() {
        if (_encoding == null || _encoding.isEmpty()) {
            return _declaredEncoding;
        }
        return _encoding;
    }

    /*
     * TODO remove this and make field final
     */
    public void setDeclaredEncoding(String declaredEncoding) {
        _declaredEncoding = declaredEncoding;
    }

    /**
     * TODO remove this and make field final
     */
    public void setDeclaredMimeType(String declaredMimeType) {
        _declaredMimeType = declaredMimeType;
    }

    /**
     * TODO remove this and make field final
     */
    public void setLocation(String location) {
        _location = location;
    }

    /**
     * TODO remove this and make field final
     */
    public void setFormat(String format) {
        _format = format;
    }

    /**
     * TODO remove this and make field final
     */
    public void setFileName(String name) {
        _fileName = name;
    }

    /**
     * TODO remove this and make field final
     */
    public void setSize(long size) {
        _size = size;
    }

    /**
     * TODO remove this and make field final
     */
    public void setEncoding(String encoding) {
        _encoding = encoding;
    }
}
