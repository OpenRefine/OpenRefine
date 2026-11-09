
package com.google.refine.operations.column;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
                () -> createOperation((String) null, TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH).validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation("", TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH).validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation("review", null, TranslationLanguage.SPANISH).validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation("review", TranslationLanguage.ENGLISH, null).validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation("review", TranslationLanguage.ENGLISH, TranslationLanguage.ENGLISH).validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation("review", TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH, null).validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation("review", TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH, "").validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation("review", TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH, "review").validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation(
                        "review", TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH, 0, 3)
                        .validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation(
                        "review", TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH, 1000, -1).validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation(
                        "review", TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH, 1000, 6).validate());
        assertThrows(IllegalArgumentException.class,
                () -> createOperation(
                        "review",
                        TranslationLanguage.ENGLISH,
                        TranslationLanguage.SPANISH,
                        "review_spanish",
                        "ftp://example.invalid/translate",
                        1000,
                        3).validate());
    }

    @Test
    public void testCreateNewColumnAndPreserveSourceDataAndRowOrder() throws Exception {
        Map<String, String> translations = Map.of(
                "one", "uno",
                "two", "dos",
                "three", "tres");
        CountingTranslationService service = new CountingTranslationService(translations);
        AbstractOperation operation = createOperation(service, TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH);

        assertEquals(operation.getColumnDependencies().get(), java.util.Set.of("review"));
        assertEquals(
                operation.getColumnsDiff().get(),
                ColumnsDiff.builder().addColumn("review_spanish", "review").build());

        runOperation(operation, project, 5000);

        assertEquals(project.columnModel.getColumnNames(), List.of("review", "review_spanish", "other"));
        int reviewIndex = project.columnModel.getColumnIndexByName("review");
        int translatedIndex = project.columnModel.getColumnByName("review_spanish").getCellIndex();
        int otherIndex = project.columnModel.getColumnByName("other").getCellIndex();
        assertEquals(project.rows.get(0).getCellValue(reviewIndex), "one");
        assertEquals(project.rows.get(0).getCellValue(translatedIndex), "uno");
        assertEquals(project.rows.get(0).getCellValue(otherIndex), "keep 1");
        assertTrue(project.rows.get(1).getCell(translatedIndex) == null);
        assertEquals(project.rows.get(1).getCellValue(otherIndex), "keep 2");
        assertEquals(project.rows.get(2).getCellValue(translatedIndex), "dos");
        assertTrue(ExpressionUtils.isError(project.rows.get(3).getCellValue(reviewIndex)));
        assertTrue(project.rows.get(3).getCell(translatedIndex) == null);
        assertEquals(project.rows.get(4).getCellValue(translatedIndex), "tres");
        assertEquals(service.calls.size(), 3);
        assertEquals(project.history.getLastPastEntries(1).size(), 1);
    }

    @Test
    public void testContinueAfterFailureAndCommitOtherTranslations() throws Exception {
        Map<String, String> translations = new HashMap<>();
        translations.put("one", "uno");
        translations.put("three", "tres");
        CountingTranslationService service = new CountingTranslationService(translations, List.of("two"));

        runOperation(createOperation(service), project, 5000);

        int translatedIndex = project.columnModel.getColumnByName("review_spanish").getCellIndex();
        assertEquals(project.rows.get(0).getCellValue(translatedIndex), "uno");
        assertTrue(ExpressionUtils.isError(project.rows.get(2).getCellValue(translatedIndex)));
        assertEquals(project.rows.get(4).getCellValue(translatedIndex), "tres");
        assertEquals(service.calls.size(), 3);
    }

    @Test
    public void testCreateEmptyColumnWithoutCallingService() throws Exception {
        Project emptyProject = createProject(
                new String[] { "review" },
                new Serializable[][] { { "" }, { null } });
        CountingTranslationService service = new CountingTranslationService(Map.of());

        runOperation(createOperation(service), emptyProject, 5000);

        assertEquals(emptyProject.columnModel.getColumnNames(), List.of("review", "review_spanish"));
        assertEquals(service.calls.size(), 0);
    }

    @Test
    public void testFailWhenTargetColumnAlreadyExists() throws Exception {
        Project existingProject = createProject(
                new String[] { "review", "review_spanish" },
                new Serializable[][] { { "one", "existing" } });
        CountingTranslationService service = new CountingTranslationService(Map.of("one", "uno"));
        AbstractOperation operation = createOperation(service);
        operation.validate();

        Process process = operation.createProcess(existingProject, new java.util.Properties());
        process.startPerforming(existingProject.getProcessManager());
        waitForProcess(process);

        assertEquals(existingProject.columnModel.getColumnNames(), List.of("review", "review_spanish"));
        assertEquals(existingProject.rows.get(0).getCellValue(1), "existing");
        assertEquals(existingProject.history.getLastPastEntries(-1).size(), 0);
    }

    @Test
    public void testCancelDiscardsPartialResults() throws Exception {
        BlockingTranslationService service = new BlockingTranslationService();
        AbstractOperation operation = createOperation(service);
        operation.validate();
        Process process = operation.createProcess(project, new java.util.Properties());

        process.startPerforming(project.getProcessManager());
        assertTrue(service.firstCallStarted.await(5, TimeUnit.SECONDS));
        process.cancel();
        service.releaseFirstCall.countDown();
        waitForProcess(process);

        assertFalse(project.columnModel.getColumnNames().contains("review_spanish"));
        assertEquals(project.history.getLastPastEntries(-1).size(), 0);
    }

    @Test
    public void testRenameColumns() {
        TranslateColumnOperation operation = createOperation(
                "review", TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH);
        TranslateColumnOperation renamed = operation.renameColumns(Map.of("review", "feedback"));

        assertEquals(renamed.getColumnName(), "feedback");
        assertEquals(renamed.getTargetColumnName(), "review_spanish");
        assertEquals(renamed.getColumnDependencies().get(), java.util.Set.of("feedback"));
    }

    @Test
    public void testRenameTargetColumn() {
        TranslateColumnOperation operation = createOperation(
                "review", TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH);
        TranslateColumnOperation renamed = operation.renameColumns(Map.of("review_spanish", "translated_review"));

        assertEquals(renamed.getColumnName(), "review");
        assertEquals(renamed.getTargetColumnName(), "translated_review");
    }

    @Test
    public void testBriefDescription() {
        AbstractOperation operation = createOperation(
                "review", TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH);
        assertEquals(
                operation.getJsonDescription(),
                OperationDescription.column_translation_brief("review", "Spanish", "review_spanish"));
    }

    private TranslateColumnOperation createOperation(
            String columnName,
            TranslationLanguage sourceLanguage,
            TranslationLanguage targetLanguage) {
        return createOperation(columnName, sourceLanguage, targetLanguage, 1000, 3);
    }

    private TranslateColumnOperation createOperation(
            String columnName,
            TranslationLanguage sourceLanguage,
            TranslationLanguage targetLanguage,
            String targetColumnName) {
        return createOperation(
                columnName,
                sourceLanguage,
                targetLanguage,
                targetColumnName,
                "http://localhost:5000/translate",
                1000,
                3);
    }

    private TranslateColumnOperation createOperation(
            String columnName,
            TranslationLanguage sourceLanguage,
            TranslationLanguage targetLanguage,
            int timeoutMillis,
            int maxRetries) {
        return createOperation(
                columnName,
                sourceLanguage,
                targetLanguage,
                "review_spanish",
                "http://localhost:5000/translate",
                timeoutMillis,
                maxRetries);
    }

    private TranslateColumnOperation createOperation(
            String columnName,
            TranslationLanguage sourceLanguage,
            TranslationLanguage targetLanguage,
            String targetColumnName,
            String endpoint,
            int timeoutMillis,
            int maxRetries) {
        return new TranslateColumnOperation(
                columnName,
                sourceLanguage,
                targetLanguage,
                targetColumnName,
                endpoint,
                timeoutMillis,
                maxRetries);
    }

    private TranslateColumnOperation createOperation(TranslationService service) {
        return createOperation(service, TranslationLanguage.ENGLISH, TranslationLanguage.SPANISH);
    }

    private TranslateColumnOperation createOperation(
            TranslationService service,
            TranslationLanguage sourceLanguage,
            TranslationLanguage targetLanguage) {
        TranslateColumnOperation operation = createOperation(
                "review",
                sourceLanguage,
                targetLanguage,
                "review_spanish",
                "http://localhost:5000/translate",
                1000,
                3);
        return new TranslateColumnOperation(
                operation.getColumnName(),
                operation.getSourceLanguage(),
                operation.getTargetLanguage(),
                operation.getTargetColumnName(),
                operation.getEndpoint(),
                operation.getTimeoutMillis(),
                operation.getMaxRetries()) {

            @Override
            protected TranslationService createTranslationService() {
                return service;
            }
        };
    }

    private static class CountingTranslationService implements TranslationService {

        final Map<String, String> translations;
        final List<String> errors;
        final List<String> calls = new ArrayList<>();

        CountingTranslationService(Map<String, String> translations) {
            this(translations, List.of());
        }

        CountingTranslationService(Map<String, String> translations, List<String> errors) {
            this.translations = translations;
            this.errors = errors;
        }

        @Override
        public List<String> translate(
                List<String> texts,
                TranslationLanguage sourceLanguage,
                TranslationLanguage targetLanguage) throws TranslationException {
            calls.addAll(texts);
            List<String> results = new ArrayList<>();
            for (String text : texts) {
                if (errors.contains(text)) {
                    throw new TranslationException("bad input");
                }
                results.add(translations.get(text));
            }
            return results;
        }
    }

    private static class BlockingTranslationService implements TranslationService {

        final CountDownLatch firstCallStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirstCall = new CountDownLatch(1);

        @Override
        public List<String> translate(
                List<String> texts,
                TranslationLanguage sourceLanguage,
                TranslationLanguage targetLanguage) throws TranslationException {
            firstCallStarted.countDown();
            try {
                releaseFirstCall.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TranslationException("Translation was canceled", e);
            }
            return texts.stream().map(text -> "uno").toList();
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
