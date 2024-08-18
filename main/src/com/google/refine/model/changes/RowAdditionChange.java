/*******************************************************************************
 * Copyright (C) 2024, OpenRefine contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.refine.ProjectManager;
import com.google.refine.history.Change;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.Pool;

public class RowAdditionChange implements Change {

    private final List<Row> _additionalRows;

    private final int _insertionIndex;
    private static String COUNT_FIELD = "count"; // Field name for sizes of `_additionalRows` in `change.txt`
    private static String INDEX_FIELD = "index"; // Field name for index to insert new rows in `change.txt`
    private static String EOC = "/ec/"; // end of change marker, not end of file, in `change.txt`

    public RowAdditionChange(List<Row> additionalRows, int insertionIndex) {
        _additionalRows = additionalRows;
        _insertionIndex = insertionIndex;
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {
            project.rows.addAll(_insertionIndex, _additionalRows);

            project.update();
            project.columnModel.clearPrecomputes();
            ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProject(project.id);
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            int startIndex = _insertionIndex;
            int endIndex = _insertionIndex + _additionalRows.size();
            project.rows.subList(startIndex, endIndex).clear();

            project.columnModel.clearPrecomputes();
            ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProject(project.id);
            project.update();
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {

        writer.write(INDEX_FIELD + "=");
        writer.write(Integer.toString(_insertionIndex));
        writer.write('\n');

        writer.write(COUNT_FIELD + "=");
        writer.write(Integer.toString(_additionalRows.size()));
        writer.write('\n');
        for (Row row : _additionalRows) {
            row.save(writer, options);
            writer.write('\n');
        }
        writer.write(EOC + "\n");
    }

    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        List<Row> rows = new ArrayList<>();
        int count;
        int index = 0;

        String line;
        while ((line = reader.readLine()) != null && !EOC.equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);

            if (INDEX_FIELD.contentEquals(field)) {
                index = Integer.parseInt(line.substring(equal + 1));
            } else if (COUNT_FIELD.contentEquals(field)) {
                count = Integer.parseInt(line.substring(equal + 1));
                for (int i = 0; i < count; i++) {
                    if ((line = reader.readLine()) != null) {
                        rows.add(Row.load(line, pool));
                    }
                }
            }
        }

        return new RowAdditionChange(rows, index);
    }
}
