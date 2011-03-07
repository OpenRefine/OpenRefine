package com.google.refine.org.deri.reconcile.sindice;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.refine.org.deri.reconcile.model.AbstractReconciliationService;
import com.google.refine.org.deri.reconcile.model.ReconciliationCandidate;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequest;
import com.google.refine.org.deri.reconcile.model.ReconciliationResponse;
import com.google.refine.org.deri.reconcile.model.SearchResultItem;
import com.google.refine.org.deri.reconcile.rdf.endpoints.QueryEndpoint;
import com.google.refine.org.deri.reconcile.rdf.endpoints.QueryEndpointImpl;
import com.google.refine.org.deri.reconcile.rdf.executors.DumpQueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.executors.QueryExecutor;
import com.google.refine.org.deri.reconcile.rdf.factories.LarqSparqlQueryFactory;
import com.google.refine.org.deri.reconcile.rdf.factories.PreviewResourceCannedQuery;
import com.google.refine.org.deri.reconcile.rdf.factories.SparqlQueryFactory;
import com.google.refine.org.deri.reconcile.util.GRefineJsonUtilities;
import com.google.refine.org.deri.reconcile.util.RdfUtilities;

import com.hp.hpl.jena.rdf.model.Model;

public class SindiceService extends AbstractReconciliationService{
	final static Logger logger = LoggerFactory.getLogger("SindiceService");

	private SparqlQueryFactory queryFactory;
	private GRefineJsonUtilities jsonUtilities;
	private SindiceBroker broker;
	private String domain;
	private RdfUtilities rdfUtilities;
	protected PreviewResourceCannedQuery previewResourceCannedQuery;
	
	public SindiceService(String id, String name,String domain,GRefineJsonUtilities jsonUtilities, RdfUtilities rdfUtilities, SindiceBroker broker) {
		super(id, name);
		this.domain = domain;
		this.queryFactory = new LarqSparqlQueryFactory();
		this.jsonUtilities = jsonUtilities;
		this.broker = broker;
		this.rdfUtilities = rdfUtilities;
		try{
			//TODO hard-coded for now.. accept as parameter if you need services to use different properties for perviewing
			InputStream in = this.getClass().getResourceAsStream("../rdf/factories/files/preview_properties.properties");
			this.previewResourceCannedQuery = new PreviewResourceCannedQuery(in);
		}catch (IOException e) {
		}
	}

	@Override
	public ReconciliationResponse reconcile(ReconciliationRequest request) {
		String type = (request.getTypes()!=null && request.getTypes().length>0)? (request.getTypes()[0]) : null;
		try {
			LinkedHashSet<String[]> urlPairs = broker.getUrlsForSimpleTermSearch(request.getQueryString(),domain,type, DEFAULT_SEARCH_LIMIT,jsonUtilities);
			ImmutableList<String> empty = ImmutableList.of();
			Set<ReconciliationCandidate> candidates = new LinkedHashSet<ReconciliationCandidate>();
			int limit = request.getLimit();
			for(String[] pair: urlPairs){
				Model model = broker.getModelForUrl(pair[0], pair[1],jsonUtilities);
				QueryExecutor queryExecutor = new DumpQueryExecutor(model); 
				QueryEndpoint endpoint = new QueryEndpointImpl(queryFactory, queryExecutor);
				request.setLimit(limit - candidates.size());
				candidates.addAll(endpoint.reconcileEntities(request, empty, 0.9));
				if(candidates.size()>=limit){
					break;
				}
			}

			//successful
			return wrapCandidates(new ArrayList<ReconciliationCandidate>(candidates));
		} catch (JSONException e) {
			throw new RuntimeException("error reconciling " + request.getQueryString() + " using Sindice", e);
		} catch (IOException e) {
			throw new RuntimeException("error reconciling " + request.getQueryString() + " using Sindice", e);
		}
	}

	@Override
	public ImmutableList<SearchResultItem> suggestType(String prefix) {
		throw new UnsupportedOperationException("Sindice Reconciliation service cannot suggest type");
	}

	@Override
	public ImmutableList<SearchResultItem> suggestProperty(String prefix) {
		throw new UnsupportedOperationException("Sindice Reconciliation service cannot suggest property");
	}

	@Override
	public ImmutableList<SearchResultItem> suggestProperty(String prefix, String subjectTypeId) {
		throw new UnsupportedOperationException("Sindice Reconciliation service cannot suggest property");
	}

	@Override
	public String getPreviewHtmlForType(String typeId) throws Exception {
		throw new UnsupportedOperationException("Sindice Reconciliation service cannot suggest/view type");
	}

	@Override
	public String getPreviewHtmlForProperty(String propertyId) throws Exception {
		throw new UnsupportedOperationException("Sindice Reconciliation service cannot suggest/view property");
	}

	@Override
	public String getPreviewHtmlForResource(String resourceId) throws Exception {
		Model model = rdfUtilities.dereferenceUri(resourceId);
		
		QueryExecutor queryExecutor = new DumpQueryExecutor(model); 
		QueryEndpoint endpoint = new QueryEndpointImpl(queryFactory, queryExecutor);
		Multimap<String, String> propertiesMap 	= endpoint.getResourcePropertiesMap(previewResourceCannedQuery, resourceId);
		VelocityContext context = new VelocityContext();
		context.put("resourceUri", resourceId);
		context.put("propertiesMap", propertiesMap);
		
		return getHtmlFromTemplate("templates/resource_preview.vt", context);
	}

	@Override
	public ImmutableList<SearchResultItem> suggestEntity(String prefix) {
		throw new UnsupportedOperationException("Sindice Reconciliation service cannot suggest entity");
	}

	private ReconciliationResponse wrapCandidates(List<? extends ReconciliationCandidate> candidates){
		ReconciliationResponse response = new ReconciliationResponse();
		response.setResults(candidates);
		return response;
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

	@Override
	public void writeAsJson(JSONWriter writer, boolean saveMode)throws JSONException {
		if(saveMode && domain==null){
			// we do not save the generic Sindice reconciliation service. it gets added at each server startup 
			return;
		}
		writer.object();
		writer.key("type"); writer.value("sindice");
		writer.key("id");writer.value(this.getId());
		writer.key("name");writer.value(this.getName());
		if(domain!=null){
			writer.key("domain");writer.value(domain);
		}
		writer.endObject();
	}

	@Override
	public void initialize(FileInputStream in) {
		//nothing to initialize
	}

	@Override
	public void save(FileOutputStream out) throws IOException {
		//nothing to save
	}
	
	static final int DEFAULT_SEARCH_LIMIT = 10;
}
