
package org.openrefine.operations.cell;

import static org.mockito.Mockito.mock;

import java.io.Serializable;

import org.openrefine.RefineTest;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TransposeColumnsIntoRowsTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        OperationRegistry.registerOperation("core", "transpose-columns-into-rows", TransposeColumnsIntoRowsOperation.class);
    }

    @Test
    public void serializeTransposeColumnsIntoRowsTestsFixedLength() throws Exception {
        String json = "{" +
                "  \"columnCount\" : 2," +
                "  \"combinedColumnName\" : \"b\"," +
                "  \"description\" : \"Transpose cells in 2 column(s) starting with b 1 into rows in one new column named b\"," +
                "  \"fillDown\" : false," +
                "  \"ignoreBlankCells\" : true," +
                "  \"keyColumnName\" : null," +
                "  \"op\" : \"core/transpose-columns-into-rows\"," +
                "  \"prependColumnName\" : false," +
                "  \"separator\" : null," +
                "  \"startColumnName\" : \"b 1\"," +
                "  \"valueColumnName\" : null" +
                "}";
        TestUtils.isSerializedTo(new TransposeColumnsIntoRowsOperation(
                "b 1", 2, true, false, "b", false, null), json, ParsingUtilities.defaultWriter);
    }

    /**
     * This shows how the transpose columns into rows operation can, in certain cases, be an inverse to the transpose
     * rows into columns operation.
     */
    @Test
    public void testTransposeBackToRecords() throws OperationException, ParsingException {
        Grid initialRecords = createGrid(
                new String[] { "a", "b 1", "b 2", "c" },
                new Serializable[][] {
                        { "1", "2", "5", "3" },
                        { "7", "8", "11", "9" }
                });

        Operation op = new TransposeColumnsIntoRowsOperation(
                "b 1", 2, true, false, "b", false, null);

        Grid expected = createGrid(
                new String[] { "a", "b", "c" },
                new Serializable[][] {
                        { "1", "2", "3" },
                        { null, "5", null },
                        { "7", "8", "9" },
                        { null, "11", null }
                });

        assertGridEquals(op.apply(initialRecords, mock(ChangeContext.class)).getGrid(), expected);
    }

    @Test
    public void testTransposeBackToRecordsNoLimit() throws OperationException, ParsingException {
        Grid initialRecords = createGrid(
                new String[] { "a", "b 1", "b 2", "c" },
                new Serializable[][] {
                        { "1", "2", "5", "3" },
                        { "7", "8", "11", "9" }
                });

        Operation op = new TransposeColumnsIntoRowsOperation(
                "b 1", 0, true, false, "b", false, null);

        Grid expected = createGrid(
                new String[] { "a", "b" },
                new Serializable[][] {
                        { "1", "2" },
                        { null, "5" },
                        { null, "3" },
                        { "7", "8", },
                        { null, "11" },
                        { null, "9" }
                });

        assertGridEquals(op.apply(initialRecords, mock(ChangeContext.class)).getGrid(), expected);
    }

    @Test
    public void testTransposeBackToRecordsKeyValue() throws OperationException, ParsingException {
        Grid initialRecords = createGrid(
                new String[] { "a", "b 1", "b 2", "c" },
                new Serializable[][] {
                        { "1", "2", "5", "3" },
                        { "7", "8", "11", "9" }
                });

        Operation op = new TransposeColumnsIntoRowsOperation(
                "b 1", 2, true, false, "key", "value");

        Grid expected = createGrid(
                new String[] { "a", "key", "value", "c" },
                new Serializable[][] {
                        { "1", "b 1", "2", "3" },
                        { null, "b 2", "5", null },
                        { "7", "b 1", "8", "9" },
                        { null, "b 2", "11", null }
                });

        assertGridEquals(op.apply(initialRecords, mock(ChangeContext.class)).getGrid(), expected);
    }

    @Test
    public void testRemoveRowForNullOrEmptyCell() throws Exception {
        Grid initialRecords = createGrid(
                new String[] { "num1", "num2" },
                new Serializable[][] {
                        { "2", "3" },
                        { "6", "" },
                        { "5", "9" }
                });

        Operation op = new TransposeColumnsIntoRowsOperation("num1", -1, true, false, "a", true, ":");

        Grid expected = createGrid(
                new String[] { "a" },
                new Serializable[][] {
                        { "num1:2" },
                        { "num2:3" },
                        { "num1:6" },
                        { "num1:5" },
                        { "num2:9" }
                });

        assertGridEquals(op.apply(initialRecords, mock(ChangeContext.class)).getGrid(), expected);
    }

}
