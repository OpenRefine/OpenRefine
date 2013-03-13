/*

Copyright 2010,2013 Google Inc. and other contributors
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.freebase.model.recon;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.freebase.util.FreebaseUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.Row;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.ReconJob;
import com.google.refine.util.ParsingUtilities;

public class IdBasedReconConfig extends StrictReconConfig {
    static public ReconConfig reconstruct(JSONObject obj) throws Exception {
        return new IdBasedReconConfig();
    }
    
    public IdBasedReconConfig() {
    }

    static protected class IdBasedReconJob extends ReconJob {
        String id;
        
        @Override
        public String getStringKey() {
            return id;
        }
    }

    @Override
    public ReconJob createJob(Project project, int rowIndex, Row row,
            String columnName, Cell cell) {
        
        IdBasedReconJob job = new IdBasedReconJob();
        String s = cell.value.toString();
        
        if (!s.startsWith("/")) {
            if (s.startsWith("92")) {
                s = "/guid/" + s;
            } else if (!s.contains("/")){
                s = "/en/" + s;
            } else {
                s = "/" + s;
            }
        }
        
        job.id = s;
        
        return job;
    }

    @Override
    public int getBatchSize() {
        return 40;
    }

    @Override
    public String getBriefDescription(Project project, String columnName) {
        return "Reconcile cells in column " + columnName + " as Freebase IDs";
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("mode"); writer.value("strict");
        writer.key("match"); writer.value("id"); 
        writer.endObject();
    }
    
    @Override
    public List<Recon> batchRecon(List<ReconJob> jobs, long historyEntryID) {
        List<Recon> recons = new ArrayList<Recon>(jobs.size());
        Map<String, Recon> idToRecon = new HashMap<String, Recon>();
        
        try {
            String query = buildQuery(jobs);
            String s = FreebaseUtils.mqlread(query);

            JSONObject o = ParsingUtilities.evaluateJsonStringToObject(s);
            if (o.has("result")) {
                JSONArray results = o.getJSONArray("result");
                int count = results.length();

                for (int i = 0; i < count; i++) {
                    JSONObject result = results.getJSONObject(i);

                    String id = result.getString("id");

                    JSONArray types = result.getJSONArray("type");
                    String[] typeIDs = new String[types.length()];
                    for (int j = 0; j < typeIDs.length; j++) {
                        typeIDs[j] = types.getString(j);
                    }

                    ReconCandidate candidate = new ReconCandidate(
                            id,
                            result.getString("name"),
                            typeIDs,
                            100
                            );

                    Recon recon = Recon.makeFreebaseRecon(historyEntryID);
                    recon.addCandidate(candidate);
                    recon.service = "mql";
                    recon.judgment = Judgment.Matched;
                    recon.judgmentAction = "auto";
                    recon.match = candidate;
                    recon.matchRank = 0;

                    idToRecon.put(id, recon);
                }
            }
        } catch (IOException e) {
            LOGGER.error("IOException during recon : ",e);
        } catch (JSONException e) {
            LOGGER.error("JSONException during recon : ",e);
        }

        for (ReconJob job : jobs) {
            String id = ((IdBasedReconJob) job).id;
            Recon recon = idToRecon.get(id);
            if (recon == null) {
                recon = createNoMatchRecon(historyEntryID);
            }
            recons.add(recon);
        }
        
        return recons;
    }

    private String buildQuery(List<ReconJob> jobs)
            throws JSONException {
        String query = null;
        {
            StringWriter stringWriter = new StringWriter();
            JSONWriter jsonWriter = new JSONWriter(stringWriter);
            
                jsonWriter.array();
                jsonWriter.object();
                
                    jsonWriter.key("id"); jsonWriter.value(null);
                    jsonWriter.key("name"); jsonWriter.value(null);
                    jsonWriter.key("guid"); jsonWriter.value(null);
                    jsonWriter.key("type"); jsonWriter.array(); jsonWriter.endArray();
                    
                    jsonWriter.key("id|=");
                        jsonWriter.array();
                        for (ReconJob job : jobs) {
                            jsonWriter.value(((IdBasedReconJob) job).id);
                        }
                        jsonWriter.endArray();
                    
                jsonWriter.endObject();
                jsonWriter.endArray();
            
            query = stringWriter.toString();
        }
        return query;
    }

}
