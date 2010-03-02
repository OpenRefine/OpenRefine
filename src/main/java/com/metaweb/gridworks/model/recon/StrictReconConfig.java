package com.metaweb.gridworks.model.recon;

import org.json.JSONObject;

abstract public class StrictReconConfig extends ReconConfig {
	private static final long serialVersionUID = 4454059850557793074L;
	
    static public ReconConfig reconstruct(JSONObject obj) throws Exception {
        String match = obj.getString("match");
        if ("key".equals(match)) {
        	return KeyBasedReconConfig.reconstruct(obj);
        }
        return null;
    }
}
