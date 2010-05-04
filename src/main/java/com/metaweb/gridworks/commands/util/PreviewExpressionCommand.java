package com.metaweb.gridworks.commands.util;

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

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.HasFields;
import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.expr.ParsingException;
import com.metaweb.gridworks.expr.WrappedCell;
import com.metaweb.gridworks.expr.WrappedRow;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.util.ParsingUtilities;

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
                } else {
                    sb.append(v.toString());
                }
            }
        }
    }
}
