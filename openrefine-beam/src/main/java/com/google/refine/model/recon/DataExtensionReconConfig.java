/*

Copyright 2010, Google Inc.
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

package com.google.refine.model.recon;

import java.util.List;
import java.util.Properties;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.model.ReconType;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.model.recon.ReconJob;

public class DataExtensionReconConfig extends StandardReconConfig {
    final public ReconType type;
    
    private final static String WARN = "Not implemented";
    
    static public ReconConfig reconstruct(JSONObject obj) throws Exception {
        JSONObject type = obj.getJSONObject("type");
        
        ReconType typ = null;
        if(obj.has("id")) {
            typ = new ReconType(obj.getString("id"),
                        obj.has("name") ? obj.getString("name") : obj.getString("id"));
        }

        return new DataExtensionReconConfig(
            obj.getString("service"),
            obj.has("identifierSpace") ? obj.getString("identifierSpace") : null,
            obj.has("schemaSpace") ? obj.getString("schemaSpace") : null,
            typ);
    }
    
    public DataExtensionReconConfig(
        String service,
        String identifierSpace,
        String schemaSpace,
        ReconType type) {
        super(
            service,
            identifierSpace,
            schemaSpace,
            type != null ? type.id : null, 
            type != null ? type.name : null,
            true,
            new ArrayList<ColumnDetail>());
        this.type = type;
    }

    @Override
    public ReconJob createJob(Project project, int rowIndex, Row row,
            String columnName, Cell cell) {
        throw new RuntimeException(WARN);
    }

    @Override
    public int getBatchSize() {
        throw new RuntimeException(WARN);
    }
   
    @Override
    public List<Recon> batchRecon(List<ReconJob> jobs, long historyEntryID) {
        throw new RuntimeException(WARN);
    }

    @Override
    public String getBriefDescription(Project project, String columnName) {
        throw new RuntimeException(WARN);
    }
}
