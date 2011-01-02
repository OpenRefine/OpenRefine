package com.google.refine.model.meta;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.commands.importing.ImportJob;

public class TextImportSource extends ImportSource {
    @Override
    protected void customWrite(JSONWriter writer, Properties options)
            throws JSONException {
    }
    
    @Override
    protected void customReconstruct(JSONObject obj) throws JSONException {
    }
    
    @Override
    public void retrieveContent(HttpServletRequest request, Properties options, ImportJob job) throws Exception {
        // TODO Auto-generated method stub
        
    }
}