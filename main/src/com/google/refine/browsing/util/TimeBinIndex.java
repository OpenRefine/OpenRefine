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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * A utility class for computing the base bins that form the base histograms of temporal range facets. It evaluates an
 * expression on all the rows of a project to get temporal values, determines how many bins to distribute those values
 * in, and bins the rows accordingly.
 * 
 * This class processes all rows rather than just the filtered rows because it needs to compute the base bins of a
 * temporal range facet, which remain unchanged as the user interacts with the facet.
 */
abstract public class TimeBinIndex {

    protected int _totalValueCount;
    protected int _timeValueCount;
    protected long _min;
    protected long _max;
    protected long _step;
    protected int[] _bins;

    protected int _timeRowCount;
    protected int _nonTimeRowCount;
    protected int _blankRowCount;
    protected int _errorRowCount;

    protected boolean _hasError = false;
    protected boolean _hasNonTime = false;
    protected boolean _hasTime = false;
    protected boolean _hasBlank = false;

    protected long[] steps = {
            1, // msec
            1000, // sec
            1000 * 60, // min
            1000 * 60 * 60, // hour
            1000 * 60 * 60 * 24, // day
            1000 * 60 * 60 * 24 * 7, // week
            1000l * 2629746l, // month (average Gregorian year / 12)
            1000l * 31556952l, // year (average Gregorian year)
            1000l * 31556952l * 10l, // decade
            1000l * 31556952l * 100l, // century
            1000l * 31556952l * 1000l, // millennium
    };

    abstract protected void iterate(Project project, RowEvaluable rowEvaluable, List<Long> allValues);

    public TimeBinIndex(Project project, RowEvaluable rowEvaluable) {
        _min = Long.MAX_VALUE;
        _max = Long.MIN_VALUE;

        List<Long> allValues = new ArrayList<Long>();

        iterate(project, rowEvaluable, allValues);

        _timeValueCount = allValues.size();

        if (_min >= _max) {
            _step = 1;
            _min = Math.min(_min, _max);
            _max = _step;
            _bins = new int[1];

            return;
        }

        long diff = _max - _min;

        for (long step : steps) {
            _step = step;
            if (diff / _step <= 100) {
                break;
            }
        }

        _bins = new int[(int) (diff / _step) + 1];
        for (long d : allValues) {
            int bin = (int) Math.max((d - _min) / _step, 0);
            _bins[bin]++;
        }
    }

    public boolean isTemporal() {
        return _timeValueCount > _totalValueCount / 2;
    }

    public long getMin() {
        return _min;
    }

    public long getMax() {
        return _max;
    }

    public long getStep() {
        return _step;
    }

    public int[] getBins() {
        return _bins;
    }

    public int getTimeRowCount() {
        return _timeRowCount;
    }

    public int getNonTimeRowCount() {
        return _nonTimeRowCount;
    }

    public int getBlankRowCount() {
        return _blankRowCount;
    }

    public int getErrorRowCount() {
        return _errorRowCount;
    }

    protected void processRow(
            Project project,
            RowEvaluable rowEvaluable,
            List<Long> allValues,
            int rowIndex,
            Row row,
            Properties bindings) {
        Object value = rowEvaluable.eval(project, rowIndex, row, bindings);

        if (ExpressionUtils.isError(value)) {
            _hasError = true;
        } else if (ExpressionUtils.isNonBlankData(value)) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    _totalValueCount++;

                    if (ExpressionUtils.isError(v)) {
                        _hasError = true;
                    } else if (ExpressionUtils.isNonBlankData(v)) {
                        if (v instanceof OffsetDateTime) {
                            _hasTime = true;
                            processValue(((OffsetDateTime) v).toInstant().toEpochMilli(), allValues);
                        } else {
                            _hasNonTime = true;
                        }
                    } else {
                        _hasBlank = true;
                    }
                }
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    _totalValueCount++;

                    if (ExpressionUtils.isError(v)) {
                        _hasError = true;
                    } else if (ExpressionUtils.isNonBlankData(v)) {
                        if (v instanceof OffsetDateTime) {
                            _hasTime = true;
                            processValue(((OffsetDateTime) v).toInstant().toEpochMilli(), allValues);
                        } else {
                            _hasNonTime = true;
                        }
                    } else {
                        _hasBlank = true;
                    }
                }
            } else {
                _totalValueCount++;

                if (value instanceof OffsetDateTime) {
                    _hasTime = true;
                    processValue(((OffsetDateTime) value).toInstant().toEpochMilli(), allValues);
                } else {
                    _hasNonTime = true;
                }
            }
        } else {
            _hasBlank = true;
        }
    }

    protected void preprocessing() {
        _hasBlank = false;
        _hasError = false;
        _hasNonTime = false;
        _hasTime = false;
    }

    protected void postprocessing() {
        if (_hasError) {
            _errorRowCount++;
        }
        if (_hasBlank) {
            _blankRowCount++;
        }
        if (_hasTime) {
            _timeRowCount++;
        }
        if (_hasNonTime) {
            _nonTimeRowCount++;
        }
    }

    protected void processValue(long v, List<Long> allValues) {
        _min = Math.min(_min, v);
        _max = Math.max(_max, v);
        allValues.add(v);
    }

}
