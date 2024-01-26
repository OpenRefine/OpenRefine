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
import java.text.CollationKey;
import java.text.Collator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.ColumnModel;

public class StringCriterion extends Criterion {

    @JsonProperty("caseSensitive")
    public boolean caseSensitive;
    @JsonIgnore
    Collator collator;

    public StringCriterion() {
        super();
        collator = Collator.getInstance();
        collator.setDecomposition(Collator.FULL_DECOMPOSITION);
    }

    @Override
    public KeyMaker createKeyMaker(ColumnModel columnModel) {
        collator.setStrength(caseSensitive ? Collator.IDENTICAL : Collator.SECONDARY);
        return new KeyMaker(columnModel, columnName) {

            @Override
            protected Serializable makeKey(Serializable value) {
                String stringValue = (ExpressionUtils.isNonBlankData(value)
                        && !(value instanceof String)) ? value.toString() : (String) value;
                if (stringValue != null) {
                    CollationKey key = collator.getCollationKey(stringValue);
                    return new CollationKeyWrapper(key, stringValue);
                } else {
                    return null;
                }
            }

            @Override
            public int compareKeys(Serializable key1, Serializable key2) {
                return ((CollationKeyWrapper) key1).compareTo((CollationKeyWrapper) key2);
            }
        };
    }

    @Override
    public String getValueType() {
        return "string";
    }

    /**
     * Wrapper to avoid the problem that {@link CollationKey} is not serializable.
     */
    private static class CollationKeyWrapper implements Serializable {

        public transient CollationKey key;
        public String originalValue; // used as fallback if key is null (after deserialization)

        public CollationKeyWrapper(CollationKey key, String originalValue) {
            this.key = key;
            this.originalValue = originalValue;
        }

        public int compareTo(CollationKeyWrapper other) {
            if (key != null && other.key != null) {
                return key.compareTo(other.key);
            } else {
                return originalValue.compareTo(other.originalValue);
            }
        }

        @Override
        public String toString() {
            return "CollationKeyWrapper [originalValue=" + originalValue + "]";
        }
    }
}
