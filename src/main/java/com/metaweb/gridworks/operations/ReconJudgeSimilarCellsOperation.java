package com.metaweb.gridworks.operations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.model.changes.CellChange;

public class ReconJudgeSimilarCellsOperation extends EngineDependentMassCellOperation {
	private static final long serialVersionUID = -5205694623711144436L;
	
	final protected String 			_similarValue;
	final protected Judgment 		_judgment;
	final protected ReconCandidate 	_match;
	final protected boolean			_shareNewTopics;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        ReconCandidate match = null;
        if (obj.has("match")) {
	        JSONObject matchObj = obj.getJSONObject("match");
	        
	        JSONArray types = matchObj.getJSONArray("types");
	        String[] typeIDs = new String[types.length()];
	        for (int i = 0; i < typeIDs.length; i++) {
	            typeIDs[i] = types.getString(i);
	        }
	        
	        match = new ReconCandidate(
                matchObj.getString("id"),
                matchObj.getString("guid"),
                matchObj.getString("name"),
                typeIDs,
                matchObj.getDouble("score")
            );
        }
        
        Judgment judgment = Judgment.None;
        if (obj.has("judgment")) {
        	judgment = Recon.stringToJudgment(obj.getString("judgment"));
        }
        
        return new ReconJudgeSimilarCellsOperation(
            engineConfig,
            obj.getString("columnName"),
            obj.getString("similarValue"),
            judgment,
            match,
            obj.has("shareNewTopics") ? obj.getBoolean("shareNewTopics") : false
        );
    }
    
	public ReconJudgeSimilarCellsOperation(
		JSONObject 		engineConfig, 
		String 			columnName, 
		String 			similarValue,
		Judgment		judgment,
		ReconCandidate 	match,
		boolean			shareNewTopics
	) {
		super(engineConfig, columnName, false);
		this._similarValue = similarValue;
		this._judgment = judgment;
		this._match = match;
		this._shareNewTopics = shareNewTopics;
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
		writer.key("description"); writer.value(getBriefDescription());
		writer.key("engineConfig"); writer.value(getEngineConfig());
		writer.key("columnName"); writer.value(_columnName);
		writer.key("similarValue"); writer.value(_similarValue);
		writer.key("judgment"); writer.value(Recon.judgmentToString(_judgment));
		if (_match != null) {
			writer.key("match"); _match.write(writer, options);
		}
		writer.key("shareNewTopics"); writer.value(_shareNewTopics);
		
		writer.endObject();
	}
	
	protected String getBriefDescription() {
		if (_judgment == Judgment.None) {
			return "Discard recon judgments for cells containing \"" +
				_similarValue + "\" in column " + _columnName;
		} else if (_judgment == Judgment.New) {
			if (_shareNewTopics) {
				return "Mark to create one single new topic for all cells containing \"" +
					_similarValue + "\" in column " + _columnName;
			} else {
				return "Mark to create one new topic for each cell containing \"" +
					_similarValue + "\" in column " + _columnName;
			}
		} else if (_judgment == Judgment.Matched) {
			return "Match topic " + 
				_match.topicName +  " (" +
				_match.topicID + ") for cells containing \"" +
				_similarValue + "\" in column " + _columnName;
		}
		throw new InternalError("Can't get here");
	}

	protected String createDescription(Column column,
			List<CellChange> cellChanges) {
		
		if (_judgment == Judgment.None) {
			return "Discard recon judgments for " + cellChanges.size() + " cells containing \"" +
				_similarValue + "\" in column " + _columnName;
		} else if (_judgment == Judgment.New) {
			if (_shareNewTopics) {
				return "Mark to create one single new topic for " + cellChanges.size() + " cells containing \"" +
					_similarValue + "\" in column " + _columnName;
			} else {
				return "Mark to create one new topic for each of " + cellChanges.size() + " cells containing \"" +
					_similarValue + "\" in column " + _columnName;
			}
		} else if (_judgment == Judgment.Matched) {
			return "Match topic " + 
				_match.topicName + " (" +
				_match.topicID + ") for " +
				cellChanges.size() + " cells containing \"" +
				_similarValue + "\" in column " + _columnName;
		}
		throw new InternalError("Can't get here");
	}

	protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges) throws Exception {
		Column column = project.columnModel.getColumnByName(_columnName);
		
		return new RowVisitor() {
			int 				_cellIndex;
			List<CellChange> 	_cellChanges;
			Map<String, Recon>  _sharedRecons = new HashMap<String, Recon>();
			
			public RowVisitor init(int cellIndex, List<CellChange> cellChanges) {
				_cellIndex = cellIndex;
				_cellChanges = cellChanges;
				return this;
			}
			
			public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
                Cell cell = row.getCell(_cellIndex);
                if (cell != null && 
                	!ExpressionUtils.isBlank(cell.value) && 
                	_similarValue.equals(cell.value)) {
                	
                	Recon recon = null;
                	if (_judgment == Judgment.New && _shareNewTopics) {
                		String s = cell.value.toString();
                		if (_sharedRecons.containsKey(s)) {
                			recon = _sharedRecons.get(s);
                		} else {
                			recon = new Recon();
                			recon.judgment = Judgment.New;
                			
                			_sharedRecons.put(s, recon);
                		}
                	} else {
                		recon = cell.recon == null ? new Recon() : cell.recon.dup();
                        if (_judgment == Judgment.Matched) {
                            recon.judgment = Recon.Judgment.Matched;
                            recon.match = _match;
                        } else if (_judgment == Judgment.New) {
                            recon.judgment = Recon.Judgment.New;
                            recon.match = null;
                        } else if (_judgment == Judgment.None) {
                            recon.judgment = Recon.Judgment.None;
                            recon.match = null;
                        }
                	}
                	
                    Cell newCell = new Cell(cell.value, recon);
                    
                    CellChange cellChange = new CellChange(rowIndex, _cellIndex, cell, newCell);
                    _cellChanges.add(cellChange);
                }
                return false;
			}
		}.init(column.getCellIndex(), cellChanges);
	}
}
