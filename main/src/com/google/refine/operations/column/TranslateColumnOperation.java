
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
import com.google.refine.model.changes.CellAtRow;
import com.google.refine.model.changes.ColumnAdditionChange;
import com.google.refine.operations.OperationDescription;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;
import com.google.refine.translate.LibreTranslateTranslationService;
import com.google.refine.translate.TranslationException;
import com.google.refine.translate.TranslationLanguage;
import com.google.refine.translate.TranslationService;

public class TranslateColumnOperation extends AbstractOperation {

    public static final int MAX_RETRIES = 5;

    @JsonProperty("columnName")
    final protected String _columnName;
    @JsonProperty("sourceLanguage")
    final protected TranslationLanguage _sourceLanguage;
    @JsonProperty("targetLanguage")
    final protected TranslationLanguage _targetLanguage;
    @JsonProperty("targetColumnName")
    final protected String _targetColumnName;
    @JsonProperty("endpoint")
    final protected String _endpoint;
    @JsonProperty("timeoutMillis")
    final protected int _timeoutMillis;
    @JsonProperty("maxRetries")
    final protected int _maxRetries;

    @JsonCreator
    public TranslateColumnOperation(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("sourceLanguage") TranslationLanguage sourceLanguage,
            @JsonProperty("targetLanguage") TranslationLanguage targetLanguage,
            @JsonProperty("targetColumnName") String targetColumnName,
            @JsonProperty("endpoint") String endpoint,
            @JsonProperty("timeoutMillis") int timeoutMillis,
            @JsonProperty("maxRetries") int maxRetries) {
        _columnName = columnName;
        _sourceLanguage = sourceLanguage;
        _targetLanguage = targetLanguage;
        _targetColumnName = targetColumnName;
        _endpoint = endpoint;
        _timeoutMillis = timeoutMillis;
        _maxRetries = maxRetries;
    }

    @Override
    public void validate() {
        Validate.notNull(_columnName, "Missing source column name");
        Validate.isTrue(!_columnName.isBlank(), "Missing source column name");
        Validate.notNull(_sourceLanguage, "Missing source language");
        Validate.notNull(_targetLanguage, "Missing target language");
        Validate.isTrue(_sourceLanguage != _targetLanguage, "Source and target languages must be different");
        Validate.notNull(_targetColumnName, "Missing target column name");
        Validate.isTrue(!_targetColumnName.isBlank(), "Missing target column name");
        Validate.isTrue(!_targetColumnName.equals(_columnName), "Target column must be different from the source column");
        Validate.isTrue(_timeoutMillis > 0, "Timeout must be greater than zero");
        Validate.isTrue(_maxRetries >= 0, "Retry count cannot be negative");
        Validate.isTrue(_maxRetries <= MAX_RETRIES, "Retry count must be at most " + MAX_RETRIES);
        validateEndpoint();
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("sourceLanguage")
    public TranslationLanguage getSourceLanguage() {
        return _sourceLanguage;
    }

    @JsonProperty("targetLanguage")
    public TranslationLanguage getTargetLanguage() {
        return _targetLanguage;
    }

    @JsonProperty("targetColumnName")
    public String getTargetColumnName() {
        return _targetColumnName;
    }

    @JsonProperty("endpoint")
    public String getEndpoint() {
        return _endpoint;
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
        return OperationDescription.column_translation_brief(
                _columnName,
                _targetLanguage.getDisplayName(),
                _targetColumnName);
    }

    @Override
    public Optional<Set<String>> getColumnDependencies() {
        return Optional.of(Set.of(_columnName));
    }

    @Override
    public Optional<ColumnsDiff> getColumnsDiff() {
        return Optional.of(ColumnsDiff.builder().addColumn(_targetColumnName, _columnName).build());
    }

    @Override
    public TranslateColumnOperation renameColumns(Map<String, String> newColumnNames) {
        return new TranslateColumnOperation(
                newColumnNames.getOrDefault(_columnName, _columnName),
                _sourceLanguage,
                _targetLanguage,
                newColumnNames.getOrDefault(_targetColumnName, _targetColumnName),
                _endpoint,
                _timeoutMillis,
                _maxRetries);
    }

    protected TranslationService createTranslationService() {
        return new LibreTranslateTranslationService(_endpoint, _timeoutMillis, _maxRetries);
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
            Column sourceColumn = _project.columnModel.getColumnByName(_columnName);
            if (sourceColumn == null) {
                _project.processManager.onFailedProcess(this, new Exception("No column named " + _columnName));
                return;
            }
            if (_project.columnModel.getColumnByName(_targetColumnName) != null) {
                _project.processManager.onFailedProcess(this, new Exception("Another column already named " + _targetColumnName));
                return;
            }

            int sourceColumnIndex = _project.columnModel.getColumnIndexByName(_columnName);
            int targetColumnIndex = sourceColumnIndex + 1;
            snapshotTargets(sourceColumn.getCellIndex());
            List<CellAtRow> translatedCells = translateTargets();
            if (_targets.isEmpty()) {
                _progress = 100;
            }
            if (_canceled) {
                return;
            }

            try {
                HistoryEntry historyEntry = new HistoryEntry(
                        _historyEntryID,
                        _project,
                        _description,
                        TranslateColumnOperation.this,
                        new ColumnAdditionChange(_targetColumnName, targetColumnIndex, translatedCells));
                _project.history.addEntry(historyEntry);
                _project.processManager.onDoneProcess(this);
            } catch (Exception e) {
                _project.processManager.onFailedProcess(this, e);
            }
        }

        private void snapshotTargets(int cellIndex) {
            synchronized (_project) {
                for (int rowIndex = 0; rowIndex < _project.rows.size(); rowIndex++) {
                    Row row = _project.rows.get(rowIndex);
                    Cell sourceCell = row.getCell(cellIndex);
                    if (sourceCell == null || sourceCell.value == null || ExpressionUtils.isError(sourceCell.value)
                            || !ExpressionUtils.isNonBlankData(sourceCell.value)) {
                        continue;
                    }
                    _targets.add(new TranslationTarget(rowIndex, sourceCell.value.toString()));
                }
            }
        }

        private List<CellAtRow> translateTargets() {
            List<CellAtRow> translatedCells = new ArrayList<>();
            int completedCount = 0;

            for (int start = 0; !_canceled && start < _targets.size(); start++) {
                TranslationTarget target = _targets.get(start);
                try {
                    List<String> translations = _translationService.translate(
                            List.of(target.text),
                            _sourceLanguage,
                            _targetLanguage);
                    if (translations.size() != 1) {
                        translatedCells.add(new CellAtRow(
                                target.rowIndex,
                                new Cell(new EvalError("Translation failed: response size mismatch"), null)));
                    } else {
                        translatedCells.add(new CellAtRow(
                                target.rowIndex,
                                new Cell(translations.get(0), null)));
                    }
                } catch (TranslationException e) {
                    if (_canceled) {
                        break;
                    }
                    translatedCells.add(new CellAtRow(
                            target.rowIndex,
                            new Cell(new EvalError("Translation failed: " + e.getMessage()), null)));
                }

                completedCount++;
                _progress = completedCount * 100 / _targets.size();
            }

            return translatedCells;
        }

        protected static class TranslationTarget {

            final protected int rowIndex;
            final protected String text;

            protected TranslationTarget(int rowIndex, String text) {
                this.rowIndex = rowIndex;
                this.text = text;
            }
        }
    }
}
