
package com.google.refine.grel.ast;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.Optional;
import java.util.Set;

import org.testng.annotations.Test;

public class BracketedExprTest extends ExprTestBase {

    @Test
    public void testGetSource() {
        when(constant.toString()).thenReturn("'foo'");
        assertEquals(new BracketedExpr(constant).toString(), "('foo')");
    }

    @Test
    public void testGetColumnDependencies() {
        assertEquals(new BracketedExpr(constant).getColumnDependencies(baseColumn), Optional.of(Set.of()));
        assertEquals(new BracketedExpr(currentColumn).getColumnDependencies(baseColumn), Optional.of(Set.of("baseColumn")));
    }

}
