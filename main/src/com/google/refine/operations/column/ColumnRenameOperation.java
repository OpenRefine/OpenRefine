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

package com.google.refine.operations.column;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.changes.ColumnRenameChange;
import com.google.refine.operations.OperationDescription;

public class ColumnRenameOperation extends AbstractOperation {

    final protected String _oldColumnName;
    final protected String _newColumnName;

    @JsonCreator
    public ColumnRenameOperation(
            @JsonProperty("oldColumnName") String oldColumnName,
            @JsonProperty("newColumnName") String newColumnName) {
        _oldColumnName = oldColumnName;
        _newColumnName = newColumnName;
    }

    @JsonProperty("oldColumnName")
    public String getOldColumnName() {
        return _oldColumnName;
    }

    @JsonProperty("newColumnName")
    public String getNewColumnName() {
        return _newColumnName;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.column_rename_brief(_oldColumnName, _newColumnName);
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        if (project.columnModel.getColumnByName(_oldColumnName) == null) {
            throw new Exception("No column named " + _oldColumnName);
        }
        if (project.columnModel.getColumnByName(_newColumnName) != null) {
            throw new Exception("Another column already named " + _newColumnName);
        }

        Change change = new ColumnRenameChange(_oldColumnName, _newColumnName);

        return new HistoryEntry(historyEntryID, project, getBriefDescription(null), ColumnRenameOperation.this, change);
    }
}
