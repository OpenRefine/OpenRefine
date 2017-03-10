package org.deri.grefine.reconcile.rdf.factories;

import java.util.List;

import org.deri.grefine.reconcile.model.ReconciliationCandidate;
import org.deri.grefine.reconcile.model.ReconciliationRequest;
import org.deri.grefine.reconcile.model.SearchResultItem;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.ResultSet;

/**
 * @author fadmaa
 * this interface provides SPARQL queries needed for reconciliation. These queries cannot be fully compliant with the the standard SPARQL 1.1 as
 * full-text search is needed and it is not part of the standard. so different implementations are needed to handle this peculiarity.  
 */
public interface SparqlQueryFactory {

	/**
	 * @param request
	 * @param searchPropertyUris
	 */
	public String getReconciliationSparqlQuery(ReconciliationRequest request, ImmutableList<String> searchPropertyUris);
	
	/**
	 * convert ResultSet into GRefineReconciliationResponse. the conversion depends on knowing how the query was phrased which is mainly affected by the method
	 * {@link #buildSelectClause(ImmutableList)}.  
	 * @param resultSet
	 * @param searchPropertyUris <i>ordered</i> list of properties used for fulltext search and for picking resource labels i.e. display name
	 * @param limit number of result items to wrap as the resultset might contain more
	 * @param matchThreshold minimum score to consider a candidate as a match
	 * @return list of candidates <em>ordered according to the score descendingly</em>
	 */
	public List<ReconciliationCandidate> wrapReconciliationResultset(ResultSet resultSet, String queryString, ImmutableList<String> searchPropertyUris, int limit, double matchThreshold);
	
	
	/**
	 * @param prefix
	 * @param limit
	 * @return sparql query for type autocomplete i.e. given this prefix give me a query to retrieve relevant classes 
	 */
	public String getTypeSuggestSparqlQuery(String prefix, int limit);
	public ImmutableList<SearchResultItem> wrapTypeSuggestResultSet(ResultSet resultSet, String prefix, int limit); 
	
	public String getTypesOfEntitiesQuery(ImmutableList<String> entityUris);
	public Multimap<String, String> wrapTypesOfEntities(ResultSet resultSet);
	
	public String getResourcePropertiesMapSparqlQuery(String resourceId, int limit);
	public Multimap<String, String> wrapResourcePropertiesMapResultSet(ResultSet resultSet, String resourceId, int limit);
	public String getResourcePropertiesMapSparqlQuery(PreviewResourceCannedQuery cannedQuery, String resourceId);
	public Multimap<String, String> wrapResourcePropertiesMapResultSet(PreviewResourceCannedQuery cannedQuery, ResultSet resultset);
	
	public String getSampleInstancesSparqlQuery(String typeId, ImmutableList<String> searchPropertyUris, int limit);
	public ImmutableList<SearchResultItem> wrapSampleInstancesResultSet(ResultSet resultSet, String typeId, ImmutableList<String> searchPropertyUris, int limit); 
	
	public String getSampleValuesOfPropertySparqlQuery(String propertyUri, int limit);
	/**
	 * @param resultSet
	 * @param propertyUri
	 * @param limit
	 * @return List of array of strings. each array is of length 2... subject then object
	 */
	public ImmutableList<String[]> wrapSampleValuesOfPropertyResultSet(ResultSet resultSet, String propertyUri, int limit);
	
	public String getPropertySuggestSparqlQuery(String prefix, String typeUri, int limit);
	public String getPropertySuggestSparqlQuery(String prefix, int limit);
	public ImmutableList<SearchResultItem> wrapPropertySuggestResultSet(ResultSet resultSet, String prefix, int limit);
	
	public String getEntitySearchSparqlQuery(String prefix ,ImmutableList<String> searchPropertyUris, int limit);
	public ImmutableList<SearchResultItem> wrapEntitySearchResultSet(ResultSet resultSet, int limit);
	
	public void write(JSONWriter writer)throws JSONException;
}
