package org.openrefine.history.dag;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.mockito.Mockito.mock;

import org.openrefine.model.ColumnModel;

public class IncompatibleSliceExceptionTests {
    
    private DagSlice slice;
    private ColumnModel model;
    private IncompatibleSliceException SUT;
    
    @BeforeMethod
    public void setUp() {
        slice = mock(DagSlice.class);
        model = mock(ColumnModel.class);
        SUT = new IncompatibleSliceException(slice, model);
    }
    
    @Test
    public void testGetters() {
        Assert.assertEquals(SUT.getColumnModel(), model);
        Assert.assertEquals(SUT.getSlice(), slice);
    }
}
