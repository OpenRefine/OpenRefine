package com.metaweb.gridworks.rdf.vocab;


public class RDFSProperty extends RDFNode{

    @Override
    public String getType() {
        return "property";
    }
    public RDFSProperty(String uRI,
            String label,String description,String prefix,String vocabularyUri) {
        super(description,uRI,label,prefix,vocabularyUri);
    }
}
