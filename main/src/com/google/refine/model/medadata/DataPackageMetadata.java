package com.google.refine.model.medadata;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    final static Logger logger = LoggerFactory.getLogger(DataPackageMetadata.class);

    public static final String DEFAULT_FILE_NAME = "datapackage.json";

    private Package _pkg;
    
    public DataPackageMetadata() {
        setFormatName(MetadataFormat.DATAPACKAGE_METADATA);
        
        _pkg = PackageExtension.buildPackageFromTemplate();
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
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
        try {
            String jsonString = FileUtils.readFileToString(metadataFile);
            _pkg = new Package(jsonString);
        } catch (ValidationException | DataPackageException | IOException e) {
            logger.error("Load from file failed" + metadataFile.getAbsolutePath(),
                    ExceptionUtils.getStackTrace(e));
        }
        
        logger.info("Data Package metadata file loaded");
        
        return this;
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
    public void write(JSONWriter jsonWriter, boolean onlyIfDirty) {
        // TODO Auto-generated method stub

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
