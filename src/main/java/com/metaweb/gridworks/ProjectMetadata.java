package com.metaweb.gridworks;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class ProjectMetadata implements Serializable, Jsonizable {
    private static final long serialVersionUID = 7959027046468240844L;
    private static final int s_expressionHistoryMax = 20; // last n expressions used in this project
    
    private final Date     _created = new Date();
    private String         _name;
    private String         _password;
    private String         _encoding;
    private int            _encodingConfidence;
    private Date           _modified = new Date();
    private List<String>   _expressions = new LinkedList<String>();
    
    public Date getCreated() {
        return _created;
    }

    public void setName(String name) {
        this._name = name;
    }

    public String getName() {
        return _name;
    }

    public void setEncoding(String encoding) {
        this._encoding = encoding;
    }

    public String getEncoding() {
        return _encoding;
    }

    public void setEncodingConfidence(int confidence) {
        this._encodingConfidence = confidence;
    }
    
    public void setEncodingConfidence(String confidence) {
        this.setEncodingConfidence(Integer.parseInt(confidence));
    }

    public int getEncodingConfidence() {
        return _encodingConfidence;
    }
    
    public void setPassword(String password) {
        this._password = password;
    }

    public String getPassword() {
        return _password;
    }
    
    public Date getModified() {
        return _modified;
    }
    
    public void updateModified() {
        _modified = new Date();
    }
    
    public void addLatestExpression(String s) {
    	_expressions.remove(s);
    	_expressions.add(0, s);
    	while (_expressions.size() > s_expressionHistoryMax) {
    		_expressions.remove(_expressions.size() - 1);
    	}
    	
    	ProjectManager.singleton.addLatestExpression(s);
    }
    
    public List<String> getExpressions() {
    	return _expressions;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
        
        writer.object();
        writer.key("name"); writer.value(getName());
        writer.key("created"); writer.value(sdf.format(getCreated()));
        writer.key("modified"); writer.value(sdf.format(_modified));
        writer.endObject();
    }
}
