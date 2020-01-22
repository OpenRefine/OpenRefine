
package org.openrefine.browsing.facets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.browsing.RowFilter;
import org.openrefine.model.Cell;
import org.openrefine.model.Row;

public class AllFacetsStateTests {

    protected RowFilter filterRowId = new RowFilter() {

        @Override
        public boolean filterRow(long rowIndex, Row row) {
            return rowIndex < 2;
        }
    };
    protected RowFilter filterFoo = new RowFilter() {

        @Override
        public boolean filterRow(long rowIndex, Row row) {
            return "foo".equals(row.getCellValue(0));
        }
    };
    protected RowFilter filterBar = new RowFilter() {

        @Override
        public boolean filterRow(long rowIndex, Row row) {
            return "bar".equals(row.getCellValue(1));
        }
    };

    protected AllFacetsState SUT;

    @BeforeMethod
    public void setUpAllFacetsState() {
        SUT = new AllFacetsState(
                Arrays.asList(
                        new FacetStateStub(0, 0, filterRowId),
                        new FacetStateStub(0, 0, filterFoo),
                        new FacetStateStub(0, 0, filterBar)));
    }

    @Test
    public void testGetFacetStates() {
        Assert.assertEquals(SUT.getFacetStates(), Arrays.asList(
                new FacetStateStub(0, 0, filterRowId),
                new FacetStateStub(0, 0, filterFoo),
                new FacetStateStub(0, 0, filterBar)));
    }

    @Test
    public void testMerge() {
        AllFacetsState sut1 = new AllFacetsState(
                Arrays.asList(
                        new FacetStateStub(1, 2, filterRowId),
                        new FacetStateStub(3, 4, filterFoo),
                        new FacetStateStub(5, 6, filterBar)));
        AllFacetsState sut2 = new AllFacetsState(
                Arrays.asList(
                        new FacetStateStub(7, 8, filterRowId),
                        new FacetStateStub(9, 10, filterFoo),
                        new FacetStateStub(11, 12, filterBar)));
        AllFacetsState expected = new AllFacetsState(
                Arrays.asList(
                        new FacetStateStub(8, 10, filterRowId),
                        new FacetStateStub(12, 14, filterFoo),
                        new FacetStateStub(16, 18, filterBar)));
        Assert.assertEquals(sut1.merge(sut2), expected);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMergeIncompatibleStates() {
        AllFacetsState sut2 = new AllFacetsState(
                Arrays.asList(
                        new FacetStateStub(1, 2, filterRowId),
                        new FacetStateStub(3, 4, filterFoo)));
        SUT.merge(sut2);
    }

    @Test
    public void testIncrementAllMatching() {
        Row row = new Row(Arrays.asList(
                new Cell("foo", null), new Cell("bar", null)));

        AllFacetsState result = SUT.increment(1, row);
        Assert.assertEquals(result.getFacetStates(), Arrays.asList(
                new FacetStateStub(1, 0, filterRowId),
                new FacetStateStub(1, 0, filterFoo),
                new FacetStateStub(1, 0, filterBar)));
    }

    @Test
    public void testIncrementAllButOneMatching() {
        Row row = new Row(Arrays.asList(
                new Cell("wrong", null), new Cell("bar", null)));

        AllFacetsState result = SUT.increment(1, row);
        Assert.assertEquals(result.getFacetStates(), Arrays.asList(
                new FacetStateStub(1, 0, filterRowId),
                new FacetStateStub(0, 1, filterFoo),
                new FacetStateStub(1, 0, filterBar)));
    }

    @Test
    public void testIncrementTwoMismatching() {
        Row row = new Row(Arrays.asList(
                new Cell("wrong", null), new Cell("fail", null)));

        AllFacetsState result = SUT.increment(1, row);
        Assert.assertEquals(result.getFacetStates(), Arrays.asList(
                new FacetStateStub(0, 0, filterRowId),
                new FacetStateStub(0, 0, filterFoo),
                new FacetStateStub(0, 0, filterBar)));
    }

    @Test
    public void testIncrementAllMismatching() {
        Row row = new Row(Arrays.asList(
                new Cell("wrong", null), new Cell("fail", null)));

        AllFacetsState result = SUT.increment(43, row);
        Assert.assertEquals(result.getFacetStates(), Arrays.asList(
                new FacetStateStub(0, 0, filterRowId),
                new FacetStateStub(0, 0, filterFoo),
                new FacetStateStub(0, 0, filterBar)));
    }

    @Test
    public void testToString() {
        FacetState state1 = mock(FacetState.class);
        FacetState state2 = mock(FacetState.class);
        when(state1.toString()).thenReturn("state1");
        when(state2.toString()).thenReturn("state2");
        SUT = new AllFacetsState(Arrays.asList(
                state1, state2));
        Assert.assertEquals(SUT.toString(), "state1, state2");
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEquals() {
        Assert.assertFalse(SUT.equals(34));
    }
}
