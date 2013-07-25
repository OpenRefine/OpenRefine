/**

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

package com.google.refine.browsing;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.browsing.facets.Facet;
import com.google.refine.browsing.facets.ListFacet;
import com.google.refine.browsing.facets.RangeFacet;
import com.google.refine.browsing.facets.ScatterplotFacet;
import com.google.refine.browsing.facets.TextSearchFacet;
import com.google.refine.browsing.facets.TimeRangeFacet;
import com.google.refine.browsing.util.ConjunctiveFilteredRecords;
import com.google.refine.browsing.util.ConjunctiveFilteredRows;
import com.google.refine.browsing.util.FilteredRecordsAsFilteredRows;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * Faceted browsing engine.
 */
public class Engine implements Jsonizable {
    static public enum Mode {
        RowBased,
        RecordBased
    }

    public final static String INCLUDE_DEPENDENT = "includeDependent";
    public final static String MODE = "mode";
    public final static String MODE_ROW_BASED = "row-based";
    public final static String MODE_RECORD_BASED = "record-based";

    protected Project _project;
    protected List<Facet> _facets = new LinkedList<Facet>();
    protected Mode _mode = Mode.RowBased;

    static public String modeToString(Mode mode) {
        return mode == Mode.RowBased ? MODE_ROW_BASED : MODE_RECORD_BASED;
    }
    static public Mode stringToMode(String s) {
        return MODE_ROW_BASED.equals(s) ? Mode.RowBased : Mode.RecordBased;
    }

    public Engine(Project project) {
        _project  = project;
    }

    public Mode getMode() {
        return _mode;
    }
    public void setMode(Mode mode) {
        _mode = mode;
    }

    public FilteredRows getAllRows() {
        return new FilteredRows() {
            @Override
            public void accept(Project project, RowVisitor visitor) {
                try {
                    visitor.start(project);

                    int c = project.rows.size();
                    for (int rowIndex = 0; rowIndex < c; rowIndex++) {
                        Row row = project.rows.get(rowIndex);
                        if (visitor.visit(project, rowIndex, row)) {
                            break;
                        }
                    }
                } finally {
                    visitor.end(project);
                }
            }
        };
    }

    public FilteredRows getAllFilteredRows() {
        return getFilteredRows(null);
    }

    public FilteredRows getFilteredRows(Facet except) {
        if (_mode == Mode.RecordBased) {
            return new FilteredRecordsAsFilteredRows(getFilteredRecords(except));
        } else if (_mode == Mode.RowBased) {
            ConjunctiveFilteredRows cfr = new ConjunctiveFilteredRows();
            for (Facet facet : _facets) {
                if (facet != except) {
                    RowFilter rowFilter = facet.getRowFilter(_project);
                    if (rowFilter != null) {
                        cfr.add(rowFilter);
                    }
                }
            }
            return cfr;
        }
        throw new InternalError("Unknown mode.");
    }

    public FilteredRecords getAllRecords() {
        return new FilteredRecords() {
            @Override
            public void accept(Project project, RecordVisitor visitor) {
                try {
                    visitor.start(project);

                    int c = project.recordModel.getRecordCount();
                    for (int r = 0; r < c; r++) {
                        visitor.visit(project, project.recordModel.getRecord(r));
                    }
                } finally {
                    visitor.end(project);
                }
            }
        };
    }

    public FilteredRecords getFilteredRecords() {
        return getFilteredRecords(null);
    }

    public FilteredRecords getFilteredRecords(Facet except) {
        if (_mode == Mode.RecordBased) {
            ConjunctiveFilteredRecords cfr = new ConjunctiveFilteredRecords();
            for (Facet facet : _facets) {
                if (facet != except) {
                    RecordFilter recordFilter = facet.getRecordFilter(_project);
                    if (recordFilter != null) {
                        cfr.add(recordFilter);
                    }
                }
            }
            return cfr;
        }
        throw new InternalError("This method should not be called when the engine is not in record mode.");
    }

    public void initializeFromJSON(JSONObject o) throws JSONException {
        if (o == null) {
            return;
        }

        if (o.has("facets") && !o.isNull("facets")) {
            JSONArray a = o.getJSONArray("facets");
            int length = a.length();

            for (int i = 0; i < length; i++) {
                JSONObject fo = a.getJSONObject(i);
                String type = fo.has("type") ? fo.getString("type") : "list";

                Facet facet = null;
                if ("list".equals(type)) {
                    facet = new ListFacet();
                } else if ("range".equals(type)) {
                    facet = new RangeFacet();
                } else if ("timerange".equals(type)) {
                    facet = new TimeRangeFacet();
                } else if ("scatterplot".equals(type)) {
                    facet = new ScatterplotFacet();
                } else if ("text".equals(type)) {
                    facet = new TextSearchFacet();
                }

                if (facet != null) {
                    facet.initializeFromJSON(_project, fo);
                    _facets.add(facet);
                }
            }
        }

        // for backward compatibility
        if (o.has(INCLUDE_DEPENDENT) && !o.isNull(INCLUDE_DEPENDENT)) {
            _mode = o.getBoolean(INCLUDE_DEPENDENT) ? Mode.RecordBased : Mode.RowBased;
        }

        if (o.has(MODE) && !o.isNull(MODE)) {
            _mode = MODE_ROW_BASED.equals(o.getString(MODE)) ? Mode.RowBased : Mode.RecordBased;
        }
    }

    public void computeFacets() throws JSONException {
        if (_mode == Mode.RowBased) {
            for (Facet facet : _facets) {
                FilteredRows filteredRows = getFilteredRows(facet);

                facet.computeChoices(_project, filteredRows);
            }
        } else if (_mode == Mode.RecordBased) {
            for (Facet facet : _facets) {
                FilteredRecords filteredRecords = getFilteredRecords(facet);

                facet.computeChoices(_project, filteredRecords);
            }
        } else {
            throw new InternalError("Unknown mode.");
        }
    }

    @Override
    public void write(JSONWriter writer, Properties options)
    throws JSONException {

        writer.object();
        writer.key("facets");
        writer.array();
        for (Facet facet : _facets) {
            facet.write(writer, options);
        }
        writer.endArray();
        writer.key(MODE); writer.value(_mode == Mode.RowBased ? MODE_ROW_BASED : MODE_RECORD_BASED);
        writer.endObject();
    }
}
