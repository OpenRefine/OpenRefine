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
import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.operations.Operation;
import org.openrefine.util.JsonViews;
import org.openrefine.util.ParsingUtilities;

/**
 * A record of applying an operation on the project at a point in time.
 */
public class HistoryEntry {

    final static Logger logger = LoggerFactory.getLogger("HistoryEntry");
    private final long id;
    private final Instant time;

    // the operation applied at this step
    private final Operation operation;

    // whether the change preserved the structure of the grid
    protected final GridPreservation gridPreservation;

    static public long allocateID() {
        return Math.round(Math.random() * 1000000) + System.currentTimeMillis();
    }

    @JsonCreator
    public HistoryEntry(
            @JsonProperty("id") long id,
            @JsonProperty("operation") Operation operation,
            @JsonProperty("gridPreservation") GridPreservation gridPreservation) {
        this(id,
                operation,
                Instant.now(),
                gridPreservation);
    }

    protected HistoryEntry(
            long id,
            Operation operation,
            Instant time,
            GridPreservation gridPreservation) {
        this.id = id;
        Validate.notNull(operation);
        this.operation = operation;
        this.time = time;
        this.gridPreservation = gridPreservation != null ? gridPreservation : GridPreservation.NO_ROW_PRESERVATION;
    }

    static public HistoryEntry load(String s) throws IOException {
        return ParsingUtilities.mapper.readValue(s, HistoryEntry.class);
    }

    @JsonProperty("description")
    public String getDescription() {
        return operation.getDescription();
    }

    @JsonProperty("id")
    public long getId() {
        return id;
    }

    @JsonProperty("time")
    public Instant getTime() {
        return time;
    }

    @JsonProperty("operation")
    @JsonView(JsonViews.SaveMode.class)
    public Operation getOperation() {
        return operation;
    }

    @JsonProperty("gridPreservation")
    public GridPreservation getGridPreservation() {
        return gridPreservation;
    }

    @Override
    public String toString() {
        return "HistoryEntry [id=" + id + ", time=" + time + ", operation=" + operation + ", gridPreservation="
                + gridPreservation + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(gridPreservation, id, operation, time);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HistoryEntry other = (HistoryEntry) obj;
        return gridPreservation == other.gridPreservation && id == other.id
                && Objects.equals(operation, other.operation) && Objects.equals(time, other.time);
    }
}
