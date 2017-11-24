package com.google.refine.model.medadata;

import java.io.File;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;


public class OKFMetaData extends AbstractMetadata {

    @Override
    public void writeToJSON(JSONWriter writer, Properties options)
            throws JSONException {
        // TODO Auto-generated method stub

    }

    @Override
    public IMetadata loadFromJSON(JSONObject obj) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMetadata loadFromFile(File metadataFile) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeToFile(File metadataFile) {
        // TODO Auto-generated method stub

    }

    @Override
    public void write(JSONWriter jsonWriter, boolean onlyIfDirty) {
        // TODO Auto-generated method stub

    }

}
