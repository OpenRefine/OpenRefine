package com.metaweb.gridworks.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class CellNode extends Node {
	private static final long serialVersionUID = 5820786756175547307L;

	final public int			cellIndex;
	final public boolean		createUnlessRecon;
	final public FreebaseType 	type;
	
	public CellNode(
		int 			cellIndex,
		boolean 		createUnlessRecon, 
		FreebaseType 	type
	) {
		this.cellIndex = cellIndex;
		this.createUnlessRecon = createUnlessRecon;
		this.type = type;
	}
	
	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub

	}

}
