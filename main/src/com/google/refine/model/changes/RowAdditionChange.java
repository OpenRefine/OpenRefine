
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
    private static String _countField = "count"; // Field name for sizes of `_additionalRows` in `change.txt`
    private static String _insertionIndexField = "index"; // Field name for index to insert new rows in `change.txt`
    private static String eoc = "/ec/"; // end of change marker, not end of file, in `change.txt`

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
            int endIndex = _additionalRows.size();
            project.rows.subList(startIndex, endIndex).clear();

            project.columnModel.clearPrecomputes();
            ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProject(project.id);
            project.update();
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {

        writer.write(_insertionIndexField + "=");
        writer.write(Integer.toString(_insertionIndex));
        writer.write('\n');

        writer.write(_countField + "=");
        writer.write(Integer.toString(_additionalRows.size()));
        writer.write('\n');
        for (Row row : _additionalRows) {
            row.save(writer, options);
            writer.write('\n');
        }
        writer.write(eoc + "\n");
    }

    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        List<Row> rows = new ArrayList<>();
        int count;
        int index = 0;

        String line;
        while ((line = reader.readLine()) != null && !eoc.equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);

            if (_insertionIndexField.contentEquals(field)) {
                index = Integer.parseInt(line.substring(equal + 1));
            } else if (_countField.contentEquals(field)) {
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
