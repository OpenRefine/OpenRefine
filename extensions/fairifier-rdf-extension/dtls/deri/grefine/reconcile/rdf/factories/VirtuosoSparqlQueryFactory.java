package org.deri.grefine.reconcile.rdf.factories;

import org.deri.grefine.reconcile.model.ReconciliationRequest;
import org.deri.grefine.reconcile.model.ReconciliationRequestContext.PropertyContext;
import org.deri.grefine.reconcile.util.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.collect.ImmutableList;

public class VirtuosoSparqlQueryFactory extends AbstractSparqlQueryFactory{

	/* (non-Javadoc)
	 *  * 
	 * a similar implementation to Jena turns out to run very slowly in Virtuoso so we used a different SPARQL style
	 * 
	 * ?entity ?p ?label.
	 * ?label bif:contains 'query' OPTION(score ?score1).
	 * FILTER (?p IN(rdfs:label, skos:prefLabel) )
	 * 
	 * The slow version *NOT USED*
	 * OPTIONAL{ ?entity rdfs:label ?label1.
	 * 			 ?label1 bif:contains 'query' OPTION(score ?score1)
	 *         }
	 * OPTIONAL{ ?entity skos:prefLabel ?label2.
	 * 			 ?label2 bif:contains 'query' OPTION(score ?score2)
	 *         }        
	 */
	@Override
	public String getReconciliationSparqlQuery(ReconciliationRequest request, ImmutableList<String> searchPropertyUris){
		String propertiesList = StringUtils.join(searchPropertyUris, "> || ", "?p=<", "FILTER (", ">)");
		String typeFilter = "";
		String typesList = prepareSparqlList(request.getTypes());
		if(!typesList.isEmpty()){
			typeFilter = TYPE_FILTER.replace("[[TYPE_URIS_LIST]]",typesList);
		}
		
		int limit = getQueryLimit(request.getLimit());
		String formattedQuery = formatTextQueryString(request.getQueryString());
		
		//prepare context filter
		StringBuilder contextFilter = new StringBuilder();
		for(PropertyContext prop : request.getContext().getProperties()){
			contextFilter.append(PROPERTY_FILTER.replace("[[PROPERTY_URI]]", prop.getPid()).replace("[[VALUE]]", prop.getV().asSparqlValue()));
		}
		
		return RECONCILE_QUERY_TEMPLATE.replace("[[QUERY]]", formattedQuery)
										.replace("[[PROPERTY_URIS_FILTER]]", propertiesList)
										.replace("[[TYPE_FILTER]]", typeFilter)
										.replace("[[CONTEXT_FILTER]]", contextFilter.toString())
										.replace("[[LIMIT]]",String.valueOf(limit));
	}
	
	@Override
	public String getTypeSuggestSparqlQuery(String prefix, int limit) {
		//We couldn't use parameterised query because ARQ 2.8.7 does not support QuerySolutionMap with sparqlService i.e. can't be used with remote SPARQL endpoints :-( #shame
		String formattedQuery = formatTextQueryString(prefix) + "*";
		return SUGGEST_TYPE_QUERY_TEMPLATE.replace("[[QUERY]]", formattedQuery).replace("[[LIMIT]]",String.valueOf(limit));
	}

	@Override
	public String getPropertySuggestSparqlQuery(String prefix, String typeUri, int limit) {
		String formattedQuery = formatTextQueryString(prefix) + "*";
		return SUGGEST_PROPERTY_WITH_SPECIFIC_SUBJECT_TYPE_QUERY_TEMPLATE.replace("[[QUERY]]", formattedQuery)
																		.replace("[[TYPE_URI]]", typeUri)
																		.replace("[[LIMIT]]",String.valueOf(limit));
	}
	
	@Override
	public String getPropertySuggestSparqlQuery(String prefix, int limit) {
		String formattedQuery = formatTextQueryString(prefix) + "*";
		return SUGGEST_PROPERTY_QUERY_TEMPLATE.replace("[[QUERY]]", formattedQuery).replace("[[LIMIT]]",String.valueOf(limit));
	}
	
	@Override
	public String getEntitySearchSparqlQuery(String prefix, ImmutableList<String> searchPropertyUris, int limit) {
		String propertiesList = StringUtils.join(searchPropertyUris, "> || ", "?p=<", "FILTER (", ">)");
		String formattedQuery = formatTextQueryString(prefix);
		return SEARCH_ENTITY_QUERY_TEMPLATE.replace("[[QUERY]]", formattedQuery)
											.replace("[[PROPERTY_URIS_FILTER]]", propertiesList)
											.replace("[[LIMIT]]",String.valueOf(limit));
	}

	@Override
	public void write(JSONWriter writer) throws JSONException {
		writer.object();
		writer.key("type");	writer.value("virtuoso");
		writer.endObject();
	}
	
	protected int getQueryLimit(int limit){
		//if each result has an average of 2 labels, we need limit*2 to get limit **unique** resource
		return limit * AVERAGE_NUM_OF_LABELS;
	}
	
	/**
	 * @param query
	 * @return formatted string for text search the format is based on lucene basic query syntax and will add '+' before each word
	 * signaling that this term is mandatory. tokenization is based on spaces  
	 */
	protected String formatTextQueryString(String query){
		return "+" + query.trim().replaceAll("[\\s]+", " +").replaceAll("'", "\\\\\\\\'");
	}
	
	private String prepareSparqlList(String[] uris){
		StringBuilder sparqlList = new StringBuilder();
		for(int i=0;i<uris.length;i++){
			String uri = uris[i];
			if(!uri.isEmpty()){
				sparqlList.append("<").append(uri).append(">,");
			}
		}
		if(sparqlList.length()>0){
			sparqlList.setLength(sparqlList.length()-1);
		}
		
		return sparqlList.toString();
	}
	
	//TODO accept as a parameter
	private static final int AVERAGE_NUM_OF_LABELS =2;
	
	private static final String RECONCILE_QUERY_TEMPLATE =
		"SELECT DISTINCT ?entity ?label ?score1 " +
		"WHERE" +
		"{" +
		"?entity ?p ?label. " +
		"?label <bif:contains> \"'[[QUERY]]'\" OPTION(score ?score1). " +
		"[[PROPERTY_URIS_FILTER]]. " +
		"[[TYPE_FILTER]]" +
		"[[CONTEXT_FILTER]]" +
		"FILTER isIRI(?entity). } ORDER BY desc(?score1) LIMIT [[LIMIT]]";
	private static final String TYPE_FILTER = 
		"?entity a ?type. " +
		"FILTER (?type IN ([[TYPE_URIS_LIST]])). ";
	
	private static final String SUGGEST_TYPE_QUERY_TEMPLATE = 
		"SELECT DISTINCT ?type ?label ?score1 " +
		"WHERE " +
		"{" +
		"[] a ?type. " +
		"?type ?label_prop ?label. " +
		"FILTER (?label_prop=<http://www.w3.org/2000/01/rdf-schema#label> || ?label_prop=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
		"?label <bif:contains> \"'[[QUERY]]'\" OPTION(score ?score1). " +
		"} ORDER BY desc(?score1) LIMIT [[LIMIT]]";
	
	private static final String SUGGEST_PROPERTY_QUERY_TEMPLATE = 
		"SELECT DISTINCT ?p ?label ?score1 " +
		"WHERE " +
		"{" +
		"[] ?p ?v. " +
		"?p ?label_prop ?label. " +
		"FILTER (?label_prop=<http://www.w3.org/2000/01/rdf-schema#label> || ?label_prop=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
		"?label <bif:contains> \"'[[QUERY]]'\" OPTION(score ?score1). " +
		"} ORDER BY desc(?score1) LIMIT [[LIMIT]]";
	
	private static final String SUGGEST_PROPERTY_WITH_SPECIFIC_SUBJECT_TYPE_QUERY_TEMPLATE =
		"SELECT DISTINCT ?p ?label ?score1 " +
		"WHERE " +
		"{" +
		"[] a <[[TYPE_URI]]>; " +
		"?p ?v. " +
		"?p ?label_prop ?label. " +
		"FILTER (?label_prop=<http://www.w3.org/2000/01/rdf-schema#label> || ?label_prop=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
		"?label <bif:contains> \"'[[QUERY]]'\" OPTION(score ?score1). " +
		"} ORDER BY desc(?score1) LIMIT [[LIMIT]]";
	
	private static final String SEARCH_ENTITY_QUERY_TEMPLATE =
		"SELECT DISTINCT ?entity ?label " +
		"WHERE" +
		"{" +
		"?entity ?p ?label. " +
		"?label <bif:contains> \"'[[QUERY]]'\" OPTION(score ?score1). " +
		"[[PROPERTY_URIS_FILTER]]. " +
		"FILTER isIRI(?entity). } ORDER BY desc(?score1) LIMIT [[LIMIT]]";
	
	private static final String PROPERTY_FILTER = "?entity <[[PROPERTY_URI]]> [[VALUE]]. ";
}
