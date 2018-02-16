package com.google.refine.model.medadata;

import com.google.refine.operations.OperationRegistry;
import com.google.refine.preference.PreferenceStore;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Properties;

import org.apache.commons.beanutils.PropertyUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

public abstract class AbstractMetadata implements IMetadata {
    private MetadataFormat formatName = MetadataFormat.UNKNOWN;
    
    protected LocalDateTime written = null;
    protected LocalDateTime           _modified;
    
    @Override
    public MetadataFormat getFormatName() {
        return formatName;
    }

    
    @Override
    public void setFormatName(MetadataFormat formatName) {
        this.formatName = formatName;
    }

    @Override
    public abstract void loadFromJSON(JSONObject obj);

    @Override
    public abstract void loadFromFile(File metadataFile);

    @Override
    public abstract void writeToFile(OperationRegistry opRegistry, PreferenceStore prefStore, File metadataFile);

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
    
    /**
     * @param jsonWriter
     *            writer to save metadatea to
     * @param onlyIfDirty
     *            true to not write unchanged metadata
     * @throws JSONException
     */
    @Override
    public void write(OperationRegistry opRegistry, PreferenceStore prefStore, JSONWriter jsonWriter, boolean onlyIfDirty) throws JSONException  {
        if (!onlyIfDirty || isDirty()) {
            Properties options = new Properties();
            options.setProperty("mode", "save");

            write(opRegistry, prefStore, jsonWriter, options);
        }
    }
    
    protected static boolean propertyExists(Object bean, String property) {
        return PropertyUtils.isReadable(bean, property) && 
               PropertyUtils.isWriteable(bean, property); 
    }
    
}
