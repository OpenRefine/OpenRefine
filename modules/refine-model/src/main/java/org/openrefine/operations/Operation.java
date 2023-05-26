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

package org.openrefine.operations;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.expr.ParsingException;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeData;

/**
 * An operation represents one step in a cleaning workflow in Refine. It applies to a single project by creating a
 * {@link Change}, which is stored in the {@link org.openrefine.history.History} by an
 * {@link org.openrefine.history.HistoryEntry}.
 * 
 * Operations only store the metadata for the transformation step. They are required to be serializable and
 * deserializable in JSON with Jackson, and the corresponding JSON object is shown in the JSON export of a workflow.
 * Therefore, the JSON serialization is expected to be stable and deserialization should be backwards-compatible.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "op", visible = true)
@JsonTypeIdResolver(OperationResolver.class)
public interface Operation {

    /**
     * Bundles up various pieces of information:
     * <ul>
     * <li>the new grid after applying the change</li>
     * <li>whether the rows or records of the original grid were preserved</li>
     * <li>a representation of the dependencies of this change</li>
     * </ul>
     */
    class ChangeResult {

        protected final Grid grid;
        protected final GridPreservation gridPreservation;

        public ChangeResult(Grid grid, GridPreservation gridPreservation) {
            this.grid = grid;
            this.gridPreservation = gridPreservation;
        }

        public Grid getGrid() {
            return grid;
        }

        public GridPreservation getGridPreservation() {
            return gridPreservation;
        }

    }

    class DoesNotApplyException extends Exception {

        public DoesNotApplyException(String message) {
            super(message);
        }

        private static final long serialVersionUID = 1L;

    }

    /**
     * Derives the new grid state from the current grid state. Executing this method should be quick (even on large
     * datasets) since it is expected to just derive the new grid from the existing one without actually executing any
     * expensive computation. Long-running computations should rather go in the derivation of a {@link ChangeData} which
     * will be fetched asynchronously.
     * 
     * @param projectState
     *            the state of the grid before the change
     * @return the state of the grid after the change
     * @throws Operation.DoesNotApplyException
     *             when the change cannot be applied to the given grid
     */
    public Operation.ChangeResult apply(Grid projectState, ChangeContext context) throws ParsingException, Operation.DoesNotApplyException;

    /**
     * Returns true when the change is derived purely from the operation metadata and does not store any data by itself.
     * In this case it does not need serializing as it can be recreated directly by {@link Operation#createChange()}.
     */
    @JsonIgnore
    default boolean isImmediate() {
        return true;
    }

    /**
     * The facets that are suggested to be created after this change is applied. This is not included in the JSON
     * serialization here, but rather in the containing HistoryEntry.
     */
    @JsonIgnore
    default List<FacetConfig> getCreatedFacets() {
        return Collections.emptyList();
    }

    /**
     * A short human-readable description of what this operation does.
     */
    @JsonProperty("description")
    public String getDescription();

    @JsonIgnore // the operation id is already added as "op" by the JsonTypeInfo annotation
    public default String getOperationId() {
        return OperationRegistry.s_opClassToName.get(this.getClass());
    }
}
