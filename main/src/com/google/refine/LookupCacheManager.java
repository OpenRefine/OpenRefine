/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.HasFieldsListImpl;
import com.google.refine.expr.WrappedRow;
import com.google.refine.expr.functions.Cross;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.LookupException;

import java.util.*;

/**
 * Manage the cache of project's lookups.
 *
 * @author Lu Liu
 */
public class LookupCacheManager {

    protected final Map<String, ProjectLookup> _lookups = new HashMap<>();

    /**
     * Computes the ProjectLookup based on combination key, returns the cached one from the HashMap if already computed.
     *
     * @param targetProject
     *            the project to look up
     * @param targetColumn
     *            the column of the target project to look up
     * @return a {@link ProjectLookup} instance of the lookup result
     */
    public ProjectLookup getLookup(long targetProject, String targetColumn) throws LookupException {
        String key = targetProject + ";" + targetColumn;
        if (!_lookups.containsKey(key)) {
            ProjectLookup lookup = new ProjectLookup(targetProject, targetColumn);
            computeLookup(lookup);

            synchronized (_lookups) {
                _lookups.put(key, lookup);
            }
        }

        return _lookups.get(key);
    }

    public void flushLookupsInvolvingProject(long projectID) {
        synchronized (_lookups) {
            for (Iterator<Map.Entry<String, ProjectLookup>> it = _lookups.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, ProjectLookup> entry = it.next();
                ProjectLookup lookup = entry.getValue();
                if (lookup.targetProjectID == projectID) {
                    it.remove();
                }
            }
        }
    }

    public void flushLookupsInvolvingProjectColumn(long projectID, String columnName) {
        synchronized (_lookups) {
            for (Iterator<Map.Entry<String, ProjectLookup>> it = _lookups.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, ProjectLookup> entry = it.next();
                ProjectLookup lookup = entry.getValue();
                if (lookup.targetProjectID == projectID && lookup.targetColumnName.equals(columnName)) {
                    it.remove();
                }
            }
        }
    }

    protected void computeLookup(ProjectLookup lookup) throws LookupException {
        if (lookup.targetProjectID < 0) {
            return;
        }

        Project targetProject = ProjectManager.singleton.getProject(lookup.targetProjectID);
        ProjectMetadata targetProjectMetadata = ProjectManager.singleton.getProjectMetadata(lookup.targetProjectID);
        if (targetProject == null) {
            return;
        }

        // if this is a lookup on the index column
        if (lookup.targetColumnName.equals(Cross.INDEX_COLUMN_NAME)) {
            for (int r = 0; r < targetProject.rows.size(); r++) {
                lookup.valueToRowIndices.put(String.valueOf(r), Collections.singletonList(r));
            }
            return; // return directly
        }

        Column targetColumn = targetProject.columnModel.getColumnByName(lookup.targetColumnName);
        if (targetColumn == null) {
            throw new LookupException(
                    "Unable to find column " + lookup.targetColumnName + " in project " + targetProjectMetadata.getName());
        }

        // We can't use for-each here, because we'll need the row index when creating WrappedRow
        int count = targetProject.rows.size();
        for (int r = 0; r < count; r++) {
            Row targetRow = targetProject.rows.get(r);
            Object value = targetRow.getCellValue(targetColumn.getCellIndex());
            if (ExpressionUtils.isNonBlankData(value)) {
                String valueStr = value.toString();
                lookup.valueToRowIndices.putIfAbsent(valueStr, new ArrayList<>());
                lookup.valueToRowIndices.get(valueStr).add(r);
            }
        }
    }

    static public class ProjectLookup {

        final public long targetProjectID;
        final public String targetColumnName;

        final public Map<Object, List<Integer>> valueToRowIndices = new HashMap<>();

        ProjectLookup(long targetProjectID, String targetColumnName) {
            this.targetProjectID = targetProjectID;
            this.targetColumnName = targetColumnName;
        }

        public HasFieldsListImpl getRows(Object value) {
            if (!ExpressionUtils.isNonBlankData(value)) return null;
            String valueStr = value.toString();
            if (valueToRowIndices.containsKey(valueStr)) {
                Project targetProject = ProjectManager.singleton.getProject(targetProjectID);
                if (targetProject != null) {
                    HasFieldsListImpl rows = new HasFieldsListImpl();
                    for (Integer r : valueToRowIndices.get(valueStr)) {
                        Row row = targetProject.rows.get(r);
                        rows.add(new WrappedRow(targetProject, r, row));
                    }

                    return rows;
                }
            }
            return null;
        }
    }
}
