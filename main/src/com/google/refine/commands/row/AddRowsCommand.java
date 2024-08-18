/*******************************************************************************
 * Copyright (C) 2024, OpenRefine contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.commands.row;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.operations.row.RowAdditionOperation;
import com.google.refine.process.Process;
import com.google.refine.util.Pool;

public class AddRowsCommand extends Command {

    static String ROWS_PARAMETER = "rows[]";
    static String INDEX_PARAMETER = "index";

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }

        try {
            Project project = getProject(request);
            List<Row> rows = getRowData(request);
            int insertionIndex = getInsertionIndex(request, project);

            AbstractOperation op = new RowAdditionOperation(rows, insertionIndex);
            Process process = op.createProcess(project, new Properties());

            performProcessAndRespond(request, response, project, process);
        } catch (Exception e) {
            respondException(response, e);
        }
    }

    public int getInsertionIndex(HttpServletRequest request, Project project) {
        String data = request.getParameter(INDEX_PARAMETER);
        int index = Integer.parseInt(data);
        if (index < 0 || index > project.rows.size()) {
            throw new IndexOutOfBoundsException("Parameter " + INDEX_PARAMETER + " out of bounds");
        }
        return index;
    }

    public List<Row> getRowData(HttpServletRequest request) throws Exception {
        String[] data = request.getParameterValues(ROWS_PARAMETER);
        if (data.length == 0) {
            throw new IllegalArgumentException("Parameter " + ROWS_PARAMETER + " is empty");
        }
        List<Row> rows = new ArrayList<>(data.length);
        Pool pool = new Pool();
        for (String rowStr : data) {
            Row row = Row.load(rowStr, pool);
            if (!Objects.equals(rowStr, "{}")) {
                throw new IllegalArgumentException("Row is not empty");
            }
            rows.add(row);
        }
        return rows;
    }

}
