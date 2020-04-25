
package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.DecoratedValue;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.ListFacet.ListFacetConfig;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;

public class MassEditChangeTests extends RefineTest {

    private GridState initialState;
    private static EngineConfig engineConfig;
    private ListFacetConfig facet;
    private static Evaluable eval = new Evaluable() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object evaluate(Properties bindings) {
            return bindings.get("value");
        }

        @Override
        public String getSource() {
            return "value";
        }

        @Override
        public String getLanguagePrefix() {
            return "grel";
        }

        @Override
        public Set<String> getColumnDependencies(String baseColumn) {
            return Collections.singleton(baseColumn);
        }

    };

    @BeforeTest
    public void setUpInitialState() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        Project project = createProject("my project", new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "v1", "a" },
                        { "v3", "a" },
                        { "", "a" },
                        { "", "b" },
                        { new EvalError("error"), "a" },
                        { "v1", "b" }
                });
        initialState = project.getCurrentGridState();
        facet = new ListFacetConfig();
        facet.columnName = "bar";
        facet.setExpression("grel:value");
        facet.selection = Collections.singletonList(new DecoratedValue("a", "a"));
        engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RowBased);
    }

    @Test
    public void testSimpleReplace() {
        MassEditChange change = new MassEditChange(engineConfig, eval, "foo", Collections.singletonMap("v1", "v2"), "hey", null);
        GridState applied = change.apply(initialState);
        Row row0 = applied.getRow(0);
        Assert.assertEquals(row0.getCellValue(0), "v2");
        Assert.assertEquals(row0.getCellValue(1), "a");
        Row row1 = applied.getRow(1);
        Assert.assertEquals(row1.getCellValue(0), "v3");
        Assert.assertEquals(row1.getCellValue(1), "a");
        Row row2 = applied.getRow(2);
        Assert.assertEquals(row2.getCellValue(0), "hey");
        Assert.assertEquals(row2.getCellValue(1), "a");
        Row row3 = applied.getRow(3);
        Assert.assertEquals(row3.getCellValue(0), "");
        Assert.assertEquals(row3.getCellValue(1), "b");
        Row row4 = applied.getRow(5);
        Assert.assertEquals(row4.getCellValue(0), "v1");
        Assert.assertEquals(row4.getCellValue(1), "b");
    }

    @Test
    public void testRecordsMode() {
        engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RecordBased);
        MassEditChange change = new MassEditChange(engineConfig, eval, "foo", Collections.singletonMap("v1", "v2"), "hey", null);
        GridState applied = change.apply(initialState);
        Row row0 = applied.getRow(0);
        Assert.assertEquals(row0.getCellValue(0), "v2");
        Assert.assertEquals(row0.getCellValue(1), "a");
        Row row1 = applied.getRow(1);
        Assert.assertEquals(row1.getCellValue(0), "v3");
        Assert.assertEquals(row1.getCellValue(1), "a");
        Row row2 = applied.getRow(2);
        Assert.assertEquals(row2.getCellValue(0), "hey");
        Assert.assertEquals(row2.getCellValue(1), "a");
        Row row3 = applied.getRow(3);
        Assert.assertEquals(row3.getCellValue(0), "hey");
        Assert.assertEquals(row3.getCellValue(1), "b");
        Row row4 = applied.getRow(5);
        Assert.assertEquals(row4.getCellValue(0), "v1");
        Assert.assertEquals(row4.getCellValue(1), "b");
    }
}
