
package org.openrefine.importers;

import java.io.Serializable;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
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
}
