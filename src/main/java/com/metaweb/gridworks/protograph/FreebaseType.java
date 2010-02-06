package com.metaweb.gridworks.protograph;

public class FreebaseType extends FreebaseTopic {
	private static final long serialVersionUID = -3070300264980791404L;
	
	final public boolean compoundValueType;

	public FreebaseType(String id, String name, boolean compoundValueType) {
		super(id, name);
		this.compoundValueType = compoundValueType;
	}

}
