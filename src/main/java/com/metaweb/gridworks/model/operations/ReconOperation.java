package com.metaweb.gridworks.model.operations;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
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
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.ReconConfig;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.CellChange;
import com.metaweb.gridworks.model.changes.MassCellChange;
import com.metaweb.gridworks.process.LongRunningProcess;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.util.ParsingUtilities;

public class ReconOperation extends EngineDependentOperation {
	private static final long serialVersionUID = 838795186905314865L;
	
	final protected int			_cellIndex;
	final protected String 		_typeID;
	
	public ReconOperation(JSONObject engineConfig, int cellIndex, String typeID) {
		super(engineConfig);
		_cellIndex = cellIndex;
		_typeID = typeID;
	}

	public Process createProcess(Project project, Properties options) throws Exception {
		Engine engine = createEngine(project);
		
		Column column = project.columnModel.getColumnByCellIndex(_cellIndex);
		if (column == null) {
			throw new Exception("No column corresponding to cell index " + _cellIndex);
		}
		
		List<ReconEntry> entries = new ArrayList<ReconEntry>(project.rows.size());
		
		FilteredRows filteredRows = engine.getAllFilteredRows(false);
		filteredRows.accept(project, new RowVisitor() {
			int cellIndex;
			List<ReconEntry> entries;
			
			public RowVisitor init(int cellIndex, List<ReconEntry> entries) {
				this.cellIndex = cellIndex;
				this.entries = entries;
				return this;
			}
			
			public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
				if (cellIndex < row.cells.size()) {
					Cell cell = row.cells.get(cellIndex);
					if (cell != null && !ExpressionUtils.isBlank(cell.value)) {
						entries.add(new ReconEntry(rowIndex, cell));
					}
				}
				return false;
			}
		}.init(_cellIndex, entries));
		
		String description = 
			"Reconcile " + entries.size() + 
			" cells in column " + column.getHeaderLabel() + 
			" to type " + _typeID;
		
		return new ReconProcess(project, description, entries);
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub
		
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
            int commonCellIndex,
            ReconConfig newReconConfig
        ) {
            super(cellChanges, commonCellIndex, false);
            _newReconConfig = newReconConfig;
        }
	    @Override
	    public void apply(Project project) {
	        synchronized (project) {
	            super.apply(project);
	            
	            Column column = project.columnModel.getColumnByCellIndex(_commonCellIndex);
	            _oldReconConfig = column.getReconConfig();
	            column.setReconConfig(_newReconConfig);
	        }
	    }
	    
	    @Override
	    public void revert(Project project) {
            synchronized (project) {
                super.revert(project);
                
                project.columnModel.getColumnByCellIndex(_commonCellIndex).setReconConfig(_oldReconConfig);
            }
	    }
	}
	
	public class ReconProcess extends LongRunningProcess implements Runnable {
		final protected Project				_project;
		final protected List<ReconEntry> 	_entries;
		
		public ReconProcess(Project project, String description, List<ReconEntry> entries) {
			super(description);
			_project = project;
			_entries = entries;
		}
		
		protected Runnable getRunnable() {
			return this;
		}
		
		public void run() {
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
				try {
					recon(valueToEntries, values, i, Math.min(i + 10, values.size()), cellChanges);
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
				_progress = i * 100 / values.size();
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					if (_canceled) {
						break;
					}
				}
			}
			
			ReconConfig reconConfig = new ReconConfig(_typeID);
			
			Change reconChange = new ReconChange(cellChanges, _cellIndex, reconConfig);
			
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
		) throws JSONException {
			
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
			
			try {
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
						
						JSONObject o2 = o.getJSONObject(key);
						if (!(o2.has("result"))) {
							continue;
						}
						
						JSONArray results = o2.getJSONArray("result");
						
						Recon recon = createRecon(value, results);
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
					}
				} finally {
					is.close();
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
		protected Recon createRecon(String text, JSONArray results) throws JSONException {
			Recon recon = new Recon();
			
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
				
				ReconCandidate candidate = new ReconCandidate(
					result.getString("id"),
					result.getString("guid"),
					result.getString("name"),
					typeIDs,
					result.getDouble("relevance:score")
				);
				
				// best match
				if (i == 0) {
					recon.features.put("nameMatch", text.equalsIgnoreCase(candidate.topicName));
					recon.features.put("nameLevenshtein", StringUtils.getLevenshteinDistance(text, candidate.topicName));
					recon.features.put("nameWordDistance", wordDistance(text, candidate.topicName));
					
					recon.features.put("typeMatch", false);
					for (String typeID : candidate.typeIDs) {
						if (_typeID.equals(typeID)) {
							recon.features.put("typeMatch", true);
							break;
						}
					}
				}
				
				recon.candidates.add(candidate);
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
