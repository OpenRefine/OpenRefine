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

package com.google.refine.operations.row;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.RowRemovalChange;
import com.google.refine.operations.OperationDescription;

public class RowDuplicatesRemovalOperation extends AbstractOperation {

    final protected List<String> _criteria;

    @JsonCreator
    public RowDuplicatesRemovalOperation(
            @JsonProperty("criteria") List<String> criteria) {
        _criteria = criteria;
    }

    @JsonProperty("criteria")
    public List<String> getRows() {
        return _criteria;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.row_remove_duplicates_brief();
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {

        List<Integer> rowIndices = new ArrayList<Integer>();
        final List<Column> criteriaColumns = new ArrayList<Column>(_criteria.size());
        for (String c : _criteria) {
            System.out.println("criteria col - " + c);
            Column toColumn = project.columnModel.getColumnByName(c);
            if (toColumn != null) {
                criteriaColumns.add(toColumn);
            }
        }

        findDuplicateRows(project, criteriaColumns, rowIndices);

        return new HistoryEntry(
                historyEntryID,
                project,
                "Duplicate rows removal completed. "+ rowIndices.size() + " rows removed." ,
                this,
                new RowRemovalChange(rowIndices));
    }

    private void findDuplicateRows(Project project, List<Column> criteriaColumns, List<Integer> rowIndices) {
        Set<String> uniqueKeys = new HashSet<>();

        int c = project.recordModel.getRecordCount();
        for (int r = 0; r < c; r++) {
            Row row = project.rows.get(r);

            // Generate a unique key based on normalized values of the specified columns
           String key = criteriaColumns.stream()
                    .map(col -> normalizeValue(row.getCell(col.getCellIndex())))
                    .collect(Collectors.joining("|"));

            if (!uniqueKeys.add(key)) {
                System.out.println("duplicate found - index : " + c + " key - " + key );
                rowIndices.add(r);
            }
        }
    }

    private static String normalizeValue(Object value) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        if (value == null) {
            return "";
        } else if (value instanceof Date) {
            return dateFormat.format((Date) value); // Format Date/DateTime
        } else if (value instanceof String && ((String) value).trim().startsWith("{")) {
            return normalizeJson((String) value); // Normalize JSON
        }
        return value.toString(); // Default: use string representation
    }

    private static String normalizeJson(String json) {
        try {
            Map<String, Object> jsonMap = new ObjectMapper().readValue(json, Map.class);
            return new ObjectMapper().writeValueAsString(jsonMap); // Sorts and formats JSON
        } catch (Exception e) {
            return json; // Fallback: return original if parsing fails
        }
    }
}
