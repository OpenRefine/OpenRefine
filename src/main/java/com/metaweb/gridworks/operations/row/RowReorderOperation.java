package com.metaweb.gridworks.operations.row;

 import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.RecordVisitor;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.browsing.Engine.Mode;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.RowReorderChange;
import com.metaweb.gridworks.operations.OperationRegistry;
import com.metaweb.gridworks.sorting.SortingRecordVisitor;
import com.metaweb.gridworks.sorting.SortingRowVisitor;

public class RowReorderOperation extends AbstractOperation {
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
    	String mode = obj.getString("mode");
        JSONObject sorting = obj.has("sorting") && !obj.isNull("sorting") ?
        		obj.getJSONObject("sorting") : null;
        
        return new RowReorderOperation(Engine.stringToMode(mode), sorting);
    }
    
    final protected Mode	   _mode;
    final protected JSONObject _sorting;
    
    public RowReorderOperation(Mode mode, JSONObject sorting) {
    	_mode = mode;
        _sorting = sorting;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("mode"); writer.value(Engine.modeToString(_mode));
        writer.key("sorting"); writer.value(_sorting);
        writer.endObject();
    }

    protected String getBriefDescription(Project project) {
        return "Reorder rows";
    }

   protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = new Engine(project);
        engine.setMode(_mode);
        
        List<Integer> rowIndices = new ArrayList<Integer>();
        if (_mode == Mode.RowBased) {
            RowVisitor visitor = new IndexingVisitor(rowIndices);
            if (_sorting != null) {
            	SortingRowVisitor srv = new SortingRowVisitor(visitor);
            	
            	srv.initializeFromJSON(project, _sorting);
            	visitor = srv;
            }
            
        	engine.getAllRows().accept(project, visitor);
        } else {
            RecordVisitor visitor = new IndexingVisitor(rowIndices);
            if (_sorting != null) {
            	SortingRecordVisitor srv = new SortingRecordVisitor(visitor);
            	
            	srv.initializeFromJSON(project, _sorting);
            	visitor = srv;
            }
            
        	engine.getAllRecords().accept(project, visitor);
        }
        
        return new HistoryEntry(
            historyEntryID,
            project, 
            "Reorder rows", 
            this, 
            new RowReorderChange(rowIndices)
        );
    }
   
    static protected class IndexingVisitor implements RowVisitor, RecordVisitor {
    	List<Integer> _indices;
    	
    	IndexingVisitor(List<Integer> indices) {
    		_indices = indices;
    	}
    	
		@Override
		public void start(Project project) {
		}

		@Override
		public void end(Project project) {
		}

		@Override
		public boolean visit(Project project, int rowIndex, Row row) {
			_indices.add(rowIndex);
			return false;
		}

		@Override
		public boolean visit(Project project, Record record) {
			for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
				_indices.add(r);
			}
			return false;
		}
    }
}
