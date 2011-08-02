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

package com.google.refine.freebase.operations;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.freebase.commands.UploadDataCommand;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;
import com.google.refine.model.changes.MassReconChange;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;

public class ImportQADataOperation extends AbstractOperation {
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        return new ImportQADataOperation();
    }
    
    public ImportQADataOperation() {
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.endObject();
    }
    
    @Override
    protected String getBriefDescription(Project project) {
        return "Import QA DAta";
    }
    
    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Integer jobID = (Integer) project.getMetadata().getPreferenceStore().get(UploadDataCommand.s_dataLoadJobIDPref);
        if (jobID == null) {
            throw new InternalError("Project is not associated with any data loading job.");
        }

        Map<Long, String> reconIDToResult = new HashMap<Long, String>();
        
        URL url = new URL("http://refinery.freebaseapps.com/get_answers/" + jobID);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(30000); // 30 seconds
        
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(conn.getInputStream()));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject obj = ParsingUtilities.evaluateJsonStringToObject(line);
                long reconID = Long.parseLong(obj.getString("recon_id").substring(3));
                
                reconIDToResult.put(reconID, obj.getString("result"));
            }
        } finally {
            reader.close();
        }
        
        Map<Long, Recon> oldRecons = new HashMap<Long, Recon>();
        Map<Long, Recon> newRecons = new HashMap<Long, Recon>();
        
        for (int r = 0; r < project.rows.size(); r++) {
            Row row = project.rows.get(r);
            
            for (int c = 0; c < row.cells.size(); c++) {
                Cell cell = row.cells.get(c);
                if (cell != null && cell.recon != null) {
                    Recon oldRecon = cell.recon;
                    
                    if (reconIDToResult.containsKey(oldRecon.id)) {
                        Recon newRecon = oldRecon.dup();
                        newRecon.setFeature(Recon.Feature_qaResult, reconIDToResult.get(oldRecon.id));
                        
                        reconIDToResult.remove(oldRecon.id);
                        
                        oldRecons.put(oldRecon.id, oldRecon);
                        newRecons.put(oldRecon.id, newRecon);
                    }
                }
            }
        }

        return new HistoryEntry(
            historyEntryID, 
            project, 
            getBriefDescription(project), 
            this,
            new MassReconChange(newRecons, oldRecons)
        );
    }
}
