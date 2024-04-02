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

import java.util.LinkedList;
import java.util.List;

import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * Encapsulate logic for visiting rows that match all give row filters. Also visit context rows and dependent rows if
 * configured so.
 */
public class ConjunctiveFilteredRows implements FilteredRows {

    final protected List<RowFilter> _rowFilters = new LinkedList<RowFilter>();

    public void add(RowFilter rowFilter) {
        _rowFilters.add(rowFilter);
    }

    @Override
    public void accept(Project project, RowVisitor visitor) {
        try {
            visitor.start(project);

            int c = project.rows.size();
            for (int rowIndex = 0; rowIndex < c; rowIndex++) {
                Row row = project.rows.get(rowIndex);
                if (matchRow(project, rowIndex, row)) {
                    if (visitRow(project, visitor, rowIndex, row)) {
                        break;
                    }
                }
            }
        } finally {
            visitor.end(project);
        }
    }

    protected boolean visitRow(Project project, RowVisitor visitor, int rowIndex, Row row) {
        return visitor.visit(project, rowIndex, row);
    }

    protected boolean matchRow(Project project, int rowIndex, Row row) {
        for (RowFilter rowFilter : _rowFilters) {
            if (!rowFilter.filterRow(project, rowIndex, row)) {
                return false;
            }
        }
        return true;
    }
}
