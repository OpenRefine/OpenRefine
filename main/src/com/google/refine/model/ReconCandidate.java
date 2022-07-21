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

package com.google.refine.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.refine.expr.HasFields;
import com.google.refine.util.ParsingUtilities;

public class ReconCandidate implements HasFields {

    @JsonProperty("id")
    final public String id;
    @JsonProperty("name")
    final public String name;
    @JsonProperty("types")
    final public String[] types;
    @JsonProperty("score")
    final public double score;

    @JsonCreator
    public ReconCandidate(
            @JsonProperty("id") String topicID,
            @JsonProperty("name") String topicName,
            @JsonProperty("types") String[] typeIDs,
            @JsonProperty("score") double score) {
        this.id = topicID;
        this.name = topicName;
        this.types = typeIDs == null ? new String[0] : typeIDs;
        this.score = score;
    }

    @Override
    public Object getField(String name, Properties bindings) {
        if ("id".equals(name)) {
            return id;
        } else if ("name".equals(name)) {
            return this.name;
        } else if ("type".equals(name)) {
            return types;
        } else if ("score".equals(name)) {
            return score;
        }
        return null;
    }

    @Override
    public boolean fieldAlsoHasFields(String name) {
        return false;
    }

    static public ReconCandidate loadStreaming(String s) throws Exception {
        return ParsingUtilities.mapper.readValue(s, ReconCandidate.class);
    }

    @Deprecated
    static public ReconCandidate loadStreaming(JsonParser jp) throws Exception {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_NULL || t != JsonToken.START_OBJECT) {
            return null;
        }

        String id = null;
        String name = null;
        List<String> types = null;
        double score = 0;

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jp.getCurrentName();
            jp.nextToken();

            if ("id".equals(fieldName)) {
                id = jp.getText();
            } else if ("name".equals(fieldName)) {
                name = jp.getText();
            } else if ("score".equals(fieldName)) {
                score = jp.getDoubleValue();
            } else if ("types".equals(fieldName)) {
                if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                    return null;
                }

                types = new ArrayList<String>();

                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    types.add(jp.getText());
                }
            }
        }

        String[] typesA;
        if (types != null) {
            typesA = new String[types.size()];
            types.toArray(typesA);
        } else {
            typesA = new String[0];
        }

        return new ReconCandidate(
                id,
                name,
                typesA,
                score);
    }
}
