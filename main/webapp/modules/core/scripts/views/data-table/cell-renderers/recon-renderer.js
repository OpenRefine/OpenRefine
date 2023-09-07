/**
 * A renderer for cells with recon data.
 */
class ReconCellRenderer {
  render(rowIndex, cellIndex, cell, cellUI) {
    if (cell && "r" in cell) {
      var self = this;
      var divContent = document.createElement('div');
      var divContentRecon = $(divContent);
      var r = cell.r;
      var service = (r.service) ? ReconciliationManager.getServiceFromUrl(r.service) : null;

      if (r.j == "new") {
        $('<span>').text(cell.v).appendTo(divContentRecon);
        $('<span>').addClass("data-table-recon-new").text("new").appendTo(divContentRecon);

        $('<a href="javascript:{}"></a>')
        .text($.i18n('core-views/choose-match'))
        .addClass("data-table-recon-action")
        .appendTo(divContentRecon).on('click',function(evt) {
          self.doRematch(rowIndex, cellIndex, cell, cellUI);
        });
      } else if (r.j == "matched" && "m" in r && r.m !== null) {
        var match = cell.r.m;
        var a = $('<a></a>')
        .text(match.name)
        .attr("target", "_blank")
        .appendTo(divContentRecon);

        if (service && (service.view) && (service.view.url)) {
          a.attr("href", encodeURI(service.view.url.replace("{{id}}", match.id)));
        }

        if (DataTableCellUI.previewMatchedCells) {
          self.previewOnHover(service, match, a, a, false, rowIndex, cellIndex, cell, cellUI);
        }

        $('<span> </span>').appendTo(divContentRecon);
        $('<a href="javascript:{}"></a>')
        .text($.i18n('core-views/choose-match'))
        .addClass("data-table-recon-action")
        .appendTo(divContentRecon)
        .on('click',function(evt) {
          self.doRematch(rowIndex, cellIndex, cell, cellUI);
        });
      } else {
        $('<span>').text(cell.v).appendTo(divContentRecon);

        if (cellUI._dataTableView._showRecon) {
          var ul = $('<div></div>').addClass("data-table-recon-candidates").appendTo(divContentRecon);
          if ("c" in r && r.c.length > 0) {
            var candidates = r.c;
            var renderCandidate = function(candidate, index) {
              var li = $('<div></div>').addClass("data-table-recon-candidate").appendTo(ul);
              var liSpan = $('<span></span>').appendTo(li);

              $('<a href="javascript:{}">&nbsp;</a>')
              .addClass("data-table-recon-match")
              .attr("title", $.i18n('core-views/match-this-cell') )
              .appendTo(liSpan).on('click',function(evt) {
                self.doMatchTopicToOneCell(candidate, rowIndex, cellIndex, cell, cellUI);
              });

              $('<a href="javascript:{}">&nbsp;</a>')
              .addClass("data-table-recon-match-similar")
              .attr("title", $.i18n('core-views/match-all-cells'))
              .appendTo(liSpan).on('click',function(evt) {
                self.doMatchTopicToSimilarCells(candidate, cellIndex, cell);
              });

              var a = $('<a></a>')
              .addClass("data-table-recon-topic")
              .attr("target", "_blank")
              .text(_.unescape(candidate.name)) // TODO: only use of _.unescape - consolidate
              .appendTo(liSpan);

              if ((service) && (service.view) && (service.view.url)) {
                a.attr("href", encodeURI(service.view.url.replace("{{id}}", candidate.id)));
              }

              self.previewOnHover(service, candidate, liSpan.parent(), liSpan, true, rowIndex, cellIndex, cell);

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
          .addClass("data-table-recon-match")
          .attr("title", $.i18n('core-views/create-topic-cell'))
          .appendTo(liNew).on('click',function(evt) {
            self.doMatchNewTopicToOneCell(rowIndex, cellIndex, cell, cellUI);
          });

          $('<a href="javascript:{}">&nbsp;</a>')
          .addClass("data-table-recon-match-similar")
          .attr("title", $.i18n('core-views/create-topic-cells'))
          .appendTo(liNew).on('click',function(evt) {
            self.doMatchNewTopicToSimilarCells(cellIndex, cell);
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

          var extraChoices = $('<div>').addClass("data-table-recon-extra").appendTo(divContentRecon);
          if (addSuggest) {
            $('<a href="javascript:{}"></a>')
            .on('click',function(evt) {
              self.searchForMatch(suggestOptions, rowIndex, cellIndex, cell, cellUI);
              return false;
            })
            .text($.i18n('core-views/search-match'))
            .appendTo(extraChoices);
          }
        }
      }
      return divContent;
    }
  }

  doRematch(rowIndex, cellIndex, cell, cellUI) {
    this.doJudgment("none", null, rowIndex, cellIndex, cell, cellUI);
  }

  doMatchNewTopicToOneCell(rowIndex, cellIndex, cell, cellUI, onDone) {
    this.doJudgment("new", null, rowIndex, cellIndex, cell, cellUI, onDone);
  }

  doClearOneCell(rowIndex, cellIndex, cellUI, onDone) {
    self.postProcessOneCell(
      {
        op: "core/recon-edit",
        rowId: rowIndex,
        columnName: Refine.cellIndexToColumn(cellIndex).name,
      },
      true,
      cellUI,
      onDone
    );
  }

  doMatchNewTopicToSimilarCells(cellIndex, cell, onDone) {
    this.doJudgmentForSimilarCells("new", { shareNewTopics: true }, cellIndex, cell, onDone);
  };

  doClearSimilarCells(cell, cellIndex, onDone) {
    this.postProcessSeveralCells(
      { 
        op: "core/recon-clear-similar-cells",
        columnName: Refine.cellIndexToColumn(cellIndex).name,
        similarValue: cell.v
      },
      true,
      onDone
    );
  }

  doMatchTopicToOneCell(candidate, rowIndex, cellIndex, cell, cellUI, onDone) {
    this.doJudgment("matched", {
      id : candidate.id,
      name: candidate.name,
      score: candidate.score,
      types: candidate.types
    }, rowIndex, cellIndex, cell, cellUI, onDone);
  }

  doMatchTopicToSimilarCells(candidate, cellIndex, cell, onDone) {
    this.doJudgmentForSimilarCells("matched", {
      match: {
        id: candidate.id,
        name: candidate.name,
        score: candidate.score,
        types: candidate.types
      }
    }, cellIndex, cell, onDone);
  }

  doJudgment(judgment, match, rowIndex, cellIndex, cell, cellUI, onDone) {
    this.postProcessOneCell(
      {
        op: "core/recon-edit",
        rowId: rowIndex,
        columnName: Refine.cellIndexToColumn(cellIndex).name,
        judgment: judgment,
        identifierSpace: (cell.r) ? cell.r.identifierSpace : null,
        schemaSpace: (cell.r) ? cell.r.schemaSpace : null,
        cellValue: cell.v,
        match: match
      },
      true,
      cellUI,
      onDone
    );
  }

  doJudgmentForSimilarCells(judgment, params, cellIndex, cell, onDone) {
    this.postProcessSeveralCells(
      $.extend(params || {}, {
        op: "core/recon-judge-similar-cells",
        columnName: Refine.cellIndexToColumn(cellIndex).name,
        similarValue: cell.v,
        judgment: judgment,
        identifierSpace: (cell.r) ? cell.r.identifierSpace : null,
            schemaSpace: (cell.r) ? cell.r.schemaSpace : null
      }),
      true,
      onDone
    );
  }

  searchForMatch(suggestOptions, rowIndex, cellIndex, cell, cellUI) {
    var self = this;
    var frame = $(DOM.loadHTML("core", "scripts/views/data-table/cell-recon-search-for-match.html"));
    var elmts = DOM.bind(frame);

    elmts.dialogHeader.html($.i18n('core-views/search-match'));
    elmts.input.attr('aria-label',$.i18n('core-views/item-to-match'));
    elmts.or_views_searchFor.html($.i18n('core-views/search-for'));
    elmts.or_views_matchOther.html($.i18n('core-views/match-other'));
    elmts.or_views_matchThis.html($.i18n('core-views/match-this'));
    elmts.okButton.html($.i18n('core-buttons/match'));
    elmts.newButton.html($.i18n('core-buttons/new-topic'));
    elmts.clearButton.html($.i18n('core-buttons/dont-reconcile'));
    elmts.cancelButton.html($.i18n('core-buttons/cancel'));

  	if (!reconMatchSilimilarCellsByDefault) {
  		elmts.radioSimilar[0].setAttribute("checked", false);
  		elmts.radioOne[0].setAttribute("checked", true);
  	}

    var level = DialogSystem.showDialog(frame);
    var dismiss = function() {
  	reconMatchSilimilarCellsByDefault = elmts.radioSimilar[0].checked;

      DialogSystem.dismissUntil(level - 1);
    };

    elmts.cellTextSpan.text(cell.v);

    var match = null;
    var commit = function() {
      if (match !== null) {
        var notable_types = null;
        if (match.notable) {
          notable_types = $.map(match.notable, function(elmt) {
            return typeof elmt == "string" ? elmt : elmt.id;
          });
        }
        if (elmts.radioSimilar[0].checked) {
          var params = {
            op: 'core/recon-judge-similar-cells',
            judgment: "matched",
            match: {
              id: match.id,
              name: match.name,
              types: notable_types
            },
            similarValue: cell.v,
            columnName: Refine.cellIndexToColumn(cellIndex).name
          };
          self.postProcessSeveralCells(params, true, dismiss);
        } else {
          var operation = {
            op: "core/recon-edit",
            judgment: "matched",
            match: {
              id: match.id,
              name: match.name,
              types: notable_types
            },
            rowId: rowIndex,
            columnName: Refine.cellIndexToColumn(cellIndex).name
          };

          self.postProcessOneCell(
            operation, true, cellUI, dismiss);
        }
      }
    };
    var commitNew = function() {
      if (elmts.radioSimilar[0].checked) {
        self.doMatchNewTopicToSimilarCells(cellIndex, cell, dismiss);
      } else {
        self.doMatchNewTopicToOneCell(rowIndex, cellIndex, cell, cellUI, dismiss);
      }
    };
    var commitClear = function() {
      if (elmts.radioSimilar[0].checked) {
        self.doClearSimilarCells(cell, cellIndex, dismiss);
      } else {
        self.doClearOneCell(rowIndex, cellIndex, cellUI, dismiss);
      }
    };

    elmts.okButton.on('click',commit);
    elmts.newButton.on('click',commitNew);
    elmts.clearButton.on('click',commitClear);
    elmts.cancelButton.on('click',dismiss);

    var suggestOptions2 = $.extend({ align: "left" }, suggestOptions
                            || { all_types: true, // FIXME: all_types isn't documented for Suggest.  Is it still implemented?
                                 filter: "(not (any type:/common/document type:/type/content type:/common/resource))" }); // blacklist documents and images
    if (suggestOptions2.service_url) {
      // Old style suggest API
      suggestOptions2.key = null;
      suggestOptions2.query_param_name = "prefix";
    }
    var suggest = elmts.input
    .val(cell.v)
    .suggest(suggestOptions2);

    suggest.on("fb-pane-show", function(e, data) {
      DialogSystem.pauseEscapeKeyHandling();
    });

    suggest.on("fb-pane-hide", function(e, data) {
      DialogSystem.setupEscapeKeyHandling();
    });

    suggest.on("fb-select", function(e, data) {
      match = data;
      commit();
    })
        .trigger('focus')
        .data("suggest").textchange();

  }

  postProcessOneCell(operation, columnStatsChanged, cellUI, onDone) {
    var self = this;

    Refine.postOperation(
      operation,
      { columnStatsChanged: columnStatsChanged },
      {
        onDone: function(o) {
          cellUI._cell = o.changeResult.cell;
          cellUI._dataTableView._updateCell(self._rowIndex, self._cellIndex, cellUI._cell);
          cellUI._render();
          if (onDone) {
            onDone();
          }
        }
      }
    );
  }

  postProcessSeveralCells(operation, columnStatsChanged, onDone) {
    
    Refine.postOperation(
      operation,
      { cellsChanged: true, columnStatsChanged: columnStatsChanged },
      { onDone: onDone }
    );
  }

  previewCandidateTopic(candidate, elmt, preview, showActions, rowIndex, cellIndex, cell, cellUI) {
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

    fakeMenu.appendTo(elmt);
    MenuSystem.positionMenuLeftRight(fakeMenu, $(elmt));

    var dismissMenu = function() {
       fakeMenu.remove();
       fakeMenu.off();
    };

    if (showActions) {
      var elmts = DOM.bind(fakeMenu);

      elmts.matchButton.html($.i18n('core-views/match-cell'));
      elmts.matchSimilarButton.html($.i18n('core-views/match-identical'));
      elmts.cancelButton.html($.i18n('core-buttons/cancel'));

      elmts.matchButton.on('click',function() {
          self.doMatchTopicToOneCell(candidate, rowIndex, cellIndex, cell, cellUI, dismissMenu);
      });
      elmts.matchSimilarButton.on('click',function() {
          self.doMatchTopicToSimilarCells(candidate, cellIndex, cell, dismissMenu);
      });
      elmts.cancelButton.on('click',function() {
          dismissMenu();
      });
    }
    return dismissMenu;
  };

  /**
   * Sets up a preview widget to appear when hovering the given DOM element.
   */
  previewOnHover(service, candidate, hoverElement, coreElement, showActions, rowIndex, cellIndex, cell, cellUI) {
      var self = this;
      var preview = null;
      if ((service) && (service.preview)) {
          preview = service.preview;
      }

      if (preview) {
          var dismissCallback = null;
          hoverElement.on('mouseenter',function(evt) {
          if (!evt.metaKey && !evt.ctrlKey) {
              dismissCallback = self.previewCandidateTopic(candidate, coreElement, preview, showActions, rowIndex, cellIndex, cell, cellUI);
              evt.preventDefault();
          }
          }).on('mouseleave',function(evt) {
          if(dismissCallback !== null) {
              dismissCallback();
          }
          });
      }
  };

}
