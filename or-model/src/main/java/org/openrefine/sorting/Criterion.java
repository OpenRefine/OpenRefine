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

package org.openrefine.sorting;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.openrefine.model.ColumnModel;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "valueType")
@JsonSubTypes({
        @Type(value = BooleanCriterion.class, name = "boolean"),
        @Type(value = DateCriterion.class, name = "date"),
        @Type(value = NumberCriterion.class, name = "number"),
        @Type(value = StringCriterion.class, name = "string") })
abstract public class Criterion implements Serializable {

    private static final long serialVersionUID = 5338294713927055245L;

    @JsonProperty("column")
    public String columnName;

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

    /**
     * Instantiates the criterion on a particular column model, making it possible to compare two rows together (since
     * we now have access to the column index of the target column).
     */
    abstract public KeyMaker createKeyMaker(ColumnModel columnModel);

    abstract public class KeyMaker implements Serializable {

        private static final long serialVersionUID = -3460406296935132526L;

        protected final int _columnIndex;

        public KeyMaker(ColumnModel columnModel, String columnName) {
            _columnIndex = columnModel.getColumnIndexByName(columnName);
        }

        abstract public int compareKeys(Serializable key1, Serializable key2);

        abstract protected Serializable makeKey(Serializable value);
    }
}
