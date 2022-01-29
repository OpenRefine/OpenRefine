
package com.google.refine.extension.gdata;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendDimensionRequest;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.refine.exporters.TabularSerializer.CellData;

public class SpreadsheetSerializerTests {

    private class SpreadsheetSerializerStub extends SpreadsheetSerializer {

        SpreadsheetSerializerStub(Sheets service, String spreadsheetId, List<Exception> exceptions) {
            super(service, spreadsheetId, exceptions);
        }

        protected List<RowData> getRows() {
            return rows;
        }

    }

    // dependencies
    StringWriter writer;
    JsonNode options = null;
    Sheets service;
    List<Exception> exceptions = new ArrayList<>();

    // System Under Test
    SpreadsheetSerializerStub SUT;

    @BeforeMethod
    public void SetUp() {
        service = mock(Sheets.class);
        SUT = new SpreadsheetSerializerStub(service, "spreadsheet1", exceptions);
        writer = new StringWriter();
    }

    @AfterMethod
    public void TearDown() {
        SUT = null;
        service = null;
        exceptions.clear();
        writer = null;
        options = null;
    }

    @Test
    public void test30columns() {
        SUT.startFile(options); // options is null, but unused
        List<CellData> cells = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            String colnum = Integer.toString(i);
            CellData cell = new CellData("col" + colnum, "text" + colnum, "text" + colnum, null);
            cells.add(cell);
        }
        SUT.addRow(cells, true);
        SUT.addRow(cells, false);

        List<Request> requests = SUT.prepareBatch(SUT.getRows());
        assertEquals(requests.size(), 2);
        for (Request request : requests) {
            if (request.getAppendDimension() instanceof AppendDimensionRequest) {
                return;
            }
        }
        fail("Failed to find AppendDimensionRequest for columns > 26");
    }

    @Test
    public void testDataTypes() {
        SUT.startFile(options); // options is null, but unused
        List<CellData> row = new ArrayList<>();
        row.add(new CellData("null value", null, "null value", null));
        row.add(new CellData("string value", "a string", "a string as string", null));
        row.add(new CellData("integer value", 42, "42", null));
        row.add(new CellData("double value", new Double(42), "42.0", null));
        row.add(new CellData("boolean value", true, "true", null));
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z"));
        row.add(new CellData("datetime value", now, now.toString(), null));

        SUT.addRow(row, false);

        List<Request> requests = SUT.prepareBatch(SUT.getRows());
        assertEquals(requests.size(), 1);
        List<RowData> rows = requests.get(0).getAppendCells().getRows();
        assertEquals(rows.size(), 1);
        List<com.google.api.services.sheets.v4.model.CellData> values = rows.get(0).getValues();
        assertEquals(values.size(), 6);
        ExtendedValue value = values.get(0).getUserEnteredValue();
        assertEquals(value.getStringValue(), "");
        value = values.get(1).getUserEnteredValue();
        assertEquals(value.getStringValue(), "a string");
        value = values.get(2).getUserEnteredValue();
        assertEquals(value.getNumberValue(), new Double(42));
        value = values.get(3).getUserEnteredValue();
        assertEquals(value.getNumberValue(), new Double(42));
        value = values.get(4).getUserEnteredValue();
        assertEquals(value.getBoolValue(), Boolean.TRUE);
        value = values.get(5).getUserEnteredValue();
        assertEquals(value.getStringValue(), now.toString());

    }
}
