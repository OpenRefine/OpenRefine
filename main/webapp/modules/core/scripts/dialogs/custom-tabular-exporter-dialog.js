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
    columns: null
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
      .attr('checked', 'checked')
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
  this._elmts.columnList.sortable({});
  
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
        .attr('value', target.id)
        .appendTo(td0);
      if (i === 0) {
        input.attr('checked', 'checked');
      }
      
      $('<label>')
        .attr('for', id)
        .text(target.label)
        .appendTo(
          $(tr.insertCell(1))
          .attr('width', '100%'));
    }
    
    this._elmts.uploadButton.click(function() { self._upload(); });
  }
  
  /*
   * Hook up event handlers.
   */
  this._elmts.encodingInput.click(function(evt) {
    Encoding.selectEncoding($(this), function() {
      self._updateOptionCode();
    });
  });
  
  this._elmts.columnList.find('.custom-tabular-exporter-dialog-column').click(function() {
    self._elmts.columnList.find('.custom-tabular-exporter-dialog-column').removeClass('selected');
    $(this).addClass('selected');
    self._selectColumn(this.getAttribute('column'));
    self._updateOptionCode();
  });
  this._elmts.selectAllButton.click(function() {
    self._elmts.columnList.find('input[type="checkbox"]').attr('checked', true);
    self._updateOptionCode();
  });
  this._elmts.deselectAllButton.click(function() {
    self._elmts.columnList.find('input[type="checkbox"]').attr('checked', false);
    self._updateOptionCode();
  });
  
  this._elmts.columnOptionPane.find('input').bind('change', function() {
    self._updateCurrentColumnOptions();
  });
  $('#custom-tabular-exporter-tabs-content').find('input').bind('change', function() {
    self._updateOptionCode();
  });
  $('#custom-tabular-exporter-tabs-download').find('input').bind('change', function() {
    self._updateOptionCode();
  });
  
  this._elmts.applyOptionCodeButton.click(function(evt) { self._applyOptionCode(); });
  this._elmts.cancelButton.click(function() { self._dismiss(); });
  this._elmts.downloadButton.click(function() { self._download(); });
  this._elmts.downloadPreviewButton.click(function(evt) { self._previewDownload(); });
  
  this._configureUIFromOptionCode(options);
  this._updateOptionCode();
};

CustomTabularExporterDialog.prototype._configureUIFromOptionCode = function(options) {
  var escapeJavascriptString = function(s) {
    return JSON.stringify([s]).replace(/^\[\s*"/, '').replace(/"\s*]$/, '');
  };
  this._dialog.find('input[name="custom-tabular-exporter-download-format"][value="' + options.format + '"]').attr('checked', 'checked');
  this._elmts.separatorInput[0].value = escapeJavascriptString(options.separator || ',');
  this._elmts.lineSeparatorInput[0].value = escapeJavascriptString(options.lineSeparator || '\n');
  this._elmts.encodingInput[0].value = options.encoding;
  this._elmts.outputColumnHeadersCheckbox.attr('checked', (options.outputColumnHeaders) ? 'checked' : '');
  this._elmts.outputEmptyRowsCheckbox.attr('checked', (options.outputBlankRows) ? 'checked' : '');
  
  if (options.columns !== null) {
    var self = this;
    this._elmts.columnList.find('.custom-tabular-exporter-dialog-column input[type="checkbox"]').attr('checked', false);
    $.each(options.columns, function() {
      var name = this.name;
      self._columnOptionMap[name] = this;
      self._elmts.columnList.find('.custom-tabular-exporter-dialog-column').each(function() {
        if (this.getAttribute('column') == name) {
          $(this).find('input[type="checkbox"]').attr('checked', 'checked');
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
  $('<input />')
  .attr("name", "options")
  .attr("value", JSON.stringify(options))
  .appendTo(form);
  if (encoding) {
    $('<input />')
    .attr("name", "encoding")
    .attr("value", encoding)
    .appendTo(form);
  }
  if (!preview) {
    $('<input />')
    .attr("name", "contentType")
    .attr("value", "application/x-unknown") // force download
    .appendTo(form);
  }
  
  document.body.appendChild(form);

  window.open("about:blank", "refine-export");
  form.submit();

  document.body.removeChild(form);
};

CustomTabularExporterDialog.prototype._selectColumn = function(columnName) {
  this._elmts.columnNameSpan.text(columnName);
  
  var columnOptions = this._columnOptionMap[columnName];
  
  this._elmts.columnOptionPane.find('input[name="custom-tabular-exporter-recon"][value="' +
    columnOptions.reconSettings.output + '"]').attr('checked', 'checked');
  this._elmts.reconBlankUnmatchedCheckbox.attr('checked', columnOptions.reconSettings.blankUnmatchedCells ? 'checked' : '');
  this._elmts.reconLinkCheckbox.attr('checked', columnOptions.reconSettings.linkToEntityPages ? 'checked' : '');
  
  this._elmts.columnOptionPane.find('input[name="custom-tabular-exporter-date"][value="' +
    columnOptions.dateSettings.format + '"]').attr('checked', 'checked');
  this._elmts.dateCustomInput.val(columnOptions.dateSettings.custom);
  this._elmts.dateLocalTimeZoneCheckbox.attr('checked', columnOptions.dateSettings.useLocalTimeZone ? 'checked' : '');
  this._elmts.omitTimeCheckbox.attr('checked', columnOptions.dateSettings.omitTime ? 'checked' : '');
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
    
    alert('Option code successfully applied.');
  } catch (e) {
    alert('Error applying option code: ' + e);
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
