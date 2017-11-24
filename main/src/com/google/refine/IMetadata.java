package com.google.refine;


import java.io.File;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
/**
 * Interface to import/export metadata 
 *
 */
public interface IMetadata extends Jsonizable {
    public String formatName = null;
    
    public ProjectMetadata loadFromJSON(JSONObject obj);
    
    public ProjectMetadata loadFromFile(File metadataFile);
    
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
    
}
