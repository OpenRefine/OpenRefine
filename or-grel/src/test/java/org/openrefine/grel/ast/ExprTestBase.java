
package org.openrefine.grel.ast;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.testng.annotations.BeforeMethod;

/**
 * Base class to test expression classes. Contains utilities to test column dependency extraction.
 * 
 * 
 * @author Antonin Delpeuch
 */
public class ExprTestBase {

    protected String baseColumn = "column";
    protected GrelExpr currentColumn;
    protected GrelExpr unanalyzable;
    protected GrelExpr twoColumns;
    protected GrelExpr constant;

    @BeforeMethod
    public void setUp() {
        currentColumn = mock(GrelExpr.class);
        unanalyzable = mock(GrelExpr.class);
        twoColumns = mock(GrelExpr.class);
        constant = mock(GrelExpr.class);

        when(currentColumn.getColumnDependencies(baseColumn))
                .thenReturn(set(baseColumn));
        when(unanalyzable.getColumnDependencies(baseColumn))
                .thenReturn(null);
        when(twoColumns.getColumnDependencies(baseColumn))
                .thenReturn(set("a", "b"));
        when(constant.getColumnDependencies(baseColumn))
                .thenReturn(set());
    }

    protected Set<String> set(String... strings) {
        return Arrays.asList(strings).stream().collect(Collectors.toSet());
    }

}
