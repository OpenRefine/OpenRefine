
package org.openrefine.importers;

import java.io.Serializable;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import org.openrefine.model.Grid;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;

public class LineBasedImporterTests extends ImporterTest {

    LineBasedImporterBase SUT;

    @BeforeMethod
    public void setUpImporter() {
        SUT = new LineBasedImporter();
    }

    @AfterMethod
    public void tearDownImporter() {
        SUT = null;
    }

    @Test
    public void testLineBasedImporter() throws Exception {
        String contents = ""
                + "foo\n"
                + "bar\n"
                + "baz";

        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        Grid parsed = parseOneString(SUT, contents, options);

        Grid expected = createGrid(new String[] { "Column 1" },
                new Serializable[][] {
                        { "foo" },
                        { "bar" },
                        { "baz" }
                });

        assertGridEquals(parsed, expected);
    }

    @Test
    public void testLinesPerRow() throws Exception {
        String contents = ""
                + "a\n"
                + "b\n"
                + "c\n"
                + "d\n"
                + "e\n"
                + "f\n";

        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(options, "linesPerRow", 2);

        Grid parsed = parseOneString(SUT, contents, options);

        Grid expected = createGrid(new String[] { "Column 1", "Column 2" },
                new Serializable[][] {
                        { "a", "b" },
                        { "c", "d" },
                        { "e", "f" }
                });

        assertGridEquals(parsed, expected);
    }

    @Test()
    public void readSimpleData_1Header_1Row() throws Exception {
        String contents = "col1\ndata1";

        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        options.put("headerLines", 1);
        Grid parsed = parseOneString(SUT, contents, options);

        Grid expected = createGrid(new String[] { "col1" },
                new Serializable[][] {
                        { "data1" }
                });

        assertGridEquals(parsed, expected);
    }

    @Test()
    public void readMixedLineData() throws Exception {
        String contents = "data1\r\ndata2\ndata3\rdata4";

        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        Grid parsed = parseOneString(SUT, contents, options);

        Grid expected = createGrid(new String[] { "Column 1" },
                new Serializable[][] {
                        { "data1" },
                        { "data2" },
                        { "data3" },
                        { "data4" }
                });

        assertGridEquals(parsed, expected);
    }

    // Antonin, 2023-03: test disabled since the functionality is not supported in 4.0
    // TODO: re-implement it
    @Test(dataProvider = "LineBasedImporter-Separators", enabled = false)
    public void readLineData(String pattern, String sep) throws Exception {
        String input = "dataa,datab,datac,datad".replace(",", sep);

        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        options.put("separator", pattern);
        Grid parsed = parseOneString(SUT, input, options);

        Grid expected = createGrid(new String[] { "Column 1" },
                new Serializable[][] {
                        { "dataa" },
                        { "datab" },
                        { "datac" },
                        { "datad" }
                });

        assertGridEquals(parsed, expected);
    }

    @DataProvider(name = "LineBasedImporter-Separators")
    public Object[][] LineBasedImporter_Separators() {
        return new Object[][] {
                { "\\r?\\n", "\n" }, { "\\\\*%%\\\\*", "*%%*" }, { ",", "," }, { "[0-9]", "1" }
        };
    }
}
