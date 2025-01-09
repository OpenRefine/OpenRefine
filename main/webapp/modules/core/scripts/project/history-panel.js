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

function HistoryPanel(div, tabHeader) {
  this._div = div;
  this._tabHeader = tabHeader;
  this.update();
}

HistoryPanel.prototype.resize = function() {
  var body = this._div.find(".history-panel-body");
  var controls = this._div.find(".history-panel-controls");
  var bodyControls = this._div.find(".history-panel-filter");
  var nowDiv = this._div.find(".history-now");

  var bodyPaddings = body.outerHeight(true) - body.height();
  body.height((this._div.height() - controls.outerHeight(true) - bodyControls.outerHeight(true) - bodyPaddings) + "px");
  body[0].scrollTop = 
    nowDiv[0].offsetTop + 
    nowDiv[0].offsetHeight - 
    body[0].offsetHeight;
};

HistoryPanel.prototype.update = function(onDone) {
  var self = this;
  Ajax.chainGetJSON(
    "command/core/get-history?" + $.param({ project: theProject.id }), null,
    function(data) {
      self._data = data;
      self._render();

      if (onDone) {
        onDone();
      }
    }
  );
};

// returns the changes "in the future", i.e. changes that have been undone
// and will be erased by any new operation applied in this state
HistoryPanel.prototype.undoneChanges = function() {
  var self = this;
  return self._data.future;
}

HistoryPanel.prototype._render = function() {
  var self = this;
  this._tabHeader.html($.i18n('core-project/undo-redo')+' <span class="count">' + this._data.past.length + ' / ' + ( this._data.future.length + this._data.past.length ) + '</span>');

  this._div.empty().off().html(DOM.loadHTML("core", "scripts/project/history-panel.html"));

  var elmts = DOM.bind(this._div);
  
  elmts.or_proj_undo.html($.i18n('core-project/undo-history'));
  elmts.or_proj_mistakes.html($.i18n('core-project/mistakes'));
  elmts.or_proj_learnMore.html($.i18n('core-project/learn-more'));
  elmts.applyLink.html($.i18n('core-project/apply'));
  elmts.extractLink.html($.i18n('core-project/extract'));
  elmts.or_proj_mistakes.html($.i18n('core-project/mistakes'));
  elmts.or_proj_filter.html($.i18n('core-project/filter'));

  var renderEntry = function(container, index, entry, lastDoneID, past) {
    var a = $(DOM.loadHTML("core", "scripts/project/history-entry.html")).appendTo(container);
    if (lastDoneID >= 0) {
      a.attr("href", "javascript:{}")
      .on('click',function(evt) {
        return self._onClickHistoryEntry(evt, entry, lastDoneID);
      })
      .on('mouseover',function() {
        if (past) {
          elmts.pastHighlightDiv.show().height(elmts.pastDiv.height() - this.offsetTop - this.offsetHeight);
        } else {
          elmts.futureHighlightDiv.show().height(this.offsetTop + this.offsetHeight);
        }
      })
      .on('mouseout',function() {
        if (past) {
          elmts.pastHighlightDiv.hide();
        } else {
          elmts.futureHighlightDiv.hide();
        }
      });
    }

    a[0].childNodes[0].appendChild(document.createTextNode(index + "."));
    a[0].childNodes[1].appendChild(document.createTextNode(entry.description));

    return a;
  };

  if (this._data.past.length > 0 || this._data.future.length > 0) {
    if (!this._data.past.length) {
      renderEntry(elmts.nowDiv, 0, {
        description: "Create project"
      }, 0, true);
    } else {
      renderEntry(elmts.pastDiv, 0, {
        description: "Create project"
      }, 0, true);

      for (var i = 0; i < this._data.past.length - 1; i++) {
        var entry = this._data.past[i];
        renderEntry(elmts.pastDiv, i + 1, entry, entry.id, true);
      }

      renderEntry(elmts.nowDiv, this._data.past.length, this._data.past[this._data.past.length - 1], -1);
    }

    if (this._data.future.length) {
      for (var i = 0; i < this._data.future.length; i++) {
        var entry = this._data.future[i];
        renderEntry(elmts.futureDiv, this._data.past.length + i + 1, entry, entry.id, false);
      }
    }

    elmts.helpDiv.hide();

    elmts.filterInput.on("keyup change input",function() {
      var filter = jQueryTrim(this.value.toLowerCase());
      if (filter.length === 0) {
        elmts.bodyDiv.find(".history-entry").removeClass("filtered-out");
      } else {
        elmts.bodyDiv.find(".history-entry").each(function() {
          var text = this.childNodes[1].firstChild.nodeValue;
          if (text.toLowerCase().indexOf(filter) >= 0) {
            $(this).removeClass("filtered-out");
          } else {
            $(this).addClass("filtered-out");
          }
        });
      }
    });
  } else {
    elmts.bodyDiv.hide();
    elmts.bodyControlsDiv.hide();
  }

  elmts.extractLink.on('click',function() { self._extractOperations(); });
  elmts.applyLink.on('click',function() { self._showApplyOperationsDialog(); });

  this.resize();
};

HistoryPanel.prototype._onClickHistoryEntry = function(evt, entry, lastDoneID) {
  var self = this;

  Refine.postCoreProcess(
      "undo-redo",
      { lastDoneID: lastDoneID },
      null,
      { everythingChanged: true, warnAgainstHistoryErasure: false }
  );
};

HistoryPanel.prototype._extractOperations = function() {
  var self = this;
  $.getJSON(
      "command/core/get-operations?" + $.param({ project: theProject.id }), 
      null,
      function(data) {
        if ("entries" in data) {
          self._showExtractOperationsDialog(data);
        }
      },
      "jsonp"
  );
};

HistoryPanel.prototype._showExtractOperationsDialog = function(json) {
  new ExtractOperationsDialog(json);
};

HistoryPanel.prototype._showApplyOperationsDialog = function() {
  new ApplyOperationsDialog();
};

