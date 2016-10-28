package org.deri.grefine.rdf.vocab;


public class RDFSClass extends RDFNode{

    @Override
    public String getType() {
        return "class";
    }

    public RDFSClass( String uri,
            String label,String description,String prefix,String vocabularyUri) {
        super(uri,label,description,prefix,vocabularyUri);
    }
    
    public RDFSClass(String uri){
    	super(uri,uri,"","","");
    }
    
}
