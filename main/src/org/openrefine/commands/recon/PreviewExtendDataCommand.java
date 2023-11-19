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

package org.openrefine.commands.recon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import org.openrefine.commands.Command;
import org.openrefine.model.Cell;
import org.openrefine.model.Column;
import org.openrefine.model.Project;
import org.openrefine.model.ReconCandidate;
import org.openrefine.model.Row;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconciledDataExtensionJob;
import org.openrefine.model.recon.ReconciledDataExtensionJob.ColumnInfo;
import org.openrefine.model.recon.ReconciledDataExtensionJob.DataExtension;
import org.openrefine.model.recon.ReconciledDataExtensionJob.DataExtensionConfig;
import org.openrefine.model.recon.StandardReconConfig;
import org.openrefine.util.ParsingUtilities;

public class PreviewExtendDataCommand extends Command {

    protected static class PreviewResponse {

        public PreviewResponse(List<ColumnInfo> columns2, List<List<Object>> rows2) {
            columns = columns2;
            rows = rows2;
        }

        @JsonProperty("code")
        protected String code = "ok";
        @JsonProperty("columns")
        protected List<ColumnInfo> columns;
        @JsonProperty("rows")
        protected List<List<Object>> rows;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        try {
            Project project = getProject(request);
            String columnName = request.getParameter("columnName");

            String rowIndicesString = request.getParameter("rowIndices");
            if (rowIndicesString == null) {
                respond(response, "{ \"code\" : \"error\", \"message\" : \"No row indices specified\" }");
                return;
            }

            String jsonString = request.getParameter("extension");
            DataExtensionConfig config = DataExtensionConfig.reconstruct(jsonString);

            List<Integer> rowIndices = ParsingUtilities.mapper.readValue(rowIndicesString, new TypeReference<List<Integer>>() {
            });
            int length = rowIndices.size();
            Column column = project.columnModel.getColumnByName(columnName);
            int cellIndex = column.getCellIndex();

            // get the endpoint to extract data from
            String endpoint = null;
            ReconConfig cfg = column.getReconConfig();
            if (cfg != null &&
                    cfg instanceof StandardReconConfig) {
                StandardReconConfig scfg = (StandardReconConfig) cfg;
                endpoint = scfg.service;
            } else {
                respond(response,
                        "{ \"code\" : \"error\", \"message\" : \"This column has not been reconciled with a standard service.\" }");
                return;
            }

            List<String> topicNames = new ArrayList<String>();
            List<String> topicIds = new ArrayList<String>();
            Set<String> ids = new HashSet<String>();
            for (int i = 0; i < length; i++) {
                int rowIndex = rowIndices.get(i);
                if (rowIndex >= 0 && rowIndex < project.rows.size()) {
                    Row row = project.rows.get(rowIndex);
                    Cell cell = row.getCell(cellIndex);
                    if (cell != null && cell.recon != null && cell.recon.match != null) {
                        topicNames.add(cell.recon.match.name);
                        topicIds.add(cell.recon.match.id);
                        ids.add(cell.recon.match.id);
                    } else {
                        topicNames.add(null);
                        topicIds.add(null);
                        ids.add(null);
                    }
                }
            }

            Map<String, ReconCandidate> reconCandidateMap = new HashMap<String, ReconCandidate>();
            ReconciledDataExtensionJob job = new ReconciledDataExtensionJob(config, endpoint);
            Map<String, DataExtension> map = job.extend(ids, reconCandidateMap);
            List<List<Object>> rows = new ArrayList<>();

            for (int r = 0; r < topicNames.size(); r++) {
                String id = topicIds.get(r);
                String topicName = topicNames.get(r);

                if (id != null && map.containsKey(id)) {
                    DataExtension ext = map.get(id);
                    boolean first = true;

                    if (ext.data.length > 0) {
                        for (Object[] row : ext.data) {
                            List<Object> jsonRow = new ArrayList<>();
                            if (first) {
                                jsonRow.add(topicName);
                                first = false;
                            } else {
                                jsonRow.add(null);
                            }

                            for (Object cell : row) {
                                jsonRow.add(cell);
                            }
                            rows.add(jsonRow);
                        }
                        continue;
                    }
                }

                List<Object> supplement = new ArrayList<>();
                if (id != null) {
                    supplement.add(new ReconCandidate(id, topicName, new String[0], 100));
                } else {
                    supplement.add("<not reconciled>");
                }
                rows.add(supplement);
            }

            respondJSON(response, new PreviewResponse(job.columns, rows));
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
