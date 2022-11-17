/*

Copyright 2011, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

Refine.LineBasedParserUI = function(controller, jobID, job, format, config,
    dataContainerElmt, progressContainerElmt, optionContainerElmt) {

  this._controller = controller;
  this._jobID = jobID;
  this._job = job;
  this._format = format;
  this._config = config;

  this._dataContainer = dataContainerElmt;
  this._progressContainer = progressContainerElmt;
  this._optionContainer = optionContainerElmt;

  this._timerID = null;
  this._initialize();
  this._updatePreview();
};
Refine.DefaultImportingController.parserUIs.LineBasedParserUI = Refine.LineBasedParserUI;

Refine.LineBasedParserUI.prototype.confirmReadyToCreateProject = function() {
  return true;
};

Refine.LineBasedParserUI.prototype.dispose = function() {
  if (this._timerID !== null) {
    window.clearTimeout(this._timerID);
    this._timerID = null;
  }
};

Refine.LineBasedParserUI.prototype.getOptions = function() {
  var options = {
    encoding: jQueryTrim(this._optionContainerElmts.encodingInput[0].value),
    recordPath: this._config.recordPath
  };

  switch (this._optionContainer.find("input[name='row-separator']:checked")[0].value) {
    case 'newline':
      options.separator = "\\r?\\n";
      break;
    default:
      options.separator = this._optionContainerElmts.rowSeparatorInput[0].value;
  }

  var parseIntDefault = function(s, def) {
    try {
      var n = parseInt(s,10);
      if (!isNaN(n)) {
        return n;
      }
    } catch (e) {
      // Ignore
    }
    return def;
  };
  options.linesPerRow = parseIntDefault(this._optionContainerElmts.linesPerRowInput[0].value, 1);

  if (this._optionContainerElmts.ignoreCheckbox[0].checked) {
    options.ignoreLines = parseIntDefault(this._optionContainerElmts.ignoreInput[0].value, -1);
  } else {
    options.ignoreLines = -1;
  }
  if (this._optionContainerElmts.limitCheckbox[0].checked) {
    options.limit = parseIntDefault(this._optionContainerElmts.limitInput[0].value, -1);
  } else {
    options.limit = -1;
  }
  if (this._optionContainerElmts.skipCheckbox[0].checked) {
    options.skipDataLines = parseIntDefault(this._optionContainerElmts.skipInput[0].value, -1);
  } else {
    options.skipDataLines = -1;
  }
  options.storeBlankRows = this._optionContainerElmts.storeBlankRowsCheckbox[0].checked;
  options.storeBlankCellsAsNulls = this._optionContainerElmts.storeBlankCellsAsNullsCheckbox[0].checked;
  options.includeFileSources = this._optionContainerElmts.includeFileSourcesCheckbox[0].checked;
  options.includeArchiveFileName = this._optionContainerElmts.includeArchiveFileCheckbox[0].checked;

  options.disableAutoPreview = this._optionContainerElmts.disableAutoPreviewCheckbox[0].checked;

  return options;
};

Refine.LineBasedParserUI.prototype._initialize = function() {
  var self = this;

  this._optionContainer.off().empty().html(
      DOM.loadHTML("core", "scripts/index/parser-interfaces/line-based-parser-ui.html"));
  this._optionContainerElmts = DOM.bind(this._optionContainer);
  this._optionContainerElmts.previewButton.on('click',function() { self._updatePreview(); });

  $('#or-import-encoding').html($.i18n('core-index-import/char-encoding'));
  this._optionContainerElmts.previewButton.html($.i18n('core-buttons/update-preview'));
  $('#or-disable-auto-preview').text($.i18n('core-index-parser/disable-auto-preview'));
  $('#or-import-parseEvery').html($.i18n('core-index-parser/parse-every'));
  $('#or-import-linesIntoRow').html($.i18n('core-index-parser/lines-into-row'));
  $('#or-import-blank').text($.i18n('core-index-parser/store-blank'));
  $('#or-import-null').text($.i18n('core-index-parser/store-nulls'));
  $('#or-import-source').html($.i18n('core-index-parser/store-source'));
  $('#or-import-archive').html($.i18n('core-index-parser/store-archive'));
  $('#or-import-ignore').text($.i18n('core-index-parser/ignore-first'));
  $('#or-import-lines').text($.i18n('core-index-parser/lines-beg'));
  $('#or-import-parse').text($.i18n('core-index-parser/parse-next'));
  $('#or-import-header').text($.i18n('core-index-parser/lines-header'));
  $('#or-import-discard').text($.i18n('core-index-parser/discard-initial'));
  $('#or-import-rows').text($.i18n('core-index-parser/rows-data'));
  $('#or-import-load').text($.i18n('core-index-parser/load-at-most'));
  $('#or-import-rows2').text($.i18n('core-index-parser/rows-data'));
  $('#or-row-separator').text($.i18n('core-index-parser/row-separator'));
  $('#or-row-separator-newline').text($.i18n('core-index-parser/row-separator-newline'));
  $('#or-row-separator-regex').text($.i18n('core-index-parser/row-separator-regex'));

  this._optionContainerElmts.encodingInput
    .val(this._config.encoding || '')
    .on('click',function() {
      Encoding.selectEncoding($(this), function() {
        self._updatePreview();
      });
    });


  var rowSeparatorValue = (this._config.separator == "\\r?\\n") ? 'newline' : 'custom';
  this._optionContainer.find(
      "input[name='row-separator'][value='" + rowSeparatorValue + "']").prop("checked", true);
  this._optionContainerElmts.rowSeparatorInput[0].value = this._config.separator;

  this._optionContainerElmts.linesPerRowInput[0].value =
    this._config.linesPerRow.toString();

  if (this._config.ignoreLines > 0) {
    this._optionContainerElmts.ignoreCheckbox.prop("checked", true);
    this._optionContainerElmts.ignoreInput[0].value = this._config.ignoreLines.toString();
  }
  if (this._config.limit > 0) {
    this._optionContainerElmts.limitCheckbox.prop("checked", true);
    this._optionContainerElmts.limitInput[0].value = this._config.limit.toString();
  }
  if (this._config.skipDataLines > 0) {
    this._optionContainerElmts.skipCheckbox.prop("checked", true);
    this._optionContainerElmts.skipInput.value[0].value = this._config.skipDataLines.toString();
  }
  if (this._config.storeBlankRows) {
    this._optionContainerElmts.storeBlankRowsCheckbox.prop("checked", true);
  }
  if (this._config.storeBlankCellsAsNulls) {
    this._optionContainerElmts.storeBlankCellsAsNullsCheckbox.prop("checked", true);
  }
  if (this._config.includeFileSources) {
    this._optionContainerElmts.includeFileSourcesCheckbox.prop("checked", true);
  }
  if (this._config.includeArchiveFileName) {
    this._optionContainerElmts.includeArchiveFileCheckbox.prop("checked", true);
  }

  if (this._config.disableAutoPreview) {
    this._optionContainerElmts.disableAutoPreviewCheckbox.prop('checked', true);
  }

  // If disableAutoPreviewCheckbox is not checked, we will schedule an automatic update
  var onChange = function() {
    if (!self._optionContainerElmts.disableAutoPreviewCheckbox[0].checked)
    {
        self._scheduleUpdatePreview();
    }
  };
  this._optionContainer.find("input").on("change", onChange);
  this._optionContainer.find("select").on("change", onChange);
};

Refine.LineBasedParserUI.prototype._scheduleUpdatePreview = function() {
  if (this._timerID !== null) {
    window.clearTimeout(this._timerID);
    this._timerID = null;
  }

  var self = this;
  this._timerID = window.setTimeout(function() {
    self._timerID = null;
    self._updatePreview();
  }, 500); // 0.5 second
};

Refine.LineBasedParserUI.prototype._updatePreview = function() {
  var self = this;

  this._progressContainer.show();

  this._controller.updateFormatAndOptions(this.getOptions(), function(result) {
    if (result.status == "ok") {
      self._controller.getPreviewData(function(projectData) {
        self._progressContainer.hide();

        new Refine.PreviewTable(projectData, self._dataContainer.off().empty());
      });
    }
  });
};
