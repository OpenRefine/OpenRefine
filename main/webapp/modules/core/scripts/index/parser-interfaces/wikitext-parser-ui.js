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

Refine.WikitextParserUI = function(controller, jobID, job, format, config,
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
Refine.DefaultImportingController.parserUIs.WikitextParserUI = Refine.WikitextParserUI;

Refine.WikitextParserUI.prototype.dispose = function() {
  if (this._timerID !== null) {
    window.clearTimeout(this._timerID);
    this._timerID = null;
  }
};

Refine.WikitextParserUI.prototype.confirmReadyToCreateProject = function() {
  return true; // always ready
};

Refine.WikitextParserUI.prototype.getOptions = function() {
  var options = {
  };
  
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
  if (this._optionContainerElmts.wikiCheckbox[0].checked) {
    options.wikiUrl = this._optionContainerElmts.wikiUrlInput[0].value;
  } else {
    options.wikiUrl = null;
  }
  if (this._optionContainerElmts.limitCheckbox[0].checked) {
    options.limit = parseIntDefault(this._optionContainerElmts.limitInput[0].value, -1);
  } else {
    options.limit = -1;
  }
  if (this._optionContainerElmts.headerLinesCheckbox[0].checked) {
    options.headerLines = parseIntDefault(this._optionContainerElmts.headerLinesInput[0].value, 1);
  } else {
    options.headerLines = -1;
  }

  options.storeBlankRows = this._optionContainerElmts.storeBlankRowsCheckbox[0].checked;
  options.blankSpanningCells = this._optionContainerElmts.blankSpanningCellsCheckbox[0].checked;
  options.includeRawTemplates = this._optionContainerElmts.includeRawTemplatesCheckbox[0].checked;
  options.parseReferences = this._optionContainerElmts.parseReferencesCheckbox[0].checked;

  options.guessCellValueTypes = this._optionContainerElmts.guessCellValueTypesCheckbox[0].checked;

  options.storeBlankCellsAsNulls = this._optionContainerElmts.storeBlankCellsAsNullsCheckbox[0].checked;
  options.includeFileSources = this._optionContainerElmts.includeFileSourcesCheckbox[0].checked;
  options.includeArchiveFileName = this._optionContainerElmts.includeArchiveFileCheckbox[0].checked;

  options.reconService = ReconciliationManager.ensureDefaultServicePresent();

  options.disableAutoPreview = this._optionContainerElmts.disableAutoPreviewCheckbox[0].checked;

  return options;
};

Refine.WikitextParserUI.prototype._initialize = function() {
  var self = this;

  this._optionContainer.off().empty().html(
      DOM.loadHTML("core", "scripts/index/parser-interfaces/wikitext-parser-ui.html"));
  this._optionContainerElmts = DOM.bind(this._optionContainer);
  this._optionContainerElmts.previewButton.on('click',function() { self._updatePreview(); });
  
  this._optionContainerElmts.previewButton.html($.i18n('core-buttons/update-preview'));
  $('#or-disable-auto-preview').text($.i18n('core-index-parser/disable-auto-preview'));
  $('#or-import-wiki-base-url').text($.i18n('core-index-parser/wiki-base-url'));
  $('#or-import-parse').text($.i18n('core-index-parser/parse-next'));
  $('#or-import-header').text($.i18n('core-index-parser/lines-header'));
  $('#or-import-load').text($.i18n('core-index-parser/load-at-most'));
  $('#or-import-rows2').text($.i18n('core-index-parser/rows-data'));
  $('#or-import-parseCell').html($.i18n('core-index-parser/parse-cell'));
  $('#or-import-blankSpanningCells').text($.i18n('core-index-parser/blank-spanning-cells'));
  $('#or-import-includeRawTemplates').text($.i18n('core-index-parser/include-raw-templates'));
  $('#or-import-parseReferences').text($.i18n('core-index-parser/parse-references'));
  $('#or-import-blank').text($.i18n('core-index-parser/store-blank'));
  $('#or-import-null').text($.i18n('core-index-parser/store-nulls'));
  $('#or-import-source').html($.i18n('core-index-parser/store-source'));
  $('#or-import-archive').html($.i18n('core-index-parser/store-archive'));


/*
  this._optionContainerElmts.encodingInput
    .val(this._config.encoding || '')
    .on('click',function() {
      Encoding.selectEncoding($(this), function() {
        self._updatePreview();
      });
    });
*/
  
  var wikiUrl = this._config.wikiUrl.toString();
  if (wikiUrl != null) {
    this._optionContainerElmts.wikiUrlInput[0].value = wikiUrl;
    this._optionContainerElmts.wikiCheckbox.prop("checked", true);
  }

   if (this._config.limit > 0) {
    this._optionContainerElmts.limitCheckbox.prop("checked", true);
    this._optionContainerElmts.limitInput[0].value = this._config.limit.toString();
  }

   if (this._config.headerLines > 0) {
    this._optionContainerElmts.headerLinesCheckbox.prop("checked", true);
    this._optionContainerElmts.headerLinesInput[0].value = this._config.headerLines.toString();
  }

  if (this._config.blankSpanningCells) {
    this._optionContainerElmts.blankSpanningCellsCheckbox.prop("checked", true);
  }

  if (this._config.includeRawTemplates) {
    this._optionContainerElmts.includeRawTemplatesCheckbox.prop("checked", true);
  }

  if (this._config.parseReferences) {
    this._optionContainerElmts.parseReferencesCheckbox.prop("checked", true);
  }

  if (this._config.storeBlankRows) {
    this._optionContainerElmts.storeBlankRowsCheckbox.prop("checked", true);
  }

  if (this._config.guessCellValueTypes) {
    this._optionContainerElmts.guessCellValueTypesCheckbox.prop("checked", true);
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

Refine.WikitextParserUI.prototype._scheduleUpdatePreview = function() {
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

Refine.WikitextParserUI.prototype._updatePreview = function() {
  var self = this;

  this._progressContainer.show();

  this._controller.updateFormatAndOptions(this.getOptions(), function(result) {
    if (result.status === "ok") {
      self._controller.getPreviewData(function(projectData) {
        self._progressContainer.hide();
        var container = self._dataContainer.off().empty();
        if (projectData.rowModel.rows.length === 0) {
           $('<div>').addClass("wikitext-parser-ui-message")
                .text($.i18n('core-index-parser/invalid-wikitext')).appendTo(container);
        } else {
           new Refine.PreviewTable(projectData, container);
        }
      });
    }
  });
};
