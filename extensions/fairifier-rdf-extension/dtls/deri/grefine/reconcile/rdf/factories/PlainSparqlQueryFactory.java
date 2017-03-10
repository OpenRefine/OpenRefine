package org.deri.grefine.reconcile.rdf.factories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deri.grefine.reconcile.model.ReconciliationCandidate;
import org.deri.grefine.reconcile.model.ReconciliationRequest;
import org.deri.grefine.reconcile.model.SearchResultItem;
import org.deri.grefine.reconcile.model.ReconciliationRequestContext.PropertyContext;
import org.deri.grefine.reconcile.rdf.factories.JenaTextSparqlQueryFactory.ScoredLabel;
import org.deri.grefine.reconcile.util.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

public class PlainSparqlQueryFactory extends AbstractSparqlQueryFactory{

	@Override
	public String getReconciliationSparqlQuery(ReconciliationRequest request, ImmutableList<String> searchPropertyUris) {
		String query = prepareQueryRE(request.getQueryString());
		String typesFilter = getTypesFilter(request.getTypes());
		String contextFilter = getContextFilter(request.getContext().getProperties());
		String fulltextFilter = getFulltextFilter(query, searchPropertyUris);
		String labelClause = getLabelClause(searchPropertyUris.size());
		
		return RECONCILE_QUERY_TEMPLATE.replace("[[LABEL_CLAUSE]]", labelClause)
										.replace("[[FULLTEXT_SEARCH_FILTER]]",fulltextFilter)
										.replace("[[TYPE_FILTER]]", typesFilter)
										.replace("[[CONTEXT_FILTER]]", contextFilter)
										.replace("[[LIMIT]]", String.valueOf(request.getLimit()));
	}

	@Override
	public List<ReconciliationCandidate> wrapReconciliationResultset(ResultSet resultSet, String queryString, ImmutableList<String> searchPropertyUris, int limit, double matchThreshold) {
		List<ReconciliationCandidate> candidates = new ArrayList<ReconciliationCandidate>();
		boolean match = false;
		boolean moreThanOneMatch = false;
		Set<String> seen = new HashSet<String>();
		while(resultSet.hasNext()){
			QuerySolution solution = resultSet.nextSolution();
			Resource entity = solution.getResource("entity");
			String entityUri = entity.getURI();
			if(seen.contains(entityUri)){
				//already seen
				continue;
			}
			seen.add(entityUri);
			ScoredLabel scoredLabel = getPreferredLabel(solution, queryString, searchPropertyUris);
			if(scoredLabel.score >= matchThreshold){
				if(match){
					moreThanOneMatch = true;
				}else{
					match = true;
				}
			}
			ReconciliationCandidate candidate = new ReconciliationCandidate(entity.getURI(), scoredLabel.label, new String[] {}, scoredLabel.score,match);
			
			candidates.add(candidate);
		}
		if(moreThanOneMatch){
			//set all candidates as match =false
			for(ReconciliationCandidate candidate:candidates){
				candidate.setMatch(false);
			}
		}
		//sort according to score
		Collections.sort(candidates, new Comparator<ReconciliationCandidate>() {

			@Override
			public int compare(ReconciliationCandidate o1,
					ReconciliationCandidate o2) {
				//descending order
				return Double.compare(o2.getScore(), o1.getScore());
			}
			});
		return candidates;
	}

	public String getExactMatchReconciliationSparqlQuery(ReconciliationRequest request, ImmutableList<String> searchPropertyUris){
		String query = prepareQuery(request.getQueryString());
		String typesFilter = getTypesFilter(request.getTypes());
		String contextFilter = getContextFilter(request.getContext().getProperties());
		String fulltextFilter = getExactMatchFulltextFilter(query, searchPropertyUris);
		String labelClause = "";
		
		return RECONCILE_QUERY_TEMPLATE.replace("[[LABEL_CLAUSE]]", labelClause)
										.replace("[[FULLTEXT_SEARCH_FILTER]]",fulltextFilter)
										.replace("[[TYPE_FILTER]]", typesFilter)
										.replace("[[CONTEXT_FILTER]]", contextFilter)
										.replace("[[LIMIT]]", String.valueOf(request.getLimit()));
	}
	
	@Override
	public String getTypeSuggestSparqlQuery(String prefix, int limit) {
		return SUGGEST_TYPE_QUERY_TEMPLATE.replace("[[QUERY]]", prepareQueryRE(prefix)).replace("[[LIMIT]]", String.valueOf(limit));
	}

	

	@Override
	public ImmutableList<SearchResultItem> wrapTypeSuggestResultSet(ResultSet resultSet, String prefix, int limit) {
		List<SearchResultItem> items = new ArrayList<SearchResultItem>();
		while(resultSet.hasNext()){
			QuerySolution solution = resultSet.nextSolution();
			String type = solution.getResource("type").getURI();
			String label = solution.getLiteral("label").getString();
			double score = StringUtils.getLevenshteinScore(label, prefix);
			items.add(new SearchResultItem(type, label, score));
		}
		Collections.sort(items, new Comparator<SearchResultItem>() {

			@Override
			public int compare(SearchResultItem o1, SearchResultItem o2) {
				//descending order
				return Double.compare(o2.getScore(), o1.getScore());
			}
		});
		
		return ImmutableList.copyOf(items);
	}

	public List<ReconciliationCandidate> wrapResultset(ResultSet resultSet,String queryString, double matchThreshold){
		List<ReconciliationCandidate> candidates = new ArrayList<ReconciliationCandidate>();
		while(resultSet.hasNext()){
			QuerySolution solution = resultSet.nextSolution();
			Resource entity = solution.getResource("entity");
			ReconciliationCandidate candidate = new ReconciliationCandidate(entity.getURI(), queryString, new String[] {}, 1.0d,true);
			
			candidates.add(candidate);
		}
		if(candidates.size() > 1 || matchThreshold > 1 ){
			//set all match =false
			for(ReconciliationCandidate candidate : candidates){
				candidate.setMatch(false);
			}
		}
		return candidates;
	}

	@Override
	public String getPropertySuggestSparqlQuery(String prefix, String typeUri, int limit) {
		return SUGGEST_PROPERTY_WITH_SPECIFIC_SUBJECT_TYPE_QUERY_TEMPLATE.replace("[[QUERY]]", prefix)
																		.replace("[[TYPE_URI]]", typeUri)
																		.replace("[[LIMIT]]", String.valueOf(limit));
	}
	
	@Override
	public String getPropertySuggestSparqlQuery(String prefix, int limit) {
		return SUGGEST_PROPERTY_QUERY_TEMPLATE.replace("[[QUERY]]", prefix)
												.replace("[[LIMIT]]", String.valueOf(limit));
	}
	
	@Override
	public String getEntitySearchSparqlQuery(String prefix,	ImmutableList<String> searchPropertyUris, int limit) {
		String labelClause = getLabelClause(searchPropertyUris.size());
		StringBuilder regexPatternClause = new StringBuilder();
		StringBuilder boundFilterClause = new StringBuilder();
		for(int i =1;i<=searchPropertyUris.size();i++){
			String propUri = searchPropertyUris.get(i-1);
			regexPatternClause.append(REGEX_SEARCH_PATTERN.replaceAll("\\[\\[INDEX\\]\\]", String.valueOf(i))
												.replace("[[QUERY]]", prefix)
												.replace("[[PROPERTY_URI]]", propUri));
			boundFilterClause.append(BOUND_LABEL_FILTER.replace("[[INDEX]]", String.valueOf(i)));
		}
		//remove the last additional " ||"
		boundFilterClause.setLength(boundFilterClause.length()-3);
		
		return SEARCH_ENTITY_QUERY_TEMPLATE.replace("[[LABEL_CLAUSE]]", labelClause)
											.replace("[[REGEX_SEARCH_PATTERN]]", regexPatternClause.toString())
											.replace("[[BOUND_LABEL_FILTER]]", boundFilterClause.toString())
											.replace("[[LIMIT]]", String.valueOf(limit));
									
	}

	
	@Override
	public void write(JSONWriter writer) throws JSONException {
		writer.object();
		writer.key("type"); writer.value("plain");
		writer.endObject();
	}

	/**
	 * @param solution
	 * @param queryString
	 * @param searchPropertyUris
	 * @return the best label... which is the one with the best score
	 */
	private ScoredLabel getPreferredLabel(QuerySolution solution, String queryString, ImmutableList<String> searchPropertyUris){
		double maxScore = -1d;
		String bestLabel = "";
		for(int i=1; i<=searchPropertyUris.size(); i++){
			Literal label = solution.getLiteral("label" + i);
			double score;
			if(label!=null){
				score = StringUtils.getLevenshteinScore(label.getString(), queryString);
				if(score>maxScore){
					maxScore = score;
					bestLabel = label.getString();
				}
			}
		}
		if(bestLabel.isEmpty()){
			//fail... should never get here as bound restrictions are added to the SPARQL query used
			throw new RuntimeException("could not find label in the resultset for " + queryString);
		}
		return new ScoredLabel(bestLabel, maxScore);
	}
	
	private String getTypesFilter(String[] types){
		if(types==null || types.length==0 || (types.length==1 && types[0].isEmpty())){
			return "";
		}
		String typesFilter = "";
		if(types.length>0){
			if(types.length==1){
				typesFilter = getSimpleTypeFilter(types[0]);
			}else{
				StringBuilder typesFilterBuilder = new StringBuilder();
				typesFilterBuilder.append("{");
				for(int i=0; i<types.length; i++){
					typesFilterBuilder.append(TYPE_FILTER.replace("[[TYPE_URI]]", types[i]));
				}
				//	remove the last additional UNION
				typesFilterBuilder.setLength(typesFilterBuilder.length()-6);
				typesFilterBuilder.append("}");
			
				typesFilter = typesFilterBuilder.toString();
			}
		}
		return typesFilter;
	}
	
	private String getFulltextFilter(String queryString, ImmutableList<String> searchPropertyUris){
		if(searchPropertyUris.size()==1){
			//simple optimization
			return SIMPLE_REGEX_SEARCH_PATTERN.replace("[[QUERY]]", queryString)
										.replace("[[PROPERTY_URI]]", searchPropertyUris.get(0));
		}
		
		StringBuilder regexPatternClause = new StringBuilder();
		StringBuilder boundFilterClause = new StringBuilder();
		for(int i =1;i<=searchPropertyUris.size();i++){
			String propUri = searchPropertyUris.get(i-1);
			regexPatternClause.append(REGEX_SEARCH_PATTERN.replaceAll("\\[\\[INDEX\\]\\]", String.valueOf(i))
												.replace("[[QUERY]]", queryString)
												.replace("[[PROPERTY_URI]]", propUri));
			boundFilterClause.append(BOUND_LABEL_FILTER.replace("[[INDEX]]", String.valueOf(i)));
		}
		//remove the last additional " ||"
		boundFilterClause.setLength(boundFilterClause.length()-3);
		
		return FULLTEXT_SEARCH_FILTER.replace("[[REGEX_SEARCH_PATTERN]]", regexPatternClause.toString())
													.replace("[[BOUND_LABEL_FILTER]]", boundFilterClause.toString());
	}
	
	private String getExactMatchFulltextFilter(String queryString, ImmutableList<String> searchPropertyUris){
		if(searchPropertyUris.size()==1){
			//simple optimization
			return SIMPLE_EXACT_MATCH_SEARCH_PATTERN.replace("[[QUERY]]", queryString)
										.replace("[[PROPERTY_URI]]", searchPropertyUris.get(0));
		}
		StringBuilder exactMatchPatternClause = new StringBuilder();
		for(int i =1;i<=searchPropertyUris.size();i++){
			String propUri = searchPropertyUris.get(i-1);
			exactMatchPatternClause.append(EXACT_MATCH_SEARCH_PATTERN.replace("[[QUERY]]", queryString)
												.replace("[[PROPERTY_URI]]", propUri)
												.replaceAll("\\[\\[INDEX\\]\\]", String.valueOf(i)));
		}
		//remove the last additional "UNION "
		exactMatchPatternClause.setLength(exactMatchPatternClause.length()-6);
		
		return EXACT_MATCH_FULLTEXT_SEARCH_FILTER.replace("[[EXACT_MATCH_SEARCH_PATTERN]]", exactMatchPatternClause.toString());
	}
	
	private String getLabelClause(int num){
		StringBuilder labelClause = new StringBuilder();
		for(int i=1;i<=num;i++){
			labelClause.append(LABEL).append(i);
		}
		return labelClause.toString();
	}
	
	private String getContextFilter(Set<PropertyContext> properties){
		StringBuilder contextFilter = new StringBuilder();
		for(PropertyContext prop:properties){
			contextFilter.append(
					PROPERTY_FILTER.replace("[[PROPERTY_URI]]", prop.getPid())
									.replace("[[VALUE]]", prop.getV().asSparqlValue())
					);
		}
		return contextFilter.toString();
	}
	
	private String getSimpleTypeFilter(String typeUri){
		return SIMPLE_TYPE_FILTER.replace("[[TYPE_URI]]", typeUri);
	}
	
	private String prepareQuery(String query){
		return query.replaceAll("'", "\\\\'");
		
	}
	
	private String prepareQueryRE(String query){
		return query.replaceAll("'", "\\\\'").replaceAll("\\?", "\\\\\\\\?").replaceAll("\\.", "\\\\\\\\.").replaceAll("\\(","\\\\\\\\(")
		.replaceAll("\\)","\\\\\\\\)");
		
//		return Pattern.quote(query.replaceAll("'", "\\\\'"));
	}
	
	private static final String RECONCILE_QUERY_TEMPLATE =
		"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
		"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
		"SELECT ?entity[[LABEL_CLAUSE]] " +
		"WHERE" +
		"{" +
		"[[FULLTEXT_SEARCH_FILTER]]" +
		"[[TYPE_FILTER]]" +
		"[[CONTEXT_FILTER]]" +
		"FILTER isIRI(?entity). }LIMIT [[LIMIT]]";
	private static final String LABEL = " ?label";
	private static final String REGEX_SEARCH_PATTERN = 
		"OPTIONAL{ " +
		"?entity <[[PROPERTY_URI]]> ?label[[INDEX]]. " +
		"FILTER regex(str(?label[[INDEX]]),'[[QUERY]]','i')" +
		"}";
	private static final String FULLTEXT_SEARCH_FILTER = 
		"{" +
		"[[REGEX_SEARCH_PATTERN]]" +
		"FILTER ([[BOUND_LABEL_FILTER]])" +
		"}" ;
	private static final String SIMPLE_REGEX_SEARCH_PATTERN = 
		"?entity <[[PROPERTY_URI]]> ?label1. " +
		"FILTER regex(str(?label1),'[[QUERY]]','i'). " ;
	private static final String SIMPLE_EXACT_MATCH_SEARCH_PATTERN = 
		"?entity <[[PROPERTY_URI]]> ?label. FILTER (str(?label) = '[[QUERY]]'). ";
	private static final String BOUND_LABEL_FILTER = " bound(?label[[INDEX]]) ||";
	private static final String TYPE_FILTER = "{?entity rdf:type <[[TYPE_URI]]>. } UNION ";
	private static final String SIMPLE_TYPE_FILTER = "?entity rdf:type <[[TYPE_URI]]>. ";
	
	private static final String EXACT_MATCH_FULLTEXT_SEARCH_FILTER =
		"{" +
		"[[EXACT_MATCH_SEARCH_PATTERN]]" +
		"}" ;
	private static final String EXACT_MATCH_SEARCH_PATTERN = 
		"{ " +
		"?entity <[[PROPERTY_URI]]> ?label[[INDEX]]. FILTER (str(?label[[INDEX]]) = '[[QUERY]]'). " +
		"}UNION ";
	private static final String PROPERTY_FILTER = "?entity <[[PROPERTY_URI]]> [[VALUE]]. ";
	
	private static final String SUGGEST_TYPE_QUERY_TEMPLATE =
			"SELECT DISTINCT ?type ?label " +
			"WHERE{" +
			"[] a ?type. " +
			"?type ?p ?label. " +
			"FILTER (?p=<http://www.w3.org/2000/01/rdf-schema#label> || ?p=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
			"FILTER regex(str(?label),'^[[QUERY]]','i')" +
			"} LIMIT [[LIMIT]]";
	
	private static final String SUGGEST_PROPERTY_WITH_SPECIFIC_SUBJECT_TYPE_QUERY_TEMPLATE =
		 	"SELECT DISTINCT ?p ?label " +
		 	"WHERE{" +
		 	"[] a <[[TYPE_URI]]>;" +
		 	"?p ?v." +
		 	"?p ?label_prop ?label. " +
		 	"FILTER (?label_prop=<http://www.w3.org/2000/01/rdf-schema#label> || ?label_prop=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
		 	"FILTER regex(str(?label),'^[[QUERY]]','i')" +
		 	"} LIMIT [[LIMIT]]";
	private static final String SUGGEST_PROPERTY_QUERY_TEMPLATE =
	 	"SELECT DISTINCT ?p ?label " +
	 	"WHERE{" +
	 	"[] ?p ?v." +
	 	"?p ?label_prop ?label. " +
	 	"FILTER (?label_prop=<http://www.w3.org/2000/01/rdf-schema#label> || ?label_prop=<http://www.w3.org/2004/02/skos/core#prefLabel>). " +
	 	"FILTER regex(str(?label),'^[[QUERY]]','i')" +
	 	"} LIMIT [[LIMIT]]";
	
	private static final String SEARCH_ENTITY_QUERY_TEMPLATE =
		"SELECT ?entity[[LABEL_CLAUSE]] " +
		"WHERE" +
		"{" +
		"[[REGEX_SEARCH_PATTERN]]" +
		"FILTER ([[BOUND_LABEL_FILTER]]). " +
		"FILTER isIRI(?entity). }LIMIT [[LIMIT]]";
}
