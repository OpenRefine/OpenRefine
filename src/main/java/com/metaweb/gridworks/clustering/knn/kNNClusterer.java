package com.metaweb.gridworks.clustering.knn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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

import edu.mit.simile.vicino.clustering.NGramClusterer;
import edu.mit.simile.vicino.clustering.VPTreeClusterer;
import edu.mit.simile.vicino.distances.BZip2Distance;
import edu.mit.simile.vicino.distances.Distance;
import edu.mit.simile.vicino.distances.GZipDistance;
import edu.mit.simile.vicino.distances.JaccardDistance;
import edu.mit.simile.vicino.distances.JaroDistance;
import edu.mit.simile.vicino.distances.JaroWinklerDistance;
import edu.mit.simile.vicino.distances.JaroWinklerTFIDFDistance;
import edu.mit.simile.vicino.distances.LevenshteinDistance;
import edu.mit.simile.vicino.distances.PPMDistance;

public class kNNClusterer extends Clusterer {

    private Distance _distance;
    
    static final protected Map<String, Distance> _distances = new HashMap<String, Distance>();

    List<Set<Serializable>> _clusters;

    Map<Serializable, Integer> _counts = new HashMap<Serializable, Integer>();

    static {
        _distances.put("levenshtein", new LevenshteinDistance());
        _distances.put("jaccard", new JaccardDistance());
        _distances.put("jaro", new JaroDistance());
        _distances.put("jaro-winkler", new JaroWinklerDistance());
        _distances.put("jaro-winkler-tfidf", new JaroWinklerTFIDFDistance());
        _distances.put("gzip", new GZipDistance());
        _distances.put("bzip2", new BZip2Distance());
        _distances.put("ppm", new PPMDistance());
    }

    class VPTreeClusteringRowVisitor implements RowVisitor {

        Distance _distance;
        JSONObject _config;
        VPTreeClusterer _clusterer;
        double _radius = 1.0f;
        
        public VPTreeClusteringRowVisitor(Distance d, JSONObject o) {
            _distance = d;
            _config = o;
            _clusterer = new VPTreeClusterer(_distance);
            try {
                JSONObject params = o.getJSONObject("params");
                _radius = params.getDouble("radius");
            } catch (JSONException e) {
                //Gridworks.warn("No parameters found, using defaults");
            }
        }
        
        public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
            Cell cell = row.getCell(_colindex);
            if (cell != null && cell.value != null) {
                Object v = cell.value;
                String s = (v instanceof String) ? ((String) v) : v.toString();
                _clusterer.populate(s);
                count(s);
            }
            return false;
        }
        
        public List<Set<Serializable>> getClusters() {
            return _clusterer.getClusters(_radius);
        }
    }

    class BlockingClusteringRowVisitor implements RowVisitor {

        Distance _distance;
        JSONObject _config;
        double _radius = 1.0d;
        int _blockingNgramSize = 6;
        HashSet<String> _data;
        NGramClusterer _clusterer;
        
        public BlockingClusteringRowVisitor(Distance d, JSONObject o) {
            _distance = d;
            _config = o;
            _data = new HashSet<String>();
            try {
                JSONObject params = o.getJSONObject("params");
                _radius = params.getDouble("radius");
                Gridworks.warn("Use radius: " + _radius);
                _blockingNgramSize = params.getInt("blocking-ngram-size");
                Gridworks.warn("Use blocking ngram size: " + _blockingNgramSize);
            } catch (JSONException e) {
                Gridworks.warn("No parameters found, using defaults");
            }
            _clusterer = new NGramClusterer(_distance, _blockingNgramSize);
        }
        
        public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
            Cell cell = row.getCell(_colindex);
            if (cell != null && cell.value != null) {
                Object v = cell.value;
                String s = (v instanceof String) ? ((String) v) : v.toString().intern();
                _clusterer.populate(s);
                count(s);
            }
            return false;
        }
        
        public List<Set<Serializable>> getClusters() {
            return _clusterer.getClusters(_radius);
        }
    }
        
    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        super.initializeFromJSON(project, o);
        _distance = _distances.get(o.getString("function").toLowerCase());
    }

    public void computeClusters(Engine engine) {
        //VPTreeClusteringRowVisitor visitor = new VPTreeClusteringRowVisitor(_distance,_config);
        BlockingClusteringRowVisitor visitor = new BlockingClusteringRowVisitor(_distance,_config);
        FilteredRows filteredRows = engine.getAllFilteredRows(false);
        filteredRows.accept(_project, visitor);
     
        _clusters = visitor.getClusters();
    }

    public static class ValuesComparator implements Comparator<Entry<Serializable,Integer>>, Serializable {
        private static final long serialVersionUID = 204469656070583155L;
        public int compare(Entry<Serializable,Integer> o1, Entry<Serializable,Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }
    
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.array();        
        for (Set<Serializable> m : _clusters) {
            if (m.size() > 1) {
                Map<Serializable,Integer> internal_counts = new HashMap<Serializable,Integer>();
                for (Serializable s : m) {
                    internal_counts.put(s,_counts.get(s));
                }
                List<Entry<Serializable,Integer>> values = new ArrayList<Entry<Serializable,Integer>>(internal_counts.entrySet());
                Collections.sort(values, new ValuesComparator());
                writer.array();        
                for (Entry<Serializable,Integer> e : values) {
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
    
    private void count(Serializable s) {
        if (_counts.containsKey(s)) {
            _counts.put(s, _counts.get(s) + 1);
        } else {
            _counts.put(s, 1);
        }
    }
}
