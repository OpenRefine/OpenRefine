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

package com.google.refine.operations.row;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.MassChange;
import com.google.refine.model.changes.RowStarChange;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationHistoryEntry;

public class RowStarOperation extends EngineDependentOperation {

    final protected boolean _starred;

    @JsonCreator
    public RowStarOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("starred") boolean starred) {
        super(engineConfig);
        _starred = starred;
    }

    @JsonProperty("starred")
    public boolean getStarred() {
        return _starred;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return _starred ? OperationDescription.row_star_brief() : OperationDescription.row_unstar_brief();
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = createEngine(project);

        List<Change> changes = new ArrayList<Change>(project.rows.size());

        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, createRowVisitor(project, changes));

        return new HistoryEntry(
                historyEntryID,
                project,
                // (_starred ? "Star" : "Unstar") + " " + changes.size() + " rows",
                _starred ? OperationHistoryEntry.row_star(changes.size()) : OperationHistoryEntry.row_unstar(changes.size()),
                this,
                new MassChange(changes, false));
    }

    protected RowVisitor createRowVisitor(Project project, List<Change> changes) throws Exception {
        return new RowVisitor() {

            List<Change> changes;

            public RowVisitor init(List<Change> changes) {
                this.changes = changes;
                return this;
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
                if (row.starred != _starred) {
                    RowStarChange change = new RowStarChange(rowIndex, _starred);

                    changes.add(change);
                }
                return false;
            }
        }.init(changes);
    }
}
