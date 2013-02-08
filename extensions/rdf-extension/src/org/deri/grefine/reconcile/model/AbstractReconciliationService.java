package org.deri.grefine.reconcile.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * @author fadmaa
 * implement the multi-query mode reconciliation based on repetitive application of single-query reconcile {@link org.deri.grefine.reconcile.model.ReconciliationService#reconcile(ReconciliationRequest)}
 * 
 */

public abstract class AbstractReconciliationService implements ReconciliationService{
	final static Logger logger = LoggerFactory.getLogger("AbstractReconciliationService");
	
	protected String name;
	protected String id;
	
	protected AbstractReconciliationService(String id, String name){
		this.name= name;
		this.id = id;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ImmutableMap<String, ReconciliationResponse> reconcile(ImmutableMap<String, ReconciliationRequest> multiQueryRequest) {
		Map<String, ReconciliationResponse> multiQueryResponse = new HashMap<String, ReconciliationResponse>();
		for(Entry<String, ReconciliationRequest> entry: multiQueryRequest.entrySet()){
			try{
				String key = entry.getKey();
				ReconciliationRequest request = entry.getValue();
				ReconciliationResponse response = reconcile(request);
				multiQueryResponse.put(key, response);
				Thread.sleep(300);
			}catch(Exception e){
				multiQueryResponse.put(entry.getKey(), new ReconciliationResponse());
				logger.error("error reconciling '" + entry.getValue().getQueryString() + "'",e);
			}
		}
		return ImmutableMap.copyOf(multiQueryResponse);
	}

	@Override
	public void writeAsJson(JSONWriter w) throws JSONException {
		writeAsJson(w,false);
	}
	
	
}
