package org.deri.grefine.reconcile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.deri.grefine.reconcile.ServiceRegistry;
import org.deri.grefine.reconcile.model.ReconciliationService;
import org.deri.grefine.reconcile.util.GRefineJsonUtilitiesImpl;
import org.deri.grefine.reconcile.util.PrefixManager;

import org.json.JSONException;

public class GRefineServiceManager {

	static public GRefineServiceManager singleton;
	
	private ServiceRegistry registry;
	private File workingDir;
	
	public GRefineServiceManager(ServiceRegistry registry,File dir) {
		this.registry = registry;
		this.workingDir = dir;
	}

	//TODO ... ugly, isn't it?
	static public synchronized void initialize(File workingDir) throws JSONException, IOException{
		if(singleton==null){
			InputStream prefixesIn = GRefineServiceManager.class.getResourceAsStream("/files/prefixes");
			PrefixManager prefixManager = new PrefixManager(prefixesIn);
			singleton = new GRefineServiceManager(new ServiceRegistry(new GRefineJsonUtilitiesImpl(),prefixManager), workingDir);
			File servicesFile= new File(workingDir,"services");
			if(servicesFile.exists()){
				FileInputStream in = new FileInputStream(servicesFile);
				singleton.registry.loadFromFile(in);				
			}
		}
	}
	
	public boolean hasService(String id){
		return registry.hasService(id);
	}

	public ReconciliationService getService(String id){
		FileInputStream in;
		try{
			in = new FileInputStream(new File(workingDir, id+".ttl"));
		}catch(FileNotFoundException e){
			in = null;
		}
		
		ReconciliationService service = registry.getService(id,in);
		if(service==null){
			throw new RuntimeException("Service '" + id + "' not found");
		}
		return service;
	}
	
	public synchronized void addService(ReconciliationService service) throws JSONException, IOException {
		registry.addService(service);
		FileOutputStream servicesFile = new FileOutputStream(new File(workingDir,"services"));
		registry.save(servicesFile);
	}
	
	public synchronized void addAndSaveService(ReconciliationService service) throws JSONException, IOException {
		FileOutputStream serviceModelFile = new FileOutputStream(new File(workingDir,service.getId()+".ttl"));
		registry.addAndSaveService(service, serviceModelFile);
		FileOutputStream servicesFile = new FileOutputStream(new File(workingDir,"services"));
		registry.save(servicesFile);
	}
	
	public String metadata(String serviceName, HttpServletRequest request){
		String callback = request.getParameter("callback");
		ReconciliationService service = getService(serviceName);
		return registry.metadata(service, request.getRequestURL().toString(), callback);
	}
	
	public String multiReconcile(String serviceName, HttpServletRequest request) throws JsonParseException, JsonMappingException, IOException{
		String queries = request.getParameter("queries");
		ReconciliationService service = getService(serviceName);
		return registry.multiReconcile(service, queries);
	}
	
	public String suggestType(String serviceName, HttpServletRequest request) throws JsonGenerationException, JsonMappingException, IOException {
		ReconciliationService service = getService(serviceName);
		String callback = request.getParameter("callback");
		String prefix = request.getParameter("prefix");
		return registry.suggestType(service, prefix, callback);
	}
	
	public String previewType(String serviceName, HttpServletRequest request) throws Exception {
		ReconciliationService service = getService(serviceName);
		String callback = request.getParameter("callback");
		String typeId = request.getParameter("id");		
		return registry.previewType(service, typeId, callback);
	}
	
	public String suggestProperty(String serviceName, HttpServletRequest request) throws JsonGenerationException, JsonMappingException, IOException {
		ReconciliationService service = getService(serviceName);
		String callback = request.getParameter("callback");
		String prefix = request.getParameter("prefix");
		String typeId = request.getParameter("schema");
		return registry.suggestProperty(service, typeId, prefix, callback);
	}
	
	public String previewProperty(String serviceName, HttpServletRequest request) throws Exception {
		ReconciliationService service = getService(serviceName);
		String callback = request.getParameter("callback");
		String propertyId = request.getParameter("id");		
		return registry.previewProperty(service, propertyId, callback);
	}
	
	public String suggestEntity(String serviceName, HttpServletRequest request) throws JsonGenerationException, JsonMappingException, IOException {
		ReconciliationService service = getService(serviceName);
		String callback = request.getParameter("callback");
		String prefix = request.getParameter("prefix");
		return registry.suggestEntity(service, prefix, callback);
	}
	
	public String previewEntity(String serviceName, HttpServletRequest request) throws Exception {
		ReconciliationService service = getService(serviceName);
		String callback = request.getParameter("callback");
		String entityId = request.getParameter("id");		
		return registry.previewEntity(service, entityId, callback);
	}
	
	public String previewResource(String serviceName, HttpServletRequest request) throws Exception {
		ReconciliationService service = getService(serviceName);
		String entityId = request.getParameter("id");		
		return registry.previewResource(service, entityId);
	}
	
	public String getHtmlOfResourcePreviewTemplate(String serviceName, HttpServletRequest request) throws Exception{
		String entityId = request.getParameter("id");
		String previewUrl = request.getRequestURI().substring(0, request.getRequestURI().lastIndexOf("/")) + "?id=" + URLEncoder.encode(entityId,"UTF-8");
		return registry.getHtmlOfResourcePreviewTemplate(previewUrl, entityId);
	}
	
	/**
	 * retain only these ids
	 * @param ids
	 * @throws IOException 
	 * @throws JSONException 
	 */
	public synchronized void synchronizeServices(Set<String> urls) throws JSONException, IOException{
		Set<String> services = this.registry.getServiceIds();
		Pattern pattern = Pattern.compile("rdf-extension\\/services\\/([-.a-zA-Z0-9_]+)");
		Set<String> ids = getServiceIds(urls,pattern);
		services.removeAll(ids);
		for(String id:services){
			ReconciliationService service =registry.removeService(id);
			File modelFile = new File(workingDir,service.getId()+".ttl");
			if(modelFile.exists()){
				modelFile.delete();
			}
		}
		FileOutputStream servicesFile = new FileOutputStream(new File(workingDir,"services"));
		registry.save(servicesFile);
	}
	
	private Set<String> getServiceIds(Set<String> urls, Pattern pattern){
		Set<String> ids = new HashSet<String>();
		for(String url:urls){
			Matcher matcher = pattern.matcher(url);
			boolean matchFound = matcher.find();
			if(matchFound){
				ids.add(matcher.group(1));
			}
		}
		return ids;
	}
	
}
