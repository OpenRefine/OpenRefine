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
    "/command/core/get-history?" + $.param({ project: theProject.id }), null,
    function(data) {
      self._data = data;
      self._render();

      if (onDone) {
        onDone();
      }
    }
  );
};

HistoryPanel.prototype._render = function() {
  var self = this;

  this._tabHeader.html('Undo / Redo <span class="count">' + this._data.past.length + '</span>');

  this._div.empty().unbind().html(DOM.loadHTML("core", "scripts/project/history-panel.html"));

  var elmts = DOM.bind(this._div);

  var renderEntry = function(container, index, entry, lastDoneID, past) {
    var a = $(DOM.loadHTML("core", "scripts/project/history-entry.html")).appendTo(container);
    if (lastDoneID >= 0) {
      a.attr("href", "javascript:{}")
      .click(function(evt) {
        return self._onClickHistoryEntry(evt, entry, lastDoneID);
      })
      .mouseover(function() {
        if (past) {
          elmts.pastHighlightDiv.show().height(elmts.pastDiv.height() - this.offsetTop - this.offsetHeight);
        } else {
          elmts.futureHighlightDiv.show().height(this.offsetTop + this.offsetHeight);
        }
      })
      .mouseout(function() {
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

    elmts.filterInput.keyup(function() {
      var filter = $.trim(this.value.toLowerCase());
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

  elmts.extractLink.click(function() { self._extractOperations(); });
  elmts.applyLink.click(function() { self._showApplyOperationsDialog(); });

  this.resize();
};

HistoryPanel.prototype._onClickHistoryEntry = function(evt, entry, lastDoneID) {
  var self = this;

  Refine.postCoreProcess(
      "undo-redo",
      { lastDoneID: lastDoneID },
      null,
      { everythingChanged: true }
  );
};

HistoryPanel.prototype._extractOperations = function() {
  var self = this;
  $.getJSON(
      "/command/core/get-operations?" + $.param({ project: theProject.id }), 
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
  var self = this;
  var frame = $(DOM.loadHTML("core", "scripts/project/history-extract-dialog.html"));
  var elmts = DOM.bind(frame);

  var entryTable = elmts.entryTable[0];
  var createEntry = function(entry) {
    var tr = entryTable.insertRow(entryTable.rows.length);
    var td0 = tr.insertCell(0);
    var td1 = tr.insertCell(1);
    td0.width = "1%";

    if ("operation" in entry) {
      entry.selected = true;

      $('<input type="checkbox" checked="true" />').appendTo(td0).click(function() {
        entry.selected = !entry.selected;
        updateJson();
      });

      $('<span>').text(entry.operation.description).appendTo(td1);
    } else {
      $('<span>').text(entry.description).css("color", "#888").appendTo(td1);
    }
  };
  for (var i = 0; i < json.entries.length; i++) {
    createEntry(json.entries[i]);
  }

  var updateJson = function() {
    var a = [];
    for (var i = 0; i < json.entries.length; i++) {
      var entry = json.entries[i];
      if ("operation" in entry && entry.selected) {
        a.push(entry.operation);
      }
    }
    elmts.textarea.text(JSON.stringify(a, null, 2));
  };
  updateJson();

  elmts.closeButton.click(function() { DialogSystem.dismissUntil(level - 1); });
  elmts.selectAllButton.click(function() {
    for (var i = 0; i < json.entries.length; i++) {
      json.entries[i].selected = true;
    }

    frame.find('input[type="checkbox"]').attr("checked", "true");
    updateJson();
  });
  elmts.unselectAllButton.click(function() {
    for (var i = 0; i < json.entries.length; i++) {
      json.entries[i].selected = false;
    }

    frame.find('input[type="checkbox"]').removeAttr("checked");
    updateJson();
  });

  var level = DialogSystem.showDialog(frame);

  elmts.textarea[0].select();
};

HistoryPanel.prototype._showApplyOperationsDialog = function() {
  var self = this;
  var frame = $(DOM.loadHTML("core", "scripts/project/history-apply-dialog.html"));
  var elmts = DOM.bind(frame);

  var fixJson = function(json) {
    json = json.trim();
    if (!json.startsWith("[")) {
      json = "[" + json;
    }
    if (!json.endsWith("]")) {
      json = json + "]";
    }

    return json.replace(/\}\s*\,\s*\]/g, "} ]").replace(/\}\s*\{/g, "}, {");
  };

  elmts.applyButton.click(function() {
    var json;

    try {
      json = elmts.textarea[0].value;
      json = fixJson(json);
      json = JSON.parse(json);
    } catch (e) {
      alert("The JSON you pasted is invalid.");
      return;
    }

    Refine.postCoreProcess(
        "apply-operations",
        {},
        { operations: JSON.stringify(json) },
        { everythingChanged: true },
        {
          onDone: function(o) {
            if (o.code == "pending") {
              // Something might have already been done and so it's good to update
              Refine.update({ everythingChanged: true });
            }
          }
        }
    );

    DialogSystem.dismissUntil(level - 1);
  });

  elmts.cancelButton.click(function() {
    DialogSystem.dismissUntil(level - 1);
  });

  var level = DialogSystem.showDialog(frame);

  elmts.textarea.focus();
};
