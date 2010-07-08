package com.metaweb.gridworks.rdf.vocab;


public class RDFSClass extends RDFNode{

    @Override
    public String getType() {
        return "class";
    }

    public RDFSClass( String uRI,
            String label,String description,String prefix,String vocabularyUri) {
        super(description,uRI,label,prefix,vocabularyUri);
    }
    
}
