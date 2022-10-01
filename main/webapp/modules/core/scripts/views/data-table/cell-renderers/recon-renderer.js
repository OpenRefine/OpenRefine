/**
 * A renderer for cells with recon data.
 */
class ReconCellRenderer {
  render(rowIndex, cellIndex, cell, cellUI) {
    if (cell && "r" in cell) {
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
          cellUI._doRematch();
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
          cellUI._previewOnHover(service, match, a, a, false);
        }

        $('<span> </span>').appendTo(divContentRecon);
        $('<a href="javascript:{}"></a>')
        .text($.i18n('core-views/choose-match'))
        .addClass("data-table-recon-action")
        .appendTo(divContentRecon)
        .on('click',function(evt) {
          cellUI._doRematch();
        });
      } else {
        $('<span>').text(cell.v).appendTo(divContentRecon);

        if (this._dataTableView._showRecon) {
          var ul = $('<div></div>').addClass("data-table-recon-candidates").appendTo(divContentRecon);
          if ("c" in r && r.c.length > 0) {
            var candidates = r.c;
            var renderCandidate = function(candidate, index) {
              var li = $('<div></div>').addClass("data-table-recon-candidate").appendTo(ul);
              var liSpan = $('<span></span>').appendTo(li);

              $('<a href="javascript:{}">&nbsp;</a>')
              .addClass("data-table-recon-match-similar")
              .attr("title", $.i18n('core-views/match-all-cells'))
              .appendTo(liSpan).on('click',function(evt) {
                cellUI._doMatchTopicToSimilarCells(candidate);
              });

              $('<a href="javascript:{}">&nbsp;</a>')
              .addClass("data-table-recon-match")
              .attr("title", $.i18n('core-views/match-this-cell') )
              .appendTo(liSpan).on('click',function(evt) {
                cellUI._doMatchTopicToOneCell(candidate);
              });

              var a = $('<a></a>')
              .addClass("data-table-recon-topic")
              .attr("target", "_blank")
              .text(_.unescape(candidate.name)) // TODO: only use of _.unescape - consolidate
              .appendTo(liSpan);

              if ((service) && (service.view) && (service.view.url)) {
                a.attr("href", encodeURI(service.view.url.replace("{{id}}", candidate.id)));
              }

              cellUI._previewOnHover(service, candidate, liSpan.parent(), liSpan, true);

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
          .appendTo(liNew).on('click',function(evt) {
            cellUI._doMatchNewTopicToSimilarCells();
          });

          $('<a href="javascript:{}">&nbsp;</a>')
          .addClass("data-table-recon-match")
          .attr("title", $.i18n('core-views/create-topic-cell'))
          .appendTo(liNew).on('click',function(evt) {
            cellUI._doMatchNewTopicToOneCell();
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
              cellUI._searchForMatch(suggestOptions);
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
}
