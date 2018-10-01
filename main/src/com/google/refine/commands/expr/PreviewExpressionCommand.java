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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import com.google.refine.Jsonizable;
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
    
    protected static interface ExpressionValue extends Jsonizable { }
    protected static class ErrorMessage implements ExpressionValue {
        @JsonProperty("message")
        protected String message;
        public ErrorMessage(String m) {
            message = m;
        }
        @Override
        public void write(JSONWriter writer, Properties options)
                throws JSONException {
            writer.object();
            writer.key("message"); writer.value(message);
            writer.endObject();
        }
        
    }
    protected static class SuccessfulEvaluation implements ExpressionValue {
        @JsonValue
        protected String value;
        protected SuccessfulEvaluation(String value) {
            this.value = value;
        }
        @Override
        public void write(JSONWriter writer, Properties options)
                throws JSONException {
            writer.value(value);
        }
        
    }
    
    protected static class PreviewResult implements Jsonizable {
        @JsonProperty("code")
        protected String code;
        @JsonProperty("message")
        @JsonInclude(Include.NON_NULL)
        protected String message;
        @JsonProperty("type")
        @JsonInclude(Include.NON_NULL)
        protected String type;
        @JsonProperty("results")
        List<ExpressionValue> results; 
        
        public PreviewResult(String code, String message, String type) {
            this.code = code;
            this.message = message;
            this.type = type;
            this.results = null;
        }

        public PreviewResult(List<ExpressionValue> evaluated) {
            this.code = "ok";
            this.message = null;
            this.type = null;
            this.results = evaluated;
        }

        @Override
        public void write(JSONWriter writer, Properties options)
                throws JSONException {
            writer.object();
            writer.key("code"); writer.value(code);
            if(message != null) {
                writer.key("message"); writer.value(message);
            }
            if(type != null) {
                writer.key("type"); writer.value(type);
            }
            if(results != null) {
                writer.key("results");
                writer.array();
                for(ExpressionValue v : results) {
                    v.write(writer, options);
                }
                writer.endArray();
            }
            writer.endObject();
        }
    }
    
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
                respondJSON(response, new PreviewResult("error", "No row indices specified", null));
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
            
            JSONArray rowIndices = ParsingUtilities.evaluateJsonStringToArray(rowIndicesString);
            int length = rowIndices.length();
            
            try {
                Evaluable eval = MetaParser.parse(expression);
                
                List<ExpressionValue> evaluated = new ArrayList<>();
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
                        evaluated.add(null);
                    } else if (ExpressionUtils.isError(result)) {
                        evaluated.add(new ErrorMessage(((EvalError) result).message));
                    } else {
                        StringBuffer sb = new StringBuffer();
                        
                        writeValue(sb, result, false);
                        
                        evaluated.add(new SuccessfulEvaluation(sb.toString()));
                    }
                }
                respondJSON(response, new PreviewResult(evaluated));
            } catch (ParsingException e) {
                respondJSON(response, new PreviewResult("error", e.getMessage(), "parser"));
            } catch (Exception e) {
                respondJSON(response, new PreviewResult("error", e.getMessage(), "other"));
            }
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
                } else if (v instanceof OffsetDateTime) {
                    sb.append("[date " + 
                            ParsingUtilities.dateToString((OffsetDateTime) v) +"]");
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
