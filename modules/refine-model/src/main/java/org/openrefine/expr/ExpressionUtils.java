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

package org.openrefine.expr;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.*;

import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Record;
import org.openrefine.model.Row;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openrefine.overlay.OverlayModel;

public class ExpressionUtils {

    static final protected Set<Binder> s_binders = new HashSet<Binder>();

    static public void registerBinder(Binder binder) {
        s_binders.add(binder);
    }

    static public Properties createBindings() {
        Properties bindings = new Properties();

        bindings.put("true", true);
        bindings.put("false", false);
        bindings.put("PI", Math.PI);

        for (Binder binder : s_binders) {
            binder.initializeBindings(bindings);
        }

        return bindings;
    }

    /**
     * Prepares the context for the evaluation of an expression by storing certain context objects within the supplied
     * bindings.
     * 
     * @param bindings
     *            the dictionary representing the variables available in the expression's context
     * @param columnModel
     *            the list of column metadata for the project being currently evaluated
     * @param row
     *            the row on which the expression is evaluated
     * @param rowIndex
     *            the index of the row on which the expression is evaluated
     * @param record
     *            the enclosing record, if available (only in records mode)
     * @param columnName
     *            the name of the base column for the expression
     * @param cell
     *            the cell at the intersection of the base column and current row
     * @param overlayModels
     *            the overlay models stored in the grid on which the expression is evaluated
     * @param projectId
     *            the id of the project this expression is evaluated on
     */
    static public void bind(
            Properties bindings,
            ColumnModel columnModel,
            Row row,
            long rowIndex,
            Record record,
            String columnName,
            Cell cell,
            Map<String, OverlayModel> overlayModels, long projectId) {
        bindings.put("rowIndex", rowIndex);
        bindings.put("row", new WrappedRow(columnModel, rowIndex, row, record));
        bindings.put("cells", new CellTuple(columnModel, row));
        if (overlayModels != null) {
            bindings.put("overlayModels", overlayModels);
        }

        if (columnName != null) {
            bindings.put("columnName", columnName);
        }
        if (columnModel != null) {
            bindings.put("columnModel", columnModel);
        }
        bindings.put("project_id", projectId);

        if (cell == null) {
            bindings.remove("cell");
            bindings.remove("value");
        } else {
            bindings.put("cell", new WrappedCell(columnName, cell));
            if (cell.value == null) {
                bindings.remove("value");
            } else {
                bindings.put("value", cell.value);
            }
        }

        for (Binder binder : s_binders) {
            binder.bind(bindings, row, rowIndex, record, columnName, cell, overlayModels, projectId);
        }
    }

    /**
     * Checks if the given expression relies on any pending cells in the given evaluation context. If so, the result of
     * its evaluation should be considered pending as well. This is meant to be an over-approximation: this method might
     * return true even if the expression can actually be evaluated without relying on any pending cells. This happens
     * for instance if the evaluable does not expose column dependencies and the evaluation context contains any pending
     * cell.
     *
     * @param evaluable
     *            the evaluable to evaluate
     */
    static public boolean dependsOnPendingValues(Evaluable evaluable, String baseColumnName, ColumnModel columnModel, Row row,
            Record record) {
        Set<Integer> columnIndicesWithPendingValues = new HashSet<>();
        if (record != null) {
            for (Row recordRow : record.getRows()) {
                extractColumnsWithPendingCells(recordRow, columnIndicesWithPendingValues);
            }
        } else {
            // we know that this row is part of the supplied record, if it is supplied
            extractColumnsWithPendingCells(row, columnIndicesWithPendingValues);
        }
        Set<String> columnDependencies = evaluable.getColumnDependencies(baseColumnName);
        if (columnDependencies == null) {
            return !columnIndicesWithPendingValues.isEmpty();
        } else {
            return columnDependencies.stream().map(columnModel::getColumnIndexByName)
                    .anyMatch(columnIndicesWithPendingValues::contains);
        }
    }

    static private void extractColumnsWithPendingCells(Row row, Set<Integer> set) {
        for (int i = 0; i != row.cells.size(); i++) {
            if (row.isCellPending(i)) {
                set.add(i);
            }
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
