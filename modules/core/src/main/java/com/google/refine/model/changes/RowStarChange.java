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

public class RowStarChange implements Change {

    final int rowIndex;
    final boolean newStarred;
    Boolean oldStarred = null;

    public RowStarChange(int rowIndex, boolean newStarred) {
        this.rowIndex = rowIndex;
        this.newStarred = newStarred;
    }

    @Override
    public void apply(Project project) {
        Row row = project.rows.get(rowIndex);
        if (oldStarred == null) {
            oldStarred = row.starred;
        }
        row.starred = newStarred;
    }

    @Override
    public void revert(Project project) {
        Row row = project.rows.get(rowIndex);

        row.starred = oldStarred;
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("row=");
        writer.write(Integer.toString(rowIndex));
        writer.write('\n');
        writer.write("newStarred=");
        writer.write(Boolean.toString(newStarred));
        writer.write('\n');
        writer.write("oldStarred=");
        writer.write(Boolean.toString(oldStarred));
        writer.write('\n');
        writer.write("/ec/\n"); // end of change marker
    }

    static public RowStarChange load(LineNumberReader reader, Pool pool) throws Exception {
        int row = -1;
        boolean oldStarred = false;
        boolean newStarred = false;

        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);

            if ("row".equals(field)) {
                row = Integer.parseInt(value);
            } else if ("oldStarred".equals(field)) {
                oldStarred = Boolean.parseBoolean(value);
            } else if ("newStarred".equals(field)) {
                newStarred = Boolean.parseBoolean(value);
            }
        }

        RowStarChange change = new RowStarChange(row, newStarred);
        change.oldStarred = oldStarred;

        return change;
    }
}
