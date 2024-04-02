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

package com.google.refine.history;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.model.Project;
import com.google.refine.util.Pool;

public class ChangeSequence implements Change {

    final protected Change[] _changes;

    public ChangeSequence(Change[] changes) {
        _changes = changes;
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {
            for (Change change : _changes) {
                change.apply(project);
            }
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            for (int i = _changes.length - 1; i >= 0; i--) {
                _changes[i].revert(project);
            }
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("count=");
        writer.write(Integer.toString(_changes.length));
        writer.write('\n');
        for (Change change : _changes) {
            History.writeOneChange(writer, change, options);
        }
        writer.write("/ec/\n"); // end of change marker
    }

    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        String line = reader.readLine();
        if (line == null) {
            line = "";
        }
        int equal = line.indexOf('=');

        assert "count".equals(line.substring(0, equal));

        int count = Integer.parseInt(line.substring(equal + 1));
        Change[] changes = new Change[count];

        for (int i = 0; i < count; i++) {
            changes[i] = History.readOneChange(reader, pool);
        }

        line = reader.readLine();
        assert "/ec/".equals(line);

        return new ChangeSequence(changes);
    }
}
