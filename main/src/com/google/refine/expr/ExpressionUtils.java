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

package com.google.refine.expr;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;


import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public class ExpressionUtils {
    
    static final protected Set<Binder> s_binders = new HashSet<Binder>();

    static public void registerBinder(Binder binder) {
        s_binders.add(binder);
    }

    static public Properties createBindings(Project project) {
        Properties bindings = new Properties();

        bindings.put("true", true);
        bindings.put("false", false);
        bindings.put("PI", Math.PI);

        bindings.put("project", project);

        for (Binder binder : s_binders) {
            binder.initializeBindings(bindings, project);
        }

        return bindings;
    }

    static public void bind(Properties bindings, Row row, int rowIndex, String columnName, Cell cell) {
        Project project = (Project) bindings.get("project");

        bindings.put("rowIndex", rowIndex);
        bindings.put("row", new WrappedRow(project, rowIndex, row));
        bindings.put("cells", new CellTuple(project, row));

        if (columnName != null) {
            bindings.put("columnName", columnName);
        }

        if (cell == null) {
            bindings.remove("cell");
            bindings.remove("value");
        } else {
            bindings.put("cell", new WrappedCell(project, columnName, cell));
            if (cell.value == null) {
                bindings.remove("value");
            } else {
                bindings.put("value", cell.value);
            }
        }

        for (Binder binder : s_binders) {
            binder.bind(bindings, row, rowIndex, columnName, cell);
        }
    }

    static public boolean isError(Object o) {
        return o instanceof EvalError;
    }
    /*
    static public boolean isBlank(Object o) {
        return o == null || (o instanceof String && ((String) o).length() == 0);
    }
    */
    static public boolean isNonBlankData(Object o) {
        return
            o != null &&
            !(o instanceof EvalError) &&
            (!(o instanceof String) || ((String) o).length() > 0);
    }

    static public boolean isTrue(Object o) {
        return o != null &&
            (o instanceof Boolean ?
                ((Boolean) o).booleanValue() :
                Boolean.parseBoolean(o.toString()));
    }

    static public boolean sameValue(Object v1, Object v2) {
        if (v1 == null) {
            return (v2 == null) ;
        } else if (v2 == null) {
            return (v1 == null);
        } else {
            return v1.equals(v2);
        }
    }

    static public boolean isStorable(Object v) {
        return v == null ||
            v instanceof Number ||
            v instanceof String ||
            v instanceof Boolean ||
            v instanceof Date ||
            v instanceof Calendar ||
            v instanceof EvalError ||
            isArrayOrList(v);
    }

    static public Serializable wrapStorable(Object v) {
        if (v instanceof JSONArray) {
            return ((JSONArray) v).toString();
        } else if (v instanceof JSONObject) {
            return ((JSONObject) v).toString();
         } else if (isArrayOrList(v)) {
            StringBuffer sb = new StringBuffer();
            writeValue(sb, v, false);
            return sb.toString();
        } else {
            return isStorable(v) ?
                (Serializable) v :
                new EvalError(v.getClass().getSimpleName() + " value not storable");
        }
    }

    static public boolean isArray(Object v) {
        return v != null && v.getClass().isArray();
    }

    static public boolean isArrayOrCollection(Object v) {
        return v != null && (v.getClass().isArray() || v instanceof Collection<?>);
    }

    static public boolean isArrayOrList(Object v) {
        return v != null && (v.getClass().isArray() || v instanceof List<?>);
    }

    @SuppressWarnings("unchecked")
    static public List<Object> toObjectList(Object v) {
        return (List<Object>) v;
    }

    @SuppressWarnings("unchecked")
    static public Collection<Object> toObjectCollection(Object v) {
        return (Collection<Object>) v;
    }

    static public void writeValue(StringBuffer sb, Object v, boolean quote) throws JSONException {
        if (isError(v)) {
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
                } else if (isArray(v)) {
                    Object[] a = (Object[]) v;
                    sb.append("[ ");
                    for (int i = 0; i < a.length; i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        writeValue(sb, a[i], true);
                    }
                    sb.append(" ]");
                } else if (isArrayOrList(v)) {
                    List<Object> list = toObjectList(v);
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
                        ParsingUtilities.dateToString(OffsetDateTime.ofInstant(c.toInstant(), ZoneId.systemDefault())) +"]");
                } else if (v instanceof LocalDateTime) {
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
