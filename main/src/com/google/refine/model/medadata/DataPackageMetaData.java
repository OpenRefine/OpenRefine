package com.google.refine.model.medadata;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.frictionlessdata.datapackage.Package;
import io.frictionlessdata.datapackage.exceptions.DataPackageException;


public class DataPackageMetaData extends AbstractMetadata {
    final static Logger logger = LoggerFactory.getLogger(DataPackageMetaData.class);
    
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
        try {
            Package dp = new Package(metadataFile.getAbsolutePath());
        } catch (ValidationException | DataPackageException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        logger.info("metadata file loaded");
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
