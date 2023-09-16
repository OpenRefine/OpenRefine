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

package org.openrefine.operations.column;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.history.Change.DoesNotApplyException;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.ModelException;
import org.openrefine.model.RowMapper;
import org.openrefine.operations.ImmediateRowMapOperation;

public class ColumnRenameOperation extends ImmediateRowMapOperation {

    final protected String _oldColumnName;
    final protected String _newColumnName;

    @JsonCreator
    public ColumnRenameOperation(
            @JsonProperty("oldColumnName") String oldColumnName,
            @JsonProperty("newColumnName") String newColumnName) {
        super(EngineConfig.ALL_ROWS);
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
    public String getDescription() {
        return "Rename column " + _oldColumnName + " to " + _newColumnName;
    }

    @Override
    public ColumnModel getNewColumnModel(GridState state) throws DoesNotApplyException {
        ColumnModel model = state.getColumnModel();
        int index = columnIndex(model, _oldColumnName);
        try {
            return model.renameColumn(index, _newColumnName);
        } catch (ModelException e) {
            throw new DoesNotApplyException(
                    String.format("Column '%s' already exists", _newColumnName));
        }
    }

    @Override
    protected RowMapper getPositiveRowMapper(GridState state) throws DoesNotApplyException {
        return RowMapper.IDENTITY;
    }
}
