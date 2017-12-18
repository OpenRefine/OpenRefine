/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.clustering.knn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.clustering.Clusterer;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

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

    final static Logger logger = LoggerFactory.getLogger("kNN_clusterer");
    
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
                //Refine.warn("No parameters found, using defaults");
            }
        }
        
        @Override
        public void start(Project project) {
            // nothing to do
        }

        @Override
        public void end(Project project) {
            // nothing to do
        }
        
        @Override
        public boolean visit(Project project, int rowIndex, Row row) {
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
                logger.debug("Use radius: {}", _radius);
                _blockingNgramSize = params.getInt("blocking-ngram-size");
                logger.debug("Use blocking ngram size: {}",_blockingNgramSize);
            } catch (JSONException e) {
                logger.debug("No parameters found, using defaults");
            }
            _clusterer = new NGramClusterer(_distance, _blockingNgramSize);
        }
        
        @Override
        public void start(Project project) {
            // nothing to do
        }

        @Override
        public void end(Project project) {
            // nothing to do
        }
        
        @Override
        public boolean visit(Project project, int rowIndex, Row row) {
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
        
    @Override
    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        super.initializeFromJSON(project, o);
        _distance = _distances.get(o.getString("function").toLowerCase());
    }

    @Override
    public void computeClusters(Engine engine) {
        //VPTreeClusteringRowVisitor visitor = new VPTreeClusteringRowVisitor(_distance,_config);
        BlockingClusteringRowVisitor visitor = new BlockingClusteringRowVisitor(_distance,_config);
        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(_project, visitor);
     
        _clusters = visitor.getClusters();
    }

    public static class ValuesComparator implements Comparator<Entry<Serializable,Integer>>, Serializable {
        private static final long serialVersionUID = 204469656070583155L;
        @Override
        public int compare(Entry<Serializable,Integer> o1, Entry<Serializable,Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }
    
    @Override
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
