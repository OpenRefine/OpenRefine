package com.metaweb.gridworks.clustering.knn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

import edu.mit.simile.vicino.Distance;
import edu.mit.simile.vicino.distances.BZip2Distance;
import edu.mit.simile.vicino.distances.GZipDistance;
import edu.mit.simile.vicino.distances.JaccardDistance;
import edu.mit.simile.vicino.distances.JaroDistance;
import edu.mit.simile.vicino.distances.LevenshteinDistance;
import edu.mit.simile.vicino.distances.PPMDistance;
import edu.mit.simile.vicino.vptree.VPTreeBuilder;

public class kNNClusterer extends Clusterer {

    private Distance _distance;
    
    static protected Map<String, Distance> _distances = new HashMap<String, Distance>();

    List<List<? extends Serializable>> _clusters;
    
    static {
        _distances.put("levenshtein", new LevenshteinDistance());
        _distances.put("jaro", new JaroDistance());
        _distances.put("jaccard", new JaccardDistance());
        _distances.put("gzip", new GZipDistance());
        _distances.put("bzip2", new BZip2Distance());
        _distances.put("ppm", new PPMDistance());
    }

    class kNNClusteringRowVisitor implements RowVisitor {

        Distance _distance;
        JSONObject _config;
        VPTreeBuilder _treeBuilder;
        float _radius;
        
        public kNNClusteringRowVisitor(Distance d, JSONObject o) {
            _distance = d;
            _config = o;
            _treeBuilder = new VPTreeBuilder(_distance);
            try {
                _radius = (float) o.getDouble("radius");
            } catch (JSONException e) {
                Gridworks.warn("No radius found, using default");
                _radius = 1.0f;
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
    
    public class SizeComparator implements Comparator<List<? extends Serializable>> {
        public int compare(List<? extends Serializable> o1, List<? extends Serializable> o2) {
            return o2.size() - o1.size();
        }
    }
    
    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        super.initializeFromJSON(project, o);
        _distance = _distances.get(o.getString("function").toLowerCase());
    }

    public void computeClusters(Engine engine) {
        kNNClusteringRowVisitor visitor = new kNNClusteringRowVisitor(_distance,_config);
        FilteredRows filteredRows = engine.getAllFilteredRows(true);
        filteredRows.accept(_project, visitor);
     
        Map<Serializable,List<? extends Serializable>> clusters = visitor.getClusters();
        _clusters = new ArrayList<List<? extends Serializable>>(clusters.values());
        Collections.sort(_clusters, new SizeComparator());
    }
    
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.array();        
        for (List<? extends Serializable> m : _clusters) {
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
