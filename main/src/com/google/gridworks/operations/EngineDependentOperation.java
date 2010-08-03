package com.google.gridworks.operations;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gridworks.browsing.Engine;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.util.ParsingUtilities;

abstract public class EngineDependentOperation extends AbstractOperation {
    final private String _engineConfigString;
    
    transient protected JSONObject    _engineConfig;
    
    protected EngineDependentOperation(JSONObject engineConfig) {
        _engineConfig = engineConfig;
        _engineConfigString = engineConfig == null || engineConfig.length() == 0
            ? null : engineConfig.toString();
    }
    
    protected Engine createEngine(Project project) throws Exception {
        Engine engine = new Engine(project);
        engine.initializeFromJSON(getEngineConfig());
        return engine;
    }
    
    protected JSONObject getEngineConfig() {
        if (_engineConfig == null && _engineConfigString != null) {
            try {
                _engineConfig = ParsingUtilities.evaluateJsonStringToObject(_engineConfigString);
            } catch (JSONException e) {
                // ignore
            }
        }
        return _engineConfig;
    }
}
