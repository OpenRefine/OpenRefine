package org.deri.grefine.rdf.vocab;


public class RDFSProperty extends RDFNode{

    public RDFSProperty(String uri, String label, String description,
			String prefix, String namespace) {
		super(uri,label,description,prefix,namespace);
	}

    public RDFSProperty(String uri){
    	super(uri,uri,"","","");
    }
	@Override
    public String getType() {
        return "property";
    }
}
