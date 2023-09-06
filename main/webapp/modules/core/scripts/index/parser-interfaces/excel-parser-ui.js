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

Refine.ExcelParserUI = function(controller, jobID, job, format, config,
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
Refine.DefaultImportingController.parserUIs.ExcelParserUI = Refine.ExcelParserUI;

Refine.ExcelParserUI.prototype.dispose = function() {
  if (this._timerID !== null) {
    window.clearTimeout(this._timerID);
    this._timerID = null;
  }
};

Refine.ExcelParserUI.prototype.confirmReadyToCreateProject = function() {
  return true; // always ready
};

Refine.ExcelParserUI.prototype.getOptions = function() {
  var options = {
    sheets: []
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
  
  var self = this;
  
  this._optionContainerElmts.sheetRecordContainer.find('input').each(function() {
    if (this.checked) {
        options.sheets.push(self._config.sheetRecords[parseInt(this.getAttribute('index'))]);
    }
  });

  if (this._optionContainerElmts.ignoreCheckbox[0].checked) {
    options.ignoreLines = parseIntDefault(this._optionContainerElmts.ignoreInput[0].value, -1);
  } else {
    options.ignoreLines = -1;
  }
  if (this._optionContainerElmts.headerLinesCheckbox[0].checked) {
    options.headerLines = parseIntDefault(this._optionContainerElmts.headerLinesInput[0].value, 0);
  } else {
    options.headerLines = 0;
  }
  if (this._optionContainerElmts.skipCheckbox[0].checked) {
    options.skipDataLines = parseIntDefault(this._optionContainerElmts.skipInput[0].value, 0);
  } else {
    options.skipDataLines = 0;
  }
  if (this._optionContainerElmts.limitCheckbox[0].checked) {
    options.limit = parseIntDefault(this._optionContainerElmts.limitInput[0].value, -1);
  } else {
    options.limit = -1;
  }
  options.storeBlankRows = this._optionContainerElmts.storeBlankRowsCheckbox[0].checked;
  options.storeBlankCellsAsNulls = this._optionContainerElmts.storeBlankCellsAsNullsCheckbox[0].checked;
  options.includeFileSources = this._optionContainerElmts.includeFileSourcesCheckbox[0].checked;
  options.includeArchiveFileName = this._optionContainerElmts.includeArchiveFileCheckbox[0].checked;
  options.forceText = this._optionContainerElmts.forceTextCheckbox[0].checked;

  options.disableAutoPreview = this._optionContainerElmts.disableAutoPreviewCheckbox[0].checked;

  return options;
};

Refine.ExcelParserUI.prototype._initialize = function() {
  var self = this;

  this._optionContainer.off().empty().html(
      DOM.loadHTML("core", "scripts/index/parser-interfaces/excel-parser-ui.html"));
  this._optionContainerElmts = DOM.bind(this._optionContainer);
  this._optionContainerElmts.previewButton.on('click',function() { self._updatePreview(); });  
  this._optionContainerElmts.previewButton.html($.i18n('core-buttons/update-preview'));
  this._optionContainerElmts.selectAllButton.on('click',function() { self._selectAll(); }); 
  this._optionContainerElmts.selectAllButton.html($.i18n('core-buttons/select-all'));
  this._optionContainerElmts.deselectAllButton.on('click',function() { self._deselectAll(); }); 
  this._optionContainerElmts.deselectAllButton.html($.i18n('core-buttons/deselect-all'));
  $('#or-disable-auto-preview').text($.i18n('core-index-parser/disable-auto-preview'));
  $('#or-import-worksheet').text($.i18n('core-index-import/import-worksheet'));
  $('#or-import-ignore').text($.i18n('core-index-parser/ignore-first'));
  $('#or-import-lines').text($.i18n('core-index-parser/lines-beg'));
  $('#or-import-parse').text($.i18n('core-index-parser/parse-next'));
  $('#or-import-header').text($.i18n('core-index-parser/lines-header'));
  $('#or-import-discard').text($.i18n('core-index-parser/discard-initial'));
  $('#or-import-rows').text($.i18n('core-index-parser/rows-data'));
  $('#or-import-load').text($.i18n('core-index-parser/load-at-most'));
  $('#or-import-rows2').text($.i18n('core-index-parser/rows-data'));
  $('#or-import-blank').text($.i18n('core-index-parser/store-blank'));
  $('#or-import-null').text($.i18n('core-index-parser/store-nulls'));
  $('#or-import-source').html($.i18n('core-index-parser/store-source'));
  $('#or-import-archive').html($.i18n('core-index-parser/store-archive'));
  $('#or-force-text').html($.i18n('core-index-parser/force-text'));

  var sheetTable = this._optionContainerElmts.sheetRecordContainer[0];
  $.each(this._config.sheetRecords, function(i, v) {
    var id = 'core-excel-worksheet-' + Math.round(Math.random() * 1000000);
    var tr = sheetTable.insertRow(sheetTable.rows.length);
    var td0 = $(tr.insertCell(0)).attr('width', '1%');
    var checkbox = $('<input>')
    .attr('id', id)
    .attr('type', 'checkbox')
    .attr('class', 'core-excel-worksheet')
    .attr('index', i)
    .appendTo(td0);
    checkbox.prop('checked', this.selected);

    $('<label>')
      .attr('for', id)
      .text(this.name)
      .appendTo(tr.insertCell(1));
    
    $('<label>')
      .attr('for', id)
      .text(this.rows + ' rows')
      .appendTo(tr.insertCell(2));
  });

  if (this._config.ignoreLines > 0) {
    this._optionContainerElmts.ignoreCheckbox.prop("checked", true);
    this._optionContainerElmts.ignoreInput[0].value = this._config.ignoreLines.toString();
  }
  if (this._config.headerLines > 0) {
    this._optionContainerElmts.headerLinesCheckbox.prop("checked", true);
    this._optionContainerElmts.headerLinesInput[0].value = this._config.headerLines.toString();
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
  if (this._config.forceText) {
    this._optionContainerElmts.forceTextCheckbox.prop("checked", true);
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

Refine.ExcelParserUI.prototype._scheduleUpdatePreview = function() {
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

Refine.ExcelParserUI.prototype._updatePreview = function() {
  var self = this;

  this._progressContainer.show();

  this._controller.updateFormatAndOptions(this.getOptions(), function(result) {
    if (result.status == "ok") {
      self._controller.getPreviewData(function(projectData) {
        self._progressContainer.hide();

        new Refine.PreviewTable(projectData, self._dataContainer.off().empty());
      });
    }
  }, function() {
	  self._progressContainer.hide();
  });
};

Refine.ExcelParserUI.prototype._selectAll = function() {
  var self = this;

  $(".core-excel-worksheet").each(function(index, value){
    $(value).prop('checked', true);
  });
  
  self._scheduleUpdatePreview();
}

Refine.ExcelParserUI.prototype._deselectAll = function() {
  var self = this;

  $(".core-excel-worksheet").each(function(index, value){
    $(value).prop('checked', false);
  });

  self._scheduleUpdatePreview();
}
