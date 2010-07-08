package com.metaweb.gridworks.rdf.vocab;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public abstract class RDFNode implements Jsonizable{
    private String preferredCURIE;
    private String description;
    private String URI;
    private String label;
    private String vocabularyPrefix;
    private String vocabularyUri;
    
    public String getVocabularyUri() {
        return vocabularyUri;
    }
    public void setVocabularyUri(String vocabularyUri) {
        this.vocabularyUri = vocabularyUri;
    }
    public String getVocabularyPrefix() {
        return vocabularyPrefix;
    }
    public void setVocabularyPrefix(String vocabularyPrefix) {
        this.vocabularyPrefix = vocabularyPrefix;
    }
    public String getPreferredCURIE() {
        return preferredCURIE;
    }
    public void setPreferredCURIE(String preferredCURIE) {
        this.preferredCURIE = preferredCURIE;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getURI() {
        return URI;
    }
    public void setURI(String uRI) {
        URI = uRI;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public RDFNode(){
        
    }
    public RDFNode(String description, String uRI,
            String label,String prefix,String vocabularyUri) {
        this.description = description;
        URI = uRI;
        this.label = label;
        this.vocabularyPrefix = prefix;
        this.preferredCURIE = composePreferredCurie();
        this.vocabularyUri = vocabularyUri;
    }
    private String composePreferredCurie(){
        String qname;
        if(this.URI==null){
            return null;
        }
        if(this.URI.indexOf("#")!=-1){
            qname = this.URI.substring(this.URI.indexOf("#")+1);
        }else{
            qname = this.URI.substring(this.URI.lastIndexOf("/")+1);
        }
        return this.vocabularyPrefix + ":" + qname;
    }
    
    
    public abstract String getType();
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        
        writer.key("type");
        writer.value(this.getType());
        writer.key("prefix");
        writer.value(vocabularyPrefix);
        writer.key("preferredCURIE");
        writer.value(this.preferredCURIE);
        writer.key("label");
        writer.value(label);
        writer.key("description");
        writer.value(description);
        writer.key("URI");
        writer.value(URI);
        writer.endObject();
    }
    
    public void writeAsSearchResult(JSONWriter writer)throws JSONException {
        writer.object();
        
        writer.key("id");
        writer.value(URI);
        
        writer.key("name");
        writer.value(preferredCURIE);
        
        writer.key("description");
        writer.value(description);
        writer.endObject();
    }
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof RDFNode)) return false;
        RDFNode n = (RDFNode) obj;
        if(n.getURI()==null || this.URI==null){
            return false;
        }
        return this.URI.equals(n.getURI());
    }
    @Override
    public int hashCode() {
        return this.URI.hashCode();
    }
    
    

    
}
