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

package com.google.refine.model.changes;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.refine.model.Cell;
import com.google.refine.util.Pool;

public class CellAtRowCellIndex {

    final public int row;
    final public int cellIndex;
    final public Cell cell;
    final static private Pattern semicolonPattern = Pattern.compile(";");

    public CellAtRowCellIndex(int row, int cellIndex, Cell cell) {
        this.row = row;
        this.cell = cell;
        this.cellIndex = cellIndex;
    }

    public void save(Writer writer, Properties options) throws IOException {
        writer.write(Integer.toString(row));
        writer.write(';');
        writer.write(Integer.toString(cellIndex));
        writer.write(';');
        if (cell != null) {
            cell.save(writer, options);
        }
    }

    static public CellAtRowCellIndex load(String s, Pool pool) throws Exception {

        Matcher m = semicolonPattern.matcher(s);

        m.find();
        int semicolon = m.start();
        m.find();
        int nextSemicolon = m.start();

        int row = Integer.parseInt(s.substring(0, semicolon));
        int cellIndex = Integer.parseInt(s.substring(semicolon + 1, nextSemicolon));
        Cell cell = nextSemicolon < s.length() - 1 ? Cell.loadStreaming(s.substring(nextSemicolon + 1), pool)
                : null;

        return new CellAtRowCellIndex(row, cellIndex, cell);
    }
}
