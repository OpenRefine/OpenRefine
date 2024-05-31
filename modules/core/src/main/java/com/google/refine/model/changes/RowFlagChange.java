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
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.history.Change;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.Pool;

public class RowFlagChange implements Change {

    final int rowIndex;
    final boolean newFlagged;
    Boolean oldFlagged = null;

    public RowFlagChange(int rowIndex, boolean newFlagged) {
        this.rowIndex = rowIndex;
        this.newFlagged = newFlagged;
    }

    @Override
    public void apply(Project project) {
        Row row = project.rows.get(rowIndex);
        if (oldFlagged == null) {
            oldFlagged = row.flagged;
        }
        row.flagged = newFlagged;
    }

    @Override
    public void revert(Project project) {
        Row row = project.rows.get(rowIndex);

        row.flagged = oldFlagged;
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("row=");
        writer.write(Integer.toString(rowIndex));
        writer.write('\n');
        writer.write("newFlagged=");
        writer.write(Boolean.toString(newFlagged));
        writer.write('\n');
        writer.write("oldFlagged=");
        writer.write(Boolean.toString(oldFlagged));
        writer.write('\n');
        writer.write("/ec/\n"); // end of change marker
    }

    static public RowFlagChange load(LineNumberReader reader, Pool pool) throws Exception {
        int row = -1;
        boolean oldFlagged = false;
        boolean newFlagged = false;

        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);

            if ("row".equals(field)) {
                row = Integer.parseInt(value);
            } else if ("oldFlagged".equals(field)) {
                oldFlagged = Boolean.parseBoolean(value);
            } else if ("newFlagged".equals(field)) {
                newFlagged = Boolean.parseBoolean(value);
            }
        }

        RowFlagChange change = new RowFlagChange(row, newFlagged);
        change.oldFlagged = oldFlagged;

        return change;
    }
}
