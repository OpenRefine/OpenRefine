/*

Copyright 2011, Google Inc.
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
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.history.Change;
import org.openrefine.model.changes.KeyValueColumnizeChange;
import org.openrefine.operations.Operation;

/**
 * Reshapes a table which contains key and value columns, such that the repeating contents in the key column become new
 * column names, and the contents of the value column are spread in the new columns.
 */
public class KeyValueColumnizeOperation implements Operation {

    final protected String _keyColumnName;
    final protected String _valueColumnName;
    final protected String _noteColumnName;

    @JsonCreator
    public KeyValueColumnizeOperation(
            @JsonProperty("keyColumnName") String keyColumnName,
            @JsonProperty("valueColumnName") String valueColumnName,
            @JsonProperty("noteColumnName") String noteColumnName) {
        _keyColumnName = keyColumnName;
        _valueColumnName = valueColumnName;
        _noteColumnName = noteColumnName;
    }

    @JsonProperty("keyColumnName")
    public String getKeyColumnName() {
        return _keyColumnName;
    }

    @JsonProperty("valueColumnName")
    public String getValueColumnName() {
        return _valueColumnName;
    }

    @JsonProperty("noteColumnName")
    public String getNoteColumnName() {
        return _noteColumnName;
    }

    @Override
    public String getDescription() {
        return "Columnize by key column " +
                _keyColumnName + " and value column " + _valueColumnName +
                (_noteColumnName != null ? (" with note column " + _noteColumnName) : "");
    }

    @Override
    public Change createChange() {
        return new KeyValueColumnizeChange(_keyColumnName, _valueColumnName, _noteColumnName);
    }

}
