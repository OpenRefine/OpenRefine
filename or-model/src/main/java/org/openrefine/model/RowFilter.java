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

package org.openrefine.model;

import java.io.Serializable;
import java.util.List;

/**
 * Interface for judging if a particular row matches or doesn't match some particular criterion, such as a facet
 * constraint.
 */
public interface RowFilter extends Serializable {

    public boolean filterRow(long rowIndex, Row row);

    /**
     * Filter which accepts any row
     */
    public static RowFilter ANY_ROW = new RowFilter() {

        private static final long serialVersionUID = 5496299113387243579L;

        @Override
        public boolean filterRow(long rowIndex, Row row) {
            return true;
        }

    };

    /**
     * A row filter which evaluates to true when all the supplied row filters do.
     */
    public static RowFilter conjunction(List<RowFilter> rowFilters) {
        if (rowFilters.isEmpty()) {
            return ANY_ROW;
        } else {
            return new RowFilter() {

                private static final long serialVersionUID = -778029463533608671L;

                @Override
                public boolean filterRow(long rowIndex, Row row) {
                    return rowFilters.stream().allMatch(f -> f.filterRow(rowIndex, row));
                }
            };
        }
    }
}
