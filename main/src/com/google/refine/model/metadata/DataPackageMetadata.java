package com.google.refine.model.metadata;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.frictionlessdata.datapackage.Package;
import io.frictionlessdata.datapackage.Resource;
import io.frictionlessdata.datapackage.exceptions.DataPackageException;


public class DataPackageMetadata extends AbstractMetadata {
    private final static Logger logger = LoggerFactory.getLogger(DataPackageMetadata.class);

    public static final String DEFAULT_FILE_NAME = "datapackage.json";

    private Package _pkg;
    
    public DataPackageMetadata() {
        setFormatName(MetadataFormat.DATAPACKAGE_METADATA);
        
        _pkg = PackageExtension.buildPackageFromTemplate();
    }
    
    @Override
    public void loadFromJSON(JSONObject obj) {
        try {
            _pkg = new Package(obj);
        } catch (ValidationException | DataPackageException | IOException e) {
            logger.error("Load from JSONObject failed" + obj.toString(4),
                    ExceptionUtils.getStackTrace(e));
        }
        
        logger.info("Data Package metadata loaded");
    }

    @Override
    public void loadFromFile(File metadataFile) {
            String jsonString = null;
            try {
                jsonString = FileUtils.readFileToString(metadataFile);
            } catch (IOException e) {
                logger.error("Load data package failed when reading from file: " + metadataFile.getAbsolutePath(),
                        ExceptionUtils.getStackTrace(e));
            }
            
            loadFromJSON(new JSONObject(jsonString));
    }
    
    /**
     * Write the package to a json file.
     */
    @Override
    public void writeToFile(File metadataFile) {
        try {
            this._pkg.save(metadataFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("IO exception when writing to file " + metadataFile.getAbsolutePath(), 
                    ExceptionUtils.getStackTrace(e));
        } catch (DataPackageException e) {
            logger.error("Data package exception when writing to file " + metadataFile.getAbsolutePath(),
                    ExceptionUtils.getStackTrace(e));
        }
    }
    
    @Override
    public void write(JSONWriter jsonWriter, Properties options)
            throws JSONException {
        StringWriter sw = new StringWriter();
        _pkg.getJson().write(sw);
        jsonWriter = new JSONWriter(sw);
    }
    
    @Override
    public void loadFromStream(InputStream inputStream) {
        try {
            this._pkg = new Package(IOUtils.toString(inputStream));
        } catch (ValidationException e) {
            logger.error("validation failed", ExceptionUtils.getStackTrace(e));
        } catch (DataPackageException e) {
            logger.error("Data package excpetion when loading from stream", ExceptionUtils.getStackTrace(e));
        } catch (IOException e) {
            logger.error("IO exception when loading from stream", ExceptionUtils.getStackTrace(e));
        }
    }
    
    public List<String> getResourcePaths() {
        List<String> listResources = new ArrayList<String>();
        
        for (Resource resource : _pkg.getResources()) {
            listResources.add((String) resource.getPath());
        }
        
        return listResources;
    }

    @Override
    public JSONObject getJSON() {
        return _pkg.getJson();
    }
    
    public Package getPackage() {
        return _pkg;
    }

    @Override
    public List<Exception> validate() {
        try {
            _pkg.validate();
        } catch (ValidationException | IOException | DataPackageException e) {
            logger.error("validate json failed", ExceptionUtils.getStackTrace(e));
        }
        
        return _pkg.getErrors();
    }
}
