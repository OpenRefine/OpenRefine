package com.metaweb.gridworks.rdf.vocab;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class VocabularyImporter {

    private static final String PREFIXES = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
            "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> ";
    private static final String CLASSES_QUERY_P1 = PREFIXES + 
    "SELECT ?resource ?label ?en_label ?description ?en_description ?definition ?en_definition " +
    "WHERE { " +
        "?resource rdf:type rdfs:Class. " +
        "OPTIONAL {?resource rdfs:label ?label.} " +
        "OPTIONAL {?resource rdfs:label ?en_label. FILTER langMatches( lang(?en_label), \"EN\" )  } " +
        "OPTIONAL {?resource rdfs:comment ?description.} " +
        "OPTIONAL {?resource rdfs:comment ?en_description. FILTER langMatches( lang(?en_description), \"EN\" )  } " +
        "OPTIONAL {?resource skos:definition ?definition.} " +
        "OPTIONAL {?resource skos:definition ?en_definition. FILTER langMatches( lang(?en_definition), \"EN\" )  } " +
        "FILTER regex(str(?resource), \"^";
    private static final String CLASSES_QUERY_P2 = "\")}";
    
    private static final String PROPERTIES_QUERY_P1 = PREFIXES + 
    "SELECT ?resource ?label ?en_label ?description ?en_description ?definition ?en_definition " +
    "WHERE { " +
        "?resource rdf:type rdf:Property. " +
        "OPTIONAL {?resource rdfs:label ?label.} " +
        "OPTIONAL {?resource rdfs:label ?en_label. FILTER langMatches( lang(?en_label), \"EN\" )  } " +
        "OPTIONAL {?resource rdfs:comment ?description.} " +
        "OPTIONAL {?resource rdfs:comment ?en_description. FILTER langMatches( lang(?en_description), \"EN\" )  } " +
        "OPTIONAL {?resource skos:definition ?definition.} " +
        "OPTIONAL {?resource skos:definition ?en_definition. FILTER langMatches( lang(?en_definition), \"EN\" )  } " +
        "FILTER regex(str(?resource), \"^";
    private static final String PROPERTIES_QUERY_P2 = "\")}"; 
    
    public Vocabulary getVocabulary(String url, String prefix, String namespace, String format){
        Model m = getModel(url, format);
        return getVocabulary(m,namespace,prefix);
    }
    
    private Model getModel(String url,String format){
        Model model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF);//ModelFactory.createDefaultModel();
        model.read(url);
        return model;
    }
    

    private Vocabulary getVocabulary(Model m, String namespace, String prefix){
        Query query = QueryFactory.create(CLASSES_QUERY_P1 + namespace.trim() + CLASSES_QUERY_P2);
        QueryExecution qe = QueryExecutionFactory.create(query, m);
        ResultSet res = qe.execSelect();
        Set<String> seen = new HashSet<String>();
        Vocabulary vocab = new Vocabulary(prefix, namespace);
        while(res.hasNext()){
            QuerySolution qs = res.nextSolution();
            String uri = qs.getResource("resource").getURI();
            if(seen.contains(uri)){
                continue;
            }
            String label = getFirstNotNull(new Literal[]{qs.getLiteral("en_label"),qs.getLiteral("label")});
            
            String description = getFirstNotNull(new Literal[]{qs.getLiteral("en_definition"),qs.getLiteral("definition"),
                    qs.getLiteral("en_description"),qs.getLiteral("description")}) ;
            RDFSClass clazz = new RDFSClass(uri, label, description,prefix,namespace);
            vocab.addClass(clazz);
        }
        
        query = QueryFactory.create(PROPERTIES_QUERY_P1 + namespace.trim() + PROPERTIES_QUERY_P2);
        qe = QueryExecutionFactory.create(query, m);
        res = qe.execSelect();
        seen = new HashSet<String>();
        while(res.hasNext()){
            QuerySolution qs = res.nextSolution();
            String uri = qs.getResource("resource").getURI();
            if(seen.contains(uri)){
                continue;
            }
            String label = getFirstNotNull(new Literal[]{qs.getLiteral("en_label"),qs.getLiteral("label")});
            
            String description = getFirstNotNull(new Literal[]{qs.getLiteral("en_definition"),qs.getLiteral("definition"),
                    qs.getLiteral("en_description"),qs.getLiteral("description")}) ;
            RDFSProperty prop = new RDFSProperty(uri, label, description,prefix,namespace);
            vocab.addProperty(prop);
        }
        
        return vocab;
    }
    
    private String getFirstNotNull(Literal[] literals){
        String s = null;
        for(int i=0;i<literals.length;i++){
            s = getString(literals[i]);
            if(s!=null){
                break;
            }
        }
        return s;
    }
    
    private String getString(Literal l){
        if(l!=null){
            return l.getString();
        }
        return null;
    }
    
}
