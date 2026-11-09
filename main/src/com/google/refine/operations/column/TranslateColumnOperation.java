
package com.google.refine.operations.column;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.Validate;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.model.changes.MassCellChange;
import com.google.refine.operations.OperationDescription;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;
import com.google.refine.translate.OpenAiCompatibleTranslationService;
import com.google.refine.translate.TranslationException;
import com.google.refine.translate.TranslationLanguage;
import com.google.refine.translate.TranslationService;

public class TranslateColumnOperation extends AbstractOperation {

    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 20;
    public static final int MAX_RETRIES = 5;

    @JsonProperty("columnName")
    final protected String _columnName;
    @JsonProperty("language")
    final protected TranslationLanguage _language;
    @JsonProperty("endpoint")
    final protected String _endpoint;
    @JsonProperty("model")
    final protected String _model;
    @JsonProperty("batchSize")
    final protected int _batchSize;
    @JsonProperty("timeoutMillis")
    final protected int _timeoutMillis;
    @JsonProperty("maxRetries")
    final protected int _maxRetries;
    @JsonIgnore
    final protected String _apiKey;

    @JsonCreator
    public TranslateColumnOperation(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("language") TranslationLanguage language,
            @JsonProperty("endpoint") String endpoint,
            @JsonProperty("model") String model,
            @JsonProperty("batchSize") int batchSize,
            @JsonProperty("timeoutMillis") int timeoutMillis,
            @JsonProperty("maxRetries") int maxRetries,
            @JsonProperty("apiKey") String apiKey) {
        _columnName = columnName;
        _language = language;
        _endpoint = endpoint;
        _model = model;
        _batchSize = batchSize;
        _timeoutMillis = timeoutMillis;
        _maxRetries = maxRetries;
        _apiKey = apiKey;
    }

    @Override
    public void validate() {
        Validate.notNull(_columnName, "Missing column name");
        Validate.notNull(_language, "Missing target language");
        Validate.notNull(_model, "Missing translation model");
        Validate.isTrue(!_model.isBlank(), "Missing translation model");
        Validate.isTrue(_batchSize >= MIN_BATCH_SIZE, "Batch size must be at least " + MIN_BATCH_SIZE);
        Validate.isTrue(_batchSize <= MAX_BATCH_SIZE, "Batch size must be at most " + MAX_BATCH_SIZE);
        Validate.isTrue(_timeoutMillis > 0, "Timeout must be greater than zero");
        Validate.isTrue(_maxRetries >= 0, "Retry count cannot be negative");
        Validate.isTrue(_maxRetries <= MAX_RETRIES, "Retry count must be at most " + MAX_RETRIES);
        Validate.notNull(_apiKey, "Missing API key");
        Validate.isTrue(!_apiKey.isBlank(), "API key cannot be blank");
        validateEndpoint();
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("language")
    public TranslationLanguage getLanguage() {
        return _language;
    }

    @JsonProperty("endpoint")
    public String getEndpoint() {
        return _endpoint;
    }

    @JsonProperty("model")
    public String getModel() {
        return _model;
    }

    @JsonProperty("batchSize")
    public int getBatchSize() {
        return _batchSize;
    }

    @JsonProperty("timeoutMillis")
    public int getTimeoutMillis() {
        return _timeoutMillis;
    }

    @JsonProperty("maxRetries")
    public int getMaxRetries() {
        return _maxRetries;
    }

    private void validateEndpoint() {
        if (_endpoint == null || _endpoint.isBlank()) {
            return;
        }
        try {
            URI uri = new URI(_endpoint);
            String scheme = uri.getScheme();
            Validate.isTrue(scheme != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null,
                    "The translation endpoint must be an absolute HTTP or HTTPS URL");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid translation endpoint", e);
        }
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.column_translation_brief(_columnName, _language.getDisplayName());
    }

    @Override
    public Optional<Set<String>> getColumnDependencies() {
        return Optional.of(Set.of(_columnName));
    }

    @Override
    public Optional<ColumnsDiff> getColumnsDiff() {
        return Optional.of(ColumnsDiff.modifySingleColumn(_columnName));
    }

    @Override
    public TranslateColumnOperation renameColumns(Map<String, String> newColumnNames) {
        return new TranslateColumnOperation(
                newColumnNames.getOrDefault(_columnName, _columnName),
                _language,
                _endpoint,
                _model,
                _batchSize,
                _timeoutMillis,
                _maxRetries,
                _apiKey);
    }

    protected TranslationService createTranslationService() {
        return new OpenAiCompatibleTranslationService(_endpoint, _model, _apiKey, _timeoutMillis, _maxRetries);
    }

    @Override
    public Process createProcess(Project project, Properties options) throws Exception {
        return new TranslateColumnProcess(project, createTranslationService(), getBriefDescription(project));
    }

    protected class TranslateColumnProcess extends LongRunningProcess implements Runnable {

        final protected Project _project;
        final protected TranslationService _translationService;
        final protected long _historyEntryID;
        final protected List<TranslationTarget> _targets = new ArrayList<>();

        protected TranslateColumnProcess(
                Project project,
                TranslationService translationService,
                String description) {
            super(description);
            _project = project;
            _translationService = translationService;
            _historyEntryID = HistoryEntry.allocateID();
        }

        @Override
        protected Runnable getRunnable() {
            return this;
        }

        @Override
        public void run() {
            Column column = _project.columnModel.getColumnByName(_columnName);
            if (column == null) {
                _project.processManager.onFailedProcess(this, new Exception("No column named " + _columnName));
                return;
            }

            int cellIndex = column.getCellIndex();
            snapshotTargets(cellIndex);
            List<CellChange> cellChanges = translateTargets(cellIndex);
            if (_targets.isEmpty()) {
                _progress = 100;
            }

            try {
                if (!cellChanges.isEmpty()) {
                    HistoryEntry historyEntry = new HistoryEntry(
                            _historyEntryID,
                            _project,
                            _description,
                            TranslateColumnOperation.this,
                            new MassCellChange(cellChanges, _columnName, true));
                    _project.history.addEntry(historyEntry);
                }
                _project.processManager.onDoneProcess(this);
            } catch (Exception e) {
                _project.processManager.onFailedProcess(this, e);
            }
        }

        private void snapshotTargets(int cellIndex) {
            synchronized (_project) {
                for (int rowIndex = 0; rowIndex < _project.rows.size(); rowIndex++) {
                    Row row = _project.rows.get(rowIndex);
                    Cell oldCell = row.getCell(cellIndex);
                    if (oldCell == null || oldCell.value == null || ExpressionUtils.isError(oldCell.value)
                            || !ExpressionUtils.isNonBlankData(oldCell.value)) {
                        continue;
                    }
                    _targets.add(new TranslationTarget(rowIndex, oldCell, oldCell.value.toString()));
                }
            }
        }

        private List<CellChange> translateTargets(int cellIndex) {
            List<CellChange> cellChanges = new ArrayList<>(_targets.size());
            int completedCount = 0;

            for (int start = 0; !_canceled && start < _targets.size(); start += _batchSize) {
                int end = Math.min(start + _batchSize, _targets.size());
                List<String> texts = new ArrayList<>(end - start);
                for (int index = start; index < end; index++) {
                    texts.add(_targets.get(index).text);
                }

                List<String> translations;
                try {
                    translations = _translationService.translate(texts, _language);
                } catch (TranslationException e) {
                    if (_canceled) {
                        break;
                    }
                    translations = null;
                    for (int index = start; index < end; index++) {
                        TranslationTarget target = _targets.get(index);
                        cellChanges.add(new CellChange(
                                target.rowIndex,
                                cellIndex,
                                target.oldCell,
                                new Cell(new EvalError("Translation failed: " + e.getMessage()), null)));
                    }
                }

                if (translations != null && translations.size() != texts.size()) {
                    for (int index = start; index < end; index++) {
                        TranslationTarget target = _targets.get(index);
                        cellChanges.add(new CellChange(
                                target.rowIndex,
                                cellIndex,
                                target.oldCell,
                                new Cell(new EvalError("Translation failed: response size mismatch"), null)));
                    }
                } else if (translations != null) {
                    for (int index = start; index < end; index++) {
                        TranslationTarget target = _targets.get(index);
                        cellChanges.add(new CellChange(
                                target.rowIndex,
                                cellIndex,
                                target.oldCell,
                                new Cell(translations.get(index - start), null)));
                    }
                }

                completedCount = Math.min(end, _targets.size());
                _progress = completedCount * 100 / _targets.size();
                if (_canceled) {
                    break;
                }
            }

            return cellChanges;
        }

        protected static class TranslationTarget {

            final protected int rowIndex;
            final protected Cell oldCell;
            final protected String text;

            protected TranslationTarget(int rowIndex, Cell oldCell, String text) {
                this.rowIndex = rowIndex;
                this.oldCell = oldCell;
                this.text = text;
            }
        }
    }
}
