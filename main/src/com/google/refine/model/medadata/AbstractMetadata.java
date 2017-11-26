package com.google.refine.model.medadata;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMetadata implements IMetadata {
    private final static Logger logger = LoggerFactory.getLogger("AbstractMetadata");
    
    protected Date written = null;
    protected Date           _modified;
    protected String         _name = "";
    protected String         _encoding = "";
    protected int _rowCount;
 // user metadata
    protected JSONArray _userMetadata = new JSONArray();; 

    @Override
    public abstract void writeToJSON(JSONWriter writer, Properties options) throws JSONException;

    @Override
    public abstract IMetadata loadFromJSON(JSONObject obj);

    @Override
    public abstract IMetadata loadFromFile(File metadataFile);

    @Override
    public abstract void writeToFile(File metadataFile);

    @Override
    public abstract void write(JSONWriter jsonWriter, boolean onlyIfDirty);

    @Override
    public String getEncoding() {
        return _encoding;
    }

    @Override
    public void setName(String name) {
        this._name = name;
        updateModified();
    }
    
    @Override
    public String getName() {
        return _name;
    }

    public void setEncoding(String encoding) {
        this._encoding = encoding;
        updateModified();
    }
    
    @Override
    public boolean isDirty() {
        return written == null || _modified.after(written);
    }

    @Override
    public Date getModified() {
        return _modified;
    }
    
    @Override
    public void updateModified() {
        _modified = new Date();
    }

    @Override
    public void setRowCount(int rowCount) {
        this._rowCount = rowCount;
        updateModified();
    }

    @Override
    public JSONArray getUserMetadata() {
        return _userMetadata;
    }
    
    public void setUserMetadata(JSONArray userMetadata) {
        this._userMetadata = userMetadata;
    }
    
    static boolean propertyExists(Object bean, String property) {
        return PropertyUtils.isReadable(bean, property) && 
               PropertyUtils.isWriteable(bean, property); 
    }
    
    @Override
    public void setAnyStringField(String metaName, String valueString)  {
        if (propertyExists(this, metaName)) {
            try {
                BeanUtils.setProperty(this, metaName, valueString);
            } catch (IllegalAccessException | InvocationTargetException ite) {
                logger.error(ExceptionUtils.getStackTrace(ite));
            }
        } else {
            updateUserMetadata(metaName, valueString);
        }
    }
    
    private void updateUserMetadata(String metaName, String valueString)  {
        for (int i = 0; i < _userMetadata.length(); i++) {
            try {
                JSONObject obj = _userMetadata.getJSONObject(i);
                if (obj.getString("name").equals(metaName)) {
                    obj.put("value", valueString);
                }
            } catch (JSONException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }

}
