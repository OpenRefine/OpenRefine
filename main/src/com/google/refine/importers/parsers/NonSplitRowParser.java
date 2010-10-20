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

package com.google.refine.importers.parsers;

import java.io.LineNumberReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.refine.importers.ImporterUtilities;
import com.google.refine.model.Cell;
import com.google.refine.model.Row;

public class NonSplitRowParser extends RowParser {

    public List<String> split(String line, LineNumberReader lineReader) {
        List<String> results = new ArrayList<String>(1);
        
        results.add(line.trim());
        
        return results;
    }
    
    public boolean parseRow(Row row, String line, boolean guessValueType, LineNumberReader lineReader) {
        if (line.trim().isEmpty()) {
            return false;
        } else {
            Serializable value = guessValueType ? ImporterUtilities.parseCellValue(line) : line;
            if (value != null) {
                row.cells.add(new Cell(value, null));
                return true;
            } else {
                row.cells.add(null);
                return false;
            }
        }
    }    
    
}
