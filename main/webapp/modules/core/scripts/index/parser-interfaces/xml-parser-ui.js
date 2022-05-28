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

Refine.XmlParserUI = function(controller, jobID, job, format, config,
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
  this._showPickRecordElementsUI();
};
Refine.DefaultImportingController.parserUIs.XmlParserUI = Refine.XmlParserUI;

Refine.XmlParserUI.prototype.dispose = function() {
  if (this._timerID !== null) {
    window.clearTimeout(this._timerID);
    this._timerID = null;
  }
};

Refine.XmlParserUI.prototype.confirmReadyToCreateProject = function() {
  if ((this._config.recordPath) && this._config.recordPath.length > 0) {
    return true;
  } else {
    window.alert($.i18n('core-index-import/warning-record-path'));
  }
};

Refine.XmlParserUI.prototype.getOptions = function() {
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

Refine.XmlParserUI.prototype._initialize = function() {
  var self = this;

  this._optionContainer.off().empty().html(
      DOM.loadHTML("core", "scripts/index/parser-interfaces/xml-parser-ui.html"));
  this._optionContainerElmts = DOM.bind(this._optionContainer);
  this._optionContainerElmts.previewButton.on('click',function() { self._updatePreview(); });

  this._optionContainerElmts.pickRecordElementsButton.html($.i18n('core-buttons/pick-record'));
  this._optionContainerElmts.previewButton.html($.i18n('core-buttons/update-preview'));
  $('#or-disable-auto-preview').text($.i18n('core-index-parser/disable-auto-preview'));
  $('#or-import-rows').text($.i18n('core-index-parser/rows-data'));
  $('#or-import-load').text($.i18n('core-index-parser/load-at-most'));
  $('#or-import-preserve').text($.i18n('core-index-parser/preserve-empty'));
  $('#or-import-trim').html($.i18n('core-index-parser/trim'));
  $('#or-import-parseCell').html($.i18n('core-index-parser/parse-cell'));
  $('#or-import-store').html($.i18n('core-index-parser/store-source'));
  $('#or-import-archive').html($.i18n('core-index-parser/store-archive'));
  
  if (this._config.limit > 0) {
    this._optionContainerElmts.limitCheckbox.prop("checked", true);
    this._optionContainerElmts.limitInput[0].value = this._config.limit.toString();
  }
  if (this._config.trimStrings) {
    this._optionContainerElmts.trimStringsCheckbox.prop("checked", false);
  }
  if (this._config.guessCellValueTypes) {
    this._optionContainerElmts.guessCellValueTypesCheckbox.prop("checked", false);
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
    self._config.recordPath = undefined;
    self._showPickRecordElementsUI();
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

Refine.XmlParserUI.prototype._showPickRecordElementsUI = function() {
  var self = this;

  this._dataContainer.off().empty().html(
      DOM.loadHTML("core", "scripts/index/parser-interfaces/xml-parser-select-ui.html"));

  $('#or-import-clickXML').text($.i18n('core-index-parser/click-xml'));
  
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
  var renderNode = function(node, container, parentPath) {
    if (node.t) {
      $('<div>').html(textAsHtml(node.t)).appendTo(container);
    } else {
      var qname = node.n;
      if (node.p) {
        qname = node.p + ':' + qname;
      }

      var t = qname;
      if (node.a) {
        t += ' ' + $.map(node.a, function(attr) {
          return attr.n + '="' + attr.v + '"';
        }).join(' ');
      }
      if (node.ns) {
        t += ' ' + $.map(node.ns, function(ns) {
          return 'xmlns' + ((ns.p) ? (':' + ns.p) : '') + '="' + ns.uri + '"';
        }).join(' ');
      }

      var path = [].concat(parentPath);
      path.push(qname);

      var div = $('<div>').addClass('elmt').appendTo(container);
      var hasSelectableChildren = false;
      var hotspot;
      if (node.c) {
        if (node.c.length == 1 && (node.c[0].t)) {
          $('<span>').html('&lt;' + t + '&gt;' + textAsHtml(node.c[0].t) + '&lt;/' + qname + '&gt;').appendTo(div);
        } else {
          $('<div>').text('<' + t + '>').appendTo(div);

          var divChildren = $('<div>').addClass('children').appendTo(div);
          $.each(node.c, function() {
            renderNode(this, divChildren, path);
          });

          $('<div>').text('</' + qname + '>').appendTo(div);
          hasSelectableChildren = true;
        }
      } else {
        $('<span>').text('<' + t + ' />').appendTo(div);
      }

      var hittest = function(evt) {
        if (hasSelectableChildren) {
          if (evt.target !== div[0] &&
              (evt.target.className == 'elmt' || evt.target.parentNode !== div[0])) {
            return false;
          }
        }
        return true;
      };
      div.attr('title', '/' + path.join('/'))
      .on('mouseover', function(evt) {
        if (hittest(evt)) {
          elmts.domContainer.find('.highlight').removeClass('highlight');
          div.addClass('highlight');
        }
      })
      .on('mouseout', function(evt) {
        div.removeClass('highlight');
      })
      .on('click',function(evt) {
        if (hittest(evt)) {
          self._setRecordPath(path);
        }
      });
    }
  };
  if (this._config.dom) {
    this._config.defaultRecordPath=[];
    renderNode(this._config.dom, elmts.domContainer, []);
  }
};

Refine.XmlParserUI.prototype._scheduleUpdatePreview = function() {
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

Refine.XmlParserUI.prototype._setRecordPath = function(path) {
  this._config.recordPath = path;
  this._updatePreview();
};

Refine.XmlParserUI.prototype._updatePreview = function() {
  var self = this;
  
  if(!this._config.recordPath){
    window.alert($.i18n('core-index-import/warning-record-path'));
    return;
  }

  this._progressContainer.show();

  var options = this.getOptions();
  // for preview, we need exact text, so it's easier to show where the columns are split
  options.guessCellValueTypes = false;

  this._controller.updateFormatAndOptions(options, function(result) {
    if (result.status == "ok") {
      self._controller.getPreviewData(function(projectData) {
        self._progressContainer.hide();

    	  if (projectData["rowModel"]["rows"].length == 0) {
		      alert($.i18n('core-index-import/load-xml-rows-error'));
	      }	 
        new Refine.PreviewTable(projectData, self._dataContainer.off().empty());
      }, 20);
    }
  });
};
