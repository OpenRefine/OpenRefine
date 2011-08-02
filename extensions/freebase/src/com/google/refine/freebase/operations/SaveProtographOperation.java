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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.freebase.protograph.Protograph;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class SaveProtographOperation extends AbstractOperation {
    final protected Protograph _protograph;
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        return new SaveProtographOperation(
            Protograph.reconstruct(obj.getJSONObject("protograph"))
        );
    }
    
    public SaveProtographOperation(Protograph protograph) {
        _protograph = protograph;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("protograph"); _protograph.write(writer, options);
        writer.endObject();
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Save schema alignment skeleton";
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Change change = new ProtographChange(_protograph);
        
        return new HistoryEntry(historyEntryID, project, getBriefDescription(project), SaveProtographOperation.this, change);
    }
    
    static public class ProtographChange implements Change {
        final protected Protograph _newProtograph;
        protected Protograph _oldProtograph;
        
        public ProtographChange(Protograph protograph) {
            _newProtograph = protograph;
        }
        
        @Override
        public void apply(Project project) {
            synchronized (project) {
                _oldProtograph = (Protograph) project.overlayModels.get("freebaseProtograph");
                
                project.overlayModels.put("freebaseProtograph", _newProtograph);
            }
        }
        
        @Override
        public void revert(Project project) {
            synchronized (project) {
                if (_oldProtograph == null) {
                    project.overlayModels.remove("freebaseProtograph");
                } else {
                    project.overlayModels.put("freebaseProtograph", _oldProtograph);
                }
            }
        }
        
        @Override
        public void save(Writer writer, Properties options) throws IOException {
            writer.write("newProtograph="); writeProtograph(_newProtograph, writer); writer.write('\n');
            writer.write("oldProtograph="); writeProtograph(_oldProtograph, writer); writer.write('\n');
            writer.write("/ec/\n"); // end of change marker
        }
        
        static public Change load(LineNumberReader reader, Pool pool) throws Exception {
            Protograph oldProtograph = null;
            Protograph newProtograph = null;
            
            String line;
            while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
                int equal = line.indexOf('=');
                CharSequence field = line.subSequence(0, equal);
                String value = line.substring(equal + 1);
                
                if ("oldProtograph".equals(field) && value.length() > 0) {
                    oldProtograph = Protograph.reconstruct(ParsingUtilities.evaluateJsonStringToObject(value));
                } else if ("newProtograph".equals(field) && value.length() > 0) {
                    newProtograph = Protograph.reconstruct(ParsingUtilities.evaluateJsonStringToObject(value));
                }
            }
            
            ProtographChange change = new ProtographChange(newProtograph);
            change._oldProtograph = oldProtograph;
            
            return change;
        }
        
        static protected void writeProtograph(Protograph p, Writer writer) throws IOException {
            if (p != null) {
                JSONWriter jsonWriter = new JSONWriter(writer);
                try {
                    p.write(jsonWriter, new Properties());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    } 
}
