
package com.google.refine.commands.column;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OnError;
import com.google.refine.operations.column.ColumnAdditionOperation;

public class AddColumnCommandTests {

    private AddColumnCommand command;
    private HttpServletRequest request;
    private Project project;
    private EngineConfig engineConfig;

    @BeforeMethod
    public void setUp() {
        command = new AddColumnCommand();
        request = mock(HttpServletRequest.class);
        project = new Project();
        engineConfig = new EngineConfig(Collections.emptyList(), Engine.Mode.RowBased);
    }

    @Test
    public void testCreateOperationWithValidParameters() throws Exception {
        when(request.getParameter("baseColumnName")).thenReturn("originalCol");
        when(request.getParameter("expression")).thenReturn("value + 1");
        when(request.getParameter("newColumnName")).thenReturn("newCol");
        when(request.getParameter("columnInsertIndex")).thenReturn("2");
        when(request.getParameter("onError")).thenReturn("set-to-blank");

        AbstractOperation op = command.createOperation(project, request, engineConfig);

        assertNotNull(op);
        assertTrue(op instanceof ColumnAdditionOperation);

        ColumnAdditionOperation columnOp = (ColumnAdditionOperation) op;
        assertEquals(columnOp.getNewColumnName(), "newCol");
        assertEquals(columnOp.getBaseColumnName(), "originalCol");
        assertEquals(columnOp.getExpression(), "value + 1");
        assertEquals(columnOp.getColumnInsertIndex(), 2);
        assertEquals(columnOp.getOnError(), OnError.SetToBlank);
    }
}
