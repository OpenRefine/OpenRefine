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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.util.ParsingUtilities;

public class ReconStats  {   
    @JsonProperty("nonBlanks")
    final public int    nonBlanks;
    @JsonProperty("newTopics")
    final public int    newTopics;
    @JsonProperty("matchedTopics")
    final public int    matchedTopics;
    
    @JsonCreator
    public ReconStats(
            @JsonProperty("nonBlanks")
            int nonBlanks,
            @JsonProperty("newTopics")
            int newTopics,
            @JsonProperty("matchedTopics")
            int matchedTopics) {
        this.nonBlanks = nonBlanks;
        this.newTopics = newTopics;
        this.matchedTopics = matchedTopics;
    }
    
    static public ReconStats create(Project project, int cellIndex) {
        int nonBlanks = 0;
        int newTopics = 0;
        int matchedTopics = 0;
        
        for (Row row : project.rows) {
            Cell cell = row.getCell(cellIndex);
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
        }
        
        return new ReconStats(nonBlanks, newTopics, matchedTopics);
    }
    
    public void save(Writer writer) {
        try {
            ParsingUtilities.defaultWriter.writeValue(writer, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
