function doTranslateColumnForColumn(column) {
  var frame = $(DOM.loadHTML("core", "scripts/views/data-table/translate-column-dialog.html"));
  var elmts = DOM.bind(frame);

  elmts.dialogHeader.text($.i18n('core-views/translate-column') + " " + column.name);
  elmts.or_views_translateSourceLanguage.text($.i18n('core-views/translate-source-language'));
  elmts.or_views_translateTargetLanguage.text($.i18n('core-views/translate-target-language'));
  elmts.or_views_translateTargetColumn.text($.i18n('core-views/translate-target-column'));
  elmts.languageEnglish.text($.i18n('core-views/translate-language-english'));
  elmts.languageSpanish.text($.i18n('core-views/translate-language-spanish'));
  elmts.languageMandarin.text($.i18n('core-views/translate-language-mandarin'));
  elmts.targetLanguageEnglish.text($.i18n('core-views/translate-language-english'));
  elmts.targetLanguageSpanish.text($.i18n('core-views/translate-language-spanish'));
  elmts.targetLanguageMandarin.text($.i18n('core-views/translate-language-mandarin'));
  elmts.or_views_translateEndpoint.text($.i18n('core-views/translate-endpoint'));
  elmts.or_views_translateTimeout.text($.i18n('core-views/translate-timeout'));
  elmts.or_views_translateMilliseconds.text($.i18n('core-views/translate-milliseconds'));
  elmts.or_views_translateRetries.text($.i18n('core-views/translate-retries'));
  elmts.translateNotice.text($.i18n('core-views/translate-notice'));
  elmts.okButton.html($.i18n('core-buttons/ok'));
  elmts.cancelButton.text($.i18n('core-buttons/cancel'));

  elmts.endpointInput[0].value = "http://localhost:5000/translate";
  elmts.targetColumnInput[0].value = column.name + "_translation";
  elmts.targetColumnInput.trigger('focus');

  var level = DialogSystem.showDialog(frame);
  var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

  var clearValidationError = function() {
    elmts.validationMessage.hide().empty();
    elmts.targetColumnInput.removeAttr('aria-invalid');
    elmts.endpointInput.removeAttr('aria-invalid');
  };

  var showValidationError = function(message) {
    elmts.validationMessage.text(message).show();
  };

  elmts.cancelButton.on('click', dismiss);
  elmts.form.on('submit', function(event) {
    event.preventDefault();

    var targetColumnName = jQueryTrim(elmts.targetColumnInput[0].value);
    var endpoint = jQueryTrim(elmts.endpointInput[0].value);
    var timeoutMillis = Number.parseInt(elmts.timeoutInput[0].value, 10);
    var maxRetries = Number.parseInt(elmts.retriesInput[0].value, 10);

    clearValidationError();

    if (targetColumnName.length === 0) {
      elmts.targetColumnInput.attr('aria-invalid', 'true');
      showValidationError($.i18n('core-views/translate-required-target-column'));
      return;
    }
    if (Refine.columnNameToColumnIndex(targetColumnName) >= 0) {
      elmts.targetColumnInput.attr('aria-invalid', 'true');
      showValidationError($.i18n('core-views/translate-existing-target-column'));
      return;
    }
    if (endpoint.length > 0) {
      try {
        var parsedEndpoint = new URL(endpoint);
        if (parsedEndpoint.protocol !== 'http:' && parsedEndpoint.protocol !== 'https:') {
          throw new Error('unsupported endpoint protocol');
        }
      } catch (error) {
        elmts.endpointInput.attr('aria-invalid', 'true');
        showValidationError($.i18n('core-views/translate-invalid-endpoint'));
        return;
      }
    }
    if (Number.isNaN(timeoutMillis) || timeoutMillis < 1) {
      showValidationError($.i18n('core-views/translate-invalid-timeout'));
      return;
    }
    if (Number.isNaN(maxRetries) || maxRetries < 0 || maxRetries > 5) {
      showValidationError($.i18n('core-views/translate-invalid-retries'));
      return;
    }

    Refine.postCoreProcess(
      "translate-column",
      {
        columnName: column.name,
        sourceLanguage: elmts.sourceLanguageInput[0].value,
        targetLanguage: elmts.targetLanguageInput[0].value,
        targetColumnName: targetColumnName,
        endpoint: endpoint,
        timeoutMillis: timeoutMillis,
        maxRetries: maxRetries
      },
      null,
      { rowIdsPreserved: true, recordIdsPreserved: true, includeEngine: false },
      { onDone: dismiss }
    );
  });
  elmts.targetColumnInput.on('input', clearValidationError);
  elmts.endpointInput.on('input', clearValidationError);
  elmts.targetColumnInput.trigger('focus');
}
