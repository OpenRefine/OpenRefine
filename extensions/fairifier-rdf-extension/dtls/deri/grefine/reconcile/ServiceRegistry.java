package org.deri.grefine.reconcile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.deri.grefine.reconcile.model.ReconciliationRequest;
import org.deri.grefine.reconcile.model.ReconciliationResponse;
import org.deri.grefine.reconcile.model.ReconciliationService;
import org.deri.grefine.reconcile.model.SearchResultItem;
import org.deri.grefine.reconcile.rdf.RdfReconciliationService;
import org.deri.grefine.reconcile.rdf.endpoints.PlainSparqlQueryEndpoint;
import org.deri.grefine.reconcile.rdf.endpoints.QueryEndpoint;
import org.deri.grefine.reconcile.rdf.endpoints.QueryEndpointImpl;
import org.deri.grefine.reconcile.rdf.executors.DumpQueryExecutor;
import org.deri.grefine.reconcile.rdf.executors.QueryExecutor;
import org.deri.grefine.reconcile.rdf.executors.RemoteQueryExecutor;
import org.deri.grefine.reconcile.rdf.executors.VirtuosoRemoteQueryExecutor;
import org.deri.grefine.reconcile.rdf.factories.BigOwlImSparqlQueryFactory;
import org.deri.grefine.reconcile.rdf.factories.JenaTextSparqlQueryFactory;
import org.deri.grefine.reconcile.rdf.factories.PlainSparqlQueryFactory;
import org.deri.grefine.reconcile.rdf.factories.SparqlQueryFactory;
import org.deri.grefine.reconcile.rdf.factories.VirtuosoSparqlQueryFactory;
import org.deri.grefine.reconcile.sindice.SindiceService;
import org.deri.grefine.reconcile.util.GRefineJsonUtilities;
import org.deri.grefine.reconcile.util.PrefixManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ServiceRegistry {

	private Map<String, ReconciliationService> services;
	private GRefineJsonUtilities grefineJsonUtilities;
	private PrefixManager prefixManager;
	
	public ServiceRegistry(GRefineJsonUtilities jsonUtilities, PrefixManager prefixer) {
		services = new HashMap<String, ReconciliationService>();
		this.grefineJsonUtilities = jsonUtilities;
		this.prefixManager = prefixer;
	}
	
	public void addService(ReconciliationService service){
		this.services.put(service.getId(), service);
	}
	
	public ReconciliationService removeService(String id){
		return this.services.remove(id);
	}
	
	public Set<String> getServiceIds(){
		return new HashSet<String>(services.keySet());
	}
	
	public ReconciliationService getService(String id, FileInputStream in){
		ReconciliationService service = services.get(id);
		if(in!=null){
			service.initialize(in);
		}
		return service;
	}

	public void addAndSaveService(ReconciliationService service, FileOutputStream out) throws IOException{
		this.addService(service);
		service.save(out);
	}
	
	public void save(FileOutputStream out) throws JSONException, IOException{
		Writer writer = new OutputStreamWriter(out);
	    try {
	    	JSONWriter jsonWriter = new JSONWriter(writer);
	    	jsonWriter.object();
	    	jsonWriter.key("services");
	    	jsonWriter.array();
	    	for(ReconciliationService service:this.services.values()){
	    		service.writeAsJson(jsonWriter,true);
	    	}
	    	jsonWriter.endArray();
	    	jsonWriter.endObject();
        } finally {
        	writer.close();
	   }
	}
	
	public boolean hasService(String id){
		return services.containsKey(id);
	}
	
	public String metadata(ReconciliationService service, String baseUrl,String callback){
		return grefineJsonUtilities.getServiceMetadataAsJsonP(service, callback, baseUrl);
	}
	
	public String multiReconcile(ReconciliationService service, String queries) throws JsonParseException, JsonMappingException, IOException{
		ImmutableMap<String, ReconciliationRequest> multiQueryRequest = grefineJsonUtilities.getMultipleRequest(queries);
		ImmutableMap<String, ReconciliationResponse> multiResult = service.reconcile(multiQueryRequest);
		String response = grefineJsonUtilities.getMultipleResponse(multiResult,prefixManager).toString();
		return response;
	}
	
	public String suggestType(ReconciliationService service,String prefix,String callback) throws JsonGenerationException, JsonMappingException, IOException {
		ImmutableList<SearchResultItem> results = service.suggestType(prefix);
		return grefineJsonUtilities.getJsonP(callback, grefineJsonUtilities.jsonizeSearchResult(results, prefix));
	}
	
	public String previewType(ReconciliationService service,String typeId, String callback) throws Exception{
		String html = service.getPreviewHtmlForType(typeId);
		return grefineJsonUtilities.getJsonP(callback,grefineJsonUtilities.jsonizeHtml(html,typeId));
	}
	
	public String previewProperty(ReconciliationService service, String propertyId, String callback) throws Exception{
		String html = service.getPreviewHtmlForProperty(propertyId);
		return grefineJsonUtilities.getJsonP(callback,grefineJsonUtilities.jsonizeHtml(html,propertyId));
	}

	public String previewEntity(ReconciliationService service, String entityId, String callback) throws Exception{
		String html = previewResource(service,entityId);
		if(html==null){
			return null;
		}
		return grefineJsonUtilities.getJsonP(callback,grefineJsonUtilities.jsonizeHtml(html,entityId));
	}

	
	public String suggestProperty(ReconciliationService service, String typeId, String prefix, String callback) throws JsonGenerationException, JsonMappingException, IOException {
		ImmutableList<SearchResultItem> results;
		if(typeId==null || typeId.isEmpty()){
			results = service.suggestProperty(prefix);
		}else{
			results = service.suggestProperty(prefix,typeId);
		}
		return grefineJsonUtilities.getJsonP(callback, grefineJsonUtilities.jsonizeSearchResult(results, prefix));
	}
	
	public String suggestEntity(ReconciliationService service,String prefix, String callback) throws JsonGenerationException, JsonMappingException, IOException {
		ImmutableList<SearchResultItem> results = service.suggestEntity(prefix);
		return grefineJsonUtilities.getJsonP(callback, grefineJsonUtilities.jsonizeSearchResult(results, prefix));
	}
	
	public String previewResource(ReconciliationService service, String resourceId) throws Exception{
		return service.getPreviewHtmlForResource(resourceId);
	}
	
	public String getHtmlOfResourcePreviewTemplate(String previewUrl, String resourceId)throws Exception{
		String templatePath = "templates/resource_preview_template.vt";
		StringWriter writer = new StringWriter();
		VelocityContext context = new VelocityContext();
		context.put("resourceUri", resourceId);
		context.put("previewResourceUrl", previewUrl);
		
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(templatePath);
		
		VelocityEngine templateEngine = new VelocityEngine();
		templateEngine.init();
		
		templateEngine.evaluate(context, writer, "rdf-reconcile-extension", new InputStreamReader(in));
		writer.close();
		String html = writer.toString();
		return html;
	}
	
	public void loadFromFile(FileInputStream in) throws JSONException, IOException{
		try {
            JSONTokener tokener = new JSONTokener(new InputStreamReader(in));
            JSONObject obj = (JSONObject) tokener.nextValue();
            JSONArray services = obj.getJSONArray("services");
            for(int i=0;i<services.length();i++){
            	JSONObject serviceObj = services.getJSONObject(i);
            	String type = serviceObj.getString("type");
            	ReconciliationService service;
            	if(type.equals("rdf")){
            		service = loadRdfServiceFromJSON(serviceObj);
            	}else if(type.equals("sindice")){
            		service = loadSindiceServiceFromJSON(serviceObj);
            	}else{
            		//unknown service ignore
            		continue;
            	}
            	this.services.put(service.getId(), service);
            }
        }finally {
            in.close();
        }
		
	}
	
	private ReconciliationService loadSindiceServiceFromJSON(JSONObject serviceObj) throws JSONException{
		String serviceId = serviceObj.getString("id");
		String name = serviceObj.getString("name");
		String domain = null;
		if(serviceObj.has("domain")){
			domain = serviceObj.getString("domain");
		}
		return new SindiceService(serviceId, name, domain);
	}
	
	private RdfReconciliationService loadRdfServiceFromJSON(JSONObject serviceObj) throws JSONException{
		String serviceId = serviceObj.getString("id");
		List<String> searchPropertyUris = new ArrayList<String>();
		JSONArray propertiesArray = serviceObj.getJSONArray("searchPropertyUris");
		for(int i=0;i<propertiesArray.length();i++){
			searchPropertyUris.add(propertiesArray.getString(i));
		}
		QueryEndpoint endpoint = loadEndpointFromJSON(serviceObj.getJSONObject("endpoint"));
		return new RdfReconciliationService(serviceId, serviceObj.getString("name"), 
				ImmutableList.copyOf(searchPropertyUris), endpoint, serviceObj.getDouble("matchThreshold"));
	}

	private QueryEndpoint loadEndpointFromJSON(JSONObject endpointObj) throws JSONException{
		String type = endpointObj.getString("type");
		QueryExecutor executor = loadQueryExecutorFromJSON(endpointObj.getJSONObject("queryExecutor"));
		SparqlQueryFactory factory = loadQueryFactoryFromJSON(endpointObj.getJSONObject("queryFactory"));
		if(type.equals("plain")){
			return new PlainSparqlQueryEndpoint((PlainSparqlQueryFactory)factory, executor);
		}else{
			//default
			return new QueryEndpointImpl(factory, executor);
		}
			
	}
	
	private QueryExecutor loadQueryExecutorFromJSON(JSONObject jsonObject) throws JSONException {
		String type = jsonObject.getString("type");
		if(type.equals("dump")){
			if(jsonObject.has("propertyUri")){
				return new DumpQueryExecutor(jsonObject.getString("propertyUri"));	
			}else{
				return new DumpQueryExecutor();
			}
		}else{
			String url = jsonObject.getString("sparql-url");
			String graph = null;
			if(jsonObject.has("default-graph-uri")){
				graph = jsonObject.getString("default-graph-uri");
			}
			if(type.equals("remote-virtuoso")){
				return new VirtuosoRemoteQueryExecutor(url, graph);
			}else{
				//plain
				return new RemoteQueryExecutor(url, graph);
			}
		}
	}
	
	private SparqlQueryFactory loadQueryFactoryFromJSON(JSONObject factoryObj) throws JSONException{
		String type = factoryObj.getString("type");
		if(type.equals("virtuoso")){
			return new VirtuosoSparqlQueryFactory();
		}else if(type.equals("jena-text")){
			return new JenaTextSparqlQueryFactory();
		}else if(type.equals("bigowlim")){
			return new BigOwlImSparqlQueryFactory();
		}else{
			//plain
			return new PlainSparqlQueryFactory();
		}
	}
	
	
}
