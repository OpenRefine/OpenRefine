package com.google.refine;


import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
/**
 * Interface to import/export metadata 
 *
 */
public interface IMetadata extends Jsonizable {
    public ProjectMetadata loadFromJSON(JSONObject obj);
    
    public ProjectMetadata loadFromFile(File metadataFile);
    
    public void writeToFile(File metadataFile);
    
    /**
     * @param jsonWriter writer to save metadatea to
     * @param onlyIfDirty true to not write unchanged metadata
     * @throws JSONException
     */
    public void write(JSONWriter jsonWriter, boolean onlyIfDirty);
    
}
