
package org.openrefine.model;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RowBuilderTests {

    @Test
    public void testBuildRow() {
        Row row = RowBuilder.create(10)
                .withCell(2, new Cell("a", null))
                .withCell(0, new Cell("b", null))
                .build();

        Assert.assertEquals(row.getCells(), Arrays.asList(new Cell("b", null), null, new Cell("a", null)));
        Assert.assertEquals(row.flagged, false);
        Assert.assertEquals(row.starred, false);
    }

    @Test
    public void testFromRow() {
        Row initialRow = new Row(
                Arrays.asList(new Cell("a", null), new Cell("b", null)), true, false);

        Row row = RowBuilder.fromRow(initialRow)
                .withCell(0, new Cell("c", null))
                .withStarred(true)
                .build(3);

        Assert.assertEquals(row.getCells(), Arrays.asList(new Cell("c", null), new Cell("b", null), null));
        Assert.assertEquals(row.flagged, true);
        Assert.assertEquals(row.starred, true);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testAlreadyBuilt() {
        RowBuilder builder = RowBuilder.create(1);
        builder.build();
        builder.build();
    }
}
