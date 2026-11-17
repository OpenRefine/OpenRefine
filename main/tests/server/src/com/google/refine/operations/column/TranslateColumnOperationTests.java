
package com.google.refine.operations.column;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;
import com.google.refine.process.Process;
import com.google.refine.translate.TranslationException;
import com.google.refine.translate.TranslationLanguage;
import com.google.refine.translate.TranslationService;
import com.google.refine.util.ParsingUtilities;

public class TranslateColumnOperationTests extends RefineTest {

    private static final String NEW_COLUMN = "review (Spanish)";
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private Project project;

    @BeforeMethod
    public void createTranslateProject() {
        project = createProject(
                new String[] { "review", "other" },
                new Serializable[][] {
                        { "one", "keep 1" },
                        { "", "keep 2" },
                        { "two", "keep 3" },
                        { new EvalError("existing error"), "keep 4" },
                        { "three", "keep 5" }
                });
    }

    @Test
    public void testValidate() {
        assertThrows(IllegalArgumentException.class,
                () -> newOperation("review", NEW_COLUMN, null, ENDPOINT, 10, 1000, 3, "key").validate());
        assertThrows(IllegalArgumentException.class,
                () -> newOperation(null, NEW_COLUMN, TranslationLanguage.SPANISH, ENDPOINT, 10, 1000, 3, "key").validate());
        assertThrows(IllegalArgumentException.class,
                () -> newOperation("review", null, TranslationLanguage.SPANISH, ENDPOINT, 10, 1000, 3, "key").validate());
        assertThrows(IllegalArgumentException.class,
                () -> newOperation("review", "  ", TranslationLanguage.SPANISH, ENDPOINT, 10, 1000, 3, "key").validate());
        assertThrows(IllegalArgumentException.class,
                () -> newOperation("review", NEW_COLUMN, TranslationLanguage.SPANISH, ENDPOINT, 10, 1000, 3, "").validate());
        assertThrows(IllegalArgumentException.class,
                () -> newOperation("review", NEW_COLUMN, TranslationLanguage.SPANISH, ENDPOINT, 21, 1000, 3, "key").validate());
        assertThrows(IllegalArgumentException.class,
                () -> newOperation("review", NEW_COLUMN, TranslationLanguage.SPANISH, ENDPOINT, 10, 0, 3, "key").validate());
        assertThrows(IllegalArgumentException.class,
                () -> newOperation("review", NEW_COLUMN, TranslationLanguage.SPANISH, ENDPOINT, 10, 1000, 6, "key").validate());
        assertThrows(IllegalArgumentException.class,
                () -> newOperation("review", NEW_COLUMN, TranslationLanguage.SPANISH,
                        "ftp://example.invalid/chat/completions", 10, 1000, 3, "key").validate());
        newOperation("review", NEW_COLUMN, TranslationLanguage.SPANISH, ENDPOINT, 10, 1000, 3, "key").validate();
    }

    @Test
    public void testTranslationsGoToNewColumnRightOfSource() throws Exception {
        runOperation(withService(new CountingTranslationService(), 20, NEW_COLUMN), project, 1000);

        assertEquals(columnNames(), List.of("review", NEW_COLUMN, "other"));
        assertEquals(newColumnValue(0), "ES: one");
        assertNull(newColumnValue(1));
        assertEquals(newColumnValue(2), "ES: two");
        assertNull(newColumnValue(3));
        assertEquals(newColumnValue(4), "ES: three");
        assertEquals(reviewValue(0), "one");
        assertEquals(reviewValue(2), "two");
        assertEquals(reviewValue(4), "three");
        assertEquals(project.rows.get(0).getCellValue(cellIndex("other")), "keep 1");
    }

    @Test
    public void testNewColumnIsInsertedRightOfSourceWhenSourceIsNotFirst() throws Exception {
        project = createProject(
                new String[] { "id", "review", "other" },
                new Serializable[][] {
                        { "1", "one", "keep 1" },
                        { "2", "two", "keep 2" }
                });

        runOperation(withService(new CountingTranslationService(), 20, NEW_COLUMN), project, 1000);

        assertEquals(columnNames(), List.of("id", "review", NEW_COLUMN, "other"));
        assertEquals(newColumnValue(1), "ES: two");
    }

    @Test
    public void testColumnDependenciesAndDiff() {
        AbstractOperation operation = newOperation("review", NEW_COLUMN, TranslationLanguage.SPANISH, ENDPOINT, 10, 1000, 3,
                "key");

        assertEquals(operation.getColumnDependencies().get(), Set.of("review"));
        assertEquals(operation.getColumnsDiff().get(), ColumnsDiff.builder().addColumn(NEW_COLUMN, "review").build());
    }

    @Test
    public void testFailedBatchStoresErrorInNewColumnAndUndoRemovesColumn() throws Exception {
        runOperation(withService(new PartialFailureTranslationService(), 1, NEW_COLUMN), project, 1000);

        assertEquals(newColumnValue(0), "ES: one");
        assertEquals(((EvalError) newColumnValue(2)).message, "Translation failed: review 67 failed");
        assertEquals(newColumnValue(4), "ES: three");
        assertEquals(reviewValue(2), "two");
        assertEquals(project.history.getLastPastEntries(10).size(), 1);
        long historyEntryId = project.history.getLastPastEntries(1).get(0).id;

        project.history.undoRedo(0);
        assertEquals(columnNames(), List.of("review", "other"));
        assertEquals(reviewValue(0), "one");

        project.history.undoRedo(historyEntryId);
        assertEquals(columnNames(), List.of("review", NEW_COLUMN, "other"));
        assertEquals(newColumnValue(0), "ES: one");
    }

    @Test
    public void testBlankAndErrorCellsAreNotTranslated() throws Exception {
        CountingTranslationService service = new CountingTranslationService();

        runOperation(withService(service, 20, NEW_COLUMN), project, 1000);

        assertEquals(service.calls, List.of(List.of("one", "two", "three")));
        assertNull(newColumnValue(1));
        assertNull(newColumnValue(3));
        assertEquals(((EvalError) reviewValue(3)).message, "existing error");
    }

    @Test
    public void testColumnWithNothingToTranslateAddsEmptyColumnWithoutCallingService() throws Exception {
        project = createProject(
                new String[] { "review", "other" },
                new Serializable[][] {
                        { "", "keep 1" },
                        { null, "keep 2" }
                });
        CountingTranslationService service = new CountingTranslationService();

        runOperation(withService(service, 20, NEW_COLUMN), project, 1000);

        assertTrue(service.calls.isEmpty());
        assertEquals(columnNames(), List.of("review", NEW_COLUMN, "other"));
        assertNull(newColumnValue(0));
        assertNull(newColumnValue(1));
    }

    @Test
    public void testNumericCellsAreSentAsText() throws Exception {
        project = createProject(
                new String[] { "review", "other" },
                new Serializable[][] {
                        { 123, "keep 1" },
                        { 45.67, "keep 2" }
                });
        CountingTranslationService service = new CountingTranslationService();

        runOperation(withService(service, 20, NEW_COLUMN), project, 1000);

        assertEquals(service.calls.get(0), List.of("123", "45.67"));
        assertEquals(reviewValue(0), 123);
    }

    @Test
    public void testMissingColumnFailsProcessWithoutHistory() throws Exception {
        AbstractOperation operation = withService(new CountingTranslationService(), 20, NEW_COLUMN);
        project.columnModel.removeColumnByCellIndex(cellIndex("review"));
        project.columnModel.update();

        runProcess(operation);

        assertTrue(project.history.getLastPastEntries(1).isEmpty());
    }

    @Test
    public void testExistingNewColumnNameFailsProcessWithoutTranslating() throws Exception {
        CountingTranslationService service = new CountingTranslationService();

        runProcess(withService(service, 20, "other"));

        assertTrue(service.calls.isEmpty());
        assertTrue(project.history.getLastPastEntries(1).isEmpty());
        assertEquals(columnNames(), List.of("review", "other"));
    }

    @Test
    public void testCancellationRetainsCompletedBatches() throws Exception {
        CancellationTranslationService service = new CancellationTranslationService();
        Process process = withService(service, 1, NEW_COLUMN).createProcess(project, new Properties());

        process.startPerforming(project.getProcessManager());
        assertTrue(service.firstCallCompleted.await(5, TimeUnit.SECONDS));
        assertTrue(service.secondCallStarted.await(5, TimeUnit.SECONDS));
        process.cancel();
        service.releaseSecondCall.countDown();
        waitForProcess(process);

        assertEquals(service.calls.size(), 2);
        assertEquals(newColumnValue(0), "ES: one");
        assertEquals(newColumnValue(2), "ES: two");
        assertNull(newColumnValue(4));
        assertEquals(reviewValue(4), "three");
        assertEquals(project.history.getLastPastEntries(10).size(), 1);
    }

    @Test
    public void testSerializationIncludesNewColumnNameButNotApiKey() throws Exception {
        String serialized = ParsingUtilities.mapper.writeValueAsString(
                newOperation("review", NEW_COLUMN, TranslationLanguage.SPANISH, ENDPOINT, 10, 1000, 3, "secret-key"));

        assertFalse(serialized.contains("secret-key"));
        assertTrue(serialized.contains("\"newColumnName\":\"review (Spanish)\""));
        assertTrue(serialized.contains("\"batchSize\":10"));
    }

    @Test
    public void testRenameColumns() {
        AbstractOperation operation = newOperation("review", NEW_COLUMN, TranslationLanguage.SPANISH, ENDPOINT, 10, 1000, 3,
                "key");

        TranslateColumnOperation renamed = (TranslateColumnOperation) operation.renameColumns(
                Map.of("review", "feedback", NEW_COLUMN, "feedback (Spanish)"));

        assertEquals(renamed.getColumnName(), "feedback");
        assertEquals(renamed.getNewColumnName(), "feedback (Spanish)");
        assertEquals(renamed.getColumnDependencies().get(), Set.of("feedback"));
    }

    @Test
    public void testBriefDescription() {
        AbstractOperation operation = newOperation("review", NEW_COLUMN, TranslationLanguage.SPANISH, ENDPOINT, 10, 1000, 3,
                "key");

        assertEquals(
                operation.getJsonDescription(),
                OperationDescription.column_translation_brief("review", "Spanish", NEW_COLUMN));
    }

    private TranslateColumnOperation newOperation(
            String columnName,
            String newColumnName,
            TranslationLanguage language,
            String endpoint,
            int batchSize,
            int timeoutMillis,
            int maxRetries,
            String apiKey) {
        return new TranslateColumnOperation(
                columnName,
                newColumnName,
                language,
                endpoint,
                "test-model",
                batchSize,
                timeoutMillis,
                maxRetries,
                apiKey);
    }

    private TranslateColumnOperation withService(TranslationService service, int batchSize, String newColumnName) {
        return new TranslateColumnOperation(
                "review",
                newColumnName,
                TranslationLanguage.SPANISH,
                ENDPOINT,
                "test-model",
                batchSize,
                1000,
                3,
                "test-key") {

            @Override
            protected TranslationService createTranslationService() {
                return service;
            }
        };
    }

    private List<String> columnNames() {
        return project.columnModel.columns.stream().map(Column::getName).collect(Collectors.toList());
    }

    private int cellIndex(String columnName) {
        return project.columnModel.getColumnByName(columnName).getCellIndex();
    }

    private Object reviewValue(int rowIndex) {
        return project.rows.get(rowIndex).getCellValue(cellIndex("review"));
    }

    private Object newColumnValue(int rowIndex) {
        return project.rows.get(rowIndex).getCellValue(cellIndex(NEW_COLUMN));
    }

    private void runProcess(AbstractOperation operation) throws Exception {
        Process process = operation.createProcess(project, new Properties());
        process.startPerforming(project.getProcessManager());
        waitForProcess(process);
    }

    private static class CountingTranslationService implements TranslationService {

        final List<List<String>> calls = new java.util.ArrayList<>();

        @Override
        public List<String> translate(List<String> texts, TranslationLanguage language) {
            calls.add(texts);
            return texts.stream().map(text -> "ES: " + text).collect(Collectors.toList());
        }
    }

    private static class PartialFailureTranslationService implements TranslationService {

        int calls;

        @Override
        public List<String> translate(List<String> texts, TranslationLanguage language)
                throws TranslationException {
            calls++;
            if (calls == 2) {
                throw new TranslationException("review 67 failed");
            }
            return texts.stream().map(text -> "ES: " + text).collect(Collectors.toList());
        }
    }

    private static class CancellationTranslationService implements TranslationService {

        final List<List<String>> calls = new java.util.ArrayList<>();
        final CountDownLatch firstCallCompleted = new CountDownLatch(1);
        final CountDownLatch secondCallStarted = new CountDownLatch(1);
        final CountDownLatch releaseSecondCall = new CountDownLatch(1);

        @Override
        public List<String> translate(List<String> texts, TranslationLanguage language) {
            calls.add(texts);
            if (calls.size() == 1) {
                try {
                    return texts.stream().map(text -> "ES: " + text).collect(Collectors.toList());
                } finally {
                    firstCallCompleted.countDown();
                }
            } else {
                secondCallStarted.countDown();
                try {
                    releaseSecondCall.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return texts.stream().map(text -> "ES: " + text).collect(Collectors.toList());
        }
    }

    private void waitForProcess(Process process) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (process.isRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertFalse(process.isRunning(), "Process failed to complete within five seconds");
    }
}
