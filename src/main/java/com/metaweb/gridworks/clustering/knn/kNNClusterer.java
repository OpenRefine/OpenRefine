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
import java.util.TreeSet;

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
import com.wcohen.ss.api.Token;
import com.wcohen.ss.tokens.NGramTokenizer;
import com.wcohen.ss.tokens.SimpleTokenizer;

import edu.mit.simile.vicino.distances.BZip2Distance;
import edu.mit.simile.vicino.distances.Distance;
import edu.mit.simile.vicino.distances.GZipDistance;
import edu.mit.simile.vicino.distances.JaccardDistance;
import edu.mit.simile.vicino.distances.JaroDistance;
import edu.mit.simile.vicino.distances.JaroWinklerDistance;
import edu.mit.simile.vicino.distances.JaroWinklerTFIDFDistance;
import edu.mit.simile.vicino.distances.LevenshteinDistance;
import edu.mit.simile.vicino.distances.PPMDistance;
import edu.mit.simile.vicino.vptree.VPTreeBuilder;

public class kNNClusterer extends Clusterer {

    private Distance _distance;
    
    static protected Map<String, Distance> _distances = new HashMap<String, Distance>();

    ArrayList<Set<Serializable>> _clusters;
    
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
        VPTreeBuilder _treeBuilder;
        double _radius = 1.0f;
        
        public VPTreeClusteringRowVisitor(Distance d, JSONObject o) {
            _distance = d;
            _config = o;
            _treeBuilder = new VPTreeBuilder(_distance);
            try {
                JSONObject params = o.getJSONObject("params");
                _radius = params.getDouble("radius");
            } catch (JSONException e) {
                //Gridworks.warn("No parameters found, using defaults");
            }
        }
        
        public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
            Cell cell = row.cells.get(_colindex);
            if (cell != null && cell.value != null) {
                Object v = cell.value;
                String s = (v instanceof String) ? ((String) v) : v.toString();
                _treeBuilder.populate(s);
            }
            return false;
        }
        
        public Map<Serializable,List<Serializable>> getClusters() {
            return _treeBuilder.getClusters(_radius);
        }
    }

    class BlockingClusteringRowVisitor implements RowVisitor {

        Distance _distance;
        JSONObject _config;
        double _radius = 1.0d;
        int _blockingNgramSize = 6;
        HashSet<String> _data;
        
        public BlockingClusteringRowVisitor(Distance d, JSONObject o) {
            _distance = d;
            _config = o;
            _data = new HashSet<String>();
            try {
                JSONObject params = o.getJSONObject("params");
                _radius = params.getDouble("radius");
                _blockingNgramSize = params.getInt("blocking-ngram-size");
            } catch (JSONException e) {
                Gridworks.warn("No parameters found, using defaults");
            }
        }
        
        public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
            Cell cell = row.getCell(_colindex);
            if (cell != null && cell.value != null) {
                Object v = cell.value;
                String s = (v instanceof String) ? ((String) v) : v.toString().intern();
                _data.add(s);
            }
            return false;
        }
        
        public Map<Serializable,Set<Serializable>> getClusters() {
            NGramTokenizer tokenizer = new NGramTokenizer(_blockingNgramSize,_blockingNgramSize,false,SimpleTokenizer.DEFAULT_TOKENIZER);

            Map<String,List<String>> blocks = new HashMap<String,List<String>>();
            
            for (String s : _data) {
                Token[] tokens = tokenizer.tokenize(s);
                for (Token t : tokens) {
                    String ss = t.getValue();
                    List<String> l = null;
                    if (!blocks.containsKey(ss)) {
                        l = new ArrayList<String>(); 
                        blocks.put(ss, l);
                    } else {
                        l = blocks.get(ss);
                    }
                    l.add(s);
                }
            }

            Map<Serializable,Set<Serializable>> clusters = new HashMap<Serializable,Set<Serializable>>();
            
            for (List<String> list : blocks.values()) {
                if (list.size() < 2) continue;
                for (String a : list) {
                    for (String b : list) {
                        if (a == b) continue;
                        if (clusters.containsKey(a) && clusters.get(a).contains(b)) continue;
                        if (clusters.containsKey(b) && clusters.get(b).contains(a)) continue;
                        double d = _distance.d(a,b);
                        if (d <= _radius || _radius < 0) {
                            Set<Serializable> l = null; 
                            if (!clusters.containsKey(a)) {
                                l = new TreeSet<Serializable>();
                                l.add(a);
                                clusters.put(a, l);
                            } else {
                                l = clusters.get(a);
                            }
                            l.add(b);
                        }
                    }
                }
            }
            
            Gridworks.log("Calculated " + _distance.getCount() + " distances");
            return clusters;
        }
    }
    
    public class SizeComparator implements Comparator<Set<Serializable>> {
        public int compare(Set<Serializable> o1, Set<Serializable> o2) {
            return o2.size() - o1.size();
        }
    }
    
    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        super.initializeFromJSON(project, o);
        _distance = _distances.get(o.getString("function").toLowerCase());
    }

    public void computeClusters(Engine engine) {
        //VPTreeClusteringRowVisitor visitor = new VPTreeClusteringRowVisitor(_distance,_config);
        BlockingClusteringRowVisitor visitor = new BlockingClusteringRowVisitor(_distance,_config);
        FilteredRows filteredRows = engine.getAllFilteredRows(true);
        filteredRows.accept(_project, visitor);
     
        Map<Serializable,Set<Serializable>> clusters = visitor.getClusters();
        _clusters = new ArrayList<Set<Serializable>>(clusters.values());
        Collections.sort(_clusters, new SizeComparator());
    }
    
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.array();        
        for (Set<Serializable> m : _clusters) {
            if (m.size() > 1) {
                writer.array();        
                for (Serializable s : m) {
                    writer.object();
                    writer.key("v"); writer.value(s);
                    writer.key("c"); writer.value(1);
                    writer.endObject();
                }
                writer.endArray();
            }
        }
        writer.endArray();
    }
}
