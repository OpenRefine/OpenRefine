package com.google.refine.model.medadata;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
/**
 * Interface to import/export metadata 
 */
public interface IMetadata extends Jsonizable {
    public IMetadata loadFromJSON(JSONObject obj);
    
    public IMetadata loadFromFile(File metadataFile);
    
    void loadFromStream(InputStream inputStream);
    
    public void writeToFile(File metadataFile);
    
    /**
     * @param jsonWriter writer to save metadatea to
     * @param onlyIfDirty true to not write unchanged metadata
     * @throws JSONException
     */
    public void write(JSONWriter jsonWriter, boolean onlyIfDirty);
    
    public MetadataFormat getFormatName();
    public void setFormatName(MetadataFormat format);
    
    public Date getModified();

    public void updateModified();
    
    public boolean isDirty();

    public JSONObject getJSON();
    
    public List<Exception> validate();
}
