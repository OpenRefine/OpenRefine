
package com.google.refine.operations.row;

import static com.google.refine.operations.OperationDescription.row_keep_matching_brief;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.DecoratedValue;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.browsing.facets.ListFacet.ListFacetConfig;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.functions.FacetCount;
import com.google.refine.grel.Function;
import com.google.refine.grel.Parser;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Cell;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class RowKeepMatchedOperationTest extends RefineTest {

    static final String ENGINE_JSON_DUPLICATES = "{\"facets\":[{\"type\":\"list\",\"name\":\"facet A\",\"columnName\":\"Column A\",\"expression\":\"facetCount(value, 'value', 'Column A') > 1\",\"omitBlank\":false,\"omitError\":false,\"selection\":[{\"v\":{\"v\":true,\"l\":\"true\"}}],\"selectBlank\":false,\"selectError\":false,\"invert\":false}],\"mode\":\"row-based\"}}";

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "row-keep-matched", RowKeepMatchedOperation.class);
    }

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    Project projectIssue567;
    Properties options;
    EngineConfig engine_config;
    Engine engine;
    Properties bindings;

    Project project;
    ListFacetConfig facet;

    @Test
    public void serializeRowKeepMatchedOperation() throws IOException {
        String json = "{"
                + "\"op\":\"core/row-keep-matched\","
                + "\"engineConfig\":{\"facets\":[],\"mode\":\"row-based\"},"
                + "\"description\":\"" + row_keep_matching_brief() + "\""
                + "}";

        RowKeepMatchedOperation operation = ParsingUtilities.mapper.readValue(json, RowKeepMatchedOperation.class);
        String serialized = ParsingUtilities.mapper.writeValueAsString(operation);

        TestUtils.isSerializedTo(operation, json);
    }

    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        projectIssue567 = createProjectWithColumns("RowKeepMatchedOperationTest", "Column A");
        project = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "", "i" }
                });

        engine = new Engine(projectIssue567);
        engine_config = EngineConfig.deserialize(ENGINE_JSON_DUPLICATES);
        engine.initializeFromConfig(engine_config);
        engine.setMode(Engine.Mode.RowBased);

        bindings = new Properties();
        bindings.put("project", projectIssue567);

        facet = new ListFacetConfig();
        facet.name = "hello";
        facet.expression = "grel:value";
        facet.columnName = "hello";
    }

    @AfterMethod
    public void TearDown() {
        MetaParser.unregisterLanguageParser("grel");
        projectIssue567 = null;
        engine = null;
        bindings = null;
    }

    private void checkRowCounts(int all, int filtered) {
        engine.getAllRows().accept(projectIssue567, new CountVerificationRowVisitor(all));
        engine.getAllFilteredRows().accept(projectIssue567, new CountVerificationRowVisitor(filtered));

        Function fc = new FacetCount();
        Integer count = (Integer) fc.call(bindings, new Object[] { "a", "value", "Column A" });
        Assert.assertEquals(count.intValue(), filtered);
    }

    @Test
    public void testIssue567() throws Exception {
        for (int i = 0; i < 5; i++) {
            Row row = new Row(5);
            row.setCell(0, new Cell(i < 4 ? "a" : "b", null));
            projectIssue567.rows.add(row);
        }
        checkRowCounts(5, 4);

        EngineDependentOperation op = new RowKeepMatchedOperation(engine_config);
        HistoryEntry historyEntry = op.createProcess(projectIssue567, options).performImmediate();
        // After keeping matched rows, we should have 4 rows (the ones with "a")
        checkRowCounts(4, 4);

        historyEntry.revert(projectIssue567);
        checkRowCounts(5, 4);
    }

    class CountVerificationRowVisitor implements RowVisitor {

        private int count = 0;
        private int target;

        private CountVerificationRowVisitor(int targetCount) {
            target = targetCount;
        }

        @Override
        public boolean visit(Project project, int rowIndex, Row row) {
            count++;
            return false;
        }

        @Override
        public void start(Project project) {
            count = 0;
        }

        @Override
        public void end(Project project) {
            Assert.assertEquals(count, target);
        }
    }

    @Test
    public void testKeepMatchedRows() throws Exception {
        facet.selection = Arrays.asList(
                new DecoratedValue("h", "h"),
                new DecoratedValue("i", "i"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RowBased);
        RowKeepMatchedOperation operation = new RowKeepMatchedOperation(engineConfig);
        runOperation(operation, project);

        Project expected = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { null, "g", "h" },
                        { null, "", "i" }
                });

        assertProjectEquals(project, expected);
    }

    @Test
    public void testKeepMatchedRecords() throws Exception {
        facet.selection = Arrays.asList(
                new DecoratedValue("h", "h"),
                new DecoratedValue("i", "i"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RecordBased);
        RowKeepMatchedOperation operation = new RowKeepMatchedOperation(engineConfig);

        runOperation(operation, project);

        Project expected = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "", "i" }
                });

        assertProjectEquals(project, expected);
    }
}
