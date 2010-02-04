package com.metaweb.gridworks.protograph;

public class FreebaseProperty extends FreebaseTopic {
	private static final long serialVersionUID = 7909539492956342421L;
	
	final protected FreebaseType _expectedType;
	
	public FreebaseProperty(String id, String name, FreebaseType expectedType) {
		super(id, name);
		_expectedType = expectedType;
	}
	
	public FreebaseType getExpectedType() {
		return _expectedType;
	}
}
