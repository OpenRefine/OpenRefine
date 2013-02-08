package org.deri.grefine.rdf.vocab;

import org.json.JSONException;
import org.json.JSONWriter;

public class Vocabulary {
	private String name;
	private String uri;

    public Vocabulary(String name, String uri){
    	this.name = name;
    	this.uri = uri;
    }
    

	public String getName() {
		return name;
	}
	public String getUri() {
		return uri;
	}
	
    public void write(JSONWriter writer)throws JSONException {
        writer.object();
        
        writer.key("name"); writer.value(name);
        writer.key("uri"); writer.value(uri);
        
        writer.endObject();
    }


	@Override
	public int hashCode() {
		return name.hashCode();
	}


	@Override
	public boolean equals(Object obj) {
		if(obj==null){
			return false;
		}
		if(obj.getClass().equals(this.getClass())){
			Vocabulary v2 = (Vocabulary) obj;
			return name.equals(v2.getName());
		}
		return false;
	}
    
    

}
