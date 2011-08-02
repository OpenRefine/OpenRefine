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

package com.google.refine.operations.cell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.operations.EngineDependentMassCellOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;

public class MassEditOperation extends EngineDependentMassCellOperation {
    final protected String         _expression;
    final protected List<Edit>     _edits;
    
    static public class Edit implements Jsonizable {
        final public List<String>     from;
        final public boolean          fromBlank;
        final public boolean          fromError;
        final public Serializable     to;
        
        public Edit(List<String> from, boolean fromBlank, boolean fromError, Serializable to) {
            this.from = from;
            this.fromBlank = fromBlank;
            this.fromError = fromError;
            this.to = to;
        }
        
        @Override
        public void write(JSONWriter writer, Properties options)
            throws JSONException {
            
            writer.object();
            writer.key("fromBlank"); writer.value(fromBlank);
            writer.key("fromError"); writer.value(fromError);
            writer.key("from");
                writer.array();
                for (String s : from) {
                    writer.value(s);
                }
                writer.endArray();
            writer.key("to"); writer.value(to);
            writer.endObject();
        }
    }
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.has("engineConfig") && !obj.isNull("engineConfig") ?
                obj.getJSONObject("engineConfig") : null;
        
        return new MassEditOperation(
            engineConfig,
            obj.getString("columnName"),
            obj.getString("expression"),
            reconstructEdits(obj.getJSONArray("edits"))
        );
    }
    
    static public List<Edit> reconstructEdits(JSONArray editsA) throws Exception {
        int editCount = editsA.length();
        
        List<Edit> edits = new ArrayList<Edit>(editCount);
        for (int i = 0; i < editCount; i++) {
            JSONObject editO = editsA.getJSONObject(i);
            
            List<String> from = null;
            if (editO.has("from") && !editO.isNull("from")) {
                JSONArray fromA = editO.getJSONArray("from");
                int fromCount = fromA.length();
                
                from = new ArrayList<String>(fromCount);
                for (int j = 0; j < fromCount; j++) {
                    from.add(fromA.getString(j));
                }
            } else {
                from = new ArrayList<String>();
            }
            
            boolean fromBlank = editO.has("fromBlank") && editO.getBoolean("fromBlank");
            boolean fromError = editO.has("fromError") && editO.getBoolean("fromError");
            
            Serializable to = (Serializable) editO.get("to");
            if (editO.has("type")) {
                String type = editO.getString("type");
                if ("date".equals(type)) {
                    to = ParsingUtilities.stringToDate((String) to);
                }
            }

            edits.add(new Edit(from, fromBlank, fromError, to));
        }
        
        return edits;
    }
    
    public MassEditOperation(JSONObject engineConfig, String columnName, String expression, List<Edit> edits) {
        super(engineConfig, columnName, true);
        _expression = expression;
        _edits = edits;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("columnName"); writer.value(_columnName);
        writer.key("expression"); writer.value(_expression);
        writer.key("edits");
            writer.array();
            for (Edit edit : _edits) {
                edit.write(writer, options);
            }
            writer.endArray();
        writer.endObject();
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Mass edit cells in column " + _columnName;
    }

    @Override
    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        return "Mass edit " + cellChanges.size() + 
            " cells in column " + column.getName();
    }

    @Override
    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        
        Evaluable eval = MetaParser.parse(_expression);
        Properties bindings = ExpressionUtils.createBindings(project);
        
        Map<String, Serializable> fromTo = new HashMap<String, Serializable>();
        Serializable fromBlankTo = null;
        Serializable fromErrorTo = null;
        
        for (Edit edit : _edits) {
            for (String s : edit.from) {
                fromTo.put(s, edit.to);
            }
            
            // the last edit wins
            if (edit.fromBlank) {
                fromBlankTo = edit.to;
            }
            if (edit.fromError) {
                fromErrorTo = edit.to;
            }
        }
        
        return new RowVisitor() {
            int                         cellIndex;
            Properties                  bindings;
            List<CellChange>            cellChanges;
            Evaluable                   eval;
            
            Map<String, Serializable>   fromTo;
            Serializable                fromBlankTo;
            Serializable                fromErrorTo;
            
            public RowVisitor init(
                int cellIndex, 
                Properties bindings, 
                List<CellChange> cellChanges, 
                Evaluable eval, 
                Map<String, Serializable> fromTo,
                Serializable fromBlankTo,
                Serializable fromErrorTo
            ) {
                this.cellIndex = cellIndex;
                this.bindings = bindings;
                this.cellChanges = cellChanges;
                this.eval = eval;
                this.fromTo = fromTo;
                this.fromBlankTo = fromBlankTo;
                this.fromErrorTo = fromErrorTo;
                return this;
            }
            
            @Override
            public void start(Project project) {
                // nothing to do
            }

            @Override
            public void end(Project project) {
                // nothing to do
            }
            
            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                Cell cell = row.getCell(cellIndex);
                Cell newCell = null;
                
                ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
                
                Object v = eval.evaluate(bindings);
                if (ExpressionUtils.isError(v)) {
                    if (fromErrorTo != null) {
                        newCell = new Cell(fromErrorTo, (cell != null) ? cell.recon : null);
                    }
                } else if (ExpressionUtils.isNonBlankData(v)) {
                    String from = v.toString();
                    Serializable to = fromTo.get(from);
                    if (to != null) {
                        newCell = new Cell(to, (cell != null) ? cell.recon : null);
                    }
                } else {
                    if (fromBlankTo != null) {
                        newCell = new Cell(fromBlankTo, (cell != null) ? cell.recon : null);
                    }
                }
                
                if (newCell != null) {
                    CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
                    cellChanges.add(cellChange);
                }
                return false;
            }
        }.init(column.getCellIndex(), bindings, cellChanges, eval, fromTo, fromBlankTo, fromErrorTo);
    }
}
