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

import com.google.refine.ProjectManager;
import com.google.refine.history.Change;
import com.google.refine.model.Project;
import com.google.refine.util.Pool;

public class ColumnRenameChange extends ColumnChange {

    final protected String _oldColumnName;
    final protected String _newColumnName;

    public ColumnRenameChange(String oldColumnName, String newColumnName) {
        _oldColumnName = oldColumnName;
        _newColumnName = newColumnName;
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {
            ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProjectColumn(project.id, _oldColumnName);
            project.columnModel.getColumnByName(_oldColumnName).setName(_newColumnName);
            project.columnModel.update();
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProjectColumn(project.id, _newColumnName);
            project.columnModel.getColumnByName(_newColumnName).setName(_oldColumnName);
            project.columnModel.update();
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("oldColumnName=");
        writer.write(_oldColumnName);
        writer.write('\n');
        writer.write("newColumnName=");
        writer.write(_newColumnName);
        writer.write('\n');
        writer.write("/ec/\n"); // end of change marker
    }

    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        String oldColumnName = null;
        String newColumnName = null;

        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);

            if ("oldColumnName".equals(field)) {
                oldColumnName = value;
            } else if ("newColumnName".equals(field)) {
                newColumnName = value;
            }
        }

        ColumnRenameChange change = new ColumnRenameChange(oldColumnName, newColumnName);

        return change;
    }
}
