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

package com.google.refine.commands.row;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordVisitor;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.commands.Command;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;
import com.google.refine.sorting.SortingConfig;
import com.google.refine.sorting.SortingRecordVisitor;
import com.google.refine.sorting.SortingRowVisitor;
import com.google.refine.util.Pool;

/**
 * Retrieves rows from a project (or importing job).
 *
 * Those rows can be requested as either: - the batch of rows starting at a given index (included), up to a certain size
 * - the batch of rows ending at a given index (excluded), again up to a given size. Filters (defined by facets) and the
 * row/record mode toggle can also be provided.
 */
public class GetRowsCommand extends Command {

    protected static class WrappedRow {

        @JsonUnwrapped
        protected final Row row;

        /**
         * The logical row index (meaning the one from the original, unsorted grid)
         */
        @JsonProperty("i")
        protected final int rowIndex;
        @JsonProperty("j")
        @JsonInclude(Include.NON_NULL)
        protected final Integer recordIndex;

        /**
         * The row index used for pagination (which can be different from the logical one if a temporary sort is
         * applied)
         */
        @JsonProperty("k")
        protected final int paginationIndex;

        protected WrappedRow(Row rowOrRecord, int rowIndex, Integer recordIndex, int paginationIndex) {
            this.row = rowOrRecord;
            this.rowIndex = rowIndex;
            this.recordIndex = recordIndex;
            this.paginationIndex = paginationIndex;
        }
    }

    protected static class JsonResult {

        /**
         * Mode of the engine (row or record based)
         */
        @JsonProperty("mode")
        protected final Mode mode;
        /**
         * Rows in the view
         */
        @JsonProperty("rows")
        protected final List<WrappedRow> rows;
        /**
         * Number of rows selected by the current filter
         */
        @JsonProperty("filtered")
        protected final int filtered;
        /**
         * Total number of rows/records in the unfiltered grid
         */
        @JsonProperty("total")
        protected final int totalCount;
        /**
         * Total number of rows in the unfiltered grid (needed to provide a link to the last page)
         */
        @JsonProperty("totalRows")
        protected final int totalRows;

        @JsonProperty("start")
        @JsonInclude(Include.NON_NULL)
        protected final Integer start;
        @JsonProperty("end")
        @JsonInclude(Include.NON_NULL)
        protected final Integer end;
        @JsonProperty("limit")
        protected final int limit;
        @JsonProperty("pool")
        protected final Pool pool;

        /**
         * The value to use as 'end' when fetching the page before this one. Can be null if there is no such page.
         */
        @JsonProperty("previousPageEnd")
        @JsonInclude(Include.NON_NULL)
        protected final Integer previousPageEnd;
        /**
         * The value to use as 'start' when fetching the page after this one. Can be null if there is no such page.
         */
        @JsonProperty("nextPageStart")
        @JsonInclude(Include.NON_NULL)
        protected final Integer nextPageStart;

        protected JsonResult(Mode mode, List<WrappedRow> rows, int filtered,
                int totalCount, int totalRows, int start, int end, int limit, Pool pool, Integer previousPageEnd, Integer nextPageStart) {
            this.mode = mode;
            this.rows = rows;
            this.filtered = filtered;
            this.totalCount = totalCount;
            this.totalRows = totalRows;
            this.start = start == -1 ? null : start;
            this.end = end == -1 ? null : end;
            this.limit = limit;
            this.pool = pool;
            this.previousPageEnd = previousPageEnd;
            this.nextPageStart = nextPageStart;
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
            checkJSONP(request); // We used to support JSONP, but don't anymore
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

            int start = getIntegerParameter(request, "start", -1);
            int end = getIntegerParameter(request, "end", -1);
            int limit = Math.max(0, getIntegerParameter(request, "limit", 20));

            if ((start == -1L) == (end == -1L)) {
                respondException(response, new IllegalArgumentException("Exactly one of 'start' and 'end' should be provided."));
                return;
            }

            Pool pool = new Pool();

            RowWritingVisitor rwv = new RowWritingVisitor(start, end, limit);

            SortingConfig sortingConfig = null;
            try {
                String sortingJson = request.getParameter("sorting");
                if (sortingJson != null) {
                    sortingConfig = SortingConfig.reconstruct(sortingJson);
                }
            } catch (IOException e) {
                respondException(response, e);
                return;
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

            // Pool all the recons occurring in the rows seen
            for (WrappedRow wr : rwv.results) {
                for (Cell c : wr.row.cells) {
                    if (c != null && c.recon != null) {
                        pool.pool(c.recon);
                    }
                }
            }

            List<WrappedRow> wrappedRows = rwv.results;

            // Compute the indices of the previous and next pages
            Integer previousPageEnd = null;
            Integer nextPageStart = null;
            if (start != -1) {
                if (start > 0) {
                    previousPageEnd = start;
                }
                if (!wrappedRows.isEmpty()) {
                    nextPageStart = wrappedRows.get(wrappedRows.size() - 1).paginationIndex + 1;
                }
            } else {
                if (!wrappedRows.isEmpty() && wrappedRows.get(0).paginationIndex > 0) {
                    previousPageEnd = wrappedRows.get(0).paginationIndex;
                }
                nextPageStart = end;
            }

            JsonResult result = new JsonResult(engine.getMode(),
                    rwv.results, rwv.total,
                    engine.getMode() == Mode.RowBased ? project.rows.size() : project.recordModel.getRecordCount(),
                    rwv.totalRows, start, end, limit, pool, previousPageEnd, nextPageStart);

            respondJSON(response, result);
        } catch (IllegalJsonpException e2) {
            respondNoJsonpException(request, response);
        } catch (Exception e) {
            respondException(response, e);
        }
    }

    static protected class RowWritingVisitor implements RowVisitor, RecordVisitor {

        final int start;
        final int end;
        final int limit;
        public LinkedList<WrappedRow> results;
        public int resultRecordSize;

        public int total;
        public int totalRows;

        public RowWritingVisitor(int start, int end, int limit) {
            this.start = start;
            this.end = end;
            this.limit = limit;
            this.results = new LinkedList<>();
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
            return visit(project, rowIndex, rowIndex, row);
        }

        @Override
        public boolean visit(Project project, int rowIndex, int sortedRowIndex, Row row) {
            if ((start != -1 && sortedRowIndex >= start && results.size() < limit) ||
                    (end != -1 && sortedRowIndex < end)) {
                if (results.size() >= limit) {
                    results.removeFirst();
                }
                internalVisit(project, rowIndex, row, sortedRowIndex);
            }
            total++;
            totalRows++;

            return false;
        }

        @Override
        public boolean visit(Project project, Record record) {
            return visit(project, record.fromRowIndex, record);
        }

        @Override
        public boolean visit(Project project, int sortedStartRowIndex, Record record) {
            if ((start != -1 && sortedStartRowIndex >= start && resultRecordSize < limit) ||
                    (end != -1 && sortedStartRowIndex < end)) {
                if (resultRecordSize >= limit) {
                    // remove the oldest record in the results
                    results.removeFirst();
                    while (results.size() > 0 && results.get(0).recordIndex == null) {
                        results.removeFirst();
                    }
                    resultRecordSize--;
                }
                internalVisit(project, record, sortedStartRowIndex);
                resultRecordSize++;
            }
            total++;
            totalRows += record.toRowIndex - record.fromRowIndex;

            return false;
        }

        protected boolean internalVisit(Project project, int rowIndex, Row row, int paginationIndex) {
            results.add(new WrappedRow(row, rowIndex, null, paginationIndex));
            return false;
        }

        protected boolean internalVisit(Project project, Record record, int sortedStartRowIndex) {
            for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
                Row row = project.rows.get(r);
                results.add(new WrappedRow(row, r, r == record.fromRowIndex ? record.recordIndex : null,
                        sortedStartRowIndex + r - record.fromRowIndex));
            }
            return false;
        }
    }
}
