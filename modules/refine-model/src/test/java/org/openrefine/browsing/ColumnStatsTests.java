
package org.openrefine.browsing;

import static org.testng.Assert.assertEquals;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.testng.annotations.Test;

import org.openrefine.browsing.columns.ColumnStats;
import org.openrefine.model.Cell;
import org.openrefine.model.recon.Recon;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ColumnStatsTests {

    String jsonSerialization = "{\n" +
            "  \"blanks\" : 1,\n" +
            "  \"booleans\" : 4,\n" +
            "  \"dates\" : 5,\n" +
            "  \"matched\" : 1,\n" +
            "  \"new\" : 1,\n" +
            "  \"nonBlanks\" : 14,\n" +
            "  \"numbers\" : 3,\n" +
            "  \"reconciled\" : 3,\n" +
            "  \"strings\" : 2\n" +
            "} ";

    ColumnStats SUT = new ColumnStats(
            1L,
            2L,
            3L,
            4L,
            5L,
            3L,
            1L,
            1L);

    ColumnStats blank = new ColumnStats(1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    ColumnStats string = new ColumnStats(0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L);
    ColumnStats number = new ColumnStats(0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L);
    ColumnStats booleanStats = new ColumnStats(0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L);
    ColumnStats date = new ColumnStats(0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L);
    ColumnStats matched = new ColumnStats(0L, 1L, 0L, 0L, 0L, 1L, 1L, 0L);
    ColumnStats newElement = new ColumnStats(0L, 1L, 0L, 0L, 0L, 1L, 0L, 1L);
    ColumnStats unmatched = new ColumnStats(0L, 1L, 0L, 0L, 0L, 1L, 0L, 0L);

    @Test
    public void testSerialize() {
        TestUtils.isSerializedTo(SUT, jsonSerialization, ParsingUtilities.defaultWriter);
    }

    @Test
    public void testDeserialize() throws JsonProcessingException {
        assertEquals(ParsingUtilities.mapper.readValue(jsonSerialization, ColumnStats.class), SUT);
    }

    @Test
    public void testWithCell() {
        assertEquals(ColumnStats.ZERO.withCell(null), blank);
        assertEquals(ColumnStats.ZERO.withCell(new Cell("", null)), blank);
        assertEquals(ColumnStats.ZERO.withCell(new Cell("foo", null)), string);
        assertEquals(ColumnStats.ZERO.withCell(new Cell(12.3, null)), number);
        assertEquals(ColumnStats.ZERO.withCell(new Cell(true, null)), booleanStats);
        assertEquals(ColumnStats.ZERO.withCell(new Cell(OffsetDateTime.now(), null)), date);
        Recon matchedRecon = Recon.makeWikidataRecon(1234L).withJudgment(Recon.Judgment.Matched);
        assertEquals(ColumnStats.ZERO.withCell(new Cell("foo", matchedRecon)), matched);
        Recon newRecon = Recon.makeWikidataRecon(1234L).withJudgment(Recon.Judgment.New);
        assertEquals(ColumnStats.ZERO.withCell(new Cell("foo", newRecon)), newElement);
        Recon unmatchedRecon = Recon.makeWikidataRecon(1234L).withJudgment(Recon.Judgment.None);
        assertEquals(ColumnStats.ZERO.withCell(new Cell("foo", unmatchedRecon)), unmatched);
    }

    @Test
    public void testSum() {
        assertEquals(ColumnStats.ZERO.sum(SUT), SUT);
    }
}
