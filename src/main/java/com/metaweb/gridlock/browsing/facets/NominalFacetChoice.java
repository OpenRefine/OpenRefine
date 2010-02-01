package com.metaweb.gridlock.browsing.facets;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridlock.Jsonizable;
import com.metaweb.gridlock.browsing.DecoratedValue;

public class NominalFacetChoice implements Jsonizable {
	final public DecoratedValue	decoratedValue;
	public int					count;
	public boolean				selected;
	
	public NominalFacetChoice(DecoratedValue decoratedValue) {
		this.decoratedValue = decoratedValue;
	}
	
	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();
		writer.key("v"); decoratedValue.write(writer, options);
		writer.key("c"); writer.value(count);
		writer.key("s"); writer.value(selected);
		writer.endObject();
	}
}
