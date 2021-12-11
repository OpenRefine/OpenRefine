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

package org.openrefine.history;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.expr.ParsingException;
import org.openrefine.model.changes.Change;
import org.openrefine.operations.Operation;
import org.openrefine.operations.Operation.NotImmediateOperationException;
import org.openrefine.util.JsonViews;
import org.openrefine.util.ParsingUtilities;

/**
 * This is the metadata of a Change. It's small, so we can load it in order to obtain information about a change without
 * actually loading the change.
 */
public class HistoryEntry {

    final static Logger logger = LoggerFactory.getLogger("HistoryEntry");
    private final long id;
    private final String description;
    private final OffsetDateTime time;

    // the abstract operation, if any, that results in the change
    private final Operation operation;

    // the actual change

    final protected Change change;

    // JsonIgnore because it is included later on in special cases, see {@link getJsonChange}.
    @JsonIgnore
    public Change getChange() {
        return change;
    }

    static public long allocateID() {
        return Math.round(Math.random() * 1000000) + System.currentTimeMillis();
    }

    @JsonCreator
    public HistoryEntry(
            @JsonProperty("id") long id,
            @JsonProperty("description") String description,
            @JsonProperty("operation") Operation operation,
            @JsonProperty("change") Change change) throws NotImmediateOperationException {
        this(id,
                description,
                operation,
                OffsetDateTime.now(ZoneId.of("Z")),
                change);
    }

    protected HistoryEntry(
            long id,
            String description,
            Operation operation,
            OffsetDateTime time,
            Change change) throws NotImmediateOperationException {
        this.id = id;
        this.description = description;
        this.operation = operation;
        this.time = time;
        Change actualChange = change;
        if (change == null && operation != null) {
            try {
                actualChange = change != null ? change : operation.createChange();
            } catch (ParsingException e) {
                // todo redesign error reporting for changes which fail to be loaded
                // for instance, suppose a language evaluator is not available:
                // how do we fail?
            }
        }
        this.change = actualChange;
    }

    static public HistoryEntry load(String s) throws IOException {
        return ParsingUtilities.mapper.readValue(s, HistoryEntry.class);
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("id")
    public long getId() {
        return id;
    }

    @JsonProperty("time")
    public OffsetDateTime getTime() {
        return time;
    }

    @JsonProperty("operation")
    @JsonView(JsonViews.SaveMode.class)
    public Operation getOperation() {
        return operation;
    }

    /**
     * Only returns a change if it needs to be serialized to JSON, i.e. if it is not directly derived from the
     * operation.
     */
    @JsonProperty("change")
    @JsonView(JsonViews.SaveMode.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Change getJsonChange() {
        if (change.isImmediate()) {
            return null;
        } else {
            return change;
        }
    }

}
