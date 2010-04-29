package com.metaweb.gridworks.operations;

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

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.CellChange;
import com.metaweb.gridworks.util.ParsingUtilities;

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

    protected String getBriefDescription(Project project) {
        return "Mass edit cells in column " + _columnName;
    }

    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        return "Mass edit " + cellChanges.size() + 
            " cells in column " + column.getName();
    }

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
            
            public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
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
