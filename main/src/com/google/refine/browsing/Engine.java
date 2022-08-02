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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.facets.Facet;
import com.google.refine.browsing.util.ConjunctiveFilteredRecords;
import com.google.refine.browsing.util.ConjunctiveFilteredRows;
import com.google.refine.browsing.util.FilteredRecordsAsFilteredRows;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * Faceted browsing engine.
 */
public class Engine {

    static public enum Mode {
        @JsonProperty("row-based")
        RowBased, @JsonProperty("record-based")
        RecordBased

    }

    public final static String INCLUDE_DEPENDENT = "includeDependent";
    public final static String MODE = "mode";
    public final static String MODE_ROW_BASED = "row-based";
    public final static String MODE_RECORD_BASED = "record-based";

    @JsonIgnore
    protected Project _project;
    @JsonProperty("facets")
    protected List<Facet> _facets = new LinkedList<Facet>();
    @JsonIgnore
    protected EngineConfig _config = new EngineConfig(Collections.emptyList(), Mode.RowBased);

    static public String modeToString(Mode mode) {
        return mode == Mode.RowBased ? MODE_ROW_BASED : MODE_RECORD_BASED;
    }

    static public Mode stringToMode(String s) {
        return MODE_ROW_BASED.equals(s) ? Mode.RowBased : Mode.RecordBased;
    }

    public Engine(Project project) {
        _project = project;
    }

    @JsonProperty("engine-mode")
    public Mode getMode() {
        return _config.getMode();
    }

    public void setMode(Mode mode) {
        _config = new EngineConfig(_config.getFacetConfigs(), mode);
    }

    @JsonIgnore
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

    @JsonIgnore
    public FilteredRows getAllFilteredRows() {
        return getFilteredRows(null);
    }

    public FilteredRows getFilteredRows(Facet except) {
        if (_config.getMode().equals(Mode.RecordBased)) {
            return new FilteredRecordsAsFilteredRows(getFilteredRecords(except));
        } else if (_config.getMode().equals(Mode.RowBased)) {
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

    @JsonIgnore
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

    @JsonIgnore
    public FilteredRecords getFilteredRecords() {
        return getFilteredRecords(null);
    }

    public FilteredRecords getFilteredRecords(Facet except) {
        if (_config.getMode().equals(Mode.RecordBased)) {
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

    public void initializeFromConfig(EngineConfig config) {
        _config = config;
        _facets = config.getFacetConfigs().stream()
                .map(c -> c.apply(_project))
                .collect(Collectors.toList());
    }

    public void computeFacets() {
        if (_config.getMode().equals(Mode.RowBased)) {
            for (Facet facet : _facets) {
                FilteredRows filteredRows = getFilteredRows(facet);

                facet.computeChoices(_project, filteredRows);
            }
        } else if (_config.getMode().equals(Mode.RecordBased)) {
            for (Facet facet : _facets) {
                FilteredRecords filteredRecords = getFilteredRecords(facet);

                facet.computeChoices(_project, filteredRecords);
            }
        } else {
            throw new InternalError("Unknown mode.");
        }
    }
}
