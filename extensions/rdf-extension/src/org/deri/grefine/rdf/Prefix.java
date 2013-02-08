package org.deri.grefine.rdf;

/**
 * @author fadmaa
 * represents a defined Namespace (a short name and full URI like foaf <http://xmlns.com/foaf/0.1/>)
 */
public class Prefix {
	
	private String name;
	private String uri;
	
	public String getName() {
		return name;
	}
	public String getUri() {
		return uri;
	}
	
	public Prefix(String name, String uri){
		this.name = name;
		this.uri = uri;
	}
}
