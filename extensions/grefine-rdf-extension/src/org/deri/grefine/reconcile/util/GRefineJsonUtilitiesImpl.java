package org.deri.grefine.reconcile.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.deri.grefine.reconcile.model.ReconciliationCandidate;
import org.deri.grefine.reconcile.model.ReconciliationRequest;
import org.deri.grefine.reconcile.model.ReconciliationResponse;
import org.deri.grefine.reconcile.model.ReconciliationService;
import org.deri.grefine.reconcile.model.SearchResultItem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class GRefineJsonUtilitiesImpl implements GRefineJsonUtilities{

	@Override
	public String getServiceMetadataAsJsonP(ReconciliationService service, String callback, String baseServiceUrl){
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode obj = mapper.createObjectNode();
		obj.put("name", service.getName());
		obj.put("schemaSpace", URI_SPACE);
		obj.put("identifierSpace", URI_SPACE);
		
		
		//view object
		ObjectNode viewObj = mapper.createObjectNode();
		viewObj.put("url", baseServiceUrl + "/view?id={{id}}");
		obj.put("view", viewObj);
		
		//preview object
		ObjectNode previewObj = mapper.createObjectNode();
		previewObj.put("url", baseServiceUrl + "/preview/template?id={{id}}");
		previewObj.put("width",430);
		previewObj.put("height",300);
		
		obj.put("preview", previewObj);
		
		//suggest
		//Global suggest object
		ObjectNode suggestObj = mapper.createObjectNode(); 
				
		//type suggest (autocomplete)
		ObjectNode typeSuggestObj = mapper.createObjectNode();
		typeSuggestObj.put("service_url", baseServiceUrl);
		typeSuggestObj.put("service_path", "/suggest/type");
		typeSuggestObj.put("flyout_service_url", baseServiceUrl);
		typeSuggestObj.put("flyout_service_path" , "/suggest/type/preview");
			
		suggestObj.put("type", typeSuggestObj);
		
		//property suggest (autocomplete)
		ObjectNode propertySuggestObj = mapper.createObjectNode();
		propertySuggestObj.put("service_url", baseServiceUrl);
		propertySuggestObj.put("service_path", "/suggest/property");
		propertySuggestObj.put("flyout_service_url", baseServiceUrl);
		propertySuggestObj.put("flyout_service_path" , "/suggest/property/preview");
			
		suggestObj.put("property", propertySuggestObj);
		
		//entity search
		ObjectNode entitySearchObj = mapper.createObjectNode();
		entitySearchObj.put("service_url", baseServiceUrl);
		entitySearchObj.put("service_path", "/suggest/entity");
		entitySearchObj.put("flyout_service_url", baseServiceUrl);
		entitySearchObj.put("flyout_service_path" , "/suggest/entity/preview");
			
		suggestObj.put("entity", entitySearchObj);
		
		obj.put("suggest", suggestObj);
		
		return getJsonP(callback, obj);
	}
	
	@Override
	public ImmutableMap<String, ReconciliationRequest> getMultipleRequest(String queries) 
															throws JsonParseException, JsonMappingException, IOException{
		Map<String, ReconciliationRequest> multiRequest = new HashMap<String, ReconciliationRequest>();
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readValue(queries, JsonNode.class);
		Iterator<String> keysIter = root.getFieldNames();
		while(keysIter.hasNext()){
			String key = keysIter.next();
			//FIXME parsed twice 
			ReconciliationRequest request = ReconciliationRequest.valueOf(root.path(key).toString());
			multiRequest.put(key, request);
		}
		
		return ImmutableMap.copyOf(multiRequest);
	}
	
	@Override
	public ObjectNode getMultipleResponse(ImmutableMap<String,ReconciliationResponse> multiResponse, PrefixManager prefixManager) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode multiResponseObj = mapper.createObjectNode();
		for(Entry<String, ReconciliationResponse> entry: multiResponse.entrySet()){
			String key = entry.getKey();
			ReconciliationResponse response  = entry.getValue();
			multiResponseObj.put(key, getResponse(response,prefixManager));
		}
		
		return multiResponseObj;
	}
	
	@Override
	public ObjectNode jsonizeSearchResult(ImmutableList<SearchResultItem> results, String prefix) throws JsonGenerationException, JsonMappingException, IOException{

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode resultObj = mapper.createObjectNode();
		resultObj.put("code", "/api/status/ok");
		resultObj.put("status", "200 OK");
		resultObj.put("prefix", prefix);
		
		ArrayNode resultArr = mapper.createArrayNode();
		for(SearchResultItem item: results){
			ObjectNode resultItemObj = mapper.createObjectNode();
			resultItemObj.put("id", item.getId());
			resultItemObj.put("name", item.getName());
			
			//FIXME id is used instead of type to enable the suggest autocomplete to function as it doesn't work when no type is given
			ObjectNode tmpObj = mapper.createObjectNode();
			tmpObj.put("id", item.getId());
			tmpObj.put("name", item.getId());
			resultItemObj.put("type", tmpObj);			
			
			resultArr.add(resultItemObj);
		}
		resultObj.put("result", resultArr);
		
		return resultObj;
	}
	
	@Override
	public ObjectNode jsonizeHtml(String html, String id){
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode resultObj = mapper.createObjectNode();
		resultObj.put("html",html);
		resultObj.put("id", id);
		
		
		
		return resultObj;
	}
	
	@Override
	public String getJsonP(String callback, ObjectNode obj){
		return callback + "(" + obj + ");";
	}
	
	@Override
	public JSONObject getJSONObjectFromUrl(URL url) throws JSONException, IOException{
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(30000);
        connection.setDoOutput(true);
        connection.connect();
        InputStream is = connection.getInputStream();
        Reader reader = new InputStreamReader(is, "UTF-8");
        String s;
        try {
        	StringBuffer sb = new StringBuffer();

            char[] chars = new char[8192];
            int c;

            while ((c = reader.read(chars)) > 0) {
                sb.insert(sb.length(), chars, 0, c);
            }

        	s =  sb.toString();
        } finally {
            reader.close();
        }
        JSONTokener t = new JSONTokener(s);
        Object o = t.nextValue();
        if (o instanceof JSONObject) {
            return (JSONObject) o;
        } else {
            throw new JSONException(s + " couldn't be parsed as JSON object");
        }
	}
	
	private ObjectNode getResponse(ReconciliationResponse response, PrefixManager prefixManager) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode responseObj = mapper.createObjectNode();
		ArrayNode resultArr = mapper.createArrayNode();
		for(ReconciliationCandidate result:response.getResults()){
			ObjectNode resultItemObj = getResultItem(result,prefixManager);
			resultArr.add(resultItemObj);
		}
		responseObj.put("result", resultArr);
		
		return responseObj;
	}
	
	private ObjectNode getResultItem(ReconciliationCandidate item, PrefixManager prefixManager){
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode resultItemObj = mapper.createObjectNode();
		resultItemObj.put("id", item.getId());
		resultItemObj.put("name", item.getName());
		resultItemObj.put("score", item.getScore());
		resultItemObj.put("match", item.isMatch());

		ArrayNode typesArr = mapper.createArrayNode();
		for(int i=0;i<item.getTypes().length;i++){
			String id = item.getTypes()[i];
			int index = getNamespaceEndPosition(id);
			String prefix = prefixManager.getPrefix(id.substring(0,index));
			ObjectNode typeObj = mapper.createObjectNode();
			typeObj.put("id", id);
			if(prefix!=null){
				String localName = id.substring(index);
				typeObj.put("name", prefix +":" + localName);
			}else{
				typeObj.put("name", id);
			}
			typesArr.add(typeObj);
		}
		resultItemObj.put("type", typesArr);
		
		return resultItemObj;
	}

	private int getNamespaceEndPosition(String uri){
		if(uri.indexOf("#")!=-1){
			return uri.indexOf("#")+1;
		}else{
			return uri.lastIndexOf("/") + 1;
		}
	}
	
	private static final String URI_SPACE = "http://www.ietf.org/rfc/rfc3986";
}
