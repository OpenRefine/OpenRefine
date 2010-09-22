package com.google.refine.operations;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.browsing.Engine;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

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
