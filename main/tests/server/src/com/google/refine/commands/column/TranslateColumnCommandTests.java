
package com.google.refine.commands.column;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.commands.CommandTestBase;
import com.google.refine.model.AbstractOperation;
import com.google.refine.operations.column.TranslateColumnOperation;
import com.google.refine.translate.TranslationLanguage;

public class TranslateColumnCommandTests extends CommandTestBase {

    private TranslateColumnCommand command;

    @BeforeMethod
    public void setUpCommand() {
        command = new TranslateColumnCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testCreateOperationWithValidParameters() {
        stubValidParameters();

        AbstractOperation operation = command.createOperation(request);

        assertTrue(operation instanceof TranslateColumnOperation);
        TranslateColumnOperation translateOperation = (TranslateColumnOperation) operation;
        assertEquals(translateOperation.getColumnName(), "reviews");
        assertEquals(translateOperation.getLanguage(), TranslationLanguage.SPANISH);
        assertEquals(translateOperation.getBatchSize(), 10);
        assertEquals(translateOperation.getTimeoutMillis(), 1000);
        assertEquals(translateOperation.getMaxRetries(), 3);
    }

    @Test
    public void testCreateOperationRejectsMissingNumericParameter() {
        stubValidParameters();
        when(request.getParameter("batchSize")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> command.createOperation(request));
    }

    @Test
    public void testCreateOperationRejectsNonNumericParameter() {
        stubValidParameters();
        when(request.getParameter("maxRetries")).thenReturn("three");

        assertThrows(IllegalArgumentException.class, () -> command.createOperation(request));
    }

    @Test
    public void testCreateOperationRejectsUnsupportedLanguage() {
        stubValidParameters();
        when(request.getParameter("language")).thenReturn("FRENCH");

        assertThrows(IllegalArgumentException.class, () -> command.createOperation(request).validate());
    }

    @Test
    public void testCreateOperationRejectsInvalidEndpoint() {
        stubValidParameters();
        when(request.getParameter("endpoint")).thenReturn("ftp://example.invalid/chat/completions");

        assertThrows(IllegalArgumentException.class, () -> command.createOperation(request).validate());
    }

    private void stubValidParameters() {
        when(request.getParameter("columnName")).thenReturn("reviews");
        when(request.getParameter("language")).thenReturn("SPANISH");
        when(request.getParameter("endpoint")).thenReturn("https://api.example.com/chat/completions");
        when(request.getParameter("model")).thenReturn("test-model");
        when(request.getParameter("batchSize")).thenReturn("10");
        when(request.getParameter("timeoutMillis")).thenReturn("1000");
        when(request.getParameter("maxRetries")).thenReturn("3");
        when(request.getParameter("apiKey")).thenReturn("test-key");
    }
}
