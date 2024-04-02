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

package com.google.refine.browsing.filters;

import java.time.OffsetDateTime;

import com.google.refine.browsing.util.RowEvaluable;
import com.google.refine.expr.ExpressionUtils;

/**
 * Judge if a row matches by evaluating a given expression on the row, based on a particular column, and checking the
 * result. It's a match if the result satisfies some time comparisons, or if the result is not a time or blank or error
 * and we want non-time or blank or error values.
 */
abstract public class ExpressionTimeComparisonRowFilter extends ExpressionNumberComparisonRowFilter {

    final protected boolean _selectTime;
    final protected boolean _selectNonTime;

    public ExpressionTimeComparisonRowFilter(
            RowEvaluable rowEvaluable,
            boolean selectTime,
            boolean selectNonTime,
            boolean selectBlank,
            boolean selectError) {
        super(rowEvaluable, selectTime, selectNonTime, selectBlank, selectError);
        _selectTime = selectTime;
        _selectNonTime = selectNonTime;
    }

    @Override
    protected boolean checkValue(Object v) {
        if (ExpressionUtils.isError(v)) {
            return _selectError;
        } else if (ExpressionUtils.isNonBlankData(v)) {
            if (v instanceof OffsetDateTime) {
                long time = ((OffsetDateTime) v).toInstant().toEpochMilli();
                return _selectTime && checkValue(time);
            } else {
                return _selectNonTime;
            }
        } else {
            return _selectBlank;
        }
    }

    // not really needed for operation, just to make extending the abstract class possible
    @Override
    protected boolean checkValue(double d) {
        return false;
    }

    abstract protected boolean checkValue(long d);
}
