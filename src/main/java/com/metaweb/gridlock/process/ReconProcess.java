package com.metaweb.gridlock.process;

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
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridlock.history.CellChange;
import com.metaweb.gridlock.history.HistoryEntry;
import com.metaweb.gridlock.history.MassCellChange;
import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Recon;
import com.metaweb.gridlock.model.ReconCandidate;
import com.metaweb.gridlock.util.ParsingUtilities;

public class ReconProcess extends LongRunningProcess implements Runnable {
	static public class ReconEntry {
		final public int rowIndex;
		final public Cell cell;
		
		public ReconEntry(int rowIndex, Cell cell) {
			this.rowIndex = rowIndex;
			this.cell = cell;
		}
	}
	
	final protected Project				_project;
	final protected int					_cellIndex;
	final protected List<ReconEntry> 	_entries;
	final protected String 				_typeID;
	
	public ReconProcess(Project project, String description, int cellIndex, List<ReconEntry> entries, String typeID) {
		super(description);
		_project = project;
		_cellIndex = cellIndex;
		_entries = entries;
		_typeID = typeID;
	}
	
	@Override
	protected Runnable getRunnable() {
		return this;
	}
	
	@Override
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
		for (int i = 0; i < values.size(); i += 20) {
			try {
				recon(valueToEntries, values, i, Math.min(i + 20, values.size()), cellChanges);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			_progress = i * 100 / values.size();
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				if (_canceled) {
					break;
				}
			}
		}
		
		MassCellChange massCellChange = new MassCellChange(cellChanges);
		HistoryEntry historyEntry = new HistoryEntry(_project, _description, massCellChange);
		
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
						
						Cell newCell = new Cell();
						newCell.value = oldCell.value;
						newCell.recon = recon;
						
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
			
			ReconCandidate candidate = new ReconCandidate();
			
			candidate.topicID = result.getString("id");
			candidate.topicGUID = result.getString("guid");
			candidate.topicName = result.getString("name");
			candidate.score = result.getDouble("relevance:score");
			
			JSONArray types = result.getJSONArray("type");
			candidate.typeIDs = new String[types.length()];
			for (int j = 0; j < candidate.typeIDs.length; j++) {
				candidate.typeIDs[j] = types.getJSONObject(j).getString("id");
			}
			
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
	
	protected double wordDistance(String s1, String s2) {
		Set<String> words1 = breakWords(s1);
		Set<String> words2 = breakWords(s2);
		return words1.size() >= words2.size() ? wordDistance(words1, words2) : wordDistance(words2, words1);
	}
	
	protected double wordDistance(Set<String> longWords, Set<String> shortWords) {
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
	
	protected Set<String> breakWords(String s) {
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
