
package org.openrefine.model;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RowMapperTests {

    Row row;

    @BeforeMethod
    public void setUp() {
        row = Mockito.mock(Row.class);
    }

    @Test
    public void testIdentity() {
        RowMapper identity = RowMapper.IDENTITY;
        Assert.assertEquals(identity.call(1234L, row), row);
    }

    @Test
    public void testConditionalMapperPositive() {
        RowFilter filter = Mockito.mock(RowFilter.class);
        Row rowAlternate = Mockito.mock(Row.class);
        Mockito.when(filter.filterRow(1234L, row)).thenReturn(true);
        Mockito.when(filter.filterRow(1234L, rowAlternate)).thenReturn(false);

        Row row1 = Mockito.mock(Row.class);
        Row row2 = Mockito.mock(Row.class);
        RowMapper positive = Mockito.mock(RowMapper.class);
        RowMapper negative = Mockito.mock(RowMapper.class);
        Mockito.when(positive.call(1234L, row)).thenReturn(row1);
        Mockito.when(negative.call(1234L, rowAlternate)).thenReturn(row2);

        RowMapper mapper = RowMapper.conditionalMapper(filter, positive, negative);
        Assert.assertEquals(mapper.call(1234L, row), row1);
        Assert.assertEquals(mapper.call(1234L, rowAlternate), row2);
    }
}
