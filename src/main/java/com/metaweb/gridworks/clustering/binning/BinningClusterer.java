package com.metaweb.gridworks.clustering.binning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.Gridworks;
import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.clustering.Clusterer;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class BinningClusterer extends Clusterer {

    private Keyer _keyer;
    
    static protected Map<String, Keyer> _keyers = new HashMap<String, Keyer>();

    List<Map<String,Integer>> _clusters;
     
    static {
        _keyers.put("fingerprint", new FingerprintKeyer());
        _keyers.put("ngram-fingerprint", new NGramFingerprintKeyer());
        _keyers.put("metaphone", new MetaphoneKeyer());
        _keyers.put("double-metaphone", new DoubleMetaphoneKeyer());
        _keyers.put("soundex", new SoundexKeyer());
    }

    class BinningRowVisitor implements RowVisitor {

        Keyer _keyer;
        Object[] _params;
        JSONObject _config;
        
        Map<String,Map<String,Integer>> _map = new HashMap<String,Map<String,Integer>>();
        
        public BinningRowVisitor(Keyer k, JSONObject o) {
            _keyer = k;
            _config = o;
            if (k instanceof NGramFingerprintKeyer) {
                try {
                    int size = _config.getJSONObject("params").getInt("ngram-size");
                    Gridworks.log("Using ngram size: " + size);
                    _params = new Object[1];
                    _params[0] = size;
                } catch (JSONException e) {
                    //Gridworks.warn("No params specified, using default");
                }
            }
        }
        
        public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
            Cell cell = row.getCell(_colindex);
            if (cell != null && cell.value != null) {
                String v = cell.value.toString();
                String s = (v instanceof String) ? ((String) v) : v.toString();
                String key = _keyer.key(s,_params);
                if (_map.containsKey(key)) {
                    Map<String,Integer> m = _map.get(key);
                    if (m.containsKey(v)) {
                        m.put(v, m.get(v) + 1);
                    } else {
                        m.put(v,1);
                    }
                } else {
                    Map<String,Integer> m = new TreeMap<String,Integer>();
                    m.put(v,1);
                    _map.put(key, m);
                }
            }
            return false;
        }
        
        public Map<String,Map<String,Integer>> getMap() {
            return _map;
        }
    }
            
    public class SizeComparator implements Comparator<Map<String,Integer>> {
        public int compare(Map<String,Integer> o1, Map<String,Integer> o2) {
            int s1 = o1.size();
            int s2 = o2.size();
            if (o1 == o2) {
                int total1 = 0;
                for (int i : o1.values()) {
                    total1 += i;
                }
                int total2 = 0;
                for (int i : o2.values()) {
                    total2 += i;
                }
                return total2 - total1;
            } else {
                return s2 - s1;
            }
        }
    }

    public class EntriesComparator implements Comparator<Entry<String,Integer>> {
        public int compare(Entry<String,Integer> o1, Entry<String,Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }
    
    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        super.initializeFromJSON(project, o);
        _keyer = _keyers.get(o.getString("function").toLowerCase());
    }

    public void computeClusters(Engine engine) {
        BinningRowVisitor visitor = new BinningRowVisitor(_keyer,_config);
        FilteredRows filteredRows = engine.getAllFilteredRows(true);
        filteredRows.accept(_project, visitor);
     
        Map<String,Map<String,Integer>> map = visitor.getMap();
        _clusters = new ArrayList<Map<String,Integer>>(map.values());
        Collections.sort(_clusters, new SizeComparator());
    }
    
    public void write(JSONWriter writer, Properties options) throws JSONException {
        EntriesComparator c = new EntriesComparator();
        
        writer.array();        
        for (Map<String,Integer> m : _clusters) {
            if (m.size() > 1) {
                writer.array();        
                List<Entry<String,Integer>> entries = new ArrayList<Entry<String,Integer>>(m.entrySet());
                Collections.sort(entries,c);
                for (Entry<String,Integer> e : entries) {
                    writer.object();
                    writer.key("v"); writer.value(e.getKey());
                    writer.key("c"); writer.value(e.getValue());
                    writer.endObject();
                }
                writer.endArray();
            }
        }
        writer.endArray();
    }
}
