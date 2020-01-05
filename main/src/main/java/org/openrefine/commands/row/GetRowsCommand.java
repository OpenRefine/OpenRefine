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

package org.openrefine.commands.row;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.FilteredRecords;
import org.openrefine.browsing.FilteredRows;
import org.openrefine.browsing.RecordVisitor;
import org.openrefine.browsing.RowVisitor;
import org.openrefine.commands.Command;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingManager;
import org.openrefine.model.Project;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.sorting.SortingRecordVisitor;
import org.openrefine.sorting.SortingRowVisitor;
import org.openrefine.util.ParsingUtilities;

public class GetRowsCommand extends Command {

    protected static class WrappedRow {

        @JsonUnwrapped
        protected final Row row;
        @JsonProperty("i")
        protected final int rowIndex;
        @JsonProperty("j")
        @JsonInclude(Include.NON_NULL)
        protected final Integer recordIndex;

        protected WrappedRow(Row rowOrRecord, int rowIndex, Integer recordIndex) {
            this.row = rowOrRecord;
            this.rowIndex = rowIndex;
            this.recordIndex = recordIndex;
        }
    }

    protected static class JsonResult {

        @JsonProperty("mode")
        protected final Mode mode;
        @JsonProperty("rows")
        protected final List<WrappedRow> rows;
        @JsonProperty("filtered")
        protected final int filtered;
        @JsonProperty("total")
        protected final int totalCount;
        @JsonProperty("start")
        protected final int start;
        @JsonProperty("limit")
        protected final int limit;

        protected JsonResult(Mode mode, List<WrappedRow> rows, int filtered,
                int totalCount, int start, int limit) {
            this.mode = mode;
            this.rows = rows;
            this.filtered = filtered;
            this.totalCount = totalCount;
            this.start = start;
            this.limit = limit;
        }
    }

    /**
     * This command accepts both POST and GET. It is not CSRF-protected as it does not incur any state change.
     */

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        internalRespond(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        internalRespond(request, response);
    }

    protected void internalRespond(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            Project project = null;

            // This command also supports retrieving rows for an importing job.
            String importingJobID = request.getParameter("importingJobID");
            if (importingJobID != null) {
                long jobID = Long.parseLong(importingJobID);
                ImportingJob job = ImportingManager.getJob(jobID);
                if (job != null) {
                    project = job.project;
                }
            }
            if (project == null) {
                project = getProject(request);
            }

            Engine engine = getEngine(request, project);
            String callback = request.getParameter("callback");

            int start = Math.min(project.rows.size(), Math.max(0, getIntegerParameter(request, "start", 0)));
            int limit = Math.min(project.rows.size() - start, Math.max(0, getIntegerParameter(request, "limit", 20)));

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", callback == null ? "application/json" : "text/javascript");

            PrintWriter writer = response.getWriter();
            if (callback != null) {
                writer.write(callback);
                writer.write("(");
            }

            RowWritingVisitor rwv = new RowWritingVisitor(start, limit);

            SortingConfig sortingConfig = null;
            try {
                String sortingJson = request.getParameter("sorting");
                if (sortingJson != null) {
                    sortingConfig = SortingConfig.reconstruct(sortingJson);
                }
            } catch (IOException e) {
            }

            if (engine.getMode() == Mode.RowBased) {
                FilteredRows filteredRows = engine.getAllFilteredRows();
                RowVisitor visitor = rwv;

                if (sortingConfig != null) {
                    SortingRowVisitor srv = new SortingRowVisitor(visitor);

                    srv.initializeFromConfig(project, sortingConfig);
                    if (srv.hasCriteria()) {
                        visitor = srv;
                    }
                }
                filteredRows.accept(project, visitor);
            } else {
                FilteredRecords filteredRecords = engine.getFilteredRecords();
                RecordVisitor visitor = rwv;

                if (sortingConfig != null) {
                    SortingRecordVisitor srv = new SortingRecordVisitor(visitor);

                    srv.initializeFromConfig(project, sortingConfig);
                    if (srv.hasCriteria()) {
                        visitor = srv;
                    }
                }
                filteredRecords.accept(project, visitor);
            }

            JsonResult result = new JsonResult(engine.getMode(),
                    rwv.results, rwv.total,
                    engine.getMode() == Mode.RowBased ? project.rows.size() : project.recordModel.getRecordCount(),
                    start, limit);

            ParsingUtilities.defaultWriter.writeValue(writer, result);
            if (callback != null) {
                writer.write(")");
            }

            // metadata refresh for row mode and record mode
            if (project.getMetadata() != null) {
                project.getMetadata().setRowCount(project.rows.size());
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }

    static protected class RowWritingVisitor implements RowVisitor, RecordVisitor {

        final int start;
        final int limit;
        public List<WrappedRow> results;

        public int total;

        public RowWritingVisitor(int start, int limit) {
            this.start = start;
            this.limit = limit;
            this.results = new ArrayList<>();
        }

        @Override
        public void start(Project project) {
            // nothing to do
        }

        @Override
        public void end(Project project) {
            // nothing to do
        }

        @Override
        public boolean visit(Project project, int rowIndex, Row row) {
            if (total >= start && total < start + limit) {
                internalVisit(project, rowIndex, row);
            }
            total++;

            return false;
        }

        @Override
        public boolean visit(Project project, Record record) {
            if (total >= start && total < start + limit) {
                internalVisit(project, record);
            }
            total++;

            return false;
        }

        public boolean internalVisit(Project project, int rowIndex, Row row) {
            results.add(new WrappedRow(row, rowIndex, null));
            return false;
        }

        protected boolean internalVisit(Project project, Record record) {
            for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
                Row row = project.rows.get(r);
                results.add(new WrappedRow(row, r, r == record.fromRowIndex ? record.recordIndex : null));
            }
            return false;
        }
    }
}
