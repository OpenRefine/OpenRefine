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

DataTableCellUI.previewMatchedCells = true;

(function() {
   
   $.ajax({
     url: "command/core/get-preference?" + $.param({
        name: "cell-ui.previewMatchedCells"
     }),
    success: function(data) {
      if (data.value && data.value == "false") {
        DataTableCellUI.previewMatchedCells = false;
     }
   },
   dataType: "json",
  });
})();

DataTableCellUI.prototype._render = function() {
  var self = this;
  var cell = this._cell;

  var divContent = $('<div/>')
  .addClass("data-table-cell-content");

  var editLink = $('<a href="javascript:{}">&nbsp;</a>')
  .addClass("data-table-cell-edit")
  .attr("title", $.i18n('core-views/edit-cell'))
  .appendTo(divContent)
  .click(function() { self._startEdit(this); });

  $(this._td).empty()
  .unbind()
  .mouseenter(function() { editLink.css("visibility", "visible"); })
  .mouseleave(function() { editLink.css("visibility", "hidden"); });

  if (!cell || ("v" in cell && cell.v === null)) {
    $('<span>').addClass("data-table-null").html('null').appendTo(divContent);
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
      .text($.i18n('core-views/choose-match'))
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
        a.attr("href", encodeURI(service.view.url.replace("{{id}}", match.id)));
      }

      if (DataTableCellUI.previewMatchedCells) {
        self._previewOnHover(service, match, a, a, false);
      }

      $('<span> </span>').appendTo(divContent);
      $('<a href="javascript:{}"></a>')
      .text($.i18n('core-views/choose-match'))
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
            var liSpan = $('<span></span>').appendTo(li);

            $('<a href="javascript:{}">&nbsp;</a>')
            .addClass("data-table-recon-match-similar")
            .attr("title", $.i18n('core-views/match-all-cells'))
            .appendTo(liSpan).click(function(evt) {
              self._doMatchTopicToSimilarCells(candidate);
            });

            $('<a href="javascript:{}">&nbsp;</a>')
            .addClass("data-table-recon-match")
            .attr("title", $.i18n('core-views/match-this-cell') )
            .appendTo(liSpan).click(function(evt) {
              self._doMatchTopicToOneCell(candidate);
            });

            var a = $('<a></a>')
            .addClass("data-table-recon-topic")
            .attr("target", "_blank")
            .text(_.unescape(candidate.name)) // TODO: only use of _.unescape - consolidate
            .appendTo(liSpan);

            if ((service) && (service.view) && (service.view.url)) {
              a.attr("href", encodeURI(service.view.url.replace("{{id}}", candidate.id)));
            }

            self._previewOnHover(service, candidate, liSpan.parent(), liSpan, true);

            var score;
            if (candidate.score < 1) {
              score = Math.round(candidate.score * 1000) / 1000;
            } else {
              score = Math.round(candidate.score);
            }
            $('<span></span>').addClass("data-table-recon-score").text("(" + score + ")").appendTo(liSpan);
          };

          for (var i = 0; i < candidates.length; i++) {
            renderCandidate(candidates[i], i);
          }
        }

        var liNew = $('<div></div>').addClass("data-table-recon-candidate").appendTo(ul);
        $('<a href="javascript:{}">&nbsp;</a>')
        .addClass("data-table-recon-match-similar")
        .attr("title", $.i18n('core-views/create-topic-cells'))
        .appendTo(liNew).click(function(evt) {
          self._doMatchNewTopicToSimilarCells();
        });

        $('<a href="javascript:{}">&nbsp;</a>')
        .addClass("data-table-recon-match")
        .attr("title", $.i18n('core-views/create-topic-cell'))
        .appendTo(liNew).click(function(evt) {
          self._doMatchNewTopicToOneCell();
        });

        $('<span>').text($.i18n('core-views/create-topic')).appendTo(liNew);

        var suggestOptions;
        var addSuggest = false;
        if ((service) && (service.suggest) && (service.suggest.entity)) {
          suggestOptions = service.suggest.entity;
          if ('view' in service && 'url' in service.view && !('view_url' in suggestOptions)) {
            suggestOptions.view_url = service.view.url;
          }
          // CORS / JSONP support
          if (service.ui && service.ui.access) {
            suggestOptions.access = service.ui.access;
          }
          addSuggest = true;
        }

        var extraChoices = $('<div>').addClass("data-table-recon-extra").appendTo(divContent);
        if (addSuggest) {
          $('<a href="javascript:{}"></a>')
          .click(function(evt) {
            self._searchForMatch(suggestOptions);
            return false;
          })
          .text($.i18n('core-views/search-match'))
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
  
  elmts.dialogHeader.html($.i18n('core-views/search-match'));
  elmts.or_views_searchFor.html($.i18n('core-views/search-for'));
  elmts.or_views_matchOther.html($.i18n('core-views/match-other'));
  elmts.or_views_matchThis.html($.i18n('core-views/match-this'));
  elmts.okButton.html($.i18n('core-buttons/match'));
  elmts.newButton.html($.i18n('core-buttons/new-topic'));
  elmts.clearButton.html($.i18n('core-buttons/dont-reconcile'));
  elmts.cancelButton.html($.i18n('core-buttons/cancel'));

  var level = DialogSystem.showDialog(frame);
  var dismiss = function() {
    DialogSystem.dismissUntil(level - 1);
  };

  elmts.cellTextSpan.text(this._cell.v);

  var match = null;
  var commit = function() {
    if (match !== null) {
      var notable_types = null;
      if (match.notable) {
        notable_types = $.map(match.notable, function(elmt) {
          return typeof elmt == "string" ? elmt : elmt.id; 
        }).join(",");
      }
      var params = {
        judgment: "matched",
        id: match.id,
        name: match.name,
        types: notable_types
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

  var suggestOptions2 = $.extend({ align: "left" }, suggestOptions 
                          || { all_types: true, // FIXME: all_types isn't documented for Suggest.  Is it still implemented?
                               filter: "(not (any type:/common/document type:/type/content type:/common/resource))" }); // blacklist documents and images
  if (suggestOptions2.service_url) {
    // Old style suggest API
    suggestOptions2.key = null;
    suggestOptions2.query_param_name = "prefix";
  }
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

DataTableCellUI.prototype._previewCandidateTopic = function(candidate, elmt, preview, showActions) {
  var self = this;
  var id = candidate.id;
  var fakeMenu = $('<div></div>')
        .addClass("menu-container");
  fakeMenu
  .width(preview.width?preview.width:414)
  .addClass('data-table-topic-popup');
  if (showActions) {
    fakeMenu
      .html(DOM.loadHTML("core", "scripts/views/data-table/cell-recon-preview-popup-header.html"));
  }

  if (preview && preview.url) { // Service has a preview URL associated with it
    var url = encodeURI(preview.url.replace("{{id}}", id));
    var iframe = $('<iframe></iframe>')
    .width(preview.width)
    .height(preview.height)
    .attr("src", url)
    .appendTo(fakeMenu);
  } else {
    return; // no preview service available
  }

  MenuSystem.positionMenuLeftRight(fakeMenu, $(elmt));
  fakeMenu.appendTo(elmt);

  var dismissMenu = function() {
     fakeMenu.remove();
     fakeMenu.unbind();  
  };

  if (showActions) {
    var elmts = DOM.bind(fakeMenu);
    
    elmts.matchButton.html($.i18n('core-views/match-cell'));
    elmts.matchSimilarButton.html($.i18n('core-views/match-identical'));
    elmts.cancelButton.html($.i18n('core-buttons/cancel'));
    
    elmts.matchButton.click(function() {
        self._doMatchTopicToOneCell(candidate);
        dismissMenu();
    });
    elmts.matchSimilarButton.click(function() {
        self._doMatchTopicToSimilarCells(candidate);
        dismissMenu();
    });
    elmts.cancelButton.click(function() {
        dismissMenu();
    });
  }
  return dismissMenu;
};

/**
 * Sets up a preview widget to appear when hovering the given DOM element.
 */
DataTableCellUI.prototype._previewOnHover = function(service, candidate, hoverElement, coreElement, showActions) {
    var self = this;
    var preview = null;
    if ((service) && (service.preview)) {
        preview = service.preview;
    }

    if (preview) {
        var dismissCallback = null;
        hoverElement.hover(function(evt) {
        if (!evt.metaKey && !evt.ctrlKey) {
            dismissCallback = self._previewCandidateTopic(candidate, coreElement, preview, showActions);
            evt.preventDefault();
        }
        }, function(evt) {
        if(dismissCallback !== null) {
            dismissCallback();
        }
        });
    }
};

DataTableCellUI.prototype._startEdit = function(elmt) {
  self = this;

  var originalContent = !this._cell || ("v" in this._cell && this._cell.v === null) ? "" : this._cell.v;

  var menu = MenuSystem.createMenu().addClass("data-table-cell-editor").width("400px");
  menu.html(DOM.loadHTML("core", "scripts/views/data-table/cell-editor.html"));
  var elmts = DOM.bind(menu);

  elmts.or_views_dataType.html($.i18n('core-views/data-type'));
  elmts.or_views_text.html($.i18n('core-views/text'));
  elmts.or_views_number.html($.i18n('core-views/number'));
  elmts.or_views_boolean.html($.i18n('core-views/boolean'));
  elmts.or_views_date.html($.i18n('core-views/date'));
  elmts.okButton.html($.i18n('core-buttons/apply'));
  elmts.or_views_enter.html($.i18n('core-buttons/enter'));
  elmts.okallButton.html($.i18n('core-buttons/apply-to-all'));
  elmts.or_views_ctrlEnter.html($.i18n('core-views/ctrl-enter'));
  elmts.cancelButton.html($.i18n('core-buttons/cancel'));
  elmts.or_views_esc.html($.i18n('core-buttons/esc'));

  var cellDataType = typeof originalContent === "string" ? "text" : typeof originalContent;
  cellDataType = (this._cell !== null && "t" in this._cell && this._cell.t !=  null) ? this._cell.t : cellDataType;
  elmts.typeSelect.val(cellDataType);

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
        alert($.i18n('core-views/not-valid-number'));
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
        alert($.i18n('core-views/not-valid-date'));
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
