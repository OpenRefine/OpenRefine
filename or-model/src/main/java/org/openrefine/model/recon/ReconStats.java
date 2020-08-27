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

package org.openrefine.model.recon;

import java.io.Serializable;

import org.openrefine.model.Row;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Stores statistics about the judgment of reconciled cells in a column.
 * @author Antonin Delpeuch
 *
 */
public interface ReconStats extends Serializable {   

    public static final ReconStats ZERO = new ReconStatsImpl(0L, 0L, 0L);
    
    @JsonCreator
    public static ReconStats create(
            @JsonProperty("nonBlanks")
            long nonBlanks,
            @JsonProperty("newTopics")
            long newTopics,
            @JsonProperty("matchedTopics")
            long matchedTopics) {
        return new ReconStatsImpl(nonBlanks, newTopics, matchedTopics);
    }
    
    @JsonProperty("nonBlanks")
    public long getNonBlanks();
    
    @JsonProperty("newTopics")
    public long getNewTopics();

    @JsonProperty("matchedTopics")
    public long getMatchedTopics();
    
    /**
     * Updates the recon statistics with an additional row
     * @param row
     * @param columnIndex
     * @return
     */
    public ReconStats withRow(Row row, int columnIndex);
    
    /**
     * Sums the statistics with those contained in another instance
     * @param other
     * @return
     */
    public ReconStats sum(ReconStats other);
}
