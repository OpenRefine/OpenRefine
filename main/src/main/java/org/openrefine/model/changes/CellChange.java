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

package org.openrefine.model.changes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

import org.openrefine.history.Change;
import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;

public class CellChange implements Change {

    @JsonProperty("rowId")
    final public long row;
    @JsonProperty("cellIndex")
    final public int cellIndex;
    @JsonProperty("newCell")
    final public Cell newCell;

    @JsonCreator
    public CellChange(
            @JsonProperty("rowId") long row,
            @JsonProperty("cellIndex") int cellIndex,
            @JsonProperty("newCell") Cell newCell) {
        this.row = row;
        this.cellIndex = cellIndex;
        this.newCell = newCell;
    }

    @Override
    public GridState apply(GridState projectState) {

        JavaPairRDD<Long, Row> newRDD = projectState.getGrid()
                .map(mapFunction(cellIndex, row, newCell))
                .keyBy(t -> (Long) t._1)
                .mapValues(t -> t._2);
        return new GridState(projectState.getColumnModel(), newRDD, projectState.getOverlayModels());
    }

    static protected Function<Tuple2<Long, Row>, Tuple2<Long, Row>> mapFunction(int cellIndex, long row, Cell newCell) {
        return new Function<Tuple2<Long, Row>, Tuple2<Long, Row>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Tuple2<Long, Row> call(Tuple2<Long, Row> tuple) throws Exception {
                if (tuple._1() == row) {
                    return new Tuple2<Long, Row>(tuple._1(), tuple._2().withCell(cellIndex, newCell));
                } else {
                    return tuple;
                }
            }
        };
    }

    @Override
    public boolean isImmediate() {
        // this change has no corresponding operation, so it can not be derived from one
        return false;
    }
}
