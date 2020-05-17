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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openrefine.expr.ExpressionUtils;

/**
 * A list of consecutive rows where only the first row has a non-blank value in the record key column (normally, the
 * first column).
 * 
 * @author Antonin Delpeuch
 */
public class Record implements Serializable {

    private static final long serialVersionUID = 1547689057610085206L;

    final private long startRowIndex;
    final private List<Row> rows;

    public Record(
            long startRowIndex,
            List<Row> rows) {
        this.startRowIndex = startRowIndex;
        this.rows = rows;
    }

    public long getStartRowId() {
        return startRowIndex;
    }

    public long getEndRowId() {
        return startRowIndex + rows.size();
    }

    public List<Row> getRows() {
        return rows;
    }

    public Iterable<IndexedRow> getIndexedRows() {
        return new Iterable<IndexedRow>() {

            @Override
            public Iterator<IndexedRow> iterator() {
                return IntStream.range(0, rows.size())
                        .mapToObj(i -> new IndexedRow(startRowIndex + i, rows.get(i)))
                        .iterator();
            }

        };

    }

    public int size() {
        return rows.size();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Record)) {
            return false;
        }
        Record otherRecord = (Record) other;
        return startRowIndex == otherRecord.getStartRowId() && rows.equals(otherRecord.getRows());
    }

    @Override
    public int hashCode() {
        return Long.hashCode(startRowIndex);
    }

    @Override
    public String toString() {
        return String.format("[Record, id %d, rows:\n%s\n]",
                startRowIndex,
                String.join("\n", rows.stream().map(r -> r.toString()).collect(Collectors.toList())));
    }

    /**
     * Determines when a row marks the start of a new record.
     */
    public static boolean isRecordStart(Row row, int keyCellIndex) {
        return ExpressionUtils.isNonBlankData(row.getCellValue(keyCellIndex))
                || row.getCells().stream().allMatch(c -> c == null || !ExpressionUtils.isNonBlankData(c.getValue()));
    }

}
