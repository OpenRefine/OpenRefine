function doTranslateColumnForColumn(column) {
  var frame = $(DOM.loadHTML("core", "scripts/views/data-table/translate-column-dialog.html"));
  var elmts = DOM.bind(frame);

  elmts.dialogHeader.text($.i18n('core-views/translate-column') + " " + column.name);
  elmts.or_views_translateLanguage.text($.i18n('core-views/translate-language'));
  elmts.languageSpanish.text($.i18n('core-views/translate-language-spanish'));
  elmts.languageMandarin.text($.i18n('core-views/translate-language-mandarin'));
  elmts.languageTelugu.text($.i18n('core-views/translate-language-telugu'));
  elmts.languageTamil.text($.i18n('core-views/translate-language-tamil'));
  elmts.languageTagalog.text($.i18n('core-views/translate-language-tagalog'));
  elmts.or_views_translateEndpoint.text($.i18n('core-views/translate-endpoint'));
  elmts.or_views_translateModel.text($.i18n('core-views/translate-model'));
  elmts.or_views_translateApiKey.text($.i18n('core-views/translate-api-key'));
  elmts.or_views_translateBatchSize.text($.i18n('core-views/translate-batch-size'));
  elmts.or_views_translateTimeout.text($.i18n('core-views/translate-timeout'));
  elmts.or_views_translateMilliseconds.text($.i18n('core-views/translate-milliseconds'));
  elmts.or_views_translateRetries.text($.i18n('core-views/translate-retries'));
  elmts.translateNotice.text($.i18n('core-views/translate-api-key-warning'));
  elmts.okButton.html($.i18n('core-buttons/ok'));
  elmts.cancelButton.text($.i18n('core-buttons/cancel'));

  var level = DialogSystem.showDialog(frame);
  var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

  var clearValidationError = function() {
    elmts.validationMessage.hide().empty();
    elmts.endpointInput.removeAttr('aria-invalid');
    elmts.modelInput.removeAttr('aria-invalid');
    elmts.apiKeyInput.removeAttr('aria-invalid');
  };

  var showValidationError = function(message) {
    elmts.validationMessage.text(message).show();
  };

  elmts.cancelButton.on('click', dismiss);
  elmts.form.on('submit', function(event) {
    event.preventDefault();

    var endpoint = jQueryTrim(elmts.endpointInput[0].value);
    var model = jQueryTrim(elmts.modelInput[0].value);
    var apiKey = elmts.apiKeyInput[0].value;
    var batchSize = Number.parseInt(elmts.batchSizeInput[0].value, 10);
    var timeoutMillis = Number.parseInt(elmts.timeoutInput[0].value, 10);
    var maxRetries = Number.parseInt(elmts.retriesInput[0].value, 10);

    clearValidationError();

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
    if (model.length === 0 || apiKey.length === 0) {
      if (model.length === 0) {
        elmts.modelInput.attr('aria-invalid', 'true');
      }
      if (apiKey.length === 0) {
        elmts.apiKeyInput.attr('aria-invalid', 'true');
      }
      showValidationError($.i18n('core-views/translate-required-fields'));
      return;
    }
    if (Number.isNaN(batchSize) || batchSize < 1 || batchSize > 20) {
      showValidationError($.i18n('core-views/translate-invalid-batch-size'));
      return;
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
        language: elmts.languageInput[0].value,
        endpoint: endpoint,
        model: model,
        batchSize: batchSize,
        timeoutMillis: timeoutMillis,
        maxRetries: maxRetries
      },
      { apiKey: apiKey },
      { rowIdsPreserved: true, recordIdsPreserved: true, includeEngine: false },
      { onDone: dismiss }
    );
  });
  elmts.endpointInput.on('input', clearValidationError);
  elmts.modelInput.on('input', clearValidationError);
  elmts.apiKeyInput.on('input', clearValidationError);
  elmts.apiKeyInput.trigger('focus');
}
