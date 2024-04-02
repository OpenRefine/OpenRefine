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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.refine.history.Change;
import com.google.refine.model.ColumnGroup;
import com.google.refine.util.ParsingUtilities;

abstract public class ColumnChange implements Change {

    static public void writeOldColumnGroups(Writer writer, Properties options,
            List<ColumnGroup> oldColumnGroups) throws IOException {
        writer.write("oldColumnGroupCount=");
        writer.write(Integer.toString(oldColumnGroups.size()));
        writer.write('\n');
        for (ColumnGroup cg : oldColumnGroups) {
            ParsingUtilities.saveWriter.writeValue(writer, cg);
            writer.write('\n');
        }
    }

    static public List<ColumnGroup> readOldColumnGroups(
            LineNumberReader reader, int oldColumnGroupCount) throws Exception {
        List<ColumnGroup> oldColumnGroups = new ArrayList<ColumnGroup>(oldColumnGroupCount);
        for (int i = 0; i < oldColumnGroupCount; i++) {
            String line = reader.readLine();
            if (line != null) {
                oldColumnGroups.add(ColumnGroup.load(line));
            }
        }
        return oldColumnGroups;
    }
}
