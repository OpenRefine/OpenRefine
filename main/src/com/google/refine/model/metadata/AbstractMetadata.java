package com.google.refine.model.metadata;

import java.io.File;
import java.time.LocalDateTime;

import org.apache.commons.beanutils.PropertyUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AbstractMetadata implements IMetadata {
    @JsonIgnore
    private MetadataFormat formatName = MetadataFormat.UNKNOWN;
    
    @JsonIgnore
    protected LocalDateTime written = null;
    @JsonProperty("modified")
    protected LocalDateTime           _modified;
    
    public MetadataFormat getFormatName() {
        return formatName;
    }

    
    public void setFormatName(MetadataFormat formatName) {
        this.formatName = formatName;
    }

    @Override
    public abstract void loadFromJSON(JSONObject obj);

    @Override
    public abstract void loadFromFile(File metadataFile);

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
    
    protected static boolean propertyExists(Object bean, String property) {
        return PropertyUtils.isReadable(bean, property) && 
               PropertyUtils.isWriteable(bean, property); 
    }
    
}
