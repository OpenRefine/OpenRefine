
package com.google.refine.grel.ast;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.testng.annotations.BeforeMethod;

import com.google.refine.expr.Evaluable;

/**
 * Base class to test expression classes. Contains utilities to test column dependency extraction.
 * 
 * @author Antonin Delpeuch
 */
public class ExprTestBase {

    protected Optional<String> baseColumn = Optional.of("baseColumn");
    protected Evaluable currentColumn;
    protected Evaluable unanalyzable;
    protected Evaluable twoColumns;
    protected Evaluable constant;

    protected Map<String, String> sampleRename = Map.of("baseColumn", "newBaseColumn", "a", "a2");
    protected Evaluable currentColumnRenamed;
    protected Evaluable twoColumnsRenamed;

    @BeforeMethod
    public void setUp() {
        currentColumn = mock(Evaluable.class);
        unanalyzable = mock(Evaluable.class);
        twoColumns = mock(Evaluable.class);
        constant = mock(Evaluable.class);

        currentColumnRenamed = mock(Evaluable.class);
        twoColumnsRenamed = mock(Evaluable.class);

        when(currentColumn.getColumnDependencies(baseColumn))
                .thenReturn(set("baseColumn"));
        when(currentColumn.renameColumnDependencies(sampleRename))
                .thenReturn(currentColumnRenamed);

        when(unanalyzable.getColumnDependencies(baseColumn))
                .thenReturn(Optional.empty());
        when(unanalyzable.renameColumnDependencies(any()))
                .thenReturn(unanalyzable);

        when(twoColumns.getColumnDependencies(baseColumn))
                .thenReturn(set("a", "b"));
        when(twoColumns.renameColumnDependencies(sampleRename))
                .thenReturn(twoColumnsRenamed);

        when(constant.getColumnDependencies(baseColumn))
                .thenReturn(set());
        when(constant.renameColumnDependencies(any()))
                .thenReturn(constant);
    }

    protected Optional<Set<String>> set(String... strings) {
        return Optional.of(Arrays.asList(strings).stream().collect(Collectors.toSet()));
    }

}
