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

Refine.DefaultImportingController.prototype._showFileSelectionPanel = function() {
  var self = this;

  this._selectedMap = {};
  this._prepareFileSelectionPanel();

  this._fileSelectionPanelElmts.nextButton.on('click',function() {
    self._commitFileSelection();
  });
  this._renderFileSelectionPanel();
  this._createProjectUI.showCustomPanel(this._fileSelectionPanel);
};

Refine.DefaultImportingController.prototype._disposeFileSelectionPanel = function() {
  if (this._fileSelectionPanelResizer) {
    $(window).off("resize", this._fileSelectionPanelResizer);
  }
  this._fileSelectionPanel.off().empty();
};

Refine.DefaultImportingController.prototype._prepareFileSelectionPanel = function() {
  var self = this; 

  this._fileSelectionPanel.off().empty().html(
      DOM.loadHTML("core", "scripts/index/default-importing-controller/file-selection-panel.html"));

  this._fileSelectionPanelElmts = DOM.bind(this._fileSelectionPanel);
  
  $('#or-import-select').text($.i18n('core-index-import/select-file'));
  $('#or-import-singleProject').text($.i18n('core-index-import/single-project'));
  $('#or-import-severalFile').text($.i18n('core-index-import/several-file'));
  $('#or-import-selExt').text($.i18n('core-index-import/sel-by-extension'));
  $('#or-import-regex').text($.i18n('core-index-import/sel-by-regex'));
  
  this._fileSelectionPanelElmts.startOverButton.html($.i18n('core-buttons/startover'));
  this._fileSelectionPanelElmts.nextButton.html($.i18n('core-buttons/conf-pars-opt'));
  this._fileSelectionPanelElmts.selectAllButton.text($.i18n('core-buttons/select-all'));
  this._fileSelectionPanelElmts.deselectAllButton.text($.i18n('core-buttons/deselect-all'));
  this._fileSelectionPanelElmts.selectRegexButton.text($.i18n('core-buttons/select'));
  this._fileSelectionPanelElmts.deselectRegexButton.text($.i18n('core-buttons/deselect'));
  
  this._fileSelectionPanelElmts.startOverButton.on('click',function() {
    self._startOver();
  });
};

Refine.DefaultImportingController.prototype._renderFileSelectionPanel = function() {
  this._renderFileSelectionPanelFileTable();
  this._renderFileSelectionPanelControlPanel();
};

Refine.DefaultImportingController.prototype._renderFileSelectionPanelFileTable = function() {
  var self = this;
  var files = this._job.config.retrievalRecord.files;

  this._fileSelectionPanelElmts.filePanel.empty();

  var fileTable = $('<table><tr><th>'+$.i18n('core-index-import/import')+'</th><th>'+$.i18n('core-index-import/name')+'</th><th>'+$.i18n('core-index-import/mime-type')+'</th><th>'+$.i18n('core-index-import/format')+'</th><th>'+$.i18n('core-index-import/size')+'</th></tr></table>')
    .appendTo(this._fileSelectionPanelElmts.filePanel)[0];

  var round = function(n) {
    return Math.round(n * 10) / 10;
  };
  var renderSize = function(fileRecord) {
    var bytes = fileRecord.size;
    var gigabytes = bytes / 1073741824;
    var megabytes = bytes / 1048576;
    var kilobytes = bytes / 1024;
    if (gigabytes > 1) {
      return round(gigabytes) + " GB";
    } else if (megabytes > 1) {
      return round(megabytes) + " MB";
    } else if (kilobytes > 1) {
      return round(kilobytes) + " KB";
    } else {
      return fileRecord.size + " bytes";
    }
  };
  var renderFile = function(fileRecord, index) {
    var id = "import-file-selection-" + Math.round(Math.random() * 1000000);
    var tr = fileTable.insertRow(fileTable.rows.length);
    $(tr).addClass((index % 2 === 0) ? 'even' : 'odd');
    
    var createLabeledCell = function(className) {
      var td = $('<td>').appendTo(tr);
      if (className) {
        td.addClass(className);
      }
      return $('<label>').attr('for', id).appendTo(td);
    };
    
    var checkbox = $('<input>')
    .attr("id", id)
    .attr("type", "checkbox")
    .attr("index", index)
    .appendTo(createLabeledCell())
    .on('click',function() {
      var fileRecord = files[index];
      if (this.checked) {
        self._selectedMap[fileRecord.location] = fileRecord;
      } else {
        delete self._selectedMap[fileRecord.location];
      }
      self._updateFileSelectionSummary();
    });
    if (fileRecord.selected) {
      // Initial selection determined on server side.
      checkbox.prop('checked', true);
      self._selectedMap[fileRecord.location] = fileRecord;
    }

    createLabeledCell("default-importing-file-selection-filename").text(fileRecord.fileName);
    createLabeledCell().text(fileRecord.declaredMimeType || fileRecord.mimeType || "unknown");
    createLabeledCell().text(fileRecord.format || "unknown");
    createLabeledCell().text(renderSize(fileRecord));
  };

  for (var i = 0; i < files.length; i++) {
    renderFile(files[i], i);
  }
};

Refine.DefaultImportingController.prototype._renderFileSelectionPanelControlPanel = function() {
  var self = this;
  var files = this._job.config.retrievalRecord.files;

  this._fileSelectionPanelElmts.extensionContainer.empty();
  this._fileSelectionPanelElmts.selectAllButton.off().on('click',function(evt) {
    for (var i = 0; i < files.length; i++) {
      var fileRecord = files[i];
      self._selectedMap[fileRecord.location] = fileRecord;
    }
    self._fileSelectionPanelElmts.filePanel.find("input").prop('checked', true);
    self._updateFileSelectionSummary();
  });
  this._fileSelectionPanelElmts.deselectAllButton.off().on('click',function(evt) {
    self._selectedMap = {};
    self._fileSelectionPanelElmts.filePanel.find("input").prop('checked', false);
    self._updateFileSelectionSummary();
  });

  var table = $('<table></table>').appendTo(this._fileSelectionPanelElmts.extensionContainer)[0];

  var renderExtension = function(extension) {
    var tr = table.insertRow(table.rows.length);
    $('<td>').css('width','25%').css('text-align','left').text(extension.extension).appendTo(tr);
    $('<td>').css('width','25%').css('text-align','left').text($.i18n('core-index-import/file-count', extension.count)).appendTo(tr);
    var actionTD = $('<td>').css('text-align','right');
    actionTD.appendTo(tr);
    $('<button>')
    .text($.i18n('core-buttons/select'))
    .addClass("button")
    .appendTo(actionTD)
    .on('click',function() {
      for (var i = 0; i < files.length; i++) {
        var file = files[i];
        if (!(file.location in self._selectedMap)) {
          if (file.fileName.endsWith(extension.extension)) {
            self._selectedMap[file.location] = file;
            self._fileSelectionPanelElmts.filePanel
            .find("input[index='" + i + "']")
            .prop('checked', true);
          }
        }
      }
      self._updateFileSelectionSummary();
    });
    $('<button>')
    .text($.i18n('core-buttons/deselect'))
    .addClass("button")
    .css('margin-left','5px')
    .appendTo(actionTD)
    .on('click',function() {
      for (var i = 0; i < files.length; i++) {
        var file = files[i];
        if (file.location in self._selectedMap) {
          if (file.fileName.endsWith(extension.extension)) {
            delete self._selectedMap[file.location];
            self._fileSelectionPanelElmts.filePanel
            .find("input[index='" + i + "']")
            .prop('checked', false);
          }
        }
      }
      self._updateFileSelectionSummary();
    });
  };
  for (var i = 0; i < this._extensions.length; i++) {
    renderExtension(this._extensions[i]);
  }

  this._updateFileSelectionSummary();

  this._fileSelectionPanelElmts.regexInput.off().on("keyup change input",function() {
    var count = 0;
    var elmts = self._fileSelectionPanelElmts.filePanel
    .find(".default-importing-file-selection-filename")
    .removeClass("highlighted");
    try {
      var regex = new RegExp(this.value);
      elmts.each(function() {
        if (regex.test($(this).text())) {
          $(this).addClass("highlighted");
          count++;
        }
      });
    } catch (e) {
      // Ignore
    }
      self._fileSelectionPanelElmts.regexSummary.text($.i18n('core-index-import/match-count', count));
  });
  this._fileSelectionPanelElmts.selectRegexButton.off().on('click',function() {
    self._fileSelectionPanelElmts.filePanel
    .find(".default-importing-file-selection-filename")
    .removeClass("highlighted");
    try {
      var regex = new RegExp(self._fileSelectionPanelElmts.regexInput[0].value);
      for (var i = 0; i < files.length; i++) {
        var file = files[i];
        if (!(file.location in self._selectedMap)) {
          if (regex.test(file.fileName)) {
            self._selectedMap[file.location] = file;
            self._fileSelectionPanelElmts.filePanel
            .find("input[index='" + i + "']")
            .prop('checked', true);
          }
        }
      }
      self._updateFileSelectionSummary();
    } catch (e) {
      // Ignore
    }
  });
  this._fileSelectionPanelElmts.deselectRegexButton.off().on('click',function() {
    self._fileSelectionPanelElmts.filePanel
    .find(".default-importing-file-selection-filename")
    .removeClass("highlighted");
    try {
      var regex = new RegExp(self._fileSelectionPanelElmts.regexInput[0].value);
      for (var i = 0; i < files.length; i++) {
        var file = files[i];
        if (file.location in self._selectedMap) {
          if (regex.test(file.fileName)) {
            delete self._selectedMap[file.location];
            self._fileSelectionPanelElmts.filePanel
            .find("input[index='" + i + "']")
            .prop('checked', false);
          }
        }
      }
      self._updateFileSelectionSummary();
    } catch (e) {
      // Ignore
    }
  });
};

Refine.DefaultImportingController.prototype._updateFileSelectionSummary = function() {
  var fileSelection = [];
  var files = this._job.config.retrievalRecord.files;
  for (var i = 0; i < files.length; i++) {
    if (files[i].location in this._selectedMap) {
      fileSelection.push(i);
    }
  }
  this._job.config.fileSelection = fileSelection;
  this._fileSelectionPanelElmts.summary.text($.i18n('core-index-import/files-selected', fileSelection.length, files.length));
};

Refine.DefaultImportingController.prototype._commitFileSelection = function() {
  if (this._job.config.fileSelection.length === 0) {
    alert();
    return;
  }

  var self = this;
  var dismissBusy = DialogSystem.showBusy($.i18n('core-index-import/inspecting-files'));
  Refine.wrapCSRF(function(token) {
    $.post(
        "command/core/importing-controller?" + $.param({
        "controller": "core/default-importing-controller",
        "jobID": self._jobID,
        "subCommand": "update-file-selection",
        "csrf_token": token
        }),
        {
        "fileSelection" : JSON.stringify(self._job.config.fileSelection)
        },
        function(data) {
        dismissBusy();

        if (!(data)) {
            self._createProjectUI.showImportJobError($.i18n('core-index-import/unknown-err'));
        } else if (data.code == "error" || !("job" in data)) {
            self._createProjectUI.showImportJobError((data.message) ? ($.i18n('core-index-import/error')+ ' ' + data.message) : $.i18n('core-index-import/unknown-err'));
        } else {
            // Different files might be selected. We start over again.
            delete self._parserOptions;

            self._job = data.job;
            self._showParsingPanel(true);
        }
        },
        "json"
    );
  });
};
