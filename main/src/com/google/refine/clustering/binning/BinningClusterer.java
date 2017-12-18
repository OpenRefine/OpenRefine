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
import java.util.Properties;
import java.util.TreeMap;

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

public class BinningClusterer extends Clusterer {

    private Keyer _keyer;
    
    static final protected Map<String, Keyer> _keyers = new HashMap<String, Keyer>();

    final static Logger logger = LoggerFactory.getLogger("binning_clusterer");
    
    List<Map<String,Integer>> _clusters;
     
    static {
        _keyers.put("fingerprint", new FingerprintKeyer());
        _keyers.put("ngram-fingerprint", new NGramFingerprintKeyer());
        _keyers.put("metaphone", new MetaphoneKeyer());
        _keyers.put("double-metaphone", new DoubleMetaphoneKeyer());
        _keyers.put("metaphone3", new Metaphone3Keyer());
        _keyers.put("soundex", new SoundexKeyer());
        _keyers.put("cologne-phonetic", new ColognePhoneticKeyer());
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
                    logger.debug("Using ngram size: {}", size);
                    _params = new Object[1];
                    _params[0] = size;
                } catch (JSONException e) {
                    //Refine.warn("No params specified, using default");
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
                String key = _keyer.key(s,_params);
                if (_map.containsKey(key)) {
                    Map<String,Integer> m = _map.get(key);
                    if (m.containsKey(s)) {
                        m.put(s, m.get(s) + 1);
                    } else {
                        m.put(s,1);
                    }
                } else {
                    Map<String,Integer> m = new TreeMap<String,Integer>();
                    m.put(s,1);
                    _map.put(key, m);
                }
            }
            return false;
        }
        
        public Map<String,Map<String,Integer>> getMap() {
            return _map;
        }
    }
            
    public static class SizeComparator implements Comparator<Map<String,Integer>>, Serializable {
        private static final long serialVersionUID = -1390696157208674054L;
        @Override
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

    public static class EntriesComparator implements Comparator<Entry<String,Integer>>, Serializable {
        private static final long serialVersionUID = 2763378036791777964L;
        @Override
        public int compare(Entry<String,Integer> o1, Entry<String,Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }
    
    @Override
    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        super.initializeFromJSON(project, o);
        _keyer = _keyers.get(o.getString("function").toLowerCase());
    }

    @Override
    public void computeClusters(Engine engine) {
        BinningRowVisitor visitor = new BinningRowVisitor(_keyer,_config);
        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(_project, visitor);
     
        Map<String,Map<String,Integer>> map = visitor.getMap();
        _clusters = new ArrayList<Map<String,Integer>>(map.values());
        Collections.sort(_clusters, new SizeComparator());
    }
    
    @Override
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
