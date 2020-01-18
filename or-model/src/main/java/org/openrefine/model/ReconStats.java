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

package org.openrefine.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function2;

import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Recon.Judgment;

public class ReconStats implements Serializable {

    private static final long serialVersionUID = -6321424927189309528L;

    @JsonProperty("nonBlanks")
    final public long nonBlanks;
    @JsonProperty("newTopics")
    final public long newTopics;
    @JsonProperty("matchedTopics")
    final public long matchedTopics;

    /**
     * Creates a summary of reconciliation statistics.
     * 
     * @param nonBlanks
     *            the number of non blank cells in the column
     * @param newTopics
     *            the number of cells matched to a new topic in the column
     * @param matchedTopics
     *            the number of cells matched to an existing topic in the column
     */
    @JsonCreator
    public ReconStats(
            @JsonProperty("nonBlanks") long nonBlanks,
            @JsonProperty("newTopics") long newTopics,
            @JsonProperty("matchedTopics") long matchedTopics) {
        this.nonBlanks = nonBlanks;
        this.newTopics = newTopics;
        this.matchedTopics = matchedTopics;
    }

    /**
     * Creates reconciliation statistics from a column of cells.
     * 
     * @param cells
     *            a RDD of cells
     * @return the statistics of their reconciliation status
     */
    static public ReconStats create(JavaRDD<Cell> cells) {

        ReconStats zero = new ReconStats(0, 0, 0);
        Function2<ReconStats, Cell, ReconStats> incrementer = new Function2<ReconStats, Cell, ReconStats>() {

            private static final long serialVersionUID = 2389723987L;

            @Override
            public ReconStats call(ReconStats stats, Cell cell) throws Exception {
                int nonBlanks = 0;
                int newTopics = 0;
                int matchedTopics = 0;
                if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                    nonBlanks++;

                    if (cell.recon != null) {
                        if (cell.recon.judgment == Judgment.New) {
                            newTopics++;
                        } else if (cell.recon.judgment == Judgment.Matched) {
                            matchedTopics++;
                        }
                    }
                }
                return new ReconStats(
                        stats.nonBlanks + nonBlanks,
                        stats.newTopics + newTopics,
                        stats.matchedTopics + matchedTopics);
            }
        };
        return cells.aggregate(zero, incrementer, (stateA, stateB) -> stateA.add(stateB));
    }

    /**
     * Adds two recon stats into a new recon stats object
     * 
     * @param other
     *            the other recon stats to add
     * @return a recon stats whose statistics are the sum of the two original ones
     */
    protected ReconStats add(ReconStats other) {
        return new ReconStats(
                nonBlanks + other.nonBlanks,
                newTopics + other.newTopics,
                matchedTopics + other.matchedTopics);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReconStats)) {
            return false;
        }
        ReconStats rs = (ReconStats) other;
        return (rs.nonBlanks == nonBlanks &&
                rs.newTopics == newTopics &&
                rs.matchedTopics == matchedTopics);
    }

    @Override
    public String toString() {
        return String.format("[ReconStats: non-blanks: %d, new: %d, matched: %d]",
                nonBlanks, newTopics, matchedTopics);
    }
}
