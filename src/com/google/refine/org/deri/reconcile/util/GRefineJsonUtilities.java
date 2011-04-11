package com.google.refine.org.deri.reconcile.util;

import java.io.IOException;
import java.net.URL;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.node.ObjectNode;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequest;
import com.google.refine.org.deri.reconcile.model.ReconciliationResponse;
import com.google.refine.org.deri.reconcile.model.ReconciliationService;
import com.google.refine.org.deri.reconcile.model.SearchResultItem;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public interface GRefineJsonUtilities {

	public String getServiceMetadataAsJsonP(ReconciliationService service, String callback, String baseServiceUrl);
	public ImmutableMap<String, ReconciliationRequest> getMultipleRequest(String queries) throws JsonParseException, JsonMappingException, IOException;
	public ObjectNode getMultipleResponse(ImmutableMap<String,ReconciliationResponse> multiResponse, PrefixManager prefixManager);
	public ObjectNode jsonizeSearchResult(ImmutableList<SearchResultItem> results, String prefix) throws JsonGenerationException, JsonMappingException, IOException;
	public ObjectNode jsonizeHtml(String html, String id);
	public String getJsonP(String callback, ObjectNode obj);
	public JSONObject getJSONObjectFromUrl(URL url) throws JSONException, IOException;	
}
