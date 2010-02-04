package com.metaweb.gridworks.model.operations;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.ParsingUtilities;

abstract public class EngineDependentOperation implements AbstractOperation {
	final private String _engineConfigString;
	
	transient protected JSONObject	_engineConfig;
	
	protected EngineDependentOperation(JSONObject engineConfig) {
		_engineConfig = engineConfig;
		_engineConfigString = engineConfig.toString();
	}
	
	protected Engine createEngine(Project project) throws Exception {
		Engine engine = new Engine(project);
		engine.initializeFromJSON(getEngineConfig());
		return engine;
	}
	
	protected JSONObject getEngineConfig() {
		if (_engineConfig == null) {
			try {
				_engineConfig = ParsingUtilities.evaluateJsonStringToObject(_engineConfigString);
			} catch (JSONException e) {
				// ignore
			}
		}
		return _engineConfig;
	}
}
