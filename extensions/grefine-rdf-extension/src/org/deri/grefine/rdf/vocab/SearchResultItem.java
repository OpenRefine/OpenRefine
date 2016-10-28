package org.deri.grefine.rdf.vocab;

import org.json.JSONException;
import org.json.JSONWriter;

public class SearchResultItem {
	private String label;
	private String id;
	private String description;
	private String prefix;
	private String localName;

	public SearchResultItem(String id, String prefix, String lname,
			String label, String description) {
		this.id = id;
		this.label = label;
		this.description = description;
		this.prefix = prefix;
		this.localName = lname;
	}

	public String getLabel() {
		return label;
	}

	public String getId() {
		return id;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getDescription() {
		return description;
	}

	public void writeAsSearchResult(JSONWriter writer) throws JSONException {
		writer.object();

		writer.key("id");
		writer.value(id);

		writer.key("name");
		writer.value(prefix + ":" + localName);

		writer.key("description");
		writer.value(id + "<br/><em>label: </em>" + label
				+ "<br/><em>description: </em>" + description);
		writer.endObject();
	}

}
