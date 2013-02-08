package org.deri.grefine.reconcile.rdf;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.deri.grefine.reconcile.model.AbstractReconciliationService;
import org.deri.grefine.reconcile.model.ReconciliationCandidate;
import org.deri.grefine.reconcile.model.ReconciliationRequest;
import org.deri.grefine.reconcile.model.ReconciliationResponse;
import org.deri.grefine.reconcile.model.SearchResultItem;
import org.deri.grefine.reconcile.rdf.endpoints.QueryEndpoint;
import org.deri.grefine.reconcile.rdf.factories.PreviewResourceCannedQuery;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

/**
 * @author fadmaa
 * also implements reconcile so that it differentiate between reconcile request without type ==> gussType Request and those with types ==> reconcile
 */
public class RdfReconciliationService extends AbstractReconciliationService{
	protected ImmutableList<String> searchPropertyUris;
	protected QueryEndpoint queryEndpoint;

	/**
	 * the limit used when guessing types... this depends on the data that are queried
	 */
	protected final int limitForSuggestion;
	protected final double matchThreshold;
//	protected final String labelPropertyUri;
	
	protected PreviewResourceCannedQuery previewResourceCannedQuery;
	
	/**
 	 * @param searchPropertyUris list of property URIs that will be used for full text search and for retrieving items name (i.e. displayed label)
	 * @param freemarkerConfig
	 */
	public RdfReconciliationService(String id, String name, ImmutableList<String> searchPropertyUris, QueryEndpoint queryEndpoint, 
			double matchThreshold){
		super(id,name);
		this.searchPropertyUris = searchPropertyUris;
		this.queryEndpoint = queryEndpoint;
		//TODO hard-coded value
		this.limitForSuggestion = LIMIT_FOR_SUGGESTION;
		this.matchThreshold = matchThreshold;
		try{
			//TODO hard-coded for now.. accept as parameter if you need services to use different properties for perviewing
			InputStream in = this.getClass().getResourceAsStream("/files/preview_properties.properties");
			this.previewResourceCannedQuery = new PreviewResourceCannedQuery(in);
		}catch (IOException e) {
		}
	}
	
	public RdfReconciliationService(String id, String name, QueryEndpoint queryEndpoint, double matchThreshold){
		this(id,name,ImmutableList.of(RDFS_LABEL),queryEndpoint,matchThreshold);
	}
	
	@Override
	public ReconciliationResponse reconcile(ReconciliationRequest request) {
		List<ReconciliationCandidate> candidates = queryEndpoint.reconcileEntities(request, searchPropertyUris, matchThreshold);
		return wrapCandidates(candidates);
	}
	
	@Override
	public ImmutableList<SearchResultItem> suggestType(String prefix) {
		return this.queryEndpoint.suggestType(prefix, limitForSuggestion);
	}
	
	@Override
	public ImmutableList<SearchResultItem> suggestProperty(String prefix) {
		return this.queryEndpoint.suggestProperty(prefix, limitForSuggestion);
	}
	
	@Override
	public ImmutableList<SearchResultItem> suggestProperty(String prefix, String subjectTypeUri) {
		return this.queryEndpoint.suggestProperty(prefix, subjectTypeUri, limitForSuggestion);
	}
	
	@Override
	public String getPreviewHtmlForType(String typeUri) throws Exception {
		List<SearchResultItem> sampleInstances = this.queryEndpoint.getSampleInstances(typeUri, searchPropertyUris,LIMIT_FOR_INSTANCES_SAMPLE);
		VelocityContext context = new VelocityContext();
		context.put("typeUri", typeUri);
		context.put("instances", sampleInstances);
		
		return getHtmlFromTemplate("templates/type_preview.vt", context);
	}
	
	@Override
	public String getPreviewHtmlForProperty(String propertyUri) throws Exception {
		List<String[]> sampleInstances = this.queryEndpoint.getSampleValuesOfProperty(propertyUri, LIMIT_FOR_PROPERTY_VALUES_SAMPLE);
		VelocityContext context = new VelocityContext();
		context.put("propertyUri", propertyUri);
		context.put("instances", sampleInstances);
		
		return getHtmlFromTemplate("templates/property_preview.vt", context);
	}
	
	@Override
	public String getPreviewHtmlForResource(String resourceUri) throws Exception{
		//Multimap<String, String> propertiesMap 	= this.queryEndpoint.getResourcePropertiesMap(resourceUri,LIMIT_FOR_RESOURCE_PREVIEW);	
		Multimap<String, String> propertiesMap 	= this.queryEndpoint.getResourcePropertiesMap(previewResourceCannedQuery, resourceUri);
		VelocityContext context = new VelocityContext();
		context.put("resourceUri", resourceUri);
		context.put("propertiesMap", propertiesMap);
		
		return getHtmlFromTemplate("templates/resource_preview.vt", context);
	}
	
	@Override
	public ImmutableList<SearchResultItem> suggestEntity(String prefix) {
		return this.queryEndpoint.searchForEntities(prefix, searchPropertyUris, limitForSuggestion);
	}

	@Override
	public void writeAsJson(JSONWriter writer,boolean saveMode)throws JSONException {
		writer.object();
		writer.key("type"); writer.value("rdf");
		writer.key("id");writer.value(this.getId());
		writer.key("name");writer.value(this.getName());
		writer.key("matchThreshold");writer.value(this.matchThreshold);
		writer.key("searchPropertyUris");
		writer.array();
		for(String uri:this.searchPropertyUris){
			writer.value(uri);
		}
		writer.endArray();
		writer.key("endpoint");
		this.queryEndpoint.write(writer);
		writer.endObject();
	}
	
	@Override
	public void save(FileOutputStream out) throws IOException{
		queryEndpoint.save(this.getId(),out);
	}
	
	@Override
	public void initialize(FileInputStream in){
		queryEndpoint.initialize(in);
	}
	
	protected String getHtmlFromTemplate(String templatePath, VelocityContext context) throws Exception{
		StringWriter writer = new StringWriter();
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(templatePath);
		
		VelocityEngine templateEngine = new VelocityEngine();
		templateEngine.init();
		
		templateEngine.evaluate(context, writer, "rdf-reconcile-extension", new InputStreamReader(in));
		writer.close();
		String html = writer.toString();
		return html;
	}
	
	private ReconciliationResponse wrapCandidates(List<ReconciliationCandidate> candidates){
		ReconciliationResponse response = new ReconciliationResponse();
		response.setResults(candidates);
		return response;
	}
	
	private static final int LIMIT_FOR_INSTANCES_SAMPLE = 5;
	private static final int LIMIT_FOR_PROPERTY_VALUES_SAMPLE = 5;
	//private static final int LIMIT_FOR_RESOURCE_PREVIEW = 15;
	private static final int LIMIT_FOR_SUGGESTION = 40;
	
	private static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
	
}

