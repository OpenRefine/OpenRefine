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

package org.openrefine.sorting;

import java.io.Serializable;

import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Grid;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;
import org.openrefine.sorting.Criterion.KeyMaker;

public class RecordSorter extends BaseSorter<Record> {

    public RecordSorter(Grid state, SortingConfig config) {
        super(state, config);
    }

    @Override
    protected Serializable makeKey(
            KeyMaker keyMaker, Criterion c, Record o) {

        Serializable error = null;
        Serializable finalKey = null;

        for (IndexedRow indexedRow : o.getIndexedRows()) {
            Serializable key = keyMaker._columnIndex == -1 ? null
                    : keyMaker.makeKey(indexedRow.getRow().getCellValue(keyMaker._columnIndex));
            if (ExpressionUtils.isError(key)) {
                error = key;
            } else if (ExpressionUtils.isNonBlankData(key)) {
                if (finalKey == null) {
                    finalKey = key;
                } else {
                    int c1 = keyMaker.compareKeys(finalKey, key);
                    if (c.reverse) {
                        if (c1 < 0) { // key > finalKey
                            finalKey = key;
                        }
                    } else {
                        if (c1 > 0) { // key < finalKey
                            finalKey = key;
                        }
                    }
                }
            }
        }

        if (finalKey != null) {
            return finalKey;
        } else if (error != null) {
            return error;
        } else {
            return null;
        }
    }
}
