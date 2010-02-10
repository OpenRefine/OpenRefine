package com.metaweb.gridworks.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class CellTopicNode extends CellNode {
	private static final long serialVersionUID = 1684854896739592911L;
	
	final public boolean		createForNoReconMatch;
	final public FreebaseType 	type;

	public CellTopicNode(
		int 			cellIndex,
		boolean 		createForNoReconMatch, 
		FreebaseType 	type
	) {
		super(cellIndex);
		
		this.createForNoReconMatch = createForNoReconMatch;
		this.type = type;
	}
	
	public void write(JSONWriter writer, Properties options)
		throws JSONException {

		writer.object();
		writer.key("nodeType"); writer.value("cell-as-topic");
		writer.key("cellIndex"); writer.value(cellIndex);
		writer.key("type"); type.write(writer, options);
		writer.key("createUnlessRecon"); writer.value(createForNoReconMatch);
		writer.endObject();
	}
}
