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

package org.openrefine.operations.row;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.FilteredRows;
import org.openrefine.browsing.RowVisitor;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.changes.RowRemovalChange;
import org.openrefine.operations.EngineDependentOperation;

public class RowRemovalOperation extends EngineDependentOperation {

    @JsonCreator
    public RowRemovalOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig) {
        super(engineConfig);
    }

    @Override
    protected String getDescription() {
        return "Remove rows";
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = createEngine(project);

        List<Integer> rowIndices = new ArrayList<Integer>();

        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, createRowVisitor(project, rowIndices));

        return new HistoryEntry(
                historyEntryID,
                project,
                "Remove " + rowIndices.size() + " rows",
                this,
                new RowRemovalChange(rowIndices));
    }

    protected RowVisitor createRowVisitor(Project project, List<Integer> rowIndices) throws Exception {
        return new RowVisitor() {

            List<Integer> rowIndices;

            public RowVisitor init(List<Integer> rowIndices) {
                this.rowIndices = rowIndices;
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
                rowIndices.add(rowIndex);

                return false;
            }
        }.init(rowIndices);
    }
}
