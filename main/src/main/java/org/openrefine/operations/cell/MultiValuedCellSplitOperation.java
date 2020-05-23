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

package org.openrefine.operations.cell;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.expr.ParsingException;
import org.openrefine.history.Change;
import org.openrefine.model.changes.ColumnSplitChange.Mode;
import org.openrefine.model.changes.MultiValuedCellSplitChange;
import org.openrefine.operations.ImmediateOperation;

public class MultiValuedCellSplitOperation extends ImmediateOperation {

    final protected String _columnName;
    final protected String _keyColumnName;
    final protected Mode _mode;
    final protected String _separator;
    final protected Boolean _regex;

    final protected int[] _fieldLengths;

    @JsonCreator
    public static MultiValuedCellSplitOperation deserialize(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("keyColumnName") String keyColumnName,
            @JsonProperty("mode") Mode mode,
            @JsonProperty("separator") String separator,
            @JsonProperty("regex") boolean regex,
            @JsonProperty("fieldLengths") int[] fieldLengths) {
        if (Mode.Separator.equals(mode)) {
            return new MultiValuedCellSplitOperation(
                    columnName,
                    keyColumnName,
                    separator,
                    regex);
        } else {
            return new MultiValuedCellSplitOperation(
                    columnName,
                    keyColumnName,
                    fieldLengths);
        }
    }

    public MultiValuedCellSplitOperation(
            String columnName,
            String keyColumnName,
            String separator,
            boolean regex) {
        _columnName = columnName;
        _keyColumnName = keyColumnName;
        _separator = separator;
        _mode = Mode.Separator;
        _regex = regex;

        _fieldLengths = null;
    }

    public MultiValuedCellSplitOperation(
            String columnName,
            String keyColumnName,
            int[] fieldLengths) {
        _columnName = columnName;
        _keyColumnName = keyColumnName;

        _mode = Mode.Lengths;
        _separator = null;
        _regex = null;

        _fieldLengths = fieldLengths;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("keyColumnName")
    public String getKeyColumnName() {
        return _keyColumnName;
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

    @JsonProperty("fieldLengths")
    @JsonInclude(Include.NON_NULL)
    public int[] getFieldLengths() {
        return _fieldLengths;
    }

    @Override
    public String getDescription() {
        return "Split multi-valued cells in column " + _columnName;
    }

    @Override
    public Change createChange() throws ParsingException {
        return new MultiValuedCellSplitChange(_columnName, _mode, _separator, _regex, _fieldLengths);
    }

}
