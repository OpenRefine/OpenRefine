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
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.commands.Command;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingManager;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.util.ParsingUtilities;

public class GetRowsCommand extends Command {

    protected static class WrappedRow {

        @JsonUnwrapped
        protected final Row row;
        @JsonProperty("i")
        protected final long rowIndex;
        @JsonProperty("j")
        @JsonInclude(Include.NON_NULL)
        protected final Integer recordIndex;

        protected WrappedRow(Row rowOrRecord, long rowIndex, Integer recordIndex) {
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
        protected final long filtered;
        @JsonProperty("total")
        protected final long totalCount;
        @JsonProperty("start")
        protected final long start;
        @JsonProperty("limit")
        protected final int limit;

        protected JsonResult(Mode mode, List<WrappedRow> rows, long filtered,
                long totalCount, long start, int limit) {
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
            GridState state = engine.getMatchingRows();

            long start = getLongParameter(request, "start", 0);
            int limit = Math.max(0, getIntegerParameter(request, "limit", 20));
            long totalCount = project.getCurrentGridState().size();

            List<Tuple2<Long, Row>> rows = state.getGrid().filter(new Function<Tuple2<Long, Row>, Boolean>() {

                private static final long serialVersionUID = 1L;

                @Override
                public Boolean call(Tuple2<Long, Row> v1) throws Exception {
                    return v1._1() >= start;
                }

            }).take(limit);

            List<WrappedRow> wrappedRows = rows.stream()
                    .map(tuple -> new WrappedRow(tuple._2(), tuple._1(), null))
                    .collect(Collectors.toList());

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", callback == null ? "application/json" : "text/javascript");

            PrintWriter writer = response.getWriter();
            if (callback != null) {
                writer.write(callback);
                writer.write("(");
            }

            // TODO add support for sorting
            /*
             * SortingConfig sortingConfig = null; try { String sortingJson = request.getParameter("sorting"); if
             * (sortingJson != null) { sortingConfig = SortingConfig.reconstruct(sortingJson); } } catch (IOException e)
             * { }
             */

            if (engine.getMode() == Mode.RecordBased) {
                throw new IllegalStateException("not implemented");

            }

            JsonResult result = new JsonResult(engine.getMode(),
                    wrappedRows, state.size(),
                    totalCount, start, limit);

            ParsingUtilities.defaultWriter.writeValue(writer, result);
            if (callback != null) {
                writer.write(")");
            }

            // metadata refresh for row mode and record mode
            if (project.getMetadata() != null) {
                project.getMetadata().setRowCount(totalCount);
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
