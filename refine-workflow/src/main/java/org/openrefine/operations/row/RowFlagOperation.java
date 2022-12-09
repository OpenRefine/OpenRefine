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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.GridState;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.ImmediateRowMapOperation;

public class RowFlagOperation extends ImmediateRowMapOperation {

    final protected boolean _flagged;

    @JsonCreator
    public RowFlagOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("flagged") boolean flagged) {
        super(engineConfig);
        _flagged = flagged;
    }

    @JsonProperty("flagged")
    public boolean getFlagged() {
        return _flagged;
    }

    @Override
    public String getDescription() {
        return (_flagged ? "Flag rows" : "Unflag rows");
    }

    @Override
    public RowInRecordMapper getPositiveRowMapper(GridState grid, ChangeContext context) {
        return rowMapper(_flagged);
    }

    protected static RowInRecordMapper rowMapper(boolean flagged) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 1941653093381723354L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                return row.withFlagged(flagged);
            }

            @Override
            public boolean preservesRecordStructure() {
                return true;
            }

        };
    }
}
