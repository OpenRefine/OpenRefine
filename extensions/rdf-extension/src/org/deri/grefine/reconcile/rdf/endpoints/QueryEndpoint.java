package org.deri.grefine.reconcile.rdf.endpoints;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.deri.grefine.reconcile.model.ReconciliationCandidate;
import org.deri.grefine.reconcile.model.ReconciliationRequest;
import org.deri.grefine.reconcile.model.SearchResultItem;
import org.deri.grefine.reconcile.rdf.factories.PreviewResourceCannedQuery;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public interface QueryEndpoint {

	public List<ReconciliationCandidate> reconcileEntities(ReconciliationRequest request, ImmutableList<String> searchPropertyUris, double matchThreshold);
	
	public ImmutableList<SearchResultItem> suggestType(String prefix, int limit);
	
	public ImmutableList<SearchResultItem> suggestProperty(String prefix, String typeUri, int limit);
	public ImmutableList<SearchResultItem> suggestProperty(String prefix, int limit);
	
	/**
	 * @param typeUri
	 * @param limit
	 * @return list of "limit"  SearchResultItem. id of each SearchResultItem will be the instance URI, while name will be a label for it 
	 */
	public ImmutableList<SearchResultItem> getSampleInstances(String typeUri, ImmutableList<String> searchPropertyUris, int limit);
	
	/**
	 * @param propertyUri
	 * @param limit
	 * @return list of String[] of length 2 where the first element is the subject, the second is the object i.e. if the result is 
	 * List([s1,o1], [s2,o2])  ==> both (s1,propertyUri,o1) and (s2,propertyUri,o2) are stated predicates
	 */
	public ImmutableList<String[]> getSampleValuesOfProperty(String propertyUri, int limit);
	
	/**
	 * @param resourceUri
	 * @param limit number of map <em>*entries*</em>
	 * @return a multimap (as some properties will have multiple values) keys are properties URIs while values are... corresponding properties values
	 */
	public Multimap<String, String> getResourcePropertiesMap(String resourceUri, int limit);
	public Multimap<String, String> getResourcePropertiesMap(PreviewResourceCannedQuery cannedQuery, String resourceUri);
	
	public ImmutableList<SearchResultItem> searchForEntities(String prefix, ImmutableList<String> searchPropertyUris, int limit);
	
	public void save(String serviceId, FileOutputStream out) throws IOException;
	
	public void write(JSONWriter writer)throws JSONException;
	public void initialize(FileInputStream in);
}
