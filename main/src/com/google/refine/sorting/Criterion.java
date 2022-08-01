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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "valueType")
@JsonSubTypes({
        @Type(value = BooleanCriterion.class, name = "boolean"),
        @Type(value = DateCriterion.class, name = "date"),
        @Type(value = NumberCriterion.class, name = "number"),
        @Type(value = StringCriterion.class, name = "string") })
abstract public class Criterion {

    @JsonProperty("column")
    public String columnName;
    @JsonIgnore
    protected int cellIndex = -2;

    // These take on positive and negative values to indicate where blanks and errors
    // go relative to non-blank values. They are also relative to each another.
    // Blanks and errors are not affected by the reverse flag.
    @JsonProperty("blankPosition")
    public int blankPosition = 1;
    @JsonProperty("errorPosition")
    public int errorPosition = 2;

    @JsonProperty("reverse")
    public boolean reverse = false;

    @JsonIgnore // already added by @JsonTypeInfo
    public abstract String getValueType();

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
