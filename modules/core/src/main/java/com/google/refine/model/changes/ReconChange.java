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

/**
 * 
 */

package com.google.refine.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

import com.google.refine.ProjectManager;
import com.google.refine.history.Change;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.ReconStats;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class ReconChange extends MassCellChange {

    final protected ReconConfig _newReconConfig;
    protected ReconStats _newReconStats;

    protected ReconConfig _oldReconConfig;
    protected ReconStats _oldReconStats;

    public ReconChange(
            List<CellChange> cellChanges,
            String commonColumnName,
            ReconConfig newReconConfig,
            ReconStats newReconStats // can be null
    ) {
        super(cellChanges, commonColumnName, false);
        _newReconConfig = newReconConfig;
        _newReconStats = newReconStats;
    }

    public ReconChange(
            CellChange[] cellChanges,
            String commonColumnName,
            ReconConfig newReconConfig,
            ReconStats newReconStats // can be null
    ) {
        super(cellChanges, commonColumnName, false);
        _newReconConfig = newReconConfig;
        _newReconStats = newReconStats;
    }

    public ReconChange(
            CellChange cellChange,
            String commonColumnName,
            ReconConfig newReconConfig,
            ReconStats newReconStats // can be null
    ) {
        super(cellChange, commonColumnName, false);
        _newReconConfig = newReconConfig;
        _newReconStats = newReconStats;
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {
            super.apply(project);

            Column column = project.columnModel.getColumnByName(_commonColumnName);

            if (_newReconStats == null) {
                _newReconStats = ReconStats.create(project, column.getCellIndex());
            }

            _oldReconConfig = column.getReconConfig();
            _oldReconStats = column.getReconStats();

            column.setReconConfig(_newReconConfig);
            column.setReconStats(_newReconStats);

            column.clearPrecomputes();
            ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProjectColumn(project.id, _commonColumnName);
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            super.revert(project);

            Column column = project.columnModel.getColumnByName(_commonColumnName);
            column.setReconConfig(_oldReconConfig);
            column.setReconStats(_oldReconStats);

            column.clearPrecomputes();
            ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProjectColumn(project.id, _commonColumnName);
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("newReconConfig=");
        if (_newReconConfig != null) {
            _newReconConfig.save(writer);
        }
        writer.write('\n');

        writer.write("newReconStats=");
        if (_newReconStats != null) {
            _newReconStats.save(writer);
        }
        writer.write('\n');

        writer.write("oldReconConfig=");
        if (_oldReconConfig != null) {
            _oldReconConfig.save(writer);
        }
        writer.write('\n');

        writer.write("oldReconStats=");
        if (_oldReconStats != null) {
            _oldReconStats.save(writer);
        }
        writer.write('\n');

        super.save(writer, options);
    }

    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        ReconConfig newReconConfig = null;
        ReconStats newReconStats = null;
        ReconConfig oldReconConfig = null;
        ReconStats oldReconStats = null;

        String commonColumnName = null;
        CellChange[] cellChanges = null;

        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');

            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);

            if ("newReconConfig".equals(field)) {
                if (value.length() > 0) {
                    newReconConfig = ReconConfig.reconstruct(value);
                }
            } else if ("newReconStats".equals(field)) {
                if (value.length() > 0) {
                    newReconStats = ParsingUtilities.mapper.readValue(value, ReconStats.class);
                }
            } else if ("oldReconConfig".equals(field)) {
                if (value.length() > 0) {
                    oldReconConfig = ReconConfig.reconstruct(value);
                }
            } else if ("oldReconStats".equals(field)) {
                if (value.length() > 0) {
                    oldReconStats = ParsingUtilities.mapper.readValue(value, ReconStats.class);
                }
            } else if ("commonColumnName".equals(field)) {
                commonColumnName = value;
            } else if ("cellChangeCount".equals(field)) {
                int cellChangeCount = Integer.parseInt(value);

                cellChanges = new CellChange[cellChangeCount];
                for (int i = 0; i < cellChangeCount; i++) {
                    cellChanges[i] = CellChange.load(reader, pool);
                }
            }
        }

        ReconChange change = new ReconChange(
                cellChanges, commonColumnName, newReconConfig, newReconStats);

        change._oldReconConfig = oldReconConfig;
        change._oldReconStats = oldReconStats;

        return change;
    }
}
