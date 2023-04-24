/*

Copyright 2010, 2022 Google Inc. & OpenRefine contributors
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

package org.openrefine.model;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.expr.EvalError;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.HasFields;
import org.openrefine.model.recon.Recon;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.StringUtils;

/**
 * A cell in the project grid. It can store data in two fields:
 * <ul>
 * <li>value: the main value of the cell</li>
 * <li>recon: reconciliation information associated with the cell, which is null when the cell is not reconciled</li>
 * </ul>
 *
 * In addition, the {@link #isPending()} flag can be set to indicate that this cell is being computed by an operation
 * and therefore its value will change in the near future. This is helpful to render partial results of operartions
 * before they complete.
 */
public class Cell implements HasFields, Serializable {

    private static final long serialVersionUID = 6587215646810559731L;

    public static Cell NULL = new Cell(null, null);
    public static Cell PENDING_NULL = new Cell(null, null, true);

    @JsonIgnore
    final public Serializable value;
    @JsonIgnore
    final public Recon recon;
    @JsonIgnore
    final public boolean pending;

    private static final Logger logger = LoggerFactory.getLogger(Cell.class);

    public Cell(Serializable value, Recon recon) {
        this.value = value;
        this.recon = recon;
        this.pending = false;
    }

    public Cell(Serializable value, Recon recon, boolean pending) {
        this.value = value;
        this.recon = recon;
        this.pending = pending;
    }

    @Override
    public Object getField(String name) {
        if ("value".equals(name)) {
            return value;
        } else if ("recon".equals(name)) {
            return recon;
        } else if ("errorMessage".equals(name)) {
            return getErrorMessage();
        }
        return null;
    }

    @Override
    public boolean fieldAlsoHasFields(String name) {
        return "recon".equals(name);
    }

    @JsonProperty("e")
    @JsonInclude(Include.NON_NULL)
    public String getErrorMessage() {
        if (ExpressionUtils.isError(value)) {
            return ((EvalError) value).message;
        }
        return null;
    }

    @JsonProperty("t")
    @JsonInclude(Include.NON_NULL)
    public String getTypeString() {
        if (value instanceof OffsetDateTime || value instanceof LocalDateTime) {
            return "date";
        }
        return null;
    }

    @JsonProperty("v")
    @JsonInclude(Include.NON_NULL)
    public Object getValue() {
        if (value != null && !ExpressionUtils.isError(value)) {
            Instant instant = null;
            if (value instanceof OffsetDateTime) {
                instant = ((OffsetDateTime) value).toInstant();
            } else if (value instanceof LocalDateTime) {
                instant = ((LocalDateTime) value).toInstant(ZoneOffset.of("Z"));
            }

            if (instant != null) {
                return instant.toString();
            } else if (value instanceof Double
                    && (((Double) value).isNaN() || ((Double) value).isInfinite())) {
                // write as a string
                return value.toString();
            } else if (value instanceof Float
                    && (((Float) value).isNaN() || ((Float) value).isInfinite())) {
                return value.toString();
            } else if (value instanceof Boolean || value instanceof Number) {
                return value;
            } else {
                return value.toString();
            }
        } else {
            return null;
        }
    }

    @JsonProperty("r")
    @JsonInclude(Include.NON_NULL)
    public Recon getRecon() {
        return recon;
    }

    @JsonProperty("p")
    @JsonInclude(Include.NON_NULL)
    public Boolean isPendingJson() {
        return pending ? true : null;
    }

    @JsonIgnore
    public boolean isPending() {
        return pending;
    }

    public void save(Writer writer) {
        try {
            ParsingUtilities.saveWriter.writeValue(writer, this);
        } catch (IOException e) {
            logger.error("Error writing cell to writer", e);
        }
    }

    static public Cell loadStreaming(String s) throws Exception {
        return ParsingUtilities.mapper.readValue(s, Cell.class);
    }

    @JsonCreator
    static public Cell deserialize(
            @JsonProperty("v") Object value,
            @JsonProperty("t") String type,
            @JsonProperty("r") Recon recon,
            @JsonProperty("e") String error,
            @JsonProperty("p") Boolean pending) {
        if ("date".equals(type)) {
            value = ParsingUtilities.stringToDate((String) value);
        }
        if (error != null) {
            value = new EvalError(error);
        }
        return new Cell((Serializable) value, recon, pending != null ? pending : false);
    }

    @Override
    public String toString() {
        if (pending) {
            return "[Cell (pending)]";
        } else if (recon == null) {
            return StringUtils.toString(value);
        } else {
            return String.format("[Cell \"%s\" %s]", StringUtils.toString(value), recon);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return value == null && !pending;
        }
        if (!(other instanceof Cell)) {
            return false;
        }
        Cell otherCell = (Cell) other;
        return (Objects.equals(value, otherCell.value) &&
                Objects.equals(recon, otherCell.recon) &&
                pending == otherCell.pending);
    }
}
