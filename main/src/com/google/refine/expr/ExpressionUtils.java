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
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

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
     * static public boolean isBlank(Object o) { return o == null || (o instanceof String && ((String) o).length() ==
     * 0); }
     */
    static public boolean isNonBlankData(Object o) {
        return o != null &&
                !(o instanceof EvalError) &&
                (!(o instanceof String) || ((String) o).length() > 0);
    }

    static public boolean isTrue(Object o) {
        return o != null &&
                (o instanceof Boolean ? ((Boolean) o).booleanValue() : Boolean.parseBoolean(o.toString()));
    }

    static public boolean sameValue(Object v1, Object v2) {
        if (v1 == null) {
            return (v2 == null);
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
                v instanceof OffsetDateTime ||
                v instanceof EvalError;
    }

    static public Serializable wrapStorable(Object v) {
        if (v instanceof ArrayNode) {
            return ((ArrayNode) v).toString();
        } else if (v instanceof ObjectNode) {
            return ((ObjectNode) v).toString();
        } else {
            return isStorable(v) ? (Serializable) v : new EvalError(v.getClass().getSimpleName() + " value not storable");
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
}
