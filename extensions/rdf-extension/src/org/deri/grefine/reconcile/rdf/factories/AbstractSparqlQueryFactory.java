package org.deri.grefine.reconcile.rdf.factories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deri.grefine.reconcile.model.ReconciliationCandidate;
import org.deri.grefine.reconcile.model.SearchResultItem;
import org.deri.grefine.reconcile.util.ResultSetWrappingUtil;
import org.deri.grefine.reconcile.util.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;

/**
 * @author fadmaa
 * provides a default implementation of {@link org.deri.grefine.reconcile.rdf.factories.SparqlQueryFactory}
 * this implementation takes care of the well-defined parts of the queries a.k.a standardised while leave the rest to actual implementation through 
 * abstract methods.
 *   
 */
public abstract class AbstractSparqlQueryFactory implements SparqlQueryFactory{

	@Override
	public String getSampleInstancesSparqlQuery(String typeUri, ImmutableList<String> searchPropertyUris, int limit){
		String labelClause = getLabelClause(searchPropertyUris.size());
		StringBuilder labelPropertyClause = new StringBuilder();
		int i=1;
		for(String propUri:searchPropertyUris){
			labelPropertyClause.append(LABEL_PROPERTY_FILTER.replace("[[PROPERTY_URI]]", propUri).replace("[[INDEX]]", String.valueOf(i)));
			i++;
		}
		return SAMPLE_INSTANCES_QUERY.replace("[[TYPE_URI]]", typeUri)
									.replace("[[LABEL_CLAUSE]]",labelClause)
									.replace("[[LABEL_PROPERTY_CLAUSE]]", labelPropertyClause)
									.replace("[[LIMIT]]", String.valueOf(limit));
	}
	
	@Override
	public String getSampleValuesOfPropertySparqlQuery(String propertyUri, int limit){
		return SAMPLE_PROPERTY_INSTANCES_QUERY.replace("[[PROPERTY_URI]]", propertyUri).replace("[[LIMIT]]", String.valueOf(limit));
	}
	
	@Override
	public String getResourcePropertiesMapSparqlQuery(String resourceId, int limit) {
		return RESOURCE_PROPERTIES_QUERY.replace("[[RESOURCE]]", resourceId).replace("[[LIMIT]]", String.valueOf(limit));		
	}
	
	@Override
	public ImmutableList<SearchResultItem> wrapTypeSuggestResultSet(ResultSet resultSet, String prefix, int limit) {
		return ResultSetWrappingUtil.resultSetToSearchResultListFilterDuplicates(resultSet, limit);
	}
	
	@Override
	public ImmutableList<SearchResultItem> wrapPropertySuggestResultSet(ResultSet resultSet, String prefix, int limit) {
		return ResultSetWrappingUtil.resultSetToSearchResultListFilterDuplicates(resultSet, limit);
	}

	@Override
	public ImmutableList<SearchResultItem> wrapEntitySearchResultSet(ResultSet resultSet, int limit) {
		return ResultSetWrappingUtil.resultSetToSearchResultListFilterDuplicates(resultSet, limit);
	}
	
	@Override
	public ImmutableList<SearchResultItem> wrapSampleInstancesResultSet(ResultSet resultSet, String typeId,ImmutableList<String> searchPropertyUris, int limit) {
		List<SearchResultItem> results = new ArrayList<SearchResultItem>();
		while(resultSet.hasNext()){
			QuerySolution sol = resultSet.next();
			String id = sol.getResource("entity").getURI();
			String name = getFirstNonNullLabel(sol,searchPropertyUris);
			double score = 0;
			results.add(new SearchResultItem(id, name, score));
		}
		return ImmutableList.copyOf(results);
	}

	private String getFirstNonNullLabel(QuerySolution sol, ImmutableList<String> searchPropertyUris) {
		for(int i=1;i<=searchPropertyUris.size();i++){
			Literal l = sol.getLiteral("label" + i);
			if(l!=null){
				return l.getString();
			}
		}
		return "";
	}

	@Override
	public ImmutableList<String[]> wrapSampleValuesOfPropertyResultSet(ResultSet resultSet, String propertyUri, int limit) {
		return ResultSetWrappingUtil.resultSetToListOfPairs(resultSet);
	}

	@Override
	public Multimap<String, String> wrapResourcePropertiesMapResultSet(ResultSet resultSet, String resourceId, int limit) {
		return ResultSetWrappingUtil.resultSetToMultimap(resultSet);
	}

	@Override
	public String getTypesOfEntitiesQuery(ImmutableList<String> entityUris) {
		String entityEqualityFilter = StringUtils.join(entityUris, "> || ", "?entity=<", "", ">");
		return TYPES_OF_ENTITIES_QUERY.replace("[[ENTITY_EQUALITY_FILTER]]", entityEqualityFilter);
	}

	@Override
	public Multimap<String, String> wrapTypesOfEntities(ResultSet resultSet) {
		return ResultSetWrappingUtil.resultSetToMultimap(resultSet);
	}

	
	@Override
	public String getResourcePropertiesMapSparqlQuery(PreviewResourceCannedQuery cannedQuery, String resourceId) {
		return cannedQuery.getPreviewQueryForResource(resourceId);
	}

	@Override
	public Multimap<String, String> wrapResourcePropertiesMapResultSet(PreviewResourceCannedQuery cannedQuery, ResultSet resultset) {
		return cannedQuery.wrapResourcePropertiesMapResultSet(resultset);
	}

	/**
	 * put the ResultSet returned from SPARQL endpoint into the {@link org.deri.grefine.reconciliation.model.GRefineReconciliationResponse GRefineReconciliationResponse} <br/>
	 * @param result
	 * @return
	 */
	@Override
	public List<ReconciliationCandidate> wrapReconciliationResultset(ResultSet result, String queryString, ImmutableList<String> searchPropertyUris, int limit, double matchThreshold){
		List<ReconciliationCandidate> candidates = new ArrayList<ReconciliationCandidate>();
		Set<String> seen = new HashSet<String>();
		boolean match = false;
		boolean moreThanOneMatchFound = false;
		double maxScore = 0.0; boolean first = true;
		while(result.hasNext()){
			QuerySolution solution = result.nextSolution();
			String entityUri = solution.getResource("entity").getURI();
			if(seen.contains(entityUri)){
				//already seen
				continue;
			}
			seen.add(entityUri);
			String label = solution.getLiteral("label").getString();
			//score returned by Lucene is only meaningful to compare results of the *same* query
			//they cannot be used as percentage see: http://wiki.apache.org/lucene-java/ScoresAsPercentages
			//they are used to weight the edit distance
			Literal scoreWieghtLiteral = solution.getLiteral("score1");
			if(scoreWieghtLiteral!=null && first){
				first = false;
				maxScore = scoreWieghtLiteral.getDouble();
			}
			double scoreWeight = scoreWieghtLiteral == null? 1 : scoreWieghtLiteral.getDouble()/maxScore;
			double score = scoreWeight * StringUtils.getLevenshteinScore(label, queryString);
			if(score>=matchThreshold){
				if(match){
					moreThanOneMatchFound = true;
				}else{
					match = true;
				}
			}
			candidates.add(new ReconciliationCandidate(entityUri, label, new String[] {}, score, match));
			
			if(candidates.size()==limit){
				//we got enough
				break;
			}
		}
		
		if(moreThanOneMatchFound){
			//set all matches to false
			for(ReconciliationCandidate candidate:candidates){
				candidate.setMatch(false);
			}
		}
		Collections.sort(candidates,new Comparator<ReconciliationCandidate>() {

			@Override
			public int compare(ReconciliationCandidate o1,ReconciliationCandidate o2) {
				//discendingly
				return Double.compare(o2.getScore(), o1.getScore());
			}
			
		});
		
		return candidates;
	}

	private String getLabelClause(int num){
		StringBuilder labelClause = new StringBuilder();
		for(int i=1;i<=num;i++){
			labelClause.append(LABEL).append(i);
		}
		return labelClause.toString();
	}
	
	private static final String SAMPLE_INSTANCES_QUERY =
														"SELECT DISTINCT ?entity [[LABEL_CLAUSE]] " +
														 "WHERE{" +
														 "?entity a <[[TYPE_URI]]>. " +
														 "[[LABEL_PROPERTY_CLAUSE]]" +
														 "}LIMIT [[LIMIT]]";
	
	private static final String SAMPLE_PROPERTY_INSTANCES_QUERY =
														"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
														"SELECT DISTINCT ?s ?o " +
														"WHERE{" +
														"?s <[[PROPERTY_URI]]> ?o. " +
														"}LIMIT [[LIMIT]]";
	
	private static final String RESOURCE_PROPERTIES_QUERY = 
														"SELECT DISTINCT ?p ?v " +
	 													 "WHERE{ " +
	 													 "<[[RESOURCE]]> ?p ?v. " +
	 													 "}LIMIT [[LIMIT]]";
	
	private static final String TYPES_OF_ENTITIES_QUERY = 	"SELECT ?entity ?type " +
															"WHERE{ " +
															"?entity a ?type. " +
															"FILTER ([[ENTITY_EQUALITY_FILTER]]). " +
															"}";
	private static final String LABEL = " ?label";
	private static final String LABEL_PROPERTY_FILTER = "OPTIONAL {?entity <[[PROPERTY_URI]]> ?label[[INDEX]]} ";
}
