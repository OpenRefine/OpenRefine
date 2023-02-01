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

package org.openrefine.operations.recon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.DataExtensionChange;
import org.openrefine.model.recon.ReconType;
import org.openrefine.model.recon.ReconciledDataExtensionJob;
import org.openrefine.model.recon.ReconciledDataExtensionJob.ColumnInfo;
import org.openrefine.model.recon.ReconciledDataExtensionJob.DataExtensionConfig;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.OperationDescription;

public class ExtendDataOperation extends EngineDependentOperation {

    @JsonProperty("baseColumnName")
    final protected String _baseColumnName;
    @JsonProperty("endpoint")
    final protected String _endpoint;
    @JsonProperty("identifierSpace")
    final protected String _identifierSpace;
    @JsonProperty("schemaSpace")
    final protected String _schemaSpace;
    @JsonProperty("extension")
    final protected DataExtensionConfig _extension;
    @JsonProperty("columnInsertIndex")
    final protected int _columnInsertIndex;

    @JsonCreator
    public ExtendDataOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("baseColumnName") String baseColumnName,
            @JsonProperty("endpoint") String endpoint,
            @JsonProperty("identifierSpace") String identifierSpace,
            @JsonProperty("schemaSpace") String schemaSpace,
            @JsonProperty("extension") DataExtensionConfig extension,
            @JsonProperty("columnInsertIndex") int columnInsertIndex) {
        super(engineConfig);

        _baseColumnName = baseColumnName;
        _endpoint = endpoint;
        _identifierSpace = identifierSpace;
        _schemaSpace = schemaSpace;
        _extension = extension;
        _columnInsertIndex = columnInsertIndex;
    }

    @Override
    public Change createChange() throws ParsingException {
        ReconciledDataExtensionJob job = new ReconciledDataExtensionJob(_extension, _endpoint, _identifierSpace, _schemaSpace);

        /**
         * Prefetch column names with an initial request.
         */
        try {
            job.extend(Collections.emptySet());
        } catch (Exception e) {
            throw new ParsingException("Unable to fetch column metadata from service: " + e.getMessage());
        }
        List<String> columnNames = new ArrayList<>();
        for (ColumnInfo info : job.columns) {
            columnNames.add(info.name);
        }

        List<ReconType> columnTypes = new ArrayList<>();
        for (ColumnInfo info : job.columns) {
            columnTypes.add(info.expectedType);
        }

        return new DataExtensionChange(
                _engineConfig,
                _baseColumnName,
                _endpoint,
                _identifierSpace,
                _schemaSpace,
                _columnInsertIndex,
                columnNames,
                columnTypes,
                _extension);
    }

    @Override
    public String getDescription() {
        return OperationDescription.recon_extend_data_brief(_columnInsertIndex, _baseColumnName);
    }
}
