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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.clustering.ClusteredEntry;
import com.google.refine.clustering.Clusterer;
import com.google.refine.clustering.ClustererConfig;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

import edu.mit.simile.vicino.clustering.NGramClusterer;
import edu.mit.simile.vicino.clustering.VPTreeClusterer;
import edu.mit.simile.vicino.distances.Distance;

public class kNNClusterer extends Clusterer {

    public static class kNNClustererConfig extends ClustererConfig {

        @JsonIgnore
        private String _distanceStr;
        @JsonIgnore
        private SimilarityDistance _distance;
        @JsonIgnore
        private kNNClustererConfigParameters _parameters = null;

        @JsonIgnore
        public SimilarityDistance getDistance() {
            return _distance;
        }

        @JsonProperty("function")
        public void setDistance(String distanceStr) {
            _distanceStr = distanceStr;
            _distance = DistanceFactory.get(_distanceStr.toLowerCase());
        }

        @JsonProperty("function")
        public String getDistanceStr() {
            return _distanceStr;
        }

        @JsonProperty("params")
        public kNNClustererConfigParameters getParameters() {
            return _parameters;
        }

        @JsonProperty("params")
        public void setParameters(kNNClustererConfigParameters params) {
            _parameters = params;
        }

        @Override
        public kNNClusterer apply(Project project) {
            kNNClusterer clusterer = new kNNClusterer();
            clusterer.initializeFromConfig(project, this);
            return clusterer;
        }

        @Override
        public String getType() {
            return "knn";
        }

    }

    public static class kNNClustererConfigParameters {

        public static final double defaultRadius = 1.0d;
        public static final int defaultBlockingNgramSize = 6;
        @JsonProperty("radius")
        public double radius = defaultRadius;
        @JsonProperty("blocking-ngram-size")
        public int blockingNgramSize = defaultBlockingNgramSize;
    }

    private SimilarityDistance _distance;
    private kNNClustererConfigParameters _params;

    List<Set<Serializable>> _clusters;

    Map<Serializable, Integer> _counts = new HashMap<Serializable, Integer>();

    final static Logger logger = LoggerFactory.getLogger("kNN_clusterer");

    class VPTreeClusteringRowVisitor implements RowVisitor {

        Distance _distance;
        kNNClustererConfigParameters _params;
        VPTreeClusterer _clusterer;

        public VPTreeClusteringRowVisitor(Distance d, kNNClustererConfigParameters params) {
            _distance = d;
            _clusterer = new VPTreeClusterer(_distance);
            _params = params;
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
            return _clusterer.getClusters(_params.radius);
        }
    }

    class BlockingClusteringRowVisitor implements RowVisitor {

        SimilarityDistance _distance;
        double _radius = 1.0d;
        int _blockingNgramSize = 6;
        HashSet<String> _data;
        NGramClusterer _clusterer;

        private class DistanceWrapper extends Distance {

            private final SimilarityDistance _d;

            protected DistanceWrapper(SimilarityDistance d) {
                _d = d;
            }

            @Override
            public double d(String arg0, String arg1) {
                return _d.compute(arg0, arg1);
            }
        }

        public BlockingClusteringRowVisitor(SimilarityDistance _distance2, kNNClustererConfigParameters params) {
            _distance = _distance2;
            _data = new HashSet<String>();
            _blockingNgramSize = params.blockingNgramSize;
            _radius = params.radius;
            _clusterer = new NGramClusterer(new DistanceWrapper(_distance), _blockingNgramSize);
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

    public void initializeFromConfig(Project project, kNNClustererConfig config) {
        super.initializeFromConfig(project, config);
        _distance = config.getDistance();
        _params = config.getParameters();
    }

    @Override
    public void computeClusters(Engine engine) {
        // VPTreeClusteringRowVisitor visitor = new VPTreeClusteringRowVisitor(_distance,_config);
        BlockingClusteringRowVisitor visitor = new BlockingClusteringRowVisitor(_distance, _params);
        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(_project, visitor);

        _clusters = visitor.getClusters();
    }

    public static class ValuesComparator implements Comparator<Entry<Serializable, Integer>>, Serializable {

        private static final long serialVersionUID = 204469656070583155L;

        @Override
        public int compare(Entry<Serializable, Integer> o1, Entry<Serializable, Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }

    protected List<ClusteredEntry> getClusteredEntries(Set<Serializable> s) {
        return s.stream()
                .map(e -> new ClusteredEntry(e, _counts.get(e)))
                .sorted(ClusteredEntry.comparator)
                .collect(Collectors.toList());
    }

    @JsonValue
    public List<List<ClusteredEntry>> getJsonRepresentation() {
        return _clusters.stream()
                .filter(m -> m.size() > 1)
                .map(m -> getClusteredEntries(m))
                .collect(Collectors.toList());
    }

    private void count(Serializable s) {
        if (_counts.containsKey(s)) {
            _counts.put(s, _counts.get(s) + 1);
        } else {
            _counts.put(s, 1);
        }
    }
}
