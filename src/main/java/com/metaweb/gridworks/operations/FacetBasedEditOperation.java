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

public class FacetBasedEditOperation extends EngineDependentMassCellOperation {
    private static final long serialVersionUID = -7698202759999537298L;

    final protected String         _expression;
    final protected List<Edit>     _edits;
    
    static public class Edit implements Serializable, Jsonizable {
        private static final long serialVersionUID = -4799990738910328002L;
        
        final public List<String>     from;
        final public Serializable     to;
        
        public Edit(List<String> from, Serializable to) {
            this.from = from;
            this.to = to;
        }
        
        public void write(JSONWriter writer, Properties options)
            throws JSONException {
            
            writer.object();
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
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new FacetBasedEditOperation(
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
            
            JSONArray fromA = editO.getJSONArray("from");
            int fromCount = fromA.length();
            
            List<String> from = new ArrayList<String>(fromCount);
            for (int j = 0; j < fromCount; j++) {
                from.add(fromA.getString(j));
            }
        
            edits.add(new Edit(from, editO.getString("to")));
        }
        
        return edits;
    }
    
    public FacetBasedEditOperation(JSONObject engineConfig, String columnName, String expression, List<Edit> edits) {
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
        return "Facet-based edit cells in column " + _columnName;
    }

    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        return "Facet-based edit " + cellChanges.size() + 
            " cells in column " + column.getHeaderLabel();
    }

    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        
        Evaluable eval = MetaParser.parse(_expression);
        Properties bindings = ExpressionUtils.createBindings(project);
        
        Map<String, Serializable> fromTo = new HashMap<String, Serializable>();
        for (Edit edit : _edits) {
            for (String s : edit.from) {
                fromTo.put(s, edit.to);
            }
        }
        
        return new RowVisitor() {
            int                         cellIndex;
            Properties                  bindings;
            List<CellChange>            cellChanges;
            Evaluable                   eval;
            Map<String, Serializable>   fromTo;
            
            public RowVisitor init(
                int cellIndex, 
                Properties bindings, 
                List<CellChange> cellChanges, 
                Evaluable eval, 
                Map<String, Serializable> fromTo
            ) {
                this.cellIndex = cellIndex;
                this.bindings = bindings;
                this.cellChanges = cellChanges;
                this.eval = eval;
                this.fromTo = fromTo;
                return this;
            }
            
            public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
                Cell cell = row.getCell(cellIndex);

                ExpressionUtils.bind(bindings, row, rowIndex, cell);
                
                Object v = eval.evaluate(bindings);
                if (v != null) {
                    String from = v.toString();
                    Serializable to = fromTo.get(from);
                    if (to != null) {
                        Cell newCell = new Cell(to, (cell != null) ? cell.recon : null);
                        CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
                        cellChanges.add(cellChange);
                    }
                }
                
                return false;
            }
        }.init(column.getCellIndex(), bindings, cellChanges, eval, fromTo);
    }
}
