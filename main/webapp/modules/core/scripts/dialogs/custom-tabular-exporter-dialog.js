/*

Copyright 2010, Google Inc.
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

function CustomTabularExporterDialog(options) {
  options = options || {
    format: 'tsv',
    lineSeparator: '\n',
    separator: '\t',
    encoding: 'UTF-8',
    outputColumnHeaders: true,
    outputBlankRows: false,
    xlsx: false,
    columns: null,
    quoteAll: false
  };
  
  this._columnOptionMap = {};
  this._createDialog(options);
}

CustomTabularExporterDialog.formats = {
  'csv': {
    extension: 'csv'
  },
  'tsv': {
    extension: 'tsv'
  },
  '*sv': {
    extension: 'txt'
  },
  'html': {
    extension: 'html'
  },
  'xls': {
    extension: 'xls'
  },
  'xlsx': {
    extension: 'xlsx'
  }
};

CustomTabularExporterDialog.uploadTargets = [];

CustomTabularExporterDialog.prototype._createDialog = function(options) {
  var self = this;
  
  this._dialog = $(DOM.loadHTML("core", "scripts/dialogs/custom-tabular-exporter-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._level = DialogSystem.showDialog(this._dialog);
  
  if (CustomTabularExporterDialog.uploadTargets.length === 0) {
    this._elmts.uploadTabHeader.remove();
    this._elmts.uploadTabBody.remove();
  }
  
  this._elmts.dialogHeader.html($.i18n('core-dialogs/custom-tab-exp'));
  this._elmts.or_dialog_content.html($.i18n('core-dialogs/content'));
  this._elmts.or_dialog_download.html($.i18n('core-dialogs/download'));
  this._elmts.or_dialog_upload.html($.i18n('core-dialogs/upload'));
  this._elmts.or_dialog_optCode.html($.i18n('core-dialogs/opt-code'));
  this._elmts.or_dialog_selAndOrd.html($.i18n('core-dialogs/sel-and-ord'));
  this._elmts.or_dialog_optFor.html($.i18n('core-dialogs/opt-for')+" ");
  this._elmts.or_dialog_forReconCell.html($.i18n('core-dialogs/for-recon-cell'));
  this._elmts.or_dialog_matchedName.html($.i18n('core-dialogs/match-ent-name'));
  this._elmts.or_dialog_cellCont.html($.i18n('core-dialogs/cell-content'));
  this._elmts.or_dialog_matchedId.html($.i18n('core-dialogs/match-ent-id'));
  this._elmts.or_dialog_linkMatch.html($.i18n('core-dialogs/link-match'));
  this._elmts.or_dialog_outNotUnMatch.html($.i18n('core-dialogs/out-not-unmatch'));
  this._elmts.or_dialog_dateIso.html($.i18n('core-dialogs/date-iso'));
  this._elmts.or_dialog_shortFormat.html($.i18n('core-dialogs/short-format'));
  this._elmts.or_dialog_mediumFormat.html($.i18n('core-dialogs/medium-format'));
  this._elmts.or_dialog_longFormat.html($.i18n('core-dialogs/long-format'));
  this._elmts.or_dialog_fullFormat.html($.i18n('core-dialogs/full-format'));
  this._elmts.or_dialog_custom.html($.i18n('core-dialogs/custom'));
  this._elmts.or_dialog_help.html($.i18n('core-dialogs/help'));
  this._elmts.or_dialog_localTime.html($.i18n('core-dialogs/local-time'));
  this._elmts.or_dialog_omitTime.html($.i18n('core-dialogs/omit-time'));
  this._elmts.selectAllButton.html($.i18n('core-buttons/select-all'));
  this._elmts.deselectAllButton.html($.i18n('core-buttons/deselect-all'));
  this._elmts.or_dialog_outColHeader.html($.i18n('core-dialogs/out-col-header'));
  this._elmts.or_dialog_outEmptyRow.html($.i18n('core-dialogs/out-empty-row'));
  this._elmts.or_dialog_ignoreFacets.html($.i18n('core-dialogs/ignore-facets'));
  this._elmts.or_dialog_lineFormat.html($.i18n('core-dialogs/line-based'));
  this._elmts.or_dialog_otherFormat.html($.i18n('core-dialogs/other-format'));
  this._elmts.or_dialog_tsv.html($.i18n('core-dialogs/tsv'));
  this._elmts.or_dialog_csv.html($.i18n('core-dialogs/csv'));
  this._elmts.or_dialog_customSep.html($.i18n('core-dialogs/custom-separator'));
  this._elmts.or_dialog_excel.html($.i18n('core-dialogs/excel'));
  this._elmts.or_dialog_excelXml.html($.i18n('core-dialogs/excel-xml'));
  this._elmts.or_dialog_htmlTable.html($.i18n('core-dialogs/html-table'));
  this._elmts.or_dialog_lineSep.html($.i18n('core-dialogs/line-sep'));
  this._elmts.or_dialog_charEnc.html($.i18n('core-dialogs/char-enc'));
  this._elmts.or_dialog_quoteAll.html($.i18n('core-dialogs/lb-formats-quotation'));
  this._elmts.downloadPreviewButton.html($.i18n('core-buttons/preview'));
  this._elmts.downloadButton.html($.i18n('core-buttons/download'));
  this._elmts.or_dialog_uploadTo.html($.i18n('core-dialogs/upload-to'));
  this._elmts.uploadButton.html($.i18n('core-buttons/upload'));
  this._elmts.or_dialog_jsonText.html($.i18n('core-dialogs/json-text'));
  this._elmts.applyOptionCodeButton.html($.i18n('core-buttons/apply'));
  this._elmts.cancelButton.html($.i18n('core-buttons/cancel'));
 
  $("#custom-tabular-exporter-tabs-content").css("display", "");
  $("#custom-tabular-exporter-tabs-download").css("display", "");
  $("#custom-tabular-exporter-tabs-upload").css("display", "");
  $("#custom-tabular-exporter-tabs-code").css("display", "");
  $("#custom-tabular-exporter-tabs").tabs();
  
  /*
   * Populate column list.
   */
  for (var i = 0; i < theProject.columnModel.columns.length; i++) {
    var column = theProject.columnModel.columns[i];
    var name = column.name;
    var div = $('<div>')
        .addClass("custom-tabular-exporter-dialog-column")
        .attr("column", name)
        .appendTo(this._elmts.columnList);
    
    $('<input>')
      .attr('type', 'checkbox')
      .prop('checked', true)
      .appendTo(div);
    $('<span>')
      .text(name)
      .appendTo(div);
    
    this._columnOptionMap[name] = {
      name: name,
      reconSettings: {
        output: 'entity-name',
        blankUnmatchedCells: false,
        linkToEntityPages: true
      },
      dateSettings: {
        format: 'iso-8601',
        custom: '',
        useLocalTimeZone: false,
        omitTime: false
      }
    };
  }
  this._elmts.columnList.sortable(
      {
        update: function () {
          self._updateOptionCode();
        }
      }
  );
  
  /*
   * Populate upload targets.
   */
  if (CustomTabularExporterDialog.uploadTargets.length > 0) {
    var table = this._elmts.uploadTargetTable[0];
    for (var i = 0; i < CustomTabularExporterDialog.uploadTargets.length; i++) {
      var id = 'custom-exporter-upload-' + Math.round(Math.random() * 1000000);
      var target = CustomTabularExporterDialog.uploadTargets[i];
      var tr = table.insertRow(table.rows.length);
      
      var td0 = $(tr.insertCell(0))
        .attr('width', '1');
      var input = $('<input>')
        .attr('id', id)
        .attr('type', 'radio')
        .attr('name', 'custom-tabular-exporter-upload-format')
        .val(target.id)
        .appendTo(td0);
      if (i === 0) {
        input.prop('checked', true);
      }
      
      $('<label>')
        .attr('for', id)
        .text(target.label)
        .appendTo(
          $(tr.insertCell(1))
          .attr('width', '100%'));
    }
    
    this._elmts.uploadButton.on('click',function() { self._upload(); });
  }
  
  /*
   * Hook up event handlers.
   */
  this._elmts.encodingInput.on('click',function(evt) {
    Encoding.selectEncoding($(this), function() {
      self._updateOptionCode();
    });
  });
  
  this._elmts.columnList.find('.custom-tabular-exporter-dialog-column').on('click',function() {
    self._elmts.columnList.find('.custom-tabular-exporter-dialog-column').removeClass('selected');
    $(this).addClass('selected');
    self._selectColumn(this.getAttribute('column'));
    self._updateOptionCode();
  });
  this._elmts.selectAllButton.on('click',function() {
    self._elmts.columnList.find('input[type="checkbox"]').prop('checked', true);
    self._updateOptionCode();
  });
  this._elmts.deselectAllButton.on('click',function() {
    self._elmts.columnList.find('input[type="checkbox"]').prop('checked', false);
    self._updateOptionCode();
  });
  
  this._elmts.columnOptionPane.find('input').on('change', function() {
    self._updateCurrentColumnOptions();
  });
  $('#custom-tabular-exporter-tabs-content').find('input').on('change', function() {
    self._updateOptionCode();
  });
  $('#custom-tabular-exporter-tabs-download').find('input').on('change', function() {
    self._updateOptionCode();
  });
  
  this._elmts.applyOptionCodeButton.on('click',function(evt) { self._applyOptionCode(); });
  this._elmts.cancelButton.on('click',function() { self._dismiss(); });
  this._elmts.downloadButton.on('click',function() { self._download(); });
  this._elmts.downloadPreviewButton.on('click',function(evt) { self._previewDownload(); });
  
  this._configureUIFromOptionCode(options);
  this._updateOptionCode();
};

CustomTabularExporterDialog.prototype._configureUIFromOptionCode = function(options) {
  var escapeJavascriptString = function(s) {
    return JSON.stringify([s]).replace(/^\[\s*"/, '').replace(/"\s*]$/, '');
  };
  this._dialog.find('input[name="custom-tabular-exporter-download-format"][value="' + options.format + '"]').prop('checked', true);
  this._elmts.separatorInput[0].value = escapeJavascriptString(options.separator || ',');
  this._elmts.lineSeparatorInput[0].value = escapeJavascriptString(options.lineSeparator || '\n');
  this._elmts.encodingInput[0].value = options.encoding;
  this._elmts.outputColumnHeadersCheckbox.prop('checked', options.outputColumnHeaders);
  this._elmts.outputEmptyRowsCheckbox.prop('checked', options.outputBlankRows);
  this._elmts.quoteAllCheckbox.prop('checked', options.quoteAll);

  if (options.columns !== null) {
    var self = this;
    this._elmts.columnList.find('.custom-tabular-exporter-dialog-column input[type="checkbox"]').prop('checked', false);
    $.each(options.columns, function() {
      var name = this.name;
      self._columnOptionMap[name] = this;
      self._elmts.columnList.find('.custom-tabular-exporter-dialog-column').each(function() {
        if (this.getAttribute('column') == name) {
          $(this).find('input[type="checkbox"]').prop('checked', true);
        }
      });
    });
  }
  this._elmts.columnList.find('.custom-tabular-exporter-dialog-column').first().addClass('selected');
  this._selectColumn(theProject.columnModel.columns[0].name);
};

CustomTabularExporterDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

CustomTabularExporterDialog.prototype._previewDownload = function() {
  this._postExport(true);
};

CustomTabularExporterDialog.prototype._download = function() {
  this._postExport(false);
  this._dismiss();
};

CustomTabularExporterDialog.prototype._postExport = function(preview) {
  var exportAllRowsCheckbox = this._elmts.exportAllRowsCheckbox[0].checked;
  var options = this._getOptionCode();
  
  var format = options.format;
  var encoding = options.encoding;
  
  delete options.format;
  delete options.encoding;
  if (preview) {
    options.limit = 10;
  }
  
  var ext = CustomTabularExporterDialog.formats[format].extension;
  var form = ExporterManager.prepareExportRowsForm(format, !exportAllRowsCheckbox, ext);

  if (preview) {
    $(form).attr("target", "refine-export");
  }
  $('<input />')
  .attr("name", "options")
  .val(JSON.stringify(options))
  .appendTo(form);
  if (encoding) {
    $('<input />')
    .attr("name", "encoding")
    .val(encoding)
    .appendTo(form);
  }
  $('<input />')
  .attr("name", "preview")
  .val(preview)
  .appendTo(form);

    document.body.appendChild(form);
    if (preview) {
      window.open(" ", "refine-export");
    }
    form.submit();
    document.body.removeChild(form);
}

CustomTabularExporterDialog.prototype._selectColumn = function(columnName) {
  this._elmts.columnNameSpan.text(columnName);
  
  var columnOptions = this._columnOptionMap[columnName];
  
  this._elmts.columnOptionPane.find('input[name="custom-tabular-exporter-recon"][value="' +
    columnOptions.reconSettings.output + '"]').prop('checked', true);
  this._elmts.reconBlankUnmatchedCheckbox.prop('checked', columnOptions.reconSettings.blankUnmatchedCells);
  this._elmts.reconLinkCheckbox.prop('checked', columnOptions.reconSettings.linkToEntityPages);
  
  this._elmts.columnOptionPane.find('input[name="custom-tabular-exporter-date"][value="' +
    columnOptions.dateSettings.format + '"]').prop('checked', true);
  this._elmts.dateCustomInput.val(columnOptions.dateSettings.custom);
  this._elmts.dateLocalTimeZoneCheckbox.prop('checked', columnOptions.dateSettings.useLocalTimeZone);
  this._elmts.omitTimeCheckbox.prop('checked', columnOptions.dateSettings.omitTime);
};

CustomTabularExporterDialog.prototype._updateCurrentColumnOptions = function() {
  var selectedColumnName = this._elmts.columnList
    .find('.custom-tabular-exporter-dialog-column.selected').attr('column');
  
  var columnOptions = this._columnOptionMap[selectedColumnName];
  
  columnOptions.reconSettings.output = this._elmts.columnOptionPane
    .find('input[name="custom-tabular-exporter-recon"]:checked').val();
  columnOptions.reconSettings.blankUnmatchedCells = this._elmts.reconBlankUnmatchedCheckbox[0].checked;
  columnOptions.reconSettings.linkToEntityPages = this._elmts.reconLinkCheckbox[0].checked;
  
  columnOptions.dateSettings.format = this._elmts.columnOptionPane
    .find('input[name="custom-tabular-exporter-date"]:checked').val();
  columnOptions.dateSettings.custom = this._elmts.dateCustomInput.val();
  columnOptions.dateSettings.useLocalTimeZone = this._elmts.dateLocalTimeZoneCheckbox[0].checked;
  columnOptions.dateSettings.omitTime = this._elmts.omitTimeCheckbox[0].checked;
};

CustomTabularExporterDialog.prototype._updateOptionCode = function() {
  this._elmts.optionCodeInput.val(JSON.stringify(this._getOptionCode(), null, 2));
};

CustomTabularExporterDialog.prototype._applyOptionCode = function() {
  var s = this._elmts.optionCodeInput.val();
  try {
    var json = JSON.parse(s);
    this._configureUIFromOptionCode(json);
    
    alert($.i18n('core-dialogs/opt-code-applied'));
  } catch (e) {
    alert($.i18n('core-dialogs/error-apply-code')+': ' + e);
  }
};

CustomTabularExporterDialog.prototype._getOptionCode = function() {
  var options = {
    format: this._dialog.find('input[name="custom-tabular-exporter-download-format"]:checked').val()
  };
  var unescapeJavascriptString = function(s) {
    try {
      return JSON.parse('"' + s + '"');
    } catch (e) {
      // We're not handling the case where the user doesn't escape double quotation marks.
      return s;
    }
  };
  if (options.format == 'tsv' || options.format == 'csv' || options.format == '*sv') {
    if (options.format == 'tsv') {
      options.separator = '\t';
    } else if (options.format == 'csv') {
      options.separator = ',';
    } else {
      options.separator = unescapeJavascriptString(this._elmts.separatorInput.val());
    }
    options.lineSeparator = unescapeJavascriptString(this._elmts.lineSeparatorInput.val());
    options.encoding = this._elmts.encodingInput.val();
    options.quoteAll = this._elmts.quoteAllCheckbox[0].checked;
  }
  options.outputColumnHeaders = this._elmts.outputColumnHeadersCheckbox[0].checked;
  options.outputBlankRows = this._elmts.outputEmptyRowsCheckbox[0].checked;
  
  options.columns = [];
  
  var self = this;
  this._elmts.columnList.find('.custom-tabular-exporter-dialog-column').each(function() {
    if ($(this).find('input[type="checkbox"]')[0].checked) {
      var name = this.getAttribute('column');
      var fullColumnOptions = self._columnOptionMap[name];
      var columnOptions = {
        name: name,
        reconSettings: $.extend({}, fullColumnOptions.reconSettings),
        dateSettings: $.extend({}, fullColumnOptions.dateSettings)
      };
      if (columnOptions.dateSettings.format != 'custom') {
        delete columnOptions.dateSettings.custom;
      }
      options.columns.push(columnOptions);
    }
  });
  
  return options;
};

CustomTabularExporterDialog.prototype._upload = function() {
  var id = this._dialog.find('input[name="custom-tabular-exporter-upload-format"]:checked').val();
  for (var i = 0; i < CustomTabularExporterDialog.uploadTargets.length; i++) {
    var target = CustomTabularExporterDialog.uploadTargets[i];
    if (id == target.id) {
      var self = this;
      var options = this._getOptionCode();
      options.format = id;
      
      target.handler(
        options,
        this._elmts.exportAllRowsCheckbox[0].checked,
        function() {
          self._dismiss();
        });
      return;
    }
  }
};
