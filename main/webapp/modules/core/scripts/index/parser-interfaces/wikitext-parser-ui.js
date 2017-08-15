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
  if (this._optionContainerElmts.limitCheckbox[0].checked) {
    options.limit = parseIntDefault(this._optionContainerElmts.limitInput[0].value, -1);
  } else {
    options.limit = -1;
  }
  options.storeBlankRows = this._optionContainerElmts.storeBlankRowsCheckbox[0].checked;
  options.blankSpanningCells = this._optionContainerElmts.blankSpanningCellsCheckbox[0].checked;

  options.guessCellValueTypes = this._optionContainerElmts.guessCellValueTypesCheckbox[0].checked;

  options.storeBlankCellsAsNulls = this._optionContainerElmts.storeBlankCellsAsNullsCheckbox[0].checked;
  options.includeFileSources = this._optionContainerElmts.includeFileSourcesCheckbox[0].checked;

  return options;
};

Refine.WikitextParserUI.prototype._initialize = function() {
  var self = this;

  this._optionContainer.unbind().empty().html(
      DOM.loadHTML("core", "scripts/index/parser-interfaces/wikitext-parser-ui.html"));
  this._optionContainerElmts = DOM.bind(this._optionContainer);
  this._optionContainerElmts.previewButton.click(function() { self._updatePreview(); });
  
  this._optionContainerElmts.previewButton.html($.i18n._('core-buttons')["update-preview"]);
  
  $('#or-import-load').text($.i18n._('core-index-parser')["load-at-most"]);
  $('#or-import-rows2').text($.i18n._('core-index-parser')["rows-data"]);
  $('#or-import-parseCell').html($.i18n._('core-index-parser')["parse-cell"]);
  $('#or-import-blankSpanningCells').text($.i18n._('core-index-parser')["blank-spanning-cells"]);
  $('#or-import-blank').text($.i18n._('core-index-parser')["store-blank"]);
  $('#or-import-null').text($.i18n._('core-index-parser')["store-nulls"]);
  $('#or-import-source').html($.i18n._('core-index-parser')["store-source"]);

/*
  this._optionContainerElmts.encodingInput
    .attr('value', this._config.encoding || '')
    .click(function() {
      Encoding.selectEncoding($(this), function() {
        self._updatePreview();
      });
    });
*/
  console.log(this._config);
  
   if (this._config.limit > 0) {
    this._optionContainerElmts.limitCheckbox.prop("checked", true);
    this._optionContainerElmts.limitInput[0].value = this._config.limit.toString();
  }

  if (this._config.blankSpanningCells) {
    this._optionContainerElmts.blankSpanningCellsCheckbox.prop("checked", true);
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

  var onChange = function() {
    self._scheduleUpdatePreview();
  };
  this._optionContainer.find("input").bind("change", onChange);
  this._optionContainer.find("select").bind("change", onChange);
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
    if (result.status == "ok") {
      self._controller.getPreviewData(function(projectData) {
        self._progressContainer.hide();

        new Refine.PreviewTable(projectData, self._dataContainer.unbind().empty());
      });
    }
  });
};
