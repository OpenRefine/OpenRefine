package com.google.refine.model.medadata;


import java.io.File;
import java.io.InputStream;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
/**
 * Interface to import/export metadata 
 *
 */
public interface IMetadata extends Jsonizable {
    public MetadataFormat formatName = MetadataFormat.UNKNOWN;
    
    public IMetadata loadFromJSON(JSONObject obj);
    
    public IMetadata loadFromFile(File metadataFile);
    
    public void writeToFile(File metadataFile);
    
    /**
     * @param jsonWriter writer to save metadatea to
     * @param onlyIfDirty true to not write unchanged metadata
     * @throws JSONException
     */
    public void write(JSONWriter jsonWriter, boolean onlyIfDirty);

    public String getEncoding();

    public Date getModified();

    public String getName();

    public boolean isDirty();

    public void setName(String projectName);

    public void setRowCount(int size);

    public void updateModified();

    public JSONArray getUserMetadata();

    public void setAnyStringField(String metaName, String valueString);

    void loadFromStream(InputStream inputStream);
    
}
