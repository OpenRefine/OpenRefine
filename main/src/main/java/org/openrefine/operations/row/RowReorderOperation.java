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
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.RecordVisitor;
import org.openrefine.browsing.RowVisitor;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Project;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.changes.RowReorderChange;
import org.openrefine.operations.AbstractOperation;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.sorting.SortingRecordVisitor;
import org.openrefine.sorting.SortingRowVisitor;

public class RowReorderOperation extends AbstractOperation {

    final protected Mode _mode;
    final protected SortingConfig _sorting;

    @JsonCreator
    public RowReorderOperation(
            @JsonProperty("mode") Mode mode,
            @JsonProperty("sorting") SortingConfig sorting) {
        _mode = mode;
        _sorting = sorting;
    }

    @JsonProperty("mode")
    public Mode getMode() {
        return _mode;
    }

    @JsonProperty("sorting")
    public SortingConfig getSortingConfig() {
        return _sorting;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Reorder rows";
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = new Engine(project);
        engine.setMode(_mode);

        List<Integer> rowIndices = new ArrayList<Integer>();
        if (_mode == Mode.RowBased) {
            RowVisitor visitor = new IndexingVisitor(rowIndices);
            if (_sorting != null) {
                SortingRowVisitor srv = new SortingRowVisitor(visitor);

                srv.initializeFromConfig(project, _sorting);
                if (srv.hasCriteria()) {
                    visitor = srv;
                }
            }

            engine.getAllRows().accept(project, visitor);
        } else {
            RecordVisitor visitor = new IndexingVisitor(rowIndices);
            if (_sorting != null) {
                SortingRecordVisitor srv = new SortingRecordVisitor(visitor);

                srv.initializeFromConfig(project, _sorting);
                if (srv.hasCriteria()) {
                    visitor = srv;
                }
            }

            engine.getAllRecords().accept(project, visitor);
        }

        return new HistoryEntry(
                historyEntryID,
                project,
                "Reorder rows",
                this,
                new RowReorderChange(rowIndices));
    }

    static protected class IndexingVisitor implements RowVisitor, RecordVisitor {

        List<Integer> _indices;

        IndexingVisitor(List<Integer> indices) {
            _indices = indices;
        }

        @Override
        public void start(Project project) {
        }

        @Override
        public void end(Project project) {
        }

        @Override
        public boolean visit(Project project, int rowIndex, Row row) {
            _indices.add(rowIndex);
            return false;
        }

        @Override
        public boolean visit(Project project, Record record) {
            for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
                _indices.add(r);
            }
            return false;
        }
    }
}
