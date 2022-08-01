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

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.refine.util.JsonViews;
import com.google.refine.util.ParsingUtilities;

public class ColumnGroup {

    final public int startColumnIndex;
    final public int columnSpan;
    final public int keyColumnIndex; // could be -1 if there is no key cell

    transient public ColumnGroup parentGroup;
    transient public List<ColumnGroup> subgroups;

    @JsonCreator
    public ColumnGroup(
            @JsonProperty("startColumnIndex") int startColumnIndex,
            @JsonProperty("columnSpan") int columnSpan,
            @JsonProperty("keyColumnIndex") int keyColumnIndex) {
        this.startColumnIndex = startColumnIndex;
        this.columnSpan = columnSpan;
        this.keyColumnIndex = keyColumnIndex;
        internalInitialize();
    }

    @JsonProperty("startColumnIndex")
    public int getStartColumnIndex() {
        return startColumnIndex;
    }

    @JsonProperty("columnSpan")
    public int getColumnSpan() {
        return columnSpan;
    }

    @JsonProperty("keyColumnIndex")
    public int getKeyColumnIndex() {
        return keyColumnIndex;
    }

    @JsonProperty("subgroups")
    @JsonView(JsonViews.NonSaveMode.class)
    @JsonInclude(Include.NON_EMPTY)
    public List<ColumnGroup> getSubGroups() {
        return subgroups;
    }

    public boolean contains(ColumnGroup g) {
        return (g.startColumnIndex >= startColumnIndex &&
                g.startColumnIndex < startColumnIndex + columnSpan);
    }

    public void save(Writer writer) {
        try {
            ParsingUtilities.defaultWriter.writeValue(writer, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public ColumnGroup load(String s) throws IOException {
        return ParsingUtilities.mapper.readValue(s, ColumnGroup.class);
    }

    protected void internalInitialize() {
        subgroups = new LinkedList<ColumnGroup>();
    }

    @Override
    public String toString() {
        return String.format("%d:%d:k=%d", startColumnIndex, columnSpan, keyColumnIndex);
    }
}
