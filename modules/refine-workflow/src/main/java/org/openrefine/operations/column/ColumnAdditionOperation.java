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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.ColumnInsertion;
import org.openrefine.model.ColumnModel;
import org.openrefine.operations.ExpressionBasedOperation;
import org.openrefine.operations.OnError;

/**
 * Adds a new column by evaluating an expression, based on a given column.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ColumnAdditionOperation extends ExpressionBasedOperation {

    final protected String _newColumnName;

    @JsonCreator
    public ColumnAdditionOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("baseColumnName") String baseColumnName,
            @JsonProperty("expression") String expression,
            @JsonProperty("onError") OnError onError,
            @JsonProperty("newColumnName") String newColumnName) {
        super(engineConfig, expression, baseColumnName, onError, 0);

        _newColumnName = newColumnName;
    }

    @JsonProperty("newColumnName")
    public String getNewColumnName() {
        return _newColumnName;
    }

    @JsonProperty("baseColumnName")
    public String getBaseColumnName() {
        return _baseColumnName;
    }

    @JsonProperty("expression")
    public String getExpression() {
        return _expression;
    }

    @JsonProperty("onError")
    public OnError getOnError() {
        return _onError;
    }

    @Override
    public String getDescription() {
        return "Create column " + _newColumnName +
                " based on column " + _baseColumnName +
                " using expression " + _expression;
    }

    @Override
    public List<ColumnInsertion> getColumnInsertions() {
        return Collections.singletonList(
                ColumnInsertion.builder()
                        .withName(_newColumnName)
                        .withInsertAt(_baseColumnName)
                        .build());

    }

    @Override
    protected boolean preservesRecordStructure(ColumnModel columnModel) {
        int baseColumnIndex = columnModel.getColumnIndexByName(_baseColumnName);
        return columnModel.getKeyColumnIndex() <= baseColumnIndex;
    }

}
