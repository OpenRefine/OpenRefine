package com.metaweb.gridworks.model.recon;

import org.json.JSONObject;

abstract public class StrictReconConfig extends ReconConfig {
    private static final long serialVersionUID = 4454059850557793074L;
    
    final static protected String s_mqlreadService = "http://api.freebase.com/api/service/mqlread";

    static public ReconConfig reconstruct(JSONObject obj) throws Exception {
        String match = obj.getString("match");
        if ("key".equals(match)) {
            return KeyBasedReconConfig.reconstruct(obj);
        } else if ("id".equals(match)) {
            return IdBasedReconConfig.reconstruct(obj);
        } else if ("guid".equals(match)) {
            return GuidBasedReconConfig.reconstruct(obj);
        }
        return null;
    }
}
