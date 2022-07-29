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

package com.google.refine.commands.column;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.browsing.util.ExpressionBasedRowEvaluable;
import com.google.refine.browsing.util.NumericBinIndex;
import com.google.refine.browsing.util.NumericBinRowIndex;
import com.google.refine.commands.Command;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class GetColumnsInfoCommand extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            Project project = getProject(request);

            JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(response.getWriter());

            writer.writeStartArray();
            for (Column column : project.columnModel.columns) {
                writer.writeStartObject();
                write(project, column, writer);
                writer.writeEndObject();
            }
            writer.writeEndArray();
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            respondException(response, e);
        }
    }

    private NumericBinIndex getBinIndex(Project project, Column column) {
        String expression = "value";
        String key = "numeric-bin:" + expression;
        Evaluable eval = null;
        try {
            eval = MetaParser.parse(expression);
        } catch (ParsingException e) {
            // this should never happen
        }
        NumericBinIndex index = (NumericBinIndex) column.getPrecompute(key);
        if (index == null) {
            index = new NumericBinRowIndex(project, new ExpressionBasedRowEvaluable(column.getName(), column.getCellIndex(), eval));
            column.setPrecompute(key, index);
        }
        return index;
    }

    private void write(Project project, Column column, JsonGenerator writer) throws IOException {
        NumericBinIndex columnIndex = getBinIndex(project, column);
        if (columnIndex != null) {
            writer.writeStringField("name", column.getName());
            boolean is_numeric = columnIndex.isNumeric();
            writer.writeBooleanField("is_numeric", is_numeric);
            writer.writeNumberField("numeric_row_count", columnIndex.getNumericRowCount());
            writer.writeNumberField("non_numeric_row_count", columnIndex.getNonNumericRowCount());
            writer.writeNumberField("error_row_count", columnIndex.getErrorRowCount());
            writer.writeNumberField("blank_row_count", columnIndex.getBlankRowCount());
            if (is_numeric) {
                writer.writeNumberField("min", columnIndex.getMin());
                writer.writeNumberField("max", columnIndex.getMax());
                writer.writeNumberField("step", columnIndex.getStep());
            }
        } else {
            writer.writeStringField("error", "error finding numeric information on the '" + column.getName() + "' column");
        }
    }
}
