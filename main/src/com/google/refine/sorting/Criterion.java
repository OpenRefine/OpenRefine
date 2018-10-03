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

import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;

abstract public class Criterion  {
    public String columnName;
    protected int cellIndex = -2;

    // These take on positive and negative values to indicate where blanks and errors
    // go relative to non-blank values. They are also relative to each another.
    // Blanks and errors are not affected by the reverse flag.
    public int blankPosition = 1;
    public int errorPosition = 2;

    public boolean reverse;

    public void initializeFromJSON(JSONObject obj) 
            throws JSONException {
        if (obj.has("column") && !obj.isNull("column")) {
            columnName = obj.getString("column");
        }

        if (obj.has("blankPosition") && !obj.isNull("blankPosition")) {
            blankPosition = obj.getInt("blankPosition");
        }
        if (obj.has("errorPosition") && !obj.isNull("errorPosition")) {
            errorPosition = obj.getInt("errorPosition");
        }

        if (obj.has("reverse") && !obj.isNull("reverse")) {
            reverse = obj.getBoolean("reverse");
        }
    }
    
    public static Criterion reconstruct(JSONObject obj) throws JSONException {
        String valueType = "string";
        if (obj.has("valueType") && !obj.isNull("valueType")) {
            valueType = obj.getString("valueType");
        }

        Criterion c = null;
        if ("boolean".equals(valueType)) {
            c = new BooleanCriterion();
        } else if ("date".equals(valueType)) {
            c = new DateCriterion();
        } else if ("number".equals(valueType)) {
            c = new NumberCriterion();
        } else {
            c = new StringCriterion(obj.getBoolean("caseSensitive"));
        }

        c.initializeFromJSON(obj);
        return c;
    }
    
    @JsonProperty("valueType")
    public abstract String getValueType();
    
    @JsonProperty("reverse")
    public boolean getReverse() {
        return reverse;
    }
    
    @JsonProperty("column")
    public String getColumnName() {
        return columnName;
    }
    
    @JsonProperty("blankPosition")
    public int getBlankPosition() {
        return blankPosition;
    }
    
    @JsonProperty("errorPosition")
    public int getErrorPosition() {
        return errorPosition;
    }

    // Returns a cached cell index
    // We delay this fetching because the column might not exist
    // at deserialization (for instance if the column is created by an operation
    // that has not been applied yet).
    protected int getCellIndex(Project project) {
        if (cellIndex == -2) {
            Column column = project.columnModel.getColumnByName(columnName);
            cellIndex = column != null ? column.getCellIndex() : -1;
        }
        return cellIndex;
    }

    // TODO: We'd like things to be more strongly typed a la the following, but
    // it's too involved to change right now
//    abstract public class Key implements Comparable<Key> {
//        abstract public int compareTo(Key key);
//    }
    
    abstract public class KeyMaker {
        public Object makeKey(Project project, Record record) {
            Object error = null;
            Object finalKey = null;

            for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
                Object key = makeKey(project, project.rows.get(r), r);
                if (ExpressionUtils.isError(key)) {
                    error = key;
                } else if (ExpressionUtils.isNonBlankData(key)) {
                    if (finalKey == null) {
                        finalKey = key;
                    } else {
                        int c = compareKeys(finalKey, key);
                        if (reverse) {
                            if (c < 0) { // key > finalKey
                                finalKey = key;
                            }
                        } else {
                            if (c > 0) { // key < finalKey
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

        public Object makeKey(Project project, Row row, int rowIndex) {
            if (getCellIndex(project) < 0) {
                return null;
            } else {
                Object value = row.getCellValue(getCellIndex(project));
                return makeKey(value);
            }
        }

        abstract public int compareKeys(Object key1, Object key2);

        abstract protected Object makeKey(Object value);
    }
    abstract public KeyMaker createKeyMaker();
}
