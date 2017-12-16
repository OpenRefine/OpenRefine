package com.google.refine.model.medadata;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Properties;

import org.apache.commons.beanutils.PropertyUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMetadata implements IMetadata {
    private final static Logger logger = LoggerFactory.getLogger("AbstractMetadata");
    
    private MetadataFormat formatName = MetadataFormat.UNKNOWN;
    
    protected LocalDateTime written = null;
    protected LocalDateTime           _modified;
    
    public MetadataFormat getFormatName() {
        return formatName;
    }

    
    public void setFormatName(MetadataFormat formatName) {
        this.formatName = formatName;
    }

    @Override
    public abstract void writeToJSON(JSONWriter writer, Properties options) throws JSONException;

    @Override
    public abstract IMetadata loadFromJSON(JSONObject obj);

    @Override
    public abstract IMetadata loadFromFile(File metadataFile);

    @Override
    public abstract void writeToFile(File metadataFile);

    @Override
    public abstract void write(JSONWriter jsonWriter, boolean onlyIfDirty);

    
    
    @Override
    public boolean isDirty() {
        return written == null || _modified.isAfter(written);
    }

    @Override
    public LocalDateTime getModified() {
        return _modified;
    }
    
    @Override
    public void updateModified() {
        _modified = LocalDateTime.now();
    }

    static boolean propertyExists(Object bean, String property) {
        return PropertyUtils.isReadable(bean, property) && 
               PropertyUtils.isWriteable(bean, property); 
    }
    
}
