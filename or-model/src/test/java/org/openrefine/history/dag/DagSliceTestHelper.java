package org.openrefine.history.dag;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;

public class DagSliceTestHelper {
    
    /**
     * Helper to create a list of column metadata in tests.
     * @param names
     * @return
     */
    public static ColumnModel columns(String... names) {
        List<String> columnNames = Arrays.asList(names);
        return new ColumnModel(
                columnNames.stream().map(n -> new ColumnMetadata(n)).collect(Collectors.toList()));
    }
    
    public static ColumnModel columns(ColumnMetadata... metadata) {
        return new ColumnModel(Arrays.asList(metadata));
    }
}
