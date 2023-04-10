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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openrefine.expr.ExpressionUtils;
import org.openrefine.util.CloseableIterator;

/**
 * A list of consecutive rows where only the first row has a non-blank value in the record key column (normally, the
 * first column).
 * <p>
 * This can also store the original row id of the first row in the record, if records have been temporarily reordered.
 * Note that this assumes that the grid has been temporarily reordered as records, not as rows, so that only the first
 * original row id needs to be recorded (so that the original ids of the other rows in the records can be deduced from
 * it).
 * 
 */
public class Record implements Serializable {

    private static final long serialVersionUID = 1547689057610085206L;

    final private long startRowIndex;
    final private Long startRowOriginalIndex;
    final private List<Row> rows;

    public Record(
            long startRowIndex,
            List<Row> rows) {
        this.startRowIndex = startRowIndex;
        this.startRowOriginalIndex = null;
        this.rows = rows;
    }

    public Record(
            long startRowIndex,
            Long startRowOriginalIndex,
            List<Row> rows) {
        this.startRowIndex = startRowIndex;
        this.startRowOriginalIndex = startRowOriginalIndex;
        this.rows = rows;
    }

    /**
     * The id of the first row in this record.
     */
    public long getStartRowId() {
        return startRowIndex;
    }

    /**
     * The id of the last row in this record.
     */
    public long getEndRowId() {
        return startRowIndex + rows.size();
    }

    /**
     * The original id (before applying a temporary sorting operation) of the first row in this record.
     * <p>
     * If no sorting has been applied, this returns null.
     */
    public Long getOriginalStartRowId() {
        return startRowOriginalIndex;
    }

    /**
     * The record index to be used in user-exposed features (expression language, filtering, UI…). If
     * {@link #getOriginalStartRowId()} is defined then it is returned, otherwise this is {@link #getStartRowId()}.
     */
    public long getLogicalStartRowId() {
        return startRowOriginalIndex != null ? startRowOriginalIndex : startRowIndex;
    }

    /**
     * The rows contained in this record.
     */
    public List<Row> getRows() {
        return rows;
    }

    public Iterable<IndexedRow> getIndexedRows() {
        return new Iterable<IndexedRow>() {

            @Override
            public Iterator<IndexedRow> iterator() {
                if (startRowOriginalIndex == null) {
                    return IntStream.range(0, rows.size())
                            .mapToObj(i -> new IndexedRow(startRowIndex + i, rows.get(i)))
                            .iterator();
                } else {
                    return IntStream.range(0, rows.size())
                            .mapToObj(i -> new IndexedRow(startRowIndex + i, startRowOriginalIndex + i, rows.get(i)))
                            .iterator();
                }
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
        return (startRowIndex == otherRecord.getStartRowId() &&
                Objects.equals(startRowOriginalIndex, otherRecord.getOriginalStartRowId()) &&
                rows.equals(otherRecord.getRows()));
    }

    @Override
    public int hashCode() {
        return Long.hashCode(startRowIndex);
    }

    @Override
    public String toString() {
        if (startRowOriginalIndex == null) {
            return String.format("[Record, id %d, rows:\n%s\n]",
                    startRowIndex,
                    rows.stream().map(Row::toString).collect(Collectors.joining("\n")));
        } else {
            return String.format("[Record (%d -> %d), rows:\n%s\n]",
                    startRowOriginalIndex,
                    startRowIndex,
                    rows.stream().map(Row::toString).collect(Collectors.joining("\n")));
        }
    }

    /**
     * Determines when a row marks the start of a new record.
     */
    public static boolean isRecordStart(Row row, int keyCellIndex) {
        return ExpressionUtils.isNonBlankData(row.getCellValue(keyCellIndex))
                || row.getCells().stream().allMatch(c -> c == null || !ExpressionUtils.isNonBlankData(c.getValue()));
    }

    /**
     * Groups a stream of indexed rows into a stream of records.
     *
     * @param parentIter
     *            the iterator of rows. They are supposed to be in their original order, meaning that all their
     *            {@link IndexedRow#getOriginalIndex()} is null.
     * @param keyCellIndex
     *            the index of the column used to group rows into records
     * @param ignoreFirstRows
     *            whether the first rows with blank record key should be ignored
     * @param additionalRows
     *            additional rows to read once the stream is consumed
     * @return a stream of records
     */
    public static CloseableIterator<Record> groupIntoRecords(
            CloseableIterator<IndexedRow> parentIter,
            int keyCellIndex,
            boolean ignoreFirstRows,
            List<Row> additionalRows) {

        return new CloseableIterator<Record>() {

            IndexedRow fetchedRowTuple = null;
            Record nextRecord = null;
            boolean additionalRowsConsumed = false;
            boolean firstRowsIgnored = !ignoreFirstRows;

            @Override
            public boolean hasNext() {
                if (nextRecord != null) {
                    return true;
                }
                buildNextRecord();
                if (firstRowsIgnored && nextRecord != null) {
                    return true;
                }
                firstRowsIgnored = true;
                buildNextRecord();
                return nextRecord != null;
            }

            @Override
            public Record next() {
                Record result = nextRecord;
                nextRecord = null;
                return result;
            }

            private void buildNextRecord() {
                List<Row> rows = new ArrayList<>();
                long startRowId = 0;
                Long originalStartRowId = null;
                if (fetchedRowTuple != null) {
                    rows.add(fetchedRowTuple.getRow());
                    startRowId = fetchedRowTuple.getIndex();
                    originalStartRowId = fetchedRowTuple.getOriginalIndex();
                    fetchedRowTuple = null;
                }
                while (parentIter.hasNext()) {
                    fetchedRowTuple = parentIter.next();
                    Row row = fetchedRowTuple.getRow();
                    if (Record.isRecordStart(row, keyCellIndex)) {
                        break;
                    }
                    rows.add(row);
                    fetchedRowTuple = null;
                }
                if (!parentIter.hasNext() && fetchedRowTuple == null && !additionalRowsConsumed) {
                    rows.addAll(additionalRows);
                    additionalRowsConsumed = true;
                }
                nextRecord = rows.isEmpty() ? null : new Record(startRowId, originalStartRowId, rows);
            }

            @Override
            public void close() {
                parentIter.close();
            }
        };
    }
}
