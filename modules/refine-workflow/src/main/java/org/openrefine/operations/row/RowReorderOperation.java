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

package org.openrefine.operations.row;

import org.openrefine.browsing.Engine.Mode;
import org.openrefine.history.GridPreservation;
import org.openrefine.history.dag.DagSlice;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.sorting.SortingConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An operation which reorders the rows of the grid permanently according to a given sorting configuration.
 * 
 * @author Antonin Delpeuch
 *
 */
public class RowReorderOperation implements Operation {

    final protected Mode _mode;
    final protected SortingConfig _sorting;

    @JsonCreator
    public RowReorderOperation(
            @JsonProperty("mode") Mode mode,
            @JsonProperty("sorting") SortingConfig sorting) {
        _mode = mode;
        _sorting = sorting;
    }

    @JsonProperty("mode")
    public Mode getMode() {
        return _mode;
    }

    @JsonProperty("sorting")
    public SortingConfig getSortingConfig() {
        return _sorting;
    }

    @Override
    public String getDescription() {
        return "Reorder rows";
    }

    @Override
    public Change createChange() {
        return new RowReorderChange();
    }

    public class RowReorderChange implements Change {

        @Override
        public boolean isImmediate() {
            return true;
        }

        @Override
        public ChangeResult apply(Grid projectState, ChangeContext context) throws DoesNotApplyException {
            Grid grid;
            if (Mode.RowBased.equals(_mode)) {
                grid = projectState.reorderRows(_sorting, true);
            } else {
                grid = projectState.reorderRecords(_sorting, true);
            }
            return new ChangeResult(grid, GridPreservation.NO_ROW_PRESERVATION, null);
        }

    }

}
