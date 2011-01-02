package com.google.refine.model.meta;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.commands.importing.ImportJob;

public class WebImportSource extends ImportSource {
    public String url;
    
    @Override
    protected void customWrite(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("url"); writer.value(url);
    }
    
    @Override
    protected void customReconstruct(JSONObject obj) throws JSONException {
        if (obj.has("url")) {
            url = obj.getString("url");
        }
    }
    
    @Override
    public void retrieveContent(HttpServletRequest request, Properties options, ImportJob job) throws Exception {
        // TODO Auto-generated method stub
        
    }
}