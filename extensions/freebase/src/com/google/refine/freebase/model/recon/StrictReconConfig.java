package com.google.refine.freebase.model.recon;

import org.json.JSONObject;

import com.google.refine.model.Recon;
import com.google.refine.model.recon.ReconConfig;

abstract public class StrictReconConfig extends ReconConfig {
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
    
    @Override
    public Recon createNewRecon(long historyEntryID) {
        return Recon.makeFreebaseRecon(historyEntryID);
    }
}
