/*

Copyright 2024 OpenRefine
All rights reserved.
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
            Column toColumn = project.columnModel.getColumnByName(c);
            if (toColumn != null) {
                criteriaColumns.add(toColumn);
            }
        }

        findDuplicateRows(project, criteriaColumns, rowIndices);

        return new HistoryEntry(
                historyEntryID,
                project,
                "Remove " + rowIndices.size() + " rows",
                this,
                new RowRemovalChange(rowIndices));
    }

    private void findDuplicateRows(Project project, List<Column> criteriaColumns, List<Integer> rowIndices) {
        Set<String> uniqueKeys = new HashSet<>();

        int c = project.recordModel.getRecordCount();
        for (int r = 0; r < c; r++) {
            Row row = project.rows.get(r);
            String key = criteriaColumns.stream()
                    .map(col -> normalizeValue(row.getCell(col.getCellIndex())))
                    .collect(Collectors.joining("|"));

            if (!uniqueKeys.add(key)) {
                rowIndices.add(r);
            }
        }
    }

    private static String normalizeValue(Object value) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        if (value == null) {
            return "";
        } else if (value instanceof Date) {
            return dateFormat.format((Date) value);
        } else if (value instanceof String && ((String) value).trim().startsWith("{")) {
            return normalizeJson((String) value);
        }
        return value.toString();
    }

    private static String normalizeJson(String json) {
        try {
            Map<String, Object> jsonMap = new ObjectMapper().readValue(json, Map.class);
            return new ObjectMapper().writeValueAsString(jsonMap);
        } catch (Exception e) {
            return json;
        }
    }
}
