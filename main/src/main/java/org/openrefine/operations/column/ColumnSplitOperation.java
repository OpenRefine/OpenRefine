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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.history.Change;
import org.openrefine.model.changes.ColumnSplitChange;
import org.openrefine.model.changes.ColumnSplitChange.Mode;
import org.openrefine.operations.EngineDependentOperation;

public class ColumnSplitOperation extends EngineDependentOperation {

    final protected String _columnName;
    final protected boolean _guessCellType;
    final protected boolean _removeOriginalColumn;
    final protected Mode _mode;

    final protected String _separator;
    final protected Boolean _regex;
    final protected Integer _maxColumns;

    final protected int[] _fieldLengths;

    @JsonCreator
    public static ColumnSplitOperation deserialize(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("guessCellType") boolean guessCellType,
            @JsonProperty("removeOriginalColumn") boolean removeOriginalColumn,
            @JsonProperty("mode") Mode mode,
            @JsonProperty("separator") String separator,
            @JsonProperty("regex") Boolean regex,
            @JsonProperty("maxColumns") Integer maxColumns,
            @JsonProperty("fieldLengths") int[] fieldLengths) {
        if (Mode.Separator.equals(mode)) {
            return new ColumnSplitOperation(
                    engineConfig,
                    columnName,
                    guessCellType,
                    removeOriginalColumn,
                    separator,
                    regex,
                    maxColumns);
        } else {
            return new ColumnSplitOperation(
                    engineConfig,
                    columnName,
                    guessCellType,
                    removeOriginalColumn,
                    fieldLengths);
        }
    }

    public ColumnSplitOperation(
            EngineConfig engineConfig,
            String columnName,
            boolean guessCellType,
            boolean removeOriginalColumn,
            String separator,
            boolean regex,
            int maxColumns) {
        super(engineConfig);

        _columnName = columnName;
        _guessCellType = guessCellType;
        _removeOriginalColumn = removeOriginalColumn;

        _mode = Mode.Separator;
        _separator = separator;
        _regex = regex;
        _maxColumns = maxColumns;

        _fieldLengths = null;
    }

    public ColumnSplitOperation(
            EngineConfig engineConfig,
            String columnName,
            boolean guessCellType,
            boolean removeOriginalColumn,
            int[] fieldLengths) {
        super(engineConfig);

        _columnName = columnName;
        _guessCellType = guessCellType;
        _removeOriginalColumn = removeOriginalColumn;

        _mode = Mode.Lengths;
        _separator = null;
        _regex = null;
        _maxColumns = null;

        _fieldLengths = fieldLengths;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("guessCellType")
    public boolean getGuessCellType() {
        return _guessCellType;
    }

    @JsonProperty("removeOriginalColumn")
    public boolean getRemoveOriginalColumn() {
        return _removeOriginalColumn;
    }

    @JsonProperty("mode")
    public Mode getMode() {
        return _mode;
    }

    @JsonProperty("separator")
    @JsonInclude(Include.NON_NULL)
    public String getSeparator() {
        return _separator;
    }

    @JsonProperty("regex")
    @JsonInclude(Include.NON_NULL)
    public Boolean getRegex() {
        return _regex;
    }

    @JsonProperty("maxColumns")
    @JsonInclude(Include.NON_NULL)
    public Integer getMaxColumns() {
        return _maxColumns;
    }

    @JsonProperty("fieldLengths")
    @JsonInclude(Include.NON_NULL)
    public int[] getFieldLengths() {
        return _fieldLengths;
    }

    @Override
    public String getDescription() {
        return "Split column " + _columnName +
                (Mode.Separator.equals(_mode) ? " by separator" : " by field lengths");
    }

    public Change createChange() throws NotImmediateOperationException {
        return new ColumnSplitChange(
                _columnName,
                _mode,
                _separator,
                _regex,
                _maxColumns,
                _fieldLengths,
                _engineConfig,
                _removeOriginalColumn,
                _guessCellType);
    }

}
