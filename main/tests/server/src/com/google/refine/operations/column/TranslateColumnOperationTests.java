
package com.google.refine.operations.column;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;
import com.google.refine.process.Process;
import com.google.refine.translate.TranslationException;
import com.google.refine.translate.TranslationLanguage;
import com.google.refine.translate.TranslationService;
import com.google.refine.util.ParsingUtilities;

public class TranslateColumnOperationTests extends RefineTest {

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
                () -> createOperation("review", null, 10, 1000, 3, "key").validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation(null, TranslationLanguage.SPANISH, 10, 1000, 3, "key").validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation("review", TranslationLanguage.SPANISH, 10, 1000, 3, "").validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation("review", TranslationLanguage.SPANISH, 21, 1000, 3, "key").validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation("review", TranslationLanguage.SPANISH, 10, 0, 3, "key").validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation("review", TranslationLanguage.SPANISH, 10, 1000, 6, "key").validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation("review", TranslationLanguage.SPANISH, 10, 1000, 3, "").validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation(
                        "review",
                        TranslationLanguage.SPANISH,
                        "ftp://example.invalid/chat/completions",
                        10,
                        1000,
                        3,
                        "key").validate());
    }

    @Test
    public void testContinueAfterPartialFailureAndCommitResults() throws Exception {
        TranslationService service = new PartialFailureTranslationService();
        AbstractOperation operation = createOperation(service, 1, TranslationLanguage.SPANISH);

        assertEquals(operation.getColumnDependencies().get(), java.util.Set.of("review"));
        assertEquals(operation.getColumnsDiff().get(), ColumnsDiff.modifySingleColumn("review"));

        runOperation(operation, project, 1000);

        int reviewIndex = project.columnModel.getColumnByName("review").getCellIndex();
        assertEquals(project.rows.get(0).getCellValue(reviewIndex), "ES: one");
        assertTrue(project.rows.get(1).getCell(reviewIndex) == null
                || "".equals(project.rows.get(1).getCellValue(reviewIndex)));
        assertTrue(ExpressionUtils.isError(project.rows.get(2).getCellValue(reviewIndex)));
        assertTrue(ExpressionUtils.isError(project.rows.get(3).getCellValue(reviewIndex)));
        assertEquals(project.rows.get(4).getCellValue(reviewIndex), "ES: three");
        long historyEntryId = project.history.getLastPastEntries(1).get(0).id;
        assertEquals(project.history.getLastPastEntries(1).size(), 1);

        project.history.undoRedo(0);
        assertEquals(project.rows.get(0).getCellValue(reviewIndex), "one");
        assertEquals(project.rows.get(2).getCellValue(reviewIndex), "two");
        assertEquals(project.rows.get(4).getCellValue(reviewIndex), "three");
        assertTrue(ExpressionUtils.isError(project.rows.get(3).getCellValue(reviewIndex)));

        project.history.undoRedo(historyEntryId);
        assertEquals(project.rows.get(0).getCellValue(reviewIndex), "ES: one");
        assertTrue(project.rows.get(1).getCell(reviewIndex) == null
                || "".equals(project.rows.get(1).getCellValue(reviewIndex)));
    }

    @Test
    public void testBlankAndErrorCellsAreNotTranslated() throws Exception {
        TranslationService service = new CountingTranslationService();
        AbstractOperation operation = createOperation(service, 20, TranslationLanguage.SPANISH);

        runOperation(operation, project, 1000);

        assertEquals(((CountingTranslationService) service).calls.size(), 1);
        assertEquals(((CountingTranslationService) service).calls.get(0), List.of("one", "two", "three"));
        int reviewIndex = project.columnModel.getColumnByName("review").getCellIndex();
        assertTrue(project.rows.get(1).getCell(reviewIndex) == null
                || "".equals(project.rows.get(1).getCellValue(reviewIndex)));
        assertEquals(((EvalError) project.rows.get(3).getCellValue(reviewIndex)).message, "existing error");
    }

    @Test
    public void testNoCellsDoesNotCallTranslationService() throws Exception {
        project = createProject(
                new String[] { "review", "other" },
                new Serializable[][] {
                        { "", "keep 1" },
                        { null, "keep 2" }
                });
        TranslationService service = new CountingTranslationService();

        runOperation(createOperation(service, 20, TranslationLanguage.SPANISH), project, 1000);

        assertTrue(((CountingTranslationService) service).calls.isEmpty());
        assertEquals(project.history.getLastPastEntries(1), List.of());
    }

    @Test
    public void testNumericCellsAreSentAsText() throws Exception {
        project = createProject(
                new String[] { "review", "other" },
                new Serializable[][] {
                        { 123, "keep 1" },
                        { 45.67, "keep 2" }
                });
        TranslationService service = new CountingTranslationService();

        runOperation(createOperation(service, 20, TranslationLanguage.SPANISH), project, 1000);

        assertEquals(((CountingTranslationService) service).calls.get(0), List.of("123", "45.67"));
    }

    @Test
    public void testMissingColumnFailsProcessWithoutHistory() throws Exception {
        AbstractOperation operation = createOperation(new CountingTranslationService(), 20, TranslationLanguage.SPANISH);
        project.columnModel.removeColumnByCellIndex(project.columnModel.getColumnByName("review").getCellIndex());
        project.columnModel.update();
        Process process = operation.createProcess(project, new java.util.Properties());

        process.startPerforming(project.getProcessManager());
        waitForProcess(process);

        assertTrue(project.history.getLastPastEntries(1).isEmpty());
    }

    @Test
    public void testCancellationRetainsCompletedBatches() throws Exception {
        CancellationTranslationService service = new CancellationTranslationService();
        Process process = createOperation(service, 1, TranslationLanguage.SPANISH)
                .createProcess(project, new java.util.Properties());

        process.startPerforming(project.getProcessManager());
        assertTrue(service.firstCallCompleted.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(service.secondCallStarted.await(5, java.util.concurrent.TimeUnit.SECONDS));
        process.cancel();
        service.releaseSecondCall.countDown();
        waitForProcess(process);

        assertEquals(service.calls.size(), 2);
        int reviewIndex = project.columnModel.getColumnByName("review").getCellIndex();
        assertEquals(project.rows.get(0).getCellValue(reviewIndex), "ES: one");
        assertEquals(project.rows.get(2).getCellValue(reviewIndex), "ES: two");
        assertEquals(project.rows.get(4).getCellValue(reviewIndex), "three");
        assertEquals(project.history.getLastPastEntries(1).size(), 1);
    }

    @Test
    public void testApiKeyIsNotSerialized() throws Exception {
        String serialized = ParsingUtilities.mapper.writeValueAsString(
                createOperation("review", TranslationLanguage.SPANISH, 10, 1000, 3, "secret-key"));

        assertFalse(serialized.contains("secret-key"));
        assertTrue(serialized.contains("\"batchSize\":10"));
    }

    @Test
    public void testRenameColumn() {
        AbstractOperation operation = createOperation("review", TranslationLanguage.SPANISH, 10, 1000, 3, "key");
        AbstractOperation renamed = operation.renameColumns(java.util.Map.of("review", "feedback"));

        assertEquals(renamed.getColumnDependencies().get(), java.util.Set.of("feedback"));
        assertEquals(((TranslateColumnOperation) renamed).getColumnName(), "feedback");
    }

    @Test
    public void testBriefDescription() {
        AbstractOperation operation = createOperation("review", TranslationLanguage.SPANISH, 10, 1000, 3, "key");
        assertEquals(
                operation.getJsonDescription(),
                OperationDescription.column_translation_brief("review", "Spanish"));
    }

    private TranslateColumnOperation createOperation(
            String columnName,
            TranslationLanguage language,
            int batchSize,
            int timeoutMillis,
            int maxRetries,
            String apiKey) {
        return createOperation(
                columnName,
                language,
                "https://api.openai.com/v1/chat/completions",
                batchSize,
                timeoutMillis,
                maxRetries,
                apiKey);
    }

    private TranslateColumnOperation createOperation(
            String columnName,
            TranslationLanguage language,
            String endpoint,
            int batchSize,
            int timeoutMillis,
            int maxRetries,
            String apiKey) {
        return new TranslateColumnOperation(
                columnName,
                language,
                endpoint,
                "test-model",
                batchSize,
                timeoutMillis,
                maxRetries,
                apiKey);
    }

    private TranslateColumnOperation createOperation(
            TranslationService service,
            int batchSize,
            TranslationLanguage language) {
        return new TranslateColumnOperation(
                "review",
                language,
                "https://api.openai.com/v1/chat/completions",
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

    private static class CountingTranslationService implements TranslationService {

        final List<List<String>> calls = new java.util.ArrayList<>();

        @Override
        public List<String> translate(List<String> texts, TranslationLanguage language) {
            calls.add(texts);
            return texts.stream().map(text -> "ES: " + text).collect(java.util.stream.Collectors.toList());
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
            return texts.stream().map(text -> "ES: " + text).collect(java.util.stream.Collectors.toList());
        }
    }

    private static class CancellationTranslationService implements TranslationService {

        final List<List<String>> calls = new java.util.ArrayList<>();
        final java.util.concurrent.CountDownLatch firstCallCompleted = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch secondCallStarted = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch releaseSecondCall = new java.util.concurrent.CountDownLatch(1);

        @Override
        public List<String> translate(List<String> texts, TranslationLanguage language) {
            calls.add(texts);
            if (calls.size() == 1) {
                try {
                    return texts.stream().map(text -> "ES: " + text).collect(java.util.stream.Collectors.toList());
                } finally {
                    firstCallCompleted.countDown();
                }
            } else {
                secondCallStarted.countDown();
                try {
                    releaseSecondCall.await(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return texts.stream().map(text -> "ES: " + text).collect(java.util.stream.Collectors.toList());
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
