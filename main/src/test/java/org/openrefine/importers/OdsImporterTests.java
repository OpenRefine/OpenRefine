package org.openrefine.importers;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.TestingDatamodelRunner;
import org.openrefine.util.ParsingUtilities;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class OdsImporterTests extends ImporterTest {
    //System Under Test
    OdsImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp(){
        super.setUp();
        DatamodelRunner runner = new TestingDatamodelRunner();
        SUT = new OdsImporter(runner);
    }
    
    @Test
    public void readOds() throws Exception {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper.readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 3, selected: true}"));
        sheets.add(ParsingUtilities.mapper.readTree("{name: \"file-source#Test Sheet 1\", fileNameAndSheetIndex: \"file-source#1\", rows: 3, selected: true}"));
        whenGetArrayOption("sheets", options, sheets);
        
        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 1);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls",options,true);
        
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("importers/sample.ods");
        
        GridState grid = null;
        grid = parseOneFile(SUT, stream);

        List<Row> rows = grid.collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());
        Assert.assertEquals(rows.size(), 6);
        Assert.assertEquals(rows.get(1).cells.size(), 6);
        Assert.assertEquals(rows.get(0).getCellValue(0), "c");
        Assert.assertEquals(rows.get(3).getCellValue(0), 3.0);
    }
}
