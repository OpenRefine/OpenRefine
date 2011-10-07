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
    window.alert('Please specify a record path first.');
  }
};

Refine.XmlParserUI.prototype.getOptions = function() {
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
  options.includeFileSources = this._optionContainerElmts.includeFileSourcesCheckbox[0].checked;

  return options;
};

Refine.XmlParserUI.prototype._initialize = function() {
  var self = this;

  this._optionContainer.unbind().empty().html(
      DOM.loadHTML("core", "scripts/index/parser-interfaces/xml-parser-ui.html"));
  this._optionContainerElmts = DOM.bind(this._optionContainer);
  this._optionContainerElmts.previewButton.click(function() { self._updatePreview(); });

  if (this._config.limit > 0) {
    this._optionContainerElmts.limitCheckbox.attr("checked", "checked");
    this._optionContainerElmts.limitInput[0].value = this._config.limit.toString();
  }
  if (this._config.includeFileSources) {
    this._optionContainerElmts.includeFileSourcesCheckbox.attr("checked", "checked");
  }
  this._optionContainerElmts.pickRecordElementsButton.click(function() {
    self._showPickRecordElementsUI();
  });

  var onChange = function() {
    self._scheduleUpdatePreview();
  };
  this._optionContainer.find("input").bind("change", onChange);
  this._optionContainer.find("select").bind("change", onChange);
};

Refine.XmlParserUI.prototype._showPickRecordElementsUI = function() {
  var self = this;

  this._dataContainer.unbind().empty().html(
      DOM.loadHTML("core", "scripts/index/parser-interfaces/xml-parser-select-ui.html"));

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
      .bind('mouseover', function(evt) {
        if (hittest(evt)) {
          elmts.domContainer.find('.highlight').removeClass('highlight');
          div.addClass('highlight');
        }
      })
      .bind('mouseout', function(evt) {
        div.removeClass('highlight');
      })
      .click(function(evt) {
        if (hittest(evt)) {
          self._setRecordPath(path);
        }
      });
    }
  };
  if (this._config.dom) {
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

  this._progressContainer.show();

  var options = this.getOptions();
  // for preview, we need exact text, so it's easier to show where the columns are split
  options.guessCellValueTypes = false;

  this._controller.updateFormatAndOptions(options, function(result) {
    if (result.status == "ok") {
      self._controller.getPreviewData(function(projectData) {
        self._progressContainer.hide();

        new Refine.PreviewTable(projectData, self._dataContainer.unbind().empty());
      }, 20);
    }
  });
};
