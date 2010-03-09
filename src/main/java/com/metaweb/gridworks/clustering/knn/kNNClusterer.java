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
import com.wcohen.ss.expt.ClusterNGramBlocker;
import com.wcohen.ss.expt.MatchData;
import com.wcohen.ss.expt.Blocker.Pair;

import edu.mit.simile.vicino.Distance;
import edu.mit.simile.vicino.distances.BZip2Distance;
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

    List<List<Serializable>> _clusters;
    
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
        float _radius;
        
        public VPTreeClusteringRowVisitor(Distance d, JSONObject o) {
            _distance = d;
            _config = o;
            _treeBuilder = new VPTreeBuilder(_distance);
            try {
                _radius = (float) o.getJSONObject("params").getDouble("radius");
            } catch (JSONException e) {
                Gridworks.warn("No radius found, using default");
                _radius = 0.1f;
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
        
        public Map<Serializable,List<? extends Serializable>> getClusters() {
            return _treeBuilder.getClusters(_radius);
        }
    }

    class BlockingClusteringRowVisitor implements RowVisitor {

        Distance _distance;
        JSONObject _config;
        MatchData _data;
        float _radius;
        HashSet<String> _set;
        
        public BlockingClusteringRowVisitor(Distance d, JSONObject o) {
            _distance = d;
            _config = o;
            _data = new MatchData();
            _set = new HashSet<String>();
            try {
                _radius = (float) o.getJSONObject("params").getDouble("radius");
            } catch (JSONException e) {
                Gridworks.warn("No radius found, using default");
                _radius = 0.1f;
            }
        }
        
        public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
            Cell cell = row.cells.get(_colindex);
            if (cell != null && cell.value != null) {
                Object v = cell.value;
                String s = (v instanceof String) ? ((String) v) : v.toString().intern();
                if (!_set.contains(s)) {
                    _set.add(s);
                    _data.addInstance("", "", s);
                }
            }
            return false;
        }
        
        public Map<Serializable,List<Serializable>> getClusters() {
            Map<Serializable,List<Serializable>> map = new HashMap<Serializable,List<Serializable>>();
            ClusterNGramBlocker blocker = new ClusterNGramBlocker();
            blocker.block(_data);
            for (int i = 0; i < blocker.numCorrectPairs(); i++) {
                Pair p = blocker.getPair(i);
                String a = p.getA().unwrap();
                String b = p.getB().unwrap();
                List<Serializable> l = null; 
                if (!map.containsKey(a)) {
                    l = new ArrayList<Serializable>(); 
                    map.put(a, l);
                } else {
                    l = map.get(a);
                }
                double d = _distance.d(a,b);
                System.out.println(a + " | " + b + ": " + d);
                if (d <= _radius) {
                    l.add(b);
                }
            }
            return map;
        }
    }
    
    public class SizeComparator implements Comparator<List<Serializable>> {
        public int compare(List<Serializable> o1, List<Serializable> o2) {
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
     
        Map<Serializable,List<Serializable>> clusters = visitor.getClusters();
        _clusters = new ArrayList<List<Serializable>>(clusters.values());
        Collections.sort(_clusters, new SizeComparator());
    }
    
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.array();        
        for (List<Serializable> m : _clusters) {
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
