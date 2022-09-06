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

package com.google.refine.clustering.binning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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

public class BinningClusterer extends Clusterer {

    public static class BinningClustererConfig extends ClustererConfig {

        @JsonIgnore
        private String _keyerName;
        @JsonIgnore
        private Keyer _keyer;
        @JsonIgnore
        private BinningParameters _parameters = null;

        @JsonIgnore
        public Keyer getKeyer() {
            return _keyer;
        }

        @JsonProperty("function")
        public void setKeyer(String keyerName) {
            _keyerName = keyerName;
            _keyer = KeyerFactory.get(_keyerName.toLowerCase());
        }

        @JsonProperty("function")
        public String getKeyerName() {
            return _keyerName;
        }

        @JsonProperty("params")
        @JsonInclude(Include.NON_NULL)
        public BinningParameters getParameters() {
            return _parameters;
        }

        @JsonProperty("params")
        public void setParameters(BinningParameters params) {
            _parameters = params;
        }

        @Override
        public BinningClusterer apply(Project project) {
            BinningClusterer clusterer = new BinningClusterer();
            clusterer.initializeFromConfig(project, this);
            return clusterer;
        }

        @Override
        public String getType() {
            return "binning";
        }

    }

    public static class BinningParameters {

        @JsonProperty("ngram-size")
        @JsonInclude(Include.NON_DEFAULT)
        public int ngramSize = 0;
    }

    protected Keyer _keyer;
    protected BinningParameters _parameters;

    final static Logger logger = LoggerFactory.getLogger("binning_clusterer");

    List<Map<String, Integer>> _clusters;

    class BinningRowVisitor implements RowVisitor {

        Keyer _keyer;
        Object[] _params;
        BinningParameters _parameters;

        Map<String, Map<String, Integer>> _map = new HashMap<String, Map<String, Integer>>();

        public BinningRowVisitor(Keyer k, BinningParameters parameters) {
            _keyer = k;
            _parameters = parameters;
            if (k instanceof NGramFingerprintKeyer) {
                if (_parameters != null) {
                    _params = new Object[1];
                    _params[0] = _parameters.ngramSize;
                }
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
                String key = _keyer.key(s, _params);
                if (_map.containsKey(key)) {
                    Map<String, Integer> m = _map.get(key);
                    if (m.containsKey(s)) {
                        m.put(s, m.get(s) + 1);
                    } else {
                        m.put(s, 1);
                    }
                } else {
                    Map<String, Integer> m = new TreeMap<String, Integer>();
                    m.put(s, 1);
                    _map.put(key, m);
                }
            }
            return false;
        }

        public Map<String, Map<String, Integer>> getMap() {
            return _map;
        }
    }

    public static class SizeComparator implements Comparator<Map<String, Integer>>, Serializable {

        private static final long serialVersionUID = -1390696157208674054L;

        @Override
        public int compare(Map<String, Integer> o1, Map<String, Integer> o2) {
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

    public static class EntriesComparator implements Comparator<Entry<String, Integer>>, Serializable {

        private static final long serialVersionUID = 2763378036791777964L;

        @Override
        public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }

    public void initializeFromConfig(Project project, BinningClustererConfig config) {
        super.initializeFromConfig(project, config);
        _keyer = config.getKeyer();
        _parameters = config.getParameters();
    }

    @Override
    public void computeClusters(Engine engine) {
        BinningRowVisitor visitor = new BinningRowVisitor(_keyer, _parameters);
        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(_project, visitor);

        Map<String, Map<String, Integer>> map = visitor.getMap();
        _clusters = new ArrayList<Map<String, Integer>>(map.values());
        Collections.sort(_clusters, new SizeComparator());
    }

    protected static Map<String, Object> entryToMap(Entry<String, Integer> entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("v", entry.getKey());
        map.put("c", entry.getValue());
        return map;
    }

    @JsonValue
    public List<List<ClusteredEntry>> getJsonRepresentation() {
        EntriesComparator c = new EntriesComparator();
        return _clusters.stream()
                .filter(m -> m.size() > 1)
                .map(m -> m.entrySet().stream()
                        .sorted(c)
                        .map(e -> new ClusteredEntry(e.getKey(), e.getValue()))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }
}
