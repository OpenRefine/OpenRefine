
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
        assertEquals(translateOperation.getSourceLanguage(), TranslationLanguage.ENGLISH);
        assertEquals(translateOperation.getTargetLanguage(), TranslationLanguage.SPANISH);
        assertEquals(translateOperation.getTargetColumnName(), "reviews_spanish");
        assertEquals(translateOperation.getEndpoint(), "http://localhost:5000/translate");
        assertEquals(translateOperation.getTimeoutMillis(), 1000);
        assertEquals(translateOperation.getMaxRetries(), 3);
    }

    @Test
    public void testCreateOperationRejectsMissingLanguage() {
        stubValidParameters();
        when(request.getParameter("sourceLanguage")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> command.createOperation(request));
    }

    @Test
    public void testCreateOperationRejectsUnsupportedLanguage() {
        stubValidParameters();
        when(request.getParameter("targetLanguage")).thenReturn("FRENCH");

        assertThrows(IllegalArgumentException.class, () -> command.createOperation(request).validate());
    }

    @Test
    public void testCreateOperationRejectsSameSourceAndTargetLanguage() {
        stubValidParameters();
        when(request.getParameter("targetLanguage")).thenReturn("ENGLISH");

        assertThrows(IllegalArgumentException.class, () -> command.createOperation(request).validate());
    }

    @Test
    public void testCreateOperationRejectsMissingTargetColumnName() {
        stubValidParameters();
        when(request.getParameter("targetColumnName")).thenReturn("");

        assertThrows(IllegalArgumentException.class, () -> command.createOperation(request).validate());
    }

    @Test
    public void testCreateOperationRejectsInvalidEndpoint() {
        stubValidParameters();
        when(request.getParameter("endpoint")).thenReturn("ftp://example.invalid/translate");

        assertThrows(IllegalArgumentException.class, () -> command.createOperation(request).validate());
    }

    @Test
    public void testCreateOperationRejectsMissingNumericParameter() {
        stubValidParameters();
        when(request.getParameter("timeoutMillis")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> command.createOperation(request));
    }

    @Test
    public void testCreateOperationRejectsNonNumericParameter() {
        stubValidParameters();
        when(request.getParameter("maxRetries")).thenReturn("three");

        assertThrows(IllegalArgumentException.class, () -> command.createOperation(request));
    }

    private void stubValidParameters() {
        when(request.getParameter("columnName")).thenReturn("reviews");
        when(request.getParameter("sourceLanguage")).thenReturn("ENGLISH");
        when(request.getParameter("targetLanguage")).thenReturn("SPANISH");
        when(request.getParameter("targetColumnName")).thenReturn("reviews_spanish");
        when(request.getParameter("endpoint")).thenReturn("http://localhost:5000/translate");
        when(request.getParameter("timeoutMillis")).thenReturn("1000");
        when(request.getParameter("maxRetries")).thenReturn("3");
    }
}
