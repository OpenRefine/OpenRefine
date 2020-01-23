
package org.openrefine.browsing.util;

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.browsing.facets.Facet;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.WrappedRow;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Row;

public class StringValuesFacetStateTests {

    Facet facet;
    ColumnModel cm;
    Evaluable dummy = new Evaluable() {

        @Override
        public Object evaluate(Properties bindings) {
            WrappedRow row = (WrappedRow) bindings.get("row");
            return row.row.getCellValue(0);
        }
    };

    StringValuesFacetState SUT;

    @BeforeTest
    public void setUpState() {
        facet = mock(Facet.class);
        cm = new ColumnModel(Arrays.asList(new ColumnMetadata("mycolumn")));
        SUT = new StringValuesFacetState(facet, cm, dummy, 0);
    }

    @Test
    public void testAccessors() {
        Assert.assertEquals(SUT.getFacet(), facet);
        Assert.assertTrue(SUT.getCounts().isEmpty());
        Assert.assertEquals(SUT.getErrorCount(), 0L);
        Assert.assertEquals(SUT.getBlankCount(), 0L);
    }

    @Test
    public void testSum() {
        StringValuesFacetState first = new StringValuesFacetState(facet, cm, dummy, 0,
                Collections.<String, Long> singletonMap("foo", 3L), 4L, 5L);
        StringValuesFacetState second = new StringValuesFacetState(facet, cm, dummy, 0,
                Collections.<String, Long> singletonMap("foo", 4L), 10L, 11L);
        StringValuesFacetState combined = first.sum(second);

        Assert.assertEquals(combined.getBlankCount(), 16L);
        Assert.assertEquals(combined.getErrorCount(), 14L);
        Assert.assertEquals(combined.getCounts(), Collections.singletonMap("foo", 7L));
    }

    @Test
    public void testEvaluate() {
        Row row = new Row(Arrays.asList(new Cell("foo", null)));
        StringValuesFacetState first = SUT.withRow(1L, row);

        Assert.assertEquals(first.getBlankCount(), 0L);
        Assert.assertEquals(first.getErrorCount(), 0L);
        Assert.assertEquals((Object) first.getCounts().get("foo"), 1L);

        StringValuesFacetState second = first.withRow(2L, row);

        Assert.assertEquals(second.getBlankCount(), 0L);
        Assert.assertEquals(second.getErrorCount(), 0L);
        Assert.assertEquals((Object) second.getCounts().get("foo"), 2L);
    }

    @Test
    public void testEvaluateBlank() {
        Row row = new Row(Arrays.asList(new Cell("", null)));
        StringValuesFacetState first = SUT.withRow(1L, row);

        Assert.assertEquals(first.getBlankCount(), 1L);
        Assert.assertEquals(first.getErrorCount(), 0L);
        Assert.assertNull(first.getCounts().get("foo"));
    }

    @Test
    public void testEvaluateError() {
        Row row = new Row(Arrays.asList(new Cell(new EvalError("error"), null)));
        StringValuesFacetState first = SUT.withRow(1L, row);

        Assert.assertEquals(first.getBlankCount(), 0L);
        Assert.assertEquals(first.getErrorCount(), 1L);
        Assert.assertNull(first.getCounts().get("foo"));
    }
}
