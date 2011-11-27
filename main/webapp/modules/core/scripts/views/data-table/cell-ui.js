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

function DataTableCellUI(dataTableView, cell, rowIndex, cellIndex, td) {
  this._dataTableView = dataTableView;
  this._cell = cell;
  this._rowIndex = rowIndex;
  this._cellIndex = cellIndex;
  this._td = td;

  this._render();
}

DataTableCellUI.prototype._render = function() {
  var self = this;
  var cell = this._cell;

  var divContent = $('<div/>')
  .addClass("data-table-cell-content");

  var editLink = $('<a href="javascript:{}">&nbsp;</a>')
  .addClass("data-table-cell-edit")
  .attr("title", "Edit this cell")
  .appendTo(divContent)
  .click(function() { self._startEdit(this); });

  $(this._td).empty()
  .unbind()
  .mouseenter(function() { editLink.css("visibility", "visible"); })
  .mouseleave(function() { editLink.css("visibility", "hidden"); });

  if (!cell || ("v" in cell && cell.v === null)) {
    $('<span>').html("&nbsp;").appendTo(divContent);
  } else if ("e" in cell) {
    $('<span>').addClass("data-table-error").text(cell.e).appendTo(divContent);
  } else if (!("r" in cell) || !cell.r) {
    if (typeof cell.v !== "string" || "t" in cell) {
      if (typeof cell.v == "number") {
        divContent.addClass("data-table-cell-content-numeric");
      }
      $('<span>')
      .addClass("data-table-value-nonstring")
      .text(cell.v)
      .appendTo(divContent);
    } else if (URL.looksLikeUrl(cell.v)) {
      $('<a>')
      .text(cell.v)
      .attr("href", cell.v)
      .attr("target", "_blank")
      .appendTo(divContent);
    } else {
      $('<span>')
      .text(cell.v)
      .appendTo(divContent);
    }
  } else {
    var r = cell.r;
    var service = (r.service) ? ReconciliationManager.getServiceFromUrl(r.service) : null;

    if (r.j == "new") {
      $('<span>').text(cell.v).appendTo(divContent);
      $('<span>').addClass("data-table-recon-new").text("new").appendTo(divContent);

      $('<a href="javascript:{}"></a>')
      .text("Choose new match")
      .addClass("data-table-recon-action")
      .appendTo(divContent).click(function(evt) {
        self._doRematch();
      });
    } else if (r.j == "matched" && "m" in r && r.m !== null) {
      var match = cell.r.m;
      var a = $('<a></a>')
      .text(match.name)
      .attr("target", "_blank")
      .appendTo(divContent);

      if (service && (service.view) && (service.view.url)) {
        a.attr("href", service.view.url.replace("{{id}}", match.id));
      } else if (ReconciliationManager.isFreebaseIdOrMid(r.identifierSpace)) {
        a.attr("href", "http://www.freebase.com/view" + match.id);
      }

      $('<span> </span>').appendTo(divContent);
      $('<a href="javascript:{}"></a>')
      .text("Choose new match")
      .addClass("data-table-recon-action")
      .appendTo(divContent)
      .click(function(evt) {
        self._doRematch();
      });
    } else {
      $('<span>').text(cell.v).appendTo(divContent);

      if (this._dataTableView._showRecon) {
        var ul = $('<div></div>').addClass("data-table-recon-candidates").appendTo(divContent);
        if ("c" in r && r.c.length > 0) {
          var candidates = r.c;
          var renderCandidate = function(candidate, index) {
            var li = $('<div></div>').addClass("data-table-recon-candidate").appendTo(ul);

            $('<a href="javascript:{}">&nbsp;</a>')
            .addClass("data-table-recon-match-similar")
            .attr("title", "Match this topic to this and all identical cells")
            .appendTo(li).click(function(evt) {
              self._doMatchTopicToSimilarCells(candidate);
            });

            $('<a href="javascript:{}">&nbsp;</a>')
            .addClass("data-table-recon-match")
            .attr("title", "Match this topic to this cell")
            .appendTo(li).click(function(evt) {
              self._doMatchTopicToOneCell(candidate);
            });

            var a = $('<a></a>')
            .addClass("data-table-recon-topic")
            .attr("target", "_blank")
            .text(candidate.name)
            .appendTo(li);

            if ((service) && (service.view) && (service.view.url)) {
              a.attr("href", service.view.url.replace("{{id}}", candidate.id));
            } else if (ReconciliationManager.isFreebaseIdOrMid(r.identifierSpace)) {
              a.attr("href", "http://www.freebase.com/view" + candidate.id);
            }

            var preview = null;
            if ((service) && (service.preview)) {
              preview = service.preview;
            } else if (ReconciliationManager.isFreebaseIdOrMid(r.identifierSpace)) {
              preview = DataTableCellUI.topicBlockPreview;
            }
            if (preview) {
              a.click(function(evt) {
                if (!evt.metaKey && !evt.ctrlKey) {
                  self._previewCandidateTopic(candidate, this, preview);
                  evt.preventDefault();
                  return false;
                }
              });
            }

            var score;
            if (candidate.score < 1) {
              score = Math.round(candidate.score * 1000) / 1000;
            } else {
              score = Math.round(candidate.score);
            }
            $('<span></span>').addClass("data-table-recon-score").text("(" + score + ")").appendTo(li);
          };

          for (var i = 0; i < candidates.length; i++) {
            renderCandidate(candidates[i], i);
          }
        }

        var liNew = $('<div></div>').addClass("data-table-recon-candidate").appendTo(ul);
        $('<a href="javascript:{}">&nbsp;</a>')
        .addClass("data-table-recon-match-similar")
        .attr("title", "Create a new topic for this and all identical cells")
        .appendTo(liNew).click(function(evt) {
          self._doMatchNewTopicToSimilarCells();
        });

        $('<a href="javascript:{}">&nbsp;</a>')
        .addClass("data-table-recon-match")
        .attr("title", "Create a new topic for this cell")
        .appendTo(liNew).click(function(evt) {
          self._doMatchNewTopicToOneCell();
        });

        $('<span>').text("Create new topic").appendTo(liNew);

        var suggestOptions;
        var addSuggest = false;
        if ((service) && (service.suggest) && (service.suggest.entity)) {
          suggestOptions = service.suggest.entity;
          addSuggest = true;
        } else if (ReconciliationManager.isFreebaseIdOrMid(r.identifierSpace)) {
          addSuggest = true;
        }

        var extraChoices = $('<div>').addClass("data-table-recon-extra").appendTo(divContent);
        if (addSuggest) {
          $('<a href="javascript:{}"></a>')
          .click(function(evt) {
            self._searchForMatch(suggestOptions);
            return false;
          })
          .text("Search for match")
          .appendTo(extraChoices);
        }
      }
    }
  }

  divContent.appendTo(this._td);
};

DataTableCellUI.prototype._doRematch = function() {
  this._doJudgment("none");
};

DataTableCellUI.prototype._doMatchNewTopicToOneCell = function() {
  this._doJudgment("new");
};

DataTableCellUI.prototype._doClearOneCell = function() {
  this._postProcessOneCell(
    "recon-clear-one-cell",
    {},
    {
      row: this._rowIndex,
      cell: this._cellIndex
    },
    true
  );
};

DataTableCellUI.prototype._doMatchNewTopicToSimilarCells = function() {
  this._doJudgmentForSimilarCells("new", {}, { shareNewTopics: true }, true);
};

DataTableCellUI.prototype._doClearSimilarCells = function() {
  this._postProcessSeveralCells(
    "recon-clear-similar-cells",
    {},
    {
      columnName: Refine.cellIndexToColumn(this._cellIndex).name,
      similarValue: this._cell.v
    },
    true
  );
};

DataTableCellUI.prototype._doMatchTopicToOneCell = function(candidate) {
  this._doJudgment("matched", {}, {
    id : candidate.id,
    name: candidate.name,
    score: candidate.score,
    types: candidate.types.join(",")
  });
};

DataTableCellUI.prototype._doMatchTopicToSimilarCells = function(candidate) {
  this._doJudgmentForSimilarCells("matched", {}, {
    id : candidate.id,
    name: candidate.name,
    score: candidate.score,
    types: candidate.types.join(",")
  }, true);
};

DataTableCellUI.prototype._doJudgment = function(judgment, params, bodyParams) {
  this._postProcessOneCell(
    "recon-judge-one-cell",
    params || {},
    $.extend(bodyParams || {}, {
      row: this._rowIndex,
      cell: this._cellIndex,
      judgment: judgment,
      identifierSpace: (this._cell.r) ? this._cell.r.identifierSpace : null,
          schemaSpace: (this._cell.r) ? this._cell.r.schemaSpace : null
    }),
    true
  );
};

DataTableCellUI.prototype._doJudgmentForSimilarCells = function(judgment, params, bodyParams) {
  this._postProcessSeveralCells(
    "recon-judge-similar-cells",
    params || {},
    $.extend(bodyParams || {}, {
      columnName: Refine.cellIndexToColumn(this._cellIndex).name,
      similarValue: this._cell.v,
      judgment: judgment,
      identifierSpace: (this._cell.r) ? this._cell.r.identifierSpace : null,
          schemaSpace: (this._cell.r) ? this._cell.r.schemaSpace : null
    }),
    true
  );
};

DataTableCellUI.prototype._searchForMatch = function(suggestOptions) {
  var self = this;
  var frame = $(DOM.loadHTML("core", "scripts/views/data-table/cell-recon-search-for-match.html"));
  var elmts = DOM.bind(frame);

  var level = DialogSystem.showDialog(frame);
  var dismiss = function() {
    DialogSystem.dismissUntil(level - 1);
  };

  elmts.cellTextSpan.text(this._cell.v);

  var match = null;
  var commit = function() {
    if (match !== null) {
      var params = {
        judgment: "matched",
        id: match.id,
        name: match.name,
        types: $.map(match.type, function(elmt) {
          return typeof elmt == "string" ? elmt : elmt.id; 
        }).join(",")
      };

      if (elmts.radioSimilar[0].checked) {
        params.similarValue = self._cell.v;
        params.columnName = Refine.cellIndexToColumn(self._cellIndex).name;

        self._postProcessSeveralCells("recon-judge-similar-cells", {}, params, true);
      } else {
        params.row = self._rowIndex;
        params.cell = self._cellIndex;

        self._postProcessOneCell("recon-judge-one-cell", {}, params, true);
      }

      dismiss();
    }
  };
  var commitNew = function() {
    if (elmts.radioSimilar[0].checked) {
      self._doMatchNewTopicToSimilarCells();
    } else {
      self._doMatchNewTopicToOneCell();
    }
    dismiss();
  };
  var commitClear = function() {
    if (elmts.radioSimilar[0].checked) {
      self._doClearSimilarCells();
    } else {
      self._doClearOneCell();
    }
    dismiss();
  };

  elmts.okButton.click(commit);
  elmts.newButton.click(commitNew);
  elmts.clearButton.click(commitClear);
  elmts.cancelButton.click(dismiss);

  var suggestOptions2 = $.extend({ align: "left" }, suggestOptions || { all_types: true });
  elmts.input
  .attr("value", this._cell.v)
  .suggest(suggestOptions2)
  .bind("fb-select", function(e, data) {
    match = data;
    commit();
  })
  .focus()
  .data("suggest").textchange();
};

DataTableCellUI.prototype._postProcessOneCell = function(command, params, bodyParams, columnStatsChanged) {
  var self = this;

  Refine.postCoreProcess(
    command, 
    params, 
    bodyParams,
    { columnStatsChanged: columnStatsChanged },
    {
      onDone: function(o) {
        if (o.cell.r) {
          o.cell.r = o.pool.recons[o.cell.r];
        }

        self._cell = o.cell;
        self._dataTableView._updateCell(self._rowIndex, self._cellIndex, self._cell);
        self._render();
      }
    }
  );
};

DataTableCellUI.prototype._postProcessSeveralCells = function(command, params, bodyParams, columnStatsChanged) {
  Refine.postCoreProcess(
    command, 
    params, 
    bodyParams,
    { cellsChanged: true, columnStatsChanged: columnStatsChanged }
  );
};

DataTableCellUI.topicBlockPreview = {
  url: 'http://www.freebase.com/widget/topic{{id}}?mode=content&blocks=[{"block":"full_info"},{"block":"article_props"}]',
  width: 430,
  height: 300
};

DataTableCellUI.prototype._previewCandidateTopic = function(candidate, elmt, preview) {
  var self = this;
  var id = candidate.id;
  var url = preview.url.replace("{{id}}", id);

  var fakeMenu = MenuSystem.createMenu();
  fakeMenu
  .width(414)
  .addClass('data-table-topic-popup')
  .html(DOM.loadHTML("core", "scripts/views/data-table/cell-recon-preview-popup-header.html"));

  var iframe = $('<iframe></iframe>')
  .width(preview.width)
  .height(preview.height)
  .attr("src", url)
  .appendTo(fakeMenu);

  MenuSystem.showMenu(fakeMenu, function(){});
  MenuSystem.positionMenuLeftRight(fakeMenu, $(elmt));

  var elmts = DOM.bind(fakeMenu);
  elmts.matchButton.click(function() {
    self._doMatchTopicToOneCell(candidate);
    MenuSystem.dismissAll();
  });
  elmts.matchSimilarButton.click(function() {
    self._doMatchTopicToSimilarCells(candidate);
    MenuSystem.dismissAll();
  });
  elmts.cancelButton.click(function() {
    MenuSystem.dismissAll();
  });
};

DataTableCellUI.prototype._startEdit = function(elmt) {
  self = this;

  var originalContent = !this._cell || ("v" in this._cell && this._cell.v === null) ? "" : this._cell.v;

  var menu = MenuSystem.createMenu().addClass("data-table-cell-editor").width("400px");
  menu.html(DOM.loadHTML("core", "scripts/views/data-table/cell-editor.html"));
  var elmts = DOM.bind(menu);

  MenuSystem.showMenu(menu, function(){});
  MenuSystem.positionMenuLeftRight(menu, $(this._td));

  var commit = function() {
    var type = elmts.typeSelect[0].value;

    var applyOthers = 0;
    if (this === elmts.okallButton[0]) {
      applyOthers = 1;
    }

    var text = elmts.textarea[0].value;
    var value = text;

    if (type == "number") {
      value = parseFloat(text);
      if (isNaN(value)) {
        alert("Not a valid number.");
        return;
      }
    } else if (type == "boolean") {
      value = ("true" == text);
    } else if (type == "date") {
      value = Date.parse(text);
      if (!value) {
        value = DateTimeUtil.parseIso8601DateTime(text);
      }
      if (!value) {
        alert("Not a valid date.");
        return;
      }
      value = value.toString("yyyy-MM-ddTHH:mm:ssZ");
    }

    MenuSystem.dismissAll();

    if (applyOthers) {
      Refine.postCoreProcess(
        "mass-edit",
        {},
        {
          columnName: Refine.cellIndexToColumn(self._cellIndex).name,
          expression: "value",
          edits: JSON.stringify([{
            from: [ originalContent ],
            to: value,
            type: type
          }])
        },
        { cellsChanged: true }
      );
    } else {
      Refine.postCoreProcess(
        "edit-one-cell", 
        {},
        {
          row: self._rowIndex,
          cell: self._cellIndex,
          value: value,
          type: type
        },
        {},
        {
          onDone: function(o) {
            if (o.cell.r) {
              o.cell.r = o.pool.recons[o.cell.r];
            }

            self._cell = o.cell;
            self._dataTableView._updateCell(self._rowIndex, self._cellIndex, self._cell);
            self._render();
            self._dataTableView._adjustDataTables();
          }
        }
      );
    }
  };

  elmts.okButton.click(commit);
  elmts.okallButton.click(commit);
  elmts.textarea
  .text(originalContent)
  .keydown(function(evt) {
    if (!evt.shiftKey) {
      if (evt.keyCode == 13) {
        if (evt.ctrlKey) {
          elmts.okallButton.trigger('click');
        } else {
          elmts.okButton.trigger('click');
        }
      } else if (evt.keyCode == 27) {
        MenuSystem.dismissAll();
      }
    }
  })
  .select()
  .focus();

  elmts.cancelButton.click(function() {
    MenuSystem.dismissAll();
  });
};