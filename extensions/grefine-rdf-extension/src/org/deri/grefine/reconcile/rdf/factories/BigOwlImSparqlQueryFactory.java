package org.deri.grefine.reconcile.rdf.factories;

import org.deri.grefine.reconcile.model.ReconciliationRequest;
import org.deri.grefine.reconcile.model.ReconciliationRequestContext.PropertyContext;
import org.deri.grefine.reconcile.util.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.collect.ImmutableList;

public class BigOwlImSparqlQueryFactory extends AbstractSparqlQueryFactory{

	@Override
	public String getReconciliationSparqlQuery(ReconciliationRequest request, ImmutableList<String> searchPropertyUris) {
		String typesFilter = "";
		if(request.getTypes().length>0){
			typesFilter = StringUtils.join(request.getTypes(), ">. } UNION ", "{?entity rdf:type <", " {", ">. }}");
		}
		//prepare context filter
		StringBuilder contextFilter = new StringBuilder();
		for(PropertyContext prop : request.getContext().getProperties()){
			contextFilter.append(PROPERTY_FILTER.replace("[[PROPERTY_URI]]", prop.getPid()).replace("[[VALUE]]", prop.getV().asSparqlValue()));
		}
		if(searchPropertyUris.size()==1){
			String labelPropertyUri = searchPropertyUris.get(0);
			return RECONCILE_SIMPLE_QUERY_TEMPLATE.replace("[[LABEL_PROPERTY_URI]]", labelPropertyUri)
								.replace("[[SEARCH_PROPERTY_URI]]", DEFAULT_BIGOWLIM_INDEX_PROPERTY)
								.replace("[[LIMIT]]", String.valueOf(request.getLimit()))
								.replace("[[TYPE_FILTER]]",typesFilter)
								.replace("[[CONTEXT_FILTER]]", contextFilter.toString())
								.replace("[[QUERY]]", request.getQueryString());
		}else{
			String labelPropertiesFilter = StringUtils.join(searchPropertyUris, "> || ", "?p=<", "FILTER (", ">)");
			return RECONCILE_QUERY_TEMPLATE.replace("[[LABEL_PROPERTIES_FILTER]]", labelPropertiesFilter)
								.replace("[[SEARCH_PROPERTY_URI]]", DEFAULT_BIGOWLIM_INDEX_PROPERTY)
								.replace("[[LIMIT]]", String.valueOf(request.getLimit()*searchPropertyUris.size()))
								.replace("[[TYPE_FILTER]]",typesFilter)
								.replace("[[CONTEXT_FILTER]]", contextFilter.toString())
								.replace("[[QUERY]]", request.getQueryString());
		}
	}

	@Override
	public String getTypeSuggestSparqlQuery(String prefix, int limit) {
		return SUGGEST_TYPE_QUERY_TEMPLATE.replace("[[QUERY]]", prefix).replace("[[LIMIT]]", String.valueOf(limit));
	}

	@Override
	public String getPropertySuggestSparqlQuery(String prefix, String typeUri,int limit) {
		return SUGGEST_PROPERTY_WITH_SUBJECT_TYPE_QUERY_TEMPLATE.replace("[[LIMIT]]", String.valueOf(limit))
							.replace("[[TYPE_URI]]",typeUri)
							.replace("[[QUERY]]",prefix);
	}

	@Override
	public String getPropertySuggestSparqlQuery(String prefix, int limit) {
		return SUGGEST_PROPERTY_QUERY_TEMPLATE.replace("[[LIMIT]]", String.valueOf(limit))
						.replace("[[QUERY]]",prefix);
	}

	@Override
	public String getEntitySearchSparqlQuery(String prefix,
			ImmutableList<String> searchPropertyUris, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(JSONWriter writer) throws JSONException {
		writer.object();
		writer.key("type");	writer.value("bigowlim");
		writer.endObject();
	}

	private static final String DEFAULT_BIGOWLIM_INDEX_PROPERTY = "http://www.ontotext.com/luceneQuery";
	private static final String RECONCILE_SIMPLE_QUERY_TEMPLATE = 
		"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "+
		"SELECT ?entity ?label " +
		"WHERE" +
		"{" +
			"?entity <[[LABEL_PROPERTY_URI]]> ?label. " +
			"?entity <[[SEARCH_PROPERTY_URI]]> \"[[QUERY]]\"." +
			"[[TYPE_FILTER]]" +
			"[[CONTEXT_FILTER]]" +
		    " FILTER (isIRI(?entity))" +
		"}" +
		"LIMIT [[LIMIT]]";
	private static final String RECONCILE_QUERY_TEMPLATE = 
		"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "+
		"SELECT ?entity ?label " +
		"WHERE" +
		"{" +
			"?entity ?p ?label. " +
			"?entity <[[SEARCH_PROPERTY_URI]]> \"[[QUERY]]\". " +
			"[[LABEL_PROPERTIES_FILTER]]" +
			"[[TYPE_FILTER]]" +
			"[[CONTEXT_FILTER]]" +
		    " FILTER (isIRI(?entity))" +
		"}" +
		"LIMIT [[LIMIT]]";
	private static final String PROPERTY_FILTER = "?entity <[[PROPERTY_URI]]> [[VALUE]]. ";
	private static final String SUGGEST_TYPE_QUERY_TEMPLATE =
		"SELECT DISTINCT ?type ?label1 " +
		"WHERE{" +
		"[] a ?type. " +
		"?type <http://www.w3.org/2000/01/rdf-schema#label> ?label1. " +
		"?type <http://www.ontotext.com/luceneQuery> \"[[QUERY]]*\". " + 
		"} LIMIT [[LIMIT]]";
	private static final String SUGGEST_PROPERTY_QUERY_TEMPLATE =
		"SELECT DISTINCT ?p ?label1 " +
		"WHERE{" +
		"[] ?p ?o. " +
		"?p <http://www.w3.org/2000/01/rdf-schema#label> ?label1. " +
		"?p <http://www.ontotext.com/luceneQuery> \"[[QUERY]]*\". " + 
		"} LIMIT [[LIMIT]]";
	private static final String SUGGEST_PROPERTY_WITH_SUBJECT_TYPE_QUERY_TEMPLATE =
		"SELECT DISTINCT ?p ?label1 " +
		"WHERE{" +
		"[] a <[[TYPE_URI]]>;" +
		"?p ?o. " +
		"?p <http://www.w3.org/2000/01/rdf-schema#label> ?label1. " +
		"?p <http://www.ontotext.com/luceneQuery> \"[[QUERY]]*\". " + 
		"} LIMIT [[LIMIT]]";
}
