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

package com.google.refine.browsing.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.refine.browsing.DecoratedValue;
import com.google.refine.browsing.RecordVisitor;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.browsing.facets.NominalFacetChoice;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;
import com.google.refine.util.StringUtils;

/**
 * Visit matched rows or records and group them into facet choices based on the values computed from a given expression.
 */
public class ExpressionNominalValueGrouper implements RowVisitor, RecordVisitor {

    static public class IndexedNominalFacetChoice extends NominalFacetChoice {

        int _latestIndex;

        public IndexedNominalFacetChoice(DecoratedValue decoratedValue, int latestIndex) {
            super(decoratedValue);
            _latestIndex = latestIndex;
        }
    }

    /*
     * Configuration
     */
    final protected Evaluable _evaluable;
    final protected String _columnName;
    final protected int _cellIndex;

    /*
     * Computed results
     */
    final public Map<Object, IndexedNominalFacetChoice> choices = new HashMap<Object, IndexedNominalFacetChoice>();
    public int blankCount = 0;
    public int errorCount = 0;

    /*
     * Scratch pad variables
     */
    protected boolean hasBlank;
    protected boolean hasError;

    public ExpressionNominalValueGrouper(Evaluable evaluable, String columnName, int cellIndex) {
        _evaluable = evaluable;
        _columnName = columnName;
        _cellIndex = cellIndex;
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
        hasError = false;
        hasBlank = false;

        Properties bindings = ExpressionUtils.createBindings(project);

        visitRow(project, rowIndex, row, bindings, rowIndex);

        if (hasError) {
            errorCount++;
        }
        if (hasBlank) {
            blankCount++;
        }

        return false;
    }

    @Override
    public boolean visit(Project project, Record record) {
        Properties bindings = ExpressionUtils.createBindings(project);

        for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
            hasError = false;
            hasBlank = false;

            Row row = project.rows.get(r);
            visitRow(project, r, row, bindings, record.recordIndex);

            if (hasError) {
                errorCount++;
            }
            if (hasBlank) {
                blankCount++;
            }
        }

        return false;
    }

    protected void visitRow(Project project, int rowIndex, Row row, Properties bindings, int index) {
        Object value = evalRow(project, rowIndex, row, bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    processValue(v, rowIndex);
                }
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    processValue(v, rowIndex);
                }
            } else {
                processValue(value, rowIndex);
            }
        } else {
            processValue(value, rowIndex);
        }
    }

    protected Object evalRow(Project project, int rowIndex, Row row, Properties bindings) {
        Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);

        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);

        return _evaluable.evaluate(bindings);
    }

    protected void processValue(Object value, int index) {
        if (ExpressionUtils.isError(value)) {
            hasError = true;
        } else if (ExpressionUtils.isNonBlankData(value)) {
            String valueString = StringUtils.toString(value);
            IndexedNominalFacetChoice facetChoice = choices.get(valueString);

            if (facetChoice != null) {
                if (facetChoice._latestIndex < index) {
                    facetChoice._latestIndex = index;
                    facetChoice.count++;
                }
            } else {
                String label = valueString;
                DecoratedValue dValue = new DecoratedValue(value, label);
                IndexedNominalFacetChoice choice = new IndexedNominalFacetChoice(dValue, index);

                choice.count = 1;
                choices.put(valueString, choice);
            }
        } else {
            hasBlank = true;
        }
    }

    public RowEvaluable getChoiceCountRowEvaluable() {
        return new RowEvaluable() {

            @Override
            public Object eval(Project project, int rowIndex, Row row, Properties bindings) {
                Object value = evalRow(project, rowIndex, row, bindings);
                return getChoiceValueCountMultiple(value);
            }

        };
    }

    public Object getChoiceValueCountMultiple(Object value) {
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] choiceValues = (Object[]) value;
                List<Integer> counts = new ArrayList<Integer>(choiceValues.length);

                for (Object choiceValue : choiceValues) {
                    counts.add(getChoiceValueCount(choiceValue));
                }
                return counts;
            } else if (value instanceof Collection<?>) {
                List<Object> choiceValues = ExpressionUtils.toObjectList(value);
                List<Integer> counts = new ArrayList<Integer>(choiceValues.size());

                int count = choiceValues.size();
                for (int i = 0; i < count; i++) {
                    counts.add(getChoiceValueCount(choiceValues.get(i)));
                }
                return counts;
            }
        }

        return getChoiceValueCount(value);
    }

    public Integer getChoiceValueCount(Object choiceValue) {
        if (ExpressionUtils.isError(choiceValue)) {
            return errorCount;
        } else if (ExpressionUtils.isNonBlankData(choiceValue)) {
            IndexedNominalFacetChoice choice = choices.get(StringUtils.toString(choiceValue));
            return choice != null ? choice.count : 0;
        } else {
            return blankCount;
        }
    }
}
