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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jsoup.helper.Validate;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.ColumnInsertion;
import org.openrefine.operations.RowMapOperation;

public class ColumnRemovalOperation extends RowMapOperation {

    final protected List<String> _columnNames;

    /**
     * Constructor.
     *
     * @param columnNames
     *            list of column names to remove
     */
    public ColumnRemovalOperation(List<String> columnNames) {
        this(null, columnNames);
    }

    /**
     * Constructor for JSON deserialization, which accepts "columnName" as a single column to remove, for compatibility
     * with previous syntaxes.
     */
    @JsonCreator
    public ColumnRemovalOperation(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("columnNames") List<String> columnNames) {
        super(EngineConfig.ALL_ROWS);
        _columnNames = new ArrayList<>(columnNames == null ? Collections.emptyList() : columnNames);
        if (columnName != null && !_columnNames.contains(columnName)) {
            _columnNames.add(columnName);
        }
        Validate.isFalse(_columnNames.isEmpty(), "Empty list of columns to remove in column removal operation");
    }

    @JsonProperty("columnNames")
    public List<String> getColumnName() {
        return _columnNames;
    }

    @Override
    public String getDescription() {
        return _columnNames.size() == 1 ? "Remove column " + _columnNames.get(0) : "Remove columns " + String.join(", ", _columnNames);
    }

    @Override
    public List<ColumnInsertion> getColumnInsertions() {
        return Collections.emptyList();
    }

    @Override
    public Set<String> getColumnDeletions() {
        return _columnNames.stream().collect(Collectors.toSet());
    }

    // engine config is never useful, so we remove it from the JSON serialization
    @Override
    @JsonIgnore
    public EngineConfig getEngineConfig() {
        return super.getEngineConfig();
    }
}
