
package org.openrefine.model.changes;

import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.Collections;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.Evaluable;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;

public class MassEditChangeTests extends RefineTest {

    private GridState initialState;
    private static EngineConfig engineConfig = mock(EngineConfig.class);
    private static Evaluable eval = new Evaluable() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object evaluate(Properties bindings) {
            return bindings.get("value");
        }

    };

    @BeforeTest
    public void setUpInitialState() {
        Project project = createProject("my project", new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "v1", 3 },
                        { "v1", 4 },
                        { "", "4" },
                        { new EvalError("error"), 5 },
                        { null, 6 }
                });
        initialState = project.getCurrentGridState();
    }

    @Test
    public void testSimpleReplace() {
        MassEditChange change = new MassEditChange(engineConfig, eval, "foo", Collections.singletonMap("v1", "v2"), "hey", null);
        GridState applied = change.apply(initialState);
        Row row0 = applied.getRow(0);
        Assert.assertEquals(row0.getCellValue(0), "v2");
        Assert.assertEquals(row0.getCellValue(1), 3);
        Row row1 = applied.getRow(1);
        Assert.assertEquals(row1.getCellValue(0), "v2");
        Assert.assertEquals(row1.getCellValue(1), 4);
        Row row2 = applied.getRow(2);
        Assert.assertEquals(row2.getCellValue(0), "hey");
        Assert.assertEquals(row2.getCellValue(1), "4");
    }
}
