
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.model.Cell;
import org.openrefine.model.Row;
import org.openrefine.model.changes.ColumnChangeByChangeData.Joiner;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ColumnChangeByChangeDataTests {

    public static String changeJson = "{\n" +
            "        \"type\": \"org.openrefine.model.changes.ColumnChangeByChangeData\"," +
            "        \"columnIndex\": 1," +
            "        \"columnName\": \"foo\"," +
            "        \"changeDataId\": \"urls\"\n" +
            "      }";

    @Test
    public void testSerialize() throws JsonParseException, JsonMappingException, IOException {
        Change change = ParsingUtilities.mapper.readValue(changeJson, Change.class);
        TestUtils.equalAsJson(changeJson, ParsingUtilities.mapper.writeValueAsString(change));
    }

    @Test
    public void testJoinerAdd() {
        Joiner joiner = new Joiner(2, true);
        Row row = joiner.call(4, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null))), new Cell(3, null));
        Assert.assertEquals(row, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null), new Cell(3, null))));
    }

    @Test
    public void testJoinerReplace() {
        Joiner joiner = new Joiner(0, false);
        Row row = joiner.call(4, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null))), new Cell(3, null));
        Assert.assertEquals(row, new Row(Arrays.asList(new Cell(3, null), new Cell(2, null))));
    }

    @Test
    public void testJoinerReplaceNull() {
        // this behaviour is important to make sure reconciling only a subset of a column does not blank out
        // the cells outside the subset
        Joiner joiner = new Joiner(0, false);
        Row row = joiner.call(4, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null))), null);
        Assert.assertEquals(row, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null))));
    }
}
