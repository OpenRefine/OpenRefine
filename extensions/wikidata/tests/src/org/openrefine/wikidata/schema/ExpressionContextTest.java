package org.openrefine.wikidata.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;

public class ExpressionContextTest extends RefineTest {
    
    Project project = null;
    
    @BeforeMethod
    public void setUp() {
        project = createCSVProject("a,b\nc\nd,e");
    }
    
    @Test
    public void testGetCellByColumnName() {
        ExpressionContext ctxt = new ExpressionContext("foo:", 1, project.rows.get(1), project.columnModel, null);
        assertEquals("e", ctxt.getCellByName("b").value);
    }
    
    @Test
    public void testNonExistentColumn() {
        ExpressionContext ctxt = new ExpressionContext("foo:", 1, project.rows.get(1), project.columnModel, null);
        assertNull(ctxt.getCellByName("auie"));
    }
    
    @Test
    public void testGetRowId() {
        ExpressionContext ctxt = new ExpressionContext("foo:", 1, project.rows.get(1), project.columnModel, null);
        assertEquals(1, ctxt.getRowId());
    }
}
