package com.metaweb.gridworks.rdf;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ConstantResourceNode extends ResourceNode{

	private String uri;


	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
	

	public ConstantResourceNode(String uri){
		this.uri = uri;
	}

	@Override
	public Resource createResource(URI baseUri, Model model, Project project,
			Row row, int rowIndex,Resource[] blanks) {
		if(this.uri!=null & this.uri.length()>0){
			String tmp;
			try {
				tmp = Util.getUri(baseUri, this.uri);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			Resource r =  model.createResource(tmp);
			return r;
		}else{
			return null;
		}
	}

	@Override
	protected void writeNode(JSONWriter writer, Properties options)
			throws JSONException {
		writer.key("nodeType"); writer.value("resource");
		writer.key("uri"); writer.value(uri);
	}
	
}
