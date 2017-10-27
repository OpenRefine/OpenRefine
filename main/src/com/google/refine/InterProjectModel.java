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

package com.google.refine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.HasFieldsListImpl;
import com.google.refine.expr.WrappedRow;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class InterProjectModel {
    static public class ProjectJoin {
        final public long   fromProjectID;
        final public String fromProjectColumnName;
        final public long   toProjectID;
        final public String toProjectColumnName;
        
        final public Map<Object, List<Integer>> valueToRowIndices = 
            new HashMap<Object, List<Integer>>();
        
        ProjectJoin(
            long   fromProjectID,
            String fromProjectColumnName,
            long   toProjectID,
            String toProjectColumnName
        ) {
            this.fromProjectID = fromProjectID;
            this.fromProjectColumnName = fromProjectColumnName;
            this.toProjectID = toProjectID;
            this.toProjectColumnName = toProjectColumnName;
        }

        public HasFieldsListImpl getRows(final Object rowKey, Pattern splitPattern) {
            Project toProject = ProjectManager.singleton.getProject(toProjectID);
            if (toProject == null) {
                return null;
            }

            HasFieldsListImpl resultFieldList = null;

            if (ExpressionUtils.isNonBlankData(rowKey)) {
                Object[] rowKeys;
                if (splitPattern != null && rowKey instanceof String) {
                    rowKeys = splitPattern.split((String) rowKey);
                } else {
                    rowKeys = new Object[]{rowKey};
                }

                resultFieldList = new HasFieldsListImpl();
                for (Object k : rowKeys) {
                    if (valueToRowIndices.containsKey(k)) {
                        for (Integer rowIndex : valueToRowIndices.get(k)) {
                            Row row = toProject.rows.get(rowIndex);
                            resultFieldList.add(new WrappedRow(toProject, rowIndex, row));
                        }
                    }
                }
            }

            // Returning null instead of an empty list is expected
            return (resultFieldList == null || resultFieldList.isEmpty()) ? null : resultFieldList;
        }

    }
    
    protected Map<String, ProjectJoin> _joins = new HashMap<String, ProjectJoin>();
    
    /**
     * Compute the ProjectJoin based on combination key, return the cached one from the HashMap if already computed
     * 
     * @param fromProject
     * @param fromColumn
     * @param toProject
     * @param toColumn
     * @param splitPattern
     * @return
     */
    public ProjectJoin getJoin(String fromProject, String fromColumn, String toProject, String toColumn, Pattern splitPattern) {
        String key = String.format("%s;%s;%s;%s;%s", fromProject, fromColumn, toProject, toColumn, String.valueOf(splitPattern));
        if (!_joins.containsKey(key)) {
            ProjectJoin join = new ProjectJoin(
                ProjectManager.singleton.getProjectID(fromProject), 
                fromColumn, 
                ProjectManager.singleton.getProjectID(toProject), 
                toColumn
            );

            computeJoin(join, splitPattern);
            
            synchronized (_joins) {
                _joins.put(key, join);
            }
        }
        
        return _joins.get(key);
    }
    
    public void flushJoinsInvolvingProject(long projectID) {
        synchronized (_joins) {
            for (Iterator<Entry<String, ProjectJoin>> it = _joins.entrySet().iterator(); it.hasNext();) {
                Entry<String, ProjectJoin> entry = it.next();
                ProjectJoin join = entry.getValue();
                if (join.fromProjectID == projectID || join.toProjectID == projectID) {
                    it.remove();
                }
            }
        }
    }

    public void flushJoinsInvolvingProjectColumn(long projectID, String columnName) {
        synchronized (_joins) {
            for (Iterator<Entry<String, ProjectJoin>> it = _joins.entrySet().iterator(); it.hasNext();) {
                Entry<String, ProjectJoin> entry = it.next();
                ProjectJoin join = entry.getValue();
                if (join.fromProjectID == projectID && join.fromProjectColumnName.equals(columnName) || 
                        join.toProjectID == projectID && join.toProjectColumnName.equals(columnName)) {
                    it.remove();
                }
            }
        }
    }

    protected void computeJoin(ProjectJoin join, Pattern splitPattern) {
        if (join.fromProjectID < 0 || join.toProjectID < 0) {
            return;
        }
        
        Project fromProject = ProjectManager.singleton.getProject(join.fromProjectID);
        Project toProject = ProjectManager.singleton.getProject(join.toProjectID);
        if (fromProject == null || toProject == null) {
            return;
        }
        
        Column fromColumn = fromProject.columnModel.getColumnByName(join.fromProjectColumnName);
        Column toColumn = toProject.columnModel.getColumnByName(join.toProjectColumnName);
        if (fromColumn == null || toColumn == null) {
            return;
        }

        for (Row fromRow : fromProject.rows) {
            Object fromRowKey = fromRow.getCellValue(fromColumn.getCellIndex());
            if (ExpressionUtils.isNonBlankData(fromRowKey)) {
                Object[] fromRowKeys;
                if (splitPattern != null && fromRowKey instanceof String) {
                    fromRowKeys = splitPattern.split((String) fromRowKey);
                } else {
                    fromRowKeys = new Object[]{fromRowKey};
                }
                for (Object k : fromRowKeys) {
                    if (!join.valueToRowIndices.containsKey(k)) {
                        join.valueToRowIndices.put(k, new ArrayList<Integer>());
                    }
                }
            }
        }
        
        int count = toProject.rows.size();
        for (int r = 0; r < count; r++) {
            Row toRow = toProject.rows.get(r);
            
            Object value = toRow.getCellValue(toColumn.getCellIndex());
            if (ExpressionUtils.isNonBlankData(value) && join.valueToRowIndices.containsKey(value)) {
                join.valueToRowIndices.get(value).add(r);
            }
        }
    }

}
