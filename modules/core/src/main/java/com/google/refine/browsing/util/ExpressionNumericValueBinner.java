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

import java.util.Collection;
import java.util.Properties;

import com.google.refine.browsing.RecordVisitor;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;

/**
 * Visit matched rows or records and slot them into bins based on the numbers computed from a given expression.
 */
public class ExpressionNumericValueBinner implements RowVisitor, RecordVisitor {

    /*
     * Configuration
     */
    final protected RowEvaluable _rowEvaluable;
    final protected NumericBinIndex _index; // base bins

    /*
     * Computed results
     */
    final public int[] bins;
    public int numericCount;
    public int nonNumericCount;
    public int blankCount;
    public int errorCount;

    /*
     * Scratchpad variables
     */
    protected boolean hasError;
    protected boolean hasBlank;
    protected boolean hasNumeric;
    protected boolean hasNonNumeric;

    public ExpressionNumericValueBinner(RowEvaluable rowEvaluable, NumericBinIndex index) {
        _rowEvaluable = rowEvaluable;
        _index = index;
        bins = new int[_index.getBins().length];
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
        resetFlags();

        Properties bindings = ExpressionUtils.createBindings(project);
        processRow(project, rowIndex, row, bindings);

        updateCounts();

        return false;
    }

    @Override
    public boolean visit(Project project, Record record) {
        resetFlags();

        Properties bindings = ExpressionUtils.createBindings(project);
        for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
            processRow(project, r, project.rows.get(r), bindings);
        }

        updateCounts();

        return false;
    }

    protected void resetFlags() {
        hasError = false;
        hasBlank = false;
        hasNumeric = false;
        hasNonNumeric = false;
    }

    protected void updateCounts() {
        if (hasError) {
            errorCount++;
        }
        if (hasBlank) {
            blankCount++;
        }
        if (hasNumeric) {
            numericCount++;
        }
        if (hasNonNumeric) {
            nonNumericCount++;
        }
    }

    protected void processRow(Project project, int rowIndex, Row row, Properties bindings) {
        Object value = _rowEvaluable.eval(project, rowIndex, row, bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    processValue(v);
                }
                return;
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    processValue(v);
                }
                return;
            } // else, fall through
        }

        processValue(value);
    }

    protected void processValue(Object value) {
        if (ExpressionUtils.isError(value)) {
            hasError = true;
        } else if (ExpressionUtils.isNonBlankData(value)) {
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                if (!Double.isInfinite(d) && !Double.isNaN(d)) {
                    hasNumeric = true;

                    int bin = (int) Math.floor((d - _index.getMin()) / _index.getStep());
                    if (bin >= 0 && bin < bins.length) { // as a precaution
                        bins[bin]++;
                    }
                } else {
                    hasError = true;
                }
            } else {
                hasNonNumeric = true;
            }
        } else {
            hasBlank = true;
        }
    }
}
