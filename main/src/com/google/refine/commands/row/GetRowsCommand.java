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
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordVisitor;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.commands.Command;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;
import com.google.refine.sorting.SortingRecordVisitor;
import com.google.refine.sorting.SortingRowVisitor;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class GetRowsCommand extends Command {
    
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
            
            Pool pool = new Pool();
            Properties options = new Properties();
            options.put("project", project);
            options.put("reconCandidateOmitTypes", true);
            options.put("pool", pool);
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", callback == null ? "application/json" : "text/javascript");
            
            PrintWriter writer = response.getWriter();
            if (callback != null) {
                writer.write(callback);
                writer.write("(");
            }
            
            JSONWriter jsonWriter = new JSONWriter(writer);
            jsonWriter.object();
            
            RowWritingVisitor rwv = new RowWritingVisitor(start, limit, jsonWriter, options);
            
            JSONObject sortingJson = null;
            try{
                String json = request.getParameter("sorting");
                sortingJson = (json == null) ? null : 
                    ParsingUtilities.evaluateJsonStringToObject(json);
            } catch (JSONException e) {
            }

            if (engine.getMode() == Mode.RowBased) {
                FilteredRows filteredRows = engine.getAllFilteredRows();
                RowVisitor visitor = rwv;
                
                if (sortingJson != null) {
                    SortingRowVisitor srv = new SortingRowVisitor(visitor);
                    
                    srv.initializeFromJSON(project, sortingJson);
                    if (srv.hasCriteria()) {
                        visitor = srv;
                    }
                }
                
                jsonWriter.key("mode"); jsonWriter.value("row-based");
                jsonWriter.key("rows"); jsonWriter.array();
                filteredRows.accept(project, visitor);
                jsonWriter.endArray();
                jsonWriter.key("filtered"); jsonWriter.value(rwv.total);
                jsonWriter.key("total"); jsonWriter.value(project.rows.size());
            } else {
                FilteredRecords filteredRecords = engine.getFilteredRecords();
                RecordVisitor visitor = rwv;
                
                if (sortingJson != null) {
                    SortingRecordVisitor srv = new SortingRecordVisitor(visitor);
                    
                    srv.initializeFromJSON(project, sortingJson);
                    if (srv.hasCriteria()) {
                        visitor = srv;
                    }
                }
                
                jsonWriter.key("mode"); jsonWriter.value("record-based");
                jsonWriter.key("rows"); jsonWriter.array();
                filteredRecords.accept(project, visitor);
                jsonWriter.endArray();
                jsonWriter.key("filtered"); jsonWriter.value(rwv.total);
                jsonWriter.key("total"); jsonWriter.value(project.recordModel.getRecordCount());
            }
            
            
            jsonWriter.key("start"); jsonWriter.value(start);
            jsonWriter.key("limit"); jsonWriter.value(limit);
            jsonWriter.key("pool"); pool.write(jsonWriter, options);
            
            jsonWriter.endObject();
            
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
        final int           start;
        final int           limit;
        final JSONWriter  writer;
        final Properties  options;
        
        public int total;
        
        public RowWritingVisitor(int start, int limit, JSONWriter writer, Properties options) {
            this.start = start;
            this.limit = limit;
            this.writer = writer;
            this.options = options;
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
            try {
                options.put("rowIndex", rowIndex);
                row.write(writer, options);
            } catch (JSONException e) {
            }
            return false;
        }
        
        protected boolean internalVisit(Project project, Record record) {
            options.put("recordIndex", record.recordIndex);
            
            for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
                try {
                    Row row = project.rows.get(r);
                    
                    options.put("rowIndex", r);
                    
                    row.write(writer, options);
                    
                } catch (JSONException e) {
                }
                
                options.remove("recordIndex");
            }
            return false;
        }
    }
}
