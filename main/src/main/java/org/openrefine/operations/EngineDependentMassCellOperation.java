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

package org.openrefine.operations;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.FilteredRows;
import org.openrefine.browsing.RowVisitor;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.Project;
import org.openrefine.model.changes.CellChange;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.MassCellChange;
import org.openrefine.operations.EngineDependentOperation;

abstract public class EngineDependentMassCellOperation extends EngineDependentOperation {

    @JsonIgnore
    final protected String _columnName;
    @JsonIgnore
    final protected boolean _updateRowContextDependencies;

    protected EngineDependentMassCellOperation(
            EngineConfig engineConfig, String columnName, boolean updateRowContextDependencies) {
        super(engineConfig);
        _columnName = columnName;
        _updateRowContextDependencies = updateRowContextDependencies;
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = createEngine(project);

        ColumnMetadata column = project.columnModel.getColumnByName(_columnName);
        if (column == null) {
            throw new Exception("No column named " + _columnName);
        }

        List<CellChange> cellChanges = new ArrayList<CellChange>(project.rows.size());

        FilteredRows filteredRows = engine.getAllFilteredRows();
        try {
            filteredRows.accept(project, createRowVisitor(project, cellChanges, historyEntryID));
        } catch (Exception e) {
            e.printStackTrace();
        }

        String description = createDescription(column, cellChanges);

        return new HistoryEntry(
                historyEntryID, project, description, this, createChange(project, column, cellChanges));
    }

    protected Change createChange(Project project, ColumnMetadata column, List<CellChange> cellChanges) {
        return new MassCellChange(
                cellChanges, column.getName(), _updateRowContextDependencies);
    }

    @JsonProperty("columnName")
    protected String getColumnName() {
        return _columnName;
    }

    abstract protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception;

    abstract protected String createDescription(ColumnMetadata column, List<CellChange> cellChanges);
}
