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

package com.google.refine.sorting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.sorting.Criterion.KeyMaker;

public class SortingRowVisitor extends BaseSorter implements RowVisitor {

    final protected RowVisitor _visitor;
    protected List<IndexedRow> _indexedRows;

    static protected class IndexedRow {

        final int index;
        final Row row;

        IndexedRow(int index, Row row) {
            this.index = index;
            this.row = row;
        }
    }

    public SortingRowVisitor(RowVisitor visitor) {
        _visitor = visitor;
    }

    @Override
    public void start(Project project) {
        int count = project.rows.size();
        _indexedRows = new ArrayList<IndexedRow>(count);
        _keys = new ArrayList<Object[]>(count);
    }

    @Override
    public void end(Project project) {
        _visitor.start(project);

        Collections.sort(_indexedRows, new Comparator<IndexedRow>() {

            Project project;

            Comparator<IndexedRow> init(Project project) {
                this.project = project;
                return this;
            }

            @Override
            public int compare(IndexedRow o1, IndexedRow o2) {
                return SortingRowVisitor.this.compare(project, o1.row, o1.index, o2.row, o2.index);
            }
        }.init(project));

        for (IndexedRow indexedRow : _indexedRows) {
            _visitor.visit(project, indexedRow.index, indexedRow.row);
        }

        _visitor.end(project);
    }

    @Override
    public boolean visit(Project project, int rowIndex, Row row) {
        _indexedRows.add(new IndexedRow(rowIndex, row));
        return false;
    }

    @Override
    protected Object makeKey(
            Project project, KeyMaker keyMaker, Criterion c, Object o, int index) {

        return keyMaker.makeKey(project, (Row) o, index);
    }
}
