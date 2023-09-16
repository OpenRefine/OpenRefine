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

Refine.JsonParserUI = function(controller, jobID, job, format, config,
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
  this._showPickRecordNodesUI();
};
Refine.DefaultImportingController.parserUIs.JsonParserUI = Refine.JsonParserUI;

Refine.JsonParserUI.prototype.dispose = function() {
  if (this._timerID !== null) {
    window.clearTimeout(this._timerID);
    this._timerID = null;
  }
};

Refine.JsonParserUI.prototype.confirmReadyToCreateProject = function() {
  if ((this._config.recordPath) && this._config.recordPath.length > 0) {
    return true;
  } else {
    window.alert($.i18n('core-index-import/warning-record-path'));
  }
};

Refine.JsonParserUI.prototype.getOptions = function() {
  if(!this._config.recordPath){
    this._setRecordPath(this._config.defaultRecordPath);
  }
  var options = {
    recordPath: this._config.recordPath
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
  
  options.trimStrings = this._optionContainerElmts.trimStringsCheckbox[0].checked;
  options.guessCellValueTypes = this._optionContainerElmts.guessCellValueTypesCheckbox[0].checked;
  options.storeEmptyStrings = this._optionContainerElmts.storeEmptyStringsCheckbox[0].checked;

  options.includeFileSources = this._optionContainerElmts.includeFileSourcesCheckbox[0].checked;
  options.includeArchiveFileName = this._optionContainerElmts.includeArchiveFileCheckbox[0].checked;

  options.disableAutoPreview = this._optionContainerElmts.disableAutoPreviewCheckbox[0].checked;

  return options;
};

Refine.JsonParserUI.prototype._initialize = function() {
  var self = this;

  this._optionContainer.off().empty().html(
      DOM.loadHTML("core", "scripts/index/parser-interfaces/json-parser-ui.html"));
  this._optionContainerElmts = DOM.bind(this._optionContainer);
  this._optionContainerElmts.previewButton.on('click',function() { self._updatePreview(); });

  this._optionContainerElmts.pickRecordElementsButton.text($.i18n('core-index-import/warning-record-path'));
  this._optionContainerElmts.previewButton.html($.i18n('core-buttons/update-preview'));
  $('#or-disable-auto-preview').text($.i18n('core-index-parser/disable-auto-preview'));
  $('#or-import-load').text($.i18n('core-index-parser/load-at-most'));
  $('#or-import-rows').text($.i18n('core-index-parser/rows-data'));
  $('#or-import-preserve').text($.i18n('core-index-parser/preserve-empty'));
  $('#or-import-trim').html($.i18n('core-index-parser/trim'));
  $('#or-import-parseCell').html($.i18n('core-index-parser/parse-cell'));
  $('#or-import-source').html($.i18n('core-index-parser/store-source'));
  $('#or-import-archive').html($.i18n('core-index-parser/store-archive'));
  $('#or-import-jsonParser').text($.i18n('core-index-parser/json-parser'));
  
  if (this._config.limit > 0) {
    this._optionContainerElmts.limitCheckbox.prop("checked", true);
    this._optionContainerElmts.limitInput[0].value = this._config.limit.toString();
  }
  if (this._config.trimStrings) {
    this._optionContainerElmts.trimStringsCheckbox.prop('checked', false);
  }
  if (this._config.guessCellValueTypes) {
    this._optionContainerElmts.guessCellValueTypesCheckbox.prop('checked', false);
  }
  if (this._config.storeEmptyStrings) {
    this._optionContainerElmts.storeEmptyStringsCheckbox.prop("checked", true);
  }
  if (this._config.includeFileSources) {
    this._optionContainerElmts.includeFileSourcesCheckbox.prop("checked", true);
  }
  if (this._config.includeArchiveFileName) {
    this._optionContainerElmts.includeArchiveFileCheckbox.prop("checked", true);
  }
  this._optionContainerElmts.pickRecordElementsButton.on('click',function() {
    self._showPickRecordNodesUI();
  });

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

Refine.JsonParserUI.prototype._showPickRecordNodesUI = function() {
  var ANONYMOUS_NODE_NAME = '_';
  var self = this;

  this._dataContainer.off().empty().html(
      DOM.loadHTML("core", "scripts/index/parser-interfaces/json-parser-select-ui.html"));

  var elmts = DOM.bind(this._dataContainer);

  var escapeElmt = $('<span>');
  var escapeHtml = function(s) {
    escapeElmt.empty().text(s);
    return escapeElmt.html();
  };
  var textAsHtml = function(s) {
    s = s.length <= 200 ? s : (s.substring(0, 200) + ' ...');
    return '<span class="text">' + escapeHtml(s) + '</span>';
  };
  var hittest = function(evt, elmt) {
    var a = $(evt.target).closest('.node');
    return a.length > 0 && a[0] == elmt[0];
  };
  var registerEvents = function(elmt, path) {
    elmt.on('mouseover', function(evt) {
      if (hittest(evt, elmt)) {
        elmts.domContainer.find('.highlight').removeClass('highlight');
        elmt.addClass('highlight');
      }
    })
    .on('mouseout', function(evt) {
      elmt.removeClass('highlight');
    })
    .on('click',function(evt) {
      if (hittest(evt, elmt)) {
        self._setRecordPath(path);
      }
    });
  };
  var renderArray = function(a, container, parentPath) {
    $('<span>').addClass('punctuation').text('[').appendTo(container);

    var parentPath2 = [].concat(parentPath);
    parentPath2.push(ANONYMOUS_NODE_NAME);

    var elementNode = null;
    for (var i = 0; i < a.length; i++) {
      if (elementNode !== null) {
        $('<span>').addClass('punctuation').text(',').appendTo(elementNode);
      }
      var dataCy = "element" + i;
      elementNode = $('<div>').addClass('node').addClass('indented').attr('data-cy', dataCy).appendTo(container);

      renderNode(a[i], elementNode, parentPath2);
    }

    $('<span>').addClass('punctuation').text(']').appendTo(container);
  };
  var renderObject = function(o, container, parentPath) {
    $('<span>').addClass('punctuation').text('{').appendTo(container);

    var elementNode = null;
    for (var key in o) {
      if (o.hasOwnProperty(key)) {
        if (elementNode !== null) {
          $('<span>').addClass('punctuation').text(',').appendTo(elementNode);
        }
        elementNode = $('<div>').addClass('node').addClass('indented').appendTo(container);

        $('<span>').text(key).addClass('field-name').appendTo(elementNode);
        $('<span>').text(': ').addClass('punctuation').appendTo(elementNode);

        var parentPath2 = [].concat(parentPath);
        parentPath2.push(key);

        renderNode(o[key], elementNode, parentPath2);
      }
    }
    $('<span>').addClass('punctuation').text('}').appendTo(container);

    registerEvents(container, parentPath);
  };
  var renderNode = function(node, container, parentPath) {
    if (node === null) {
      $('<span>').addClass('literal').text('null').appendTo(container);
    } else {
      if ($.isPlainObject(node)) {
        renderObject(node, container, parentPath);
      } else if ($.isArray(node)) {
        renderArray(node, container, parentPath);
      } else {
        $('<span>').addClass('literal').text(node.toString()).appendTo(container);
        registerEvents(container, parentPath);
      }
    }
  };
  rootNode = $('<div>').addClass('node').addClass('indented').appendTo(elmts.domContainer);
  this._config.defaultRecordPath=[ANONYMOUS_NODE_NAME];
  renderNode(this._config.dom, rootNode, [ANONYMOUS_NODE_NAME]);
};

Refine.JsonParserUI.prototype._scheduleUpdatePreview = function() {
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

Refine.JsonParserUI.prototype._setRecordPath = function(path) {
  this._config.recordPath = path;
  this._updatePreview();
};

Refine.JsonParserUI.prototype._updatePreview = function() {
  var self = this;

  this._progressContainer.show();

  var options = this.getOptions();
  // for preview, we need exact text, so it's easier to show where the columns are split
  options.guessCellValueTypes = false;

  this._controller.updateFormatAndOptions(options, function(result) {
    if (result.status == "ok") {
      self._controller.getPreviewData(function(projectData) {
        self._progressContainer.hide();
        
    if (projectData["rowModel"]["rows"].length == 0) {
		alert($.i18n('core-index-import/load-json-rows-error'));
	}

        new Refine.PreviewTable(projectData, self._dataContainer.off().empty());
      }, 100);
     } else {
	   self._progressContainer.hide();
       }
  });
};
