package org.deri.grefine.reconcile.model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public interface ReconciliationService{

	/**
	 * @return a unique ID for the service
	 */
	public String getId();
	
	/**
	 * @return a human readable name of the service
	 */
	public String getName();
	
	/**
	 * reconcile a single reconciliation request from Google Refine (single-query mode)
	 * @param request
	 * @return
	 */
	public ReconciliationResponse reconcile(ReconciliationRequest request);
	
	/**
	 * reconcile a map of queries (multi-query mode)
	 * @param multiQueryRequest
	 * @param httpRequest
	 * @return a map of Google Refine results. the map should use the same keys as in the input.
	 * {@link org.deri.grefine.reconciliation.rdf.RdfReconciliationService RdfReconciliationService} provides default implementation based on {@link #reconcile(ReconciliationRequest)}
	 */
	public ImmutableMap<String, ReconciliationResponse> reconcile(ImmutableMap<String, ReconciliationRequest> multiQueryRequest);
	
	/**
	 * used for autocomplete
	 * @param prefix the query string 
	 * @return ranked list of types matching the prefix
	 */
	public ImmutableList<SearchResultItem> suggestType(String prefix);
	
	/**
	 * used for autocomplete
	 * @param prefix the query string 
	 * @return ranked list of properties matching the prefix
	 */
	public ImmutableList<SearchResultItem> suggestProperty(String prefix);
	
	/**
	 * used for autocomplete
	 * subjectTypeUri restricts properties to only those satisfying (s,p,o) exists ans s is of type subjectTypeId
	 * @param prefix the query string 
	 * @param subjectTypeId the ID of the 
	 * @return ranked list of properties matching the prefix and have a subject of type subjectTypeId
	 */
	public ImmutableList<SearchResultItem> suggestProperty(String prefix, String subjectTypeId);
	
	
	/**
	 * @param typeId
	 * @return HTML for type preview. this should be an HTML portion and not a full document(i.e. DIV (without HEAD, BODY...) )
	 * should give a good brief overview of the type identified by typeId to help the user identify it
	 */
	public String getPreviewHtmlForType(String typeId) throws Exception;
	
	/**
	 * @param propertyId
	 * @return HTML for property preview. this should be an HTML portion and not a full document(i.e. DIV (without HEAD, BODY...) )
	 * should give a good brief overview of the property identified by propertyId to help the user identify it
	 */
	public String getPreviewHtmlForProperty(String propertyId) throws Exception;
	
	/**
	 * @param resourceId
	 * @return HTML for resource preview. this should be an HTML portion and not a full document(i.e. DIV (without HEAD, BODY...) )
	 * should give a good brief overview of the resource identified by resourceId to help the user identify it
	 */
	public String getPreviewHtmlForResource(String resourceId) throws Exception;
	
	/**
	 * used for autocomplete
	 * @param prefix the query string 
	 * @return ranked list of entities matching the prefix
	 */
	public ImmutableList<SearchResultItem> suggestEntity(String prefix);
	
	/**
	 * save the data if any. this method should only save the data of the service not its metadata. metadata is id, name and other configuration
	 * example of data to be save is the RDF model for a dump-based service.
	 */
	public void save(FileOutputStream out) throws IOException;

	public void writeAsJson(JSONWriter w)throws JSONException;
	public void writeAsJson(JSONWriter w, boolean saveMode)throws JSONException;
	
	/**
	 * @param in inputstream to read a file containing a model for the service(dump-based only) in Turtle.
	 */
	public void initialize(FileInputStream in);
}
