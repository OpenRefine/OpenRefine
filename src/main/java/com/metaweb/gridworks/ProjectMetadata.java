package com.metaweb.gridworks;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.util.JSONUtilities;
import com.metaweb.gridworks.util.ParsingUtilities;

public class ProjectMetadata implements Jsonizable {
    private static final int s_expressionHistoryMax = 20; // last n expressions used in this project
    
    private final Date     _created;
    private Date           _modified;
    private String         _name;
    private String         _password;
    
    private String         _encoding;
    private int            _encodingConfidence;
    private List<String>   _expressions = new LinkedList<String>();
    
    static public ProjectMetadata loadFromJSON(JSONObject obj) {
        ProjectMetadata pm = new ProjectMetadata(JSONUtilities.getDate(obj, "modified", new Date()));
        
        pm._modified = JSONUtilities.getDate(obj, "modified", new Date());
        pm._name = JSONUtilities.getString(obj, "name", "<Error recovering project name>");
        pm._password = JSONUtilities.getString(obj, "password", "");
        
        pm._encoding = JSONUtilities.getString(obj, "encoding", "");
        pm._encodingConfidence = JSONUtilities.getInt(obj, "encodingConfidence", 0);
        
        JSONUtilities.getStringList(obj, "expressions", pm._expressions);
        
        return pm;
    }
    
    protected ProjectMetadata(Date date) {
        _created = date;
    }

    public ProjectMetadata() {
        _created = new Date();
        _modified = _created;
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("name"); writer.value(_name);
        writer.key("created"); writer.value(ParsingUtilities.dateToString(_created));
        writer.key("modified"); writer.value(ParsingUtilities.dateToString(_modified));
        
        if ("save".equals(options.getProperty("mode"))) {
            writer.key("password"); writer.value(_password);
            
            writer.key("encoding"); writer.value(_encoding);
            writer.key("encodingConfidence"); writer.value(_encodingConfidence);
            writer.key("expressions"); JSONUtilities.writeStringList(writer, _expressions);
        }
        writer.endObject();
    }
    
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
}
