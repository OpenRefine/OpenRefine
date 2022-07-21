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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import com.google.refine.ProjectManager;
import com.google.refine.history.Change;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class MassReconChange implements Change {

    final protected Map<Long, Recon> _newRecons;
    final protected Map<Long, Recon> _oldRecons;

    public MassReconChange(Map<Long, Recon> newRecons, Map<Long, Recon> oldRecons) {
        _newRecons = newRecons;
        _oldRecons = oldRecons;
    }

    @Override
    public void apply(Project project) {
        switchRecons(project, _newRecons);
    }

    @Override
    public void revert(Project project) {
        switchRecons(project, _oldRecons);
    }

    protected void switchRecons(Project project, Map<Long, Recon> reconMap) {
        synchronized (project) {
            HashSet<String> flushedColumn = new HashSet<String>();
            for (Row row : project.rows) {
                for (int c = 0; c < row.cells.size(); c++) {
                    Cell cell = row.cells.get(c);
                    if (cell != null && cell.recon != null) {
                        Recon recon = cell.recon;

                        if (reconMap.containsKey(recon.id)) {
                            // skip the flushing if already done
                            String columnName = project.columnModel.getColumnByCellIndex(c).getName();
                            if (!flushedColumn.contains(columnName)) {
                                ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProjectColumn(project.id,
                                        columnName);
                                flushedColumn.add(columnName);
                            }

                            row.setCell(c, new Cell(cell.value, reconMap.get(recon.id)));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writeRecons(writer, options, _oldRecons, "oldReconCount");
        writeRecons(writer, options, _newRecons, "newReconCount");
        writer.write("/ec/\n"); // end of change marker
    }

    protected void writeRecons(Writer writer, Properties options, Map<Long, Recon> recons, String key) throws IOException {
        writer.write(key + "=");
        writer.write(Integer.toString(recons.size()));
        writer.write('\n');
        for (Recon recon : recons.values()) {
            Pool pool = (Pool) options.get("pool");
            pool.poolReconCandidates(recon);

            ParsingUtilities.saveWriter.writeValue(writer, recon);
            writer.write("\n");
        }
    }

    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        Map<Long, Recon> oldRecons = new HashMap<Long, Recon>();
        Map<Long, Recon> newRecons = new HashMap<Long, Recon>();

        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);

            if ("oldReconCount".equals(field)) {
                loadRecons(reader, pool, oldRecons, value);
            } else if ("newReconCount".equals(field)) {
                loadRecons(reader, pool, newRecons, value);
            }
        }

        MassReconChange change = new MassReconChange(newRecons, oldRecons);

        return change;
    }

    static protected void loadRecons(LineNumberReader reader, Pool pool, Map<Long, Recon> recons, String countString) throws Exception {
        int count = Integer.parseInt(countString);

        for (int i = 0; i < count; i++) {
            String line = reader.readLine();
            Recon recon = Recon.loadStreaming(line);

            recons.put(recon.id, recon);
        }
    }
}
