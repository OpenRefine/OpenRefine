package com.google.refine.model.metadata;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.json.JSONObject;

/**
 * Interface to import/export metadata 
 */
public interface IMetadata  {
    public void loadFromJSON(JSONObject obj);
    
    public void loadFromFile(File metadataFile);
    
    public void loadFromStream(InputStream inputStream);
    
    public MetadataFormat getFormatName();
    public void setFormatName(MetadataFormat format);
    
    public LocalDateTime getModified();

    public void updateModified();
    
    public boolean isDirty();

    public List<Exception> validate();
}
