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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.commands.Command;
import org.openrefine.history.History;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingManager;
import org.openrefine.model.Grid;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Project;
import org.openrefine.model.Record;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.sorting.SortingConfig;

/**
 * Retrieves rows from a project (or importing job). Those rows can be requested as either:
 * <ul>
 * <li>the batch of rows starting at a given index (included), up to a certain size</li>
 * <li>the batch of rows ending at a given index (excluded), again up to a given size.</li>
 * </ul>
 * Filters (defined by facets), sorting options and the row/record mode toggle can also be provided.
 */
public class GetRowsCommand extends Command {

    protected static class WrappedRow {

        @JsonUnwrapped
        protected final Row row;

        /**
         * The logical row index (meaning the one from the original, unsorted grid)
         */
        @JsonProperty("i")
        protected final long rowIndex;
        @JsonProperty("j")
        @JsonInclude(Include.NON_NULL)
        protected final Long recordIndex;

        /**
         * The row index used for pagination (which can be different from the logical one if a temporary sort is
         * applied)
         */
        @JsonProperty("k")
        protected final long paginationIndex;

        protected WrappedRow(Row rowOrRecord, long rowIndex, Long recordIndex, long paginationIndex) {
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

        @JsonProperty("start")
        @JsonInclude(Include.NON_NULL)
        protected final Long start;
        @JsonProperty("end")
        @JsonInclude(Include.NON_NULL)
        protected final Long end;
        @JsonProperty("limit")
        protected final int limit;

        /**
         * The value to use as 'end' when fetching the page before this one. Can be null if there is no such page.
         */
        @JsonProperty("previousPageId")
        @JsonInclude(Include.NON_NULL)
        protected final Long previousPageId;
        /**
         * The value to use as 'start' when fetching the page after this one. Can be null if there is no such page.
         */
        @JsonProperty("nextPageId")
        @JsonInclude(Include.NON_NULL)
        protected final Long nextPageId;

        @JsonProperty("needsRefreshing")
        protected final boolean needsRefreshing;

        @JsonProperty("historyEntryId")
        protected final long historyEntryId;

        protected JsonResult(Mode mode, long historyEntryId, List<WrappedRow> rows, long start, long end, int limit, Long previousPageId,
                Long nextPageId, boolean refreshNeeded) {
            this.mode = mode;
            this.historyEntryId = historyEntryId;
            this.rows = rows;
            this.start = start == -1 ? null : start;
            this.end = end == -1 ? null : end;
            this.limit = Math.min(rows.size(), limit);
            this.previousPageId = previousPageId;
            this.nextPageId = nextPageId;
            this.needsRefreshing = refreshNeeded;
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

    protected static List<WrappedRow> recordToWrappedRows(Record record) {
        List<Row> rows = record.getRows();
        return IntStream.range(0, rows.size())
                .mapToObj(i -> new WrappedRow(
                        rows.get(i),
                        record.getLogicalStartRowId() + i,
                        i == 0 ? record.getLogicalStartRowId() : null,
                        record.getStartRowId() + i))
                .collect(Collectors.toList());
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
                    project = job.getProject();
                }
            }
            if (project == null) {
                project = getProject(request);
            }

            History history = project.getHistory();
            Engine engine;
            Grid entireGrid;
            boolean gridNeedsRefreshing;
            long historyEntryId;
            synchronized (history) {
                history.refreshCurrentGrid();
                engine = getEngine(request, project);
                entireGrid = history.getCurrentGrid();
                gridNeedsRefreshing = history.currentGridNeedsRefreshing();
                historyEntryId = history.getCurrentEntryId();
            }

            long start = getLongParameter(request, "start", -1L);
            long end = getLongParameter(request, "end", -1L);
            int limit = Math.max(0, getIntegerParameter(request, "limit", 20));

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            if ((start == -1L) == (end == -1L)) {
                throw new IllegalArgumentException("Exactly one of 'start' and 'end' should be provided.");
            }

            SortingConfig sortingConfig = SortingConfig.NO_SORTING;
            try {
                String sortingJson = request.getParameter("sorting");
                if (sortingJson != null) {
                    sortingConfig = SortingConfig.reconstruct(sortingJson);
                }
            } catch (IOException e) {
                respondException(response, e);
                return;
            }

            Grid sortedGrid = entireGrid;
            if (!SortingConfig.NO_SORTING.equals(sortingConfig)) {
                // TODO cache this appropriately
                sortedGrid = entireGrid.reorderRows(sortingConfig, false);
            }

            List<WrappedRow> wrappedRows;
            Long previousPageId = null;
            Long nextPageId = null;
            if (engine.getMode() == Mode.RowBased) {
                RowFilter combinedRowFilters = engine.combinedRowFilters();

                List<IndexedRow> rows;
                if (start != -1L) {
                    rows = sortedGrid.getRowsAfter(combinedRowFilters, start, limit + 1);
                    if (rows.size() == limit + 1) {
                        nextPageId = rows.get(limit).getIndex();
                        rows = rows.subList(0, limit);
                    }

                    if (start > 0) {
                        previousPageId = start;
                    }
                } else {
                    rows = sortedGrid.getRowsBefore(combinedRowFilters, end, limit + 1);
                    if (rows.size() == limit + 1) {
                        previousPageId = rows.get(0).getIndex() + 1;
                        rows = rows.subList(1, limit + 1);
                    } else if (rows.size() < limit) {
                        rows.addAll(sortedGrid.getRowsAfter(combinedRowFilters, end, limit - rows.size()));
                    }
                    if (end < entireGrid.rowCount()) {
                        nextPageId = end;
                    }
                }

                wrappedRows = rows.stream()
                        .map(tuple -> new WrappedRow(tuple.getRow(), tuple.getLogicalIndex(), null, tuple.getIndex()))
                        .collect(Collectors.toList());
            } else {
                RecordFilter combinedRecordFilters = engine.combinedRecordFilters();
                List<Record> records;
                if (start != -1L) {
                    records = sortedGrid.getRecordsAfter(combinedRecordFilters, start, limit + 1);
                    if (records.size() == limit + 1) {
                        nextPageId = records.get(limit).getStartRowId();
                        records = records.subList(0, limit);
                    }
                    if (start > 0) {
                        previousPageId = start;
                    }
                } else {
                    records = sortedGrid.getRecordsBefore(combinedRecordFilters, end, limit + 1);
                    if (records.size() == limit + 1) {
                        previousPageId = records.get(0).getStartRowId() + 1;
                        records = records.subList(1, limit + 1);
                    } else if (records.size() < limit) {
                        records.addAll(sortedGrid.getRecordsAfter(combinedRecordFilters, end, limit - records.size()));
                    }
                    if (end < entireGrid.rowCount()) {
                        nextPageId = end;
                    }
                }

                wrappedRows = records.stream()
                        .flatMap(record -> recordToWrappedRows(record).stream())
                        .collect(Collectors.toList());
            }

            // the first condition is overly optimistic: we could also need to refresh even if we are returning
            // as many rows as the limit, because perhaps those rows are coming from distinct partitions and so
            // the fetching is happening between them, leading to a change of the filtered rows in view.
            // That being said, this does not feel critical, and the performance benefit of not refreshing in such
            // a case feels higher.
            boolean needsRefreshing = (wrappedRows.size() < limit && gridNeedsRefreshing)
                    || wrappedRows.stream().anyMatch(
                            wrappedRow -> wrappedRow.row.getCells().stream().anyMatch(cell -> cell != null && cell.isPending()));

            JsonResult result = new JsonResult(engine.getMode(), historyEntryId,
                    wrappedRows, start, end,
                    limit, previousPageId, nextPageId, needsRefreshing);

            respondJSON(response, result);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
