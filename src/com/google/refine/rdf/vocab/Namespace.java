package com.google.refine.rdf.vocab;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;

public class Namespace implements Jsonizable, Comparable<Namespace> {

	public String prefix;
	public String uri;

	public Namespace(String p, String u){
		this.prefix = p;
		this.uri = u;
	}

	@Override
	public int hashCode() {
		return prefix.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==null){
			return false;
		}
		if(obj.getClass().equals(this.getClass())){
			Namespace ns = (Namespace)obj;
			return this.prefix.equals(ns.prefix);
		}
		return false;
	}
	
	public void write(JSONWriter writer,Properties options)throws JSONException{
		writer.object();
		writer.key("prefix");writer.value(prefix);
		writer.key("uri");writer.value(uri);
		writer.endObject();
	}

	@Override
	public int compareTo(Namespace o) {
		return prefix.compareTo(o.prefix);
	}
}
