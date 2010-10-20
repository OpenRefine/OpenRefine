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

package com.google.refine.commands.expr;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.commands.Command;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.HasFields;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.expr.WrappedCell;
import com.google.refine.expr.WrappedRow;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public class PreviewExpressionCommand extends Command {
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            int cellIndex = Integer.parseInt(request.getParameter("cellIndex"));
            String columnName = cellIndex < 0 ? "" : project.columnModel.getColumnByCellIndex(cellIndex).getName();
            
            String expression = request.getParameter("expression");
            String rowIndicesString = request.getParameter("rowIndices");
            if (rowIndicesString == null) {
                respond(response, "{ \"code\" : \"error\", \"message\" : \"No row indices specified\" }");
                return;
            }
            
            boolean repeat = "true".equals(request.getParameter("repeat"));
            int repeatCount = 10;
            if (repeat) {
                String repeatCountString = request.getParameter("repeatCount");
                try {
                    repeatCount = Math.max(Math.min(Integer.parseInt(repeatCountString), 10), 0);
                } catch (Exception e) {
                }
            }
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONArray rowIndices = ParsingUtilities.evaluateJsonStringToArray(rowIndicesString);
            int length = rowIndices.length();
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            
            try {
                Evaluable eval = MetaParser.parse(expression);
                
                writer.key("code"); writer.value("ok");
                writer.key("results"); writer.array();
                
                Properties bindings = ExpressionUtils.createBindings(project);
                for (int i = 0; i < length; i++) {
                    Object result = null;
                    
                    int rowIndex = rowIndices.getInt(i);
                    if (rowIndex >= 0 && rowIndex < project.rows.size()) {
                        Row row = project.rows.get(rowIndex);
                        Cell cell = row.getCell(cellIndex);
                            
                        try {
                            ExpressionUtils.bind(bindings, row, rowIndex, columnName, cell);
                            result = eval.evaluate(bindings);
                            
                            if (repeat) {
                                for (int r = 0; r < repeatCount && ExpressionUtils.isStorable(result); r++) {
                                    Cell newCell = new Cell((Serializable) result, (cell != null) ? cell.recon : null);
                                    ExpressionUtils.bind(bindings, row, rowIndex, columnName, newCell);
                                    
                                    Object newResult = eval.evaluate(bindings);
                                    if (ExpressionUtils.isError(newResult)) {
                                        break;
                                    } else if (ExpressionUtils.sameValue(result, newResult)) {
                                        break;
                                    } else {
                                        result = newResult;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    
                    if (result == null) {
                        writer.value(null);
                    } else if (ExpressionUtils.isError(result)) {
                        writer.object();
                        writer.key("message"); writer.value(((EvalError) result).message);
                        writer.endObject();
                    } else {
                        StringBuffer sb = new StringBuffer();
                        
                        writeValue(sb, result, false);
                        
                        writer.value(sb.toString());
                    }
                }
                writer.endArray();
            } catch (ParsingException e) {
                writer.key("code"); writer.value("error");
                writer.key("type"); writer.value("parser");
                writer.key("message"); writer.value(e.getMessage());
            } catch (Exception e) {
                writer.key("code"); writer.value("error");
                writer.key("type"); writer.value("other");
                writer.key("message"); writer.value(e.getMessage());
            }
            
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
    
    static protected void writeValue(StringBuffer sb, Object v, boolean quote) throws JSONException {
        if (ExpressionUtils.isError(v)) {
            sb.append("[error: " + ((EvalError) v).message + "]");
        } else {
            if (v == null) {
                sb.append("null");
            } else {
                if (v instanceof WrappedCell) {
                    sb.append("[object Cell]");
                } else if (v instanceof WrappedRow) {
                    sb.append("[object Row]");
                } else if (v instanceof JSONObject) {
                   sb.append(((JSONObject) v).toString());
                } else if (v instanceof JSONArray) {
                    sb.append(((JSONArray) v).toString());
                } else if (ExpressionUtils.isArray(v)) {
                    Object[] a = (Object[]) v;
                    sb.append("[ ");
                    for (int i = 0; i < a.length; i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        writeValue(sb, a[i], true);
                    }
                    sb.append(" ]");
                } else if (ExpressionUtils.isArrayOrList(v)) {
                    List<Object> list = ExpressionUtils.toObjectList(v);
                    sb.append("[ ");
                    for (int i = 0; i < list.size(); i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        writeValue(sb, list.get(i), true);
                    }
                    sb.append(" ]");
                } else if (v instanceof HasFields) {
                    sb.append("[object " + v.getClass().getSimpleName() + "]");
                } else if (v instanceof Calendar) {
                    Calendar c = (Calendar) v;
                    
                    sb.append("[date " + 
                        ParsingUtilities.dateToString(c.getTime()) +"]");
                } else if (v instanceof Date) {
                    sb.append("[date " + 
                            ParsingUtilities.dateToString((Date) v) +"]");
                } else if (v instanceof String) {
                    if (quote) {
                        sb.append(JSONObject.quote((String) v));
                    } else {
                        sb.append((String) v);
                    }
                } else if (v instanceof Double || v instanceof Float) {
                    Number n = (Number) v;
                    if (n.doubleValue() - n.longValue() == 0.0) {
                        sb.append(n.longValue());
                    } else {
                        sb.append(n.doubleValue());
                    }
                } else {
                    sb.append(v.toString());
                }
            }
        }
    }
}
