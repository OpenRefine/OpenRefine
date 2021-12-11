
package org.openrefine.importers;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.TestingDatamodelRunner;
import org.openrefine.util.ParsingUtilities;

public class OdsImporterTests extends ImporterTest {

    // System Under Test
    OdsImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        DatamodelRunner runner = new TestingDatamodelRunner();
        SUT = new OdsImporter(runner);
    }

    @Test
    public void readMultiSheetOds() throws Exception {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 3, selected: true}"));
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 1\", fileNameAndSheetIndex: \"file-source#1\", rows: 3, selected: true}"));
        options.set("sheets", sheets);

        options.put("ignoreLines", 0);
        options.put("headerLines", 1);
        options.put("skipDataLines", 0);
        options.put("limit", -1);
        options.put("storeBlankCellsAsNulls", true);

        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("importers/sample.ods");

        GridState grid = parseOneFile(SUT, stream);

        List<Row> rows = grid.collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());
        Assert.assertEquals(rows.size(), 6);
        Assert.assertEquals(rows.get(1).cells.size(), 5);
        Assert.assertEquals(rows.get(0).getCellValue(0), "c");
        Assert.assertEquals(rows.get(3).getCellValue(0), 3.0);
    }

    @Test
    public void readOdsDataTypes() throws Exception {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        options.set("sheets", sheets);

        options.put("ignoreLines", 0);
        options.put("headerLines", 1);
        options.put("skipDataLines", 0);
        options.put("limit", 5);
        options.put("storeBlankCellsAsNulls", true);

        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("importers/films.ods");
        GridState grid = parseOneFile(SUT, stream);

        List<Row> rows = grid.collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());
        assertEquals(rows.size(), 5);
        Row row = rows.get(0);
        assertEquals(row.cells.size(), 9);
        assertEquals((String) row.getCellValue(1), "2 Days In New York");
        // TODO day not taken into account because of timezone dependency issues
        assertEquals(((OffsetDateTime) row.getCellValue(3)).toString().substring(0, 7), "2012-03");
        assertEquals(((Number) row.getCellValue(5)).doubleValue(), 4.5, 0.0000001);

        assertFalse((Boolean) row.getCellValue(7));
        assertTrue((Boolean) rows.get(1).getCellValue(7));

        assertNull((String) rows.get(2).getCellValue(2));
    }
}
