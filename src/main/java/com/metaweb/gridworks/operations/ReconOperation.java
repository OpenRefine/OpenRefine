package com.metaweb.gridworks.operations;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.ReconConfig;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.model.changes.CellChange;
import com.metaweb.gridworks.model.changes.MassCellChange;
import com.metaweb.gridworks.process.LongRunningProcess;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.util.ParsingUtilities;

public class ReconOperation extends EngineDependentOperation {
	private static final long serialVersionUID = 838795186905314865L;
	
	final protected String		_columnName;
	final protected String 		_typeID;
	final protected String 		_typeName;
	final protected boolean     _autoMatch;
	final protected double      _minScore;
	
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new ReconOperation(
            engineConfig, 
            obj.getString("columnName"),
            obj.getString("typeID"),
            obj.getString("typeName"),
            obj.getBoolean("autoMatch"),
            obj.getDouble("minScore")
        );
    }
    
	public ReconOperation(
        JSONObject engineConfig, 
        String columnName, 
        String typeID, 
        String typeName, 
        boolean autoMatch, 
        double minScore
    ) {
		super(engineConfig);
		_columnName = columnName;
		_typeID = typeID;
		_typeName = typeName;
		_autoMatch = autoMatch;
		_minScore = minScore;
	}

	public Process createProcess(Project project, Properties options) throws Exception {
		return new ReconProcess(
			project, 
			getEngineConfig(),
			getBriefDescription()
		);
	}
	
	protected String getBriefDescription() {
        return "Reconcile cells in column " + _columnName + " to type " + _typeID;
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
		writer.key("description"); writer.value(getBriefDescription());
		writer.key("columnName"); writer.value(_columnName);
		writer.key("typeID"); writer.value(_typeID);
		writer.key("typeName"); writer.value(_typeName);
        writer.key("autoMatch"); writer.value(_autoMatch);
        writer.key("minScore"); writer.value(_minScore);
		writer.key("engineConfig"); writer.value(getEngineConfig());
		writer.endObject();
	}

	static protected class ReconEntry {
		final public int rowIndex;
		final public Cell cell;
		
		public ReconEntry(int rowIndex, Cell cell) {
			this.rowIndex = rowIndex;
			this.cell = cell;
		}
	}
	
	static protected class ReconChange extends MassCellChange {
        private static final long serialVersionUID = 7048806528587330543L;
        
        final protected ReconConfig _newReconConfig;
        protected ReconConfig _oldReconConfig;
        
        public ReconChange(
            List<CellChange> cellChanges,
            String commonColumnName,
            ReconConfig newReconConfig
        ) {
            super(cellChanges, commonColumnName, false);
            _newReconConfig = newReconConfig;
        }
	    @Override
	    public void apply(Project project) {
	        synchronized (project) {
	            super.apply(project);
	            
	            Column column = project.columnModel.getColumnByName(_commonColumnName);
	            _oldReconConfig = column.getReconConfig();
	            column.setReconConfig(_newReconConfig);
	        }
	    }
	    
	    @Override
	    public void revert(Project project) {
            synchronized (project) {
                super.revert(project);
                
                project.columnModel.getColumnByName(_commonColumnName)
                	.setReconConfig(_oldReconConfig);
            }
	    }
	}
	
	public class ReconProcess extends LongRunningProcess implements Runnable {
		final protected Project		_project;
		final protected JSONObject	_engineConfig;
		protected List<ReconEntry> 	_entries;
		protected int				_cellIndex;
		
		public ReconProcess(
			Project project, 
			JSONObject engineConfig, 
			String description
		) {
			super(description);
			_project = project;
			_engineConfig = engineConfig;
		}
		
		protected Runnable getRunnable() {
			return this;
		}
		
		protected void populateEntries() throws Exception {
			Engine engine = new Engine(_project);
			engine.initializeFromJSON(_engineConfig);
			
			Column column = _project.columnModel.getColumnByName(_columnName);
			if (column == null) {
				throw new Exception("No column named " + _columnName);
			}
			
			_entries = new ArrayList<ReconEntry>(_project.rows.size());
			_cellIndex = column.getCellIndex();
			
			FilteredRows filteredRows = engine.getAllFilteredRows(false);
			filteredRows.accept(_project, new RowVisitor() {
				public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
					if (_cellIndex < row.cells.size()) {
						Cell cell = row.cells.get(_cellIndex);
						if (cell != null && !ExpressionUtils.isBlank(cell.value)) {
							_entries.add(new ReconEntry(rowIndex, cell));
						}
					}
					return false;
				}
			});
		}
		
		public void run() {
			try {
				populateEntries();
			} catch (Exception e2) {
				// TODO : Not sure what to do here?
				e2.printStackTrace();
			}
			
			Map<String, List<ReconEntry>> valueToEntries = new HashMap<String, List<ReconEntry>>();
			
			for (ReconEntry entry : _entries) {
				Object value = entry.cell.value;
				if (value != null && value instanceof String) {
					List<ReconEntry> entries2;
					if (valueToEntries.containsKey(value)) {
						entries2 = valueToEntries.get(value);
					} else {
						entries2 = new LinkedList<ReconEntry>();
						valueToEntries.put((String) value, entries2);
					}
					entries2.add(entry);
				}
			}
			
			List<CellChange> cellChanges = new ArrayList<CellChange>(_entries.size());
			List<String> values = new ArrayList<String>(valueToEntries.keySet());
			for (int i = 0; i < values.size(); i += 10) {
				recon(valueToEntries, values, i, Math.min(i + 10, values.size()), cellChanges);
				
				_progress = i * 100 / values.size();
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					if (_canceled) {
						break;
					}
				}
			}
			
			ReconConfig reconConfig = new ReconConfig(_typeID, _typeName);
			
			Change reconChange = new ReconChange(cellChanges, _columnName, reconConfig);
			
			HistoryEntry historyEntry = new HistoryEntry(
				_project, 
				_description, 
				ReconOperation.this, 
				reconChange
			);
			
			_project.history.addEntry(historyEntry);
			_project.processManager.onDoneProcess(this);
		}
		
		protected void recon(
			Map<String, List<ReconEntry>> valueToEntries, 
			List<String> values, 
			int from, 
			int to,
			List<CellChange> cellChanges
		) {
			try {
				StringWriter stringWriter = new StringWriter();
				JSONWriter jsonWriter = new JSONWriter(stringWriter);
				
				jsonWriter.object();
				for (int i = 0; from + i < to; i++) {
					jsonWriter.key("q" + i + ":search");
					
					jsonWriter.object();
					
					jsonWriter.key("query"); jsonWriter.value(values.get(from + i));
					jsonWriter.key("limit"); jsonWriter.value(5);
					jsonWriter.key("type"); jsonWriter.value(_typeID);
					jsonWriter.key("type_strict"); jsonWriter.value("should");
					jsonWriter.key("indent"); jsonWriter.value(1);
					jsonWriter.key("type_exclude"); jsonWriter.value("/common/image");
					jsonWriter.key("domain_exclude"); jsonWriter.value("/freebase");
					
					jsonWriter.endObject();
				}
				jsonWriter.endObject();
				
				StringBuffer sb = new StringBuffer();
				sb.append("http://api.freebase.com/api/service/search?indent=1&queries=");
				sb.append(ParsingUtilities.encode(stringWriter.toString()));
				
				URL url = new URL(sb.toString());
				URLConnection connection = url.openConnection();
				connection.setConnectTimeout(5000);
				connection.connect();
				
				InputStream is = connection.getInputStream();
				try {
					String s = ParsingUtilities.inputStreamToString(is);
					JSONObject o = ParsingUtilities.evaluateJsonStringToObject(s);
					
					for (int i = 0; from + i < to; i++) {
						String value = values.get(from + i);
						String key = "q" + i + ":search";
						if (!o.has(key)) {
							continue;
						}
						
						Recon recon;
						
						JSONObject o2 = o.getJSONObject(key);
						if (o2.has("result")) {
							JSONArray results = o2.getJSONArray("result");
							
							recon = createRecon(value, results);
						} else {
							recon = new Recon();
						}
						
						for (ReconEntry entry : valueToEntries.get(value)) {
							Cell oldCell = entry.cell;
							
							Cell newCell = new Cell(oldCell.value, recon);
							
							CellChange cellChange = new CellChange(
								entry.rowIndex, 
								_cellIndex, 
								oldCell, 
								newCell
							);
							cellChanges.add(cellChange);
						}
						
						valueToEntries.remove(value);
					}
				} finally {
					is.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			for (List<ReconEntry> entries : valueToEntries.values()) {
				Recon recon = new Recon();
				
				for (ReconEntry entry : entries) {
					Cell oldCell = entry.cell;
					Cell newCell = new Cell(oldCell.value, recon);
					
					CellChange cellChange = new CellChange(
						entry.rowIndex, 
						_cellIndex, 
						oldCell, 
						newCell
					);
					cellChanges.add(cellChange);
				}
			}
		}
	
		protected Recon createRecon(String text, JSONArray results) {
			Recon recon = new Recon();
			try {
				int length = results.length();
				for (int i = 0; i < length && recon.candidates.size() < 3; i++) {
					JSONObject result = results.getJSONObject(i);
					if (!result.has("name")) {
						continue;
					}
					
					JSONArray types = result.getJSONArray("type");
					String[] typeIDs = new String[types.length()];
					for (int j = 0; j < typeIDs.length; j++) {
						typeIDs[j] = types.getJSONObject(j).getString("id");
					}
					
					double score = result.getDouble("relevance:score");
					ReconCandidate candidate = new ReconCandidate(
						result.getString("id"),
						result.getString("guid"),
						result.getString("name"),
						typeIDs,
						score
					);
					
					// best match
					if (i == 0) {
						recon.setFeature(Recon.Feature_nameMatch, text.equalsIgnoreCase(candidate.topicName));
						recon.setFeature(Recon.Feature_nameLevenshtein, StringUtils.getLevenshteinDistance(text, candidate.topicName));
						recon.setFeature(Recon.Feature_nameWordDistance, wordDistance(text, candidate.topicName));
						
						recon.setFeature(Recon.Feature_typeMatch, false);
						for (String typeID : candidate.typeIDs) {
							if (_typeID.equals(typeID)) {
								recon.setFeature(Recon.Feature_typeMatch, true);
		                        if (_autoMatch && score >= _minScore) {
		                            recon.match = candidate;
		                            recon.judgment = Judgment.Matched;
		                        }
								break;
							}
						}
					}
					
					recon.candidates.add(candidate);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return recon;
		}
	}
	
	static protected double wordDistance(String s1, String s2) {
		Set<String> words1 = breakWords(s1);
		Set<String> words2 = breakWords(s2);
		return words1.size() >= words2.size() ? wordDistance(words1, words2) : wordDistance(words2, words1);
	}
	
	static protected double wordDistance(Set<String> longWords, Set<String> shortWords) {
		double common = 0;
		for (String word : shortWords) {
			if (longWords.contains(word)) {
				common++;
			}
		}
		return common / longWords.size();
	}
	
	static protected Set<String> s_stopWords;
	static {
		s_stopWords = new HashSet<String>();
		s_stopWords.add("the");
		s_stopWords.add("a");
		s_stopWords.add("and");
		s_stopWords.add("of");
		s_stopWords.add("on");
		s_stopWords.add("in");
		s_stopWords.add("at");
		s_stopWords.add("by");
	}
	
	static protected Set<String> breakWords(String s) {
		String[] words = s.toLowerCase().split("\\s+");
		
		Set<String> set = new HashSet<String>(words.length);
		for (String word : words) {
			if (!s_stopWords.contains(word)) {
				set.add(word);
			}
		}
		return set;
	}
}
