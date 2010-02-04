package com.metaweb.gridworks.protograph;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class Link implements Serializable, Jsonizable {
	private static final long serialVersionUID = 2908086768260322876L;
	
	final protected FreebaseProperty 	_property;
	final protected Node				_target;
	
	public Link(FreebaseProperty property, Node target) {
		_property = property;
		_target = target;
	}
	
	public FreebaseProperty getProperty() {
		return _property;
	}
	
	public Node getTarget() {
		return _target;
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub
		
	}

}
