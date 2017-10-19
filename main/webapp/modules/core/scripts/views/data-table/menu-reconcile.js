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

DataTableColumnHeaderUI.extendMenu(function(column, columnHeaderUI, menu) {
  var doReconcile = function() {
    new ReconDialog(column);
  };

  var doReconDiscardJudgments = function() {
    Refine.postCoreProcess(
      "recon-discard-judgments",
      { columnName: column.name, clearData: false },
      null,
      { cellsChanged: true, columnStatsChanged: true }
    );
  };

  var doClearReconData = function() {
    Refine.postCoreProcess(
      "recon-discard-judgments",
      { columnName: column.name, clearData: true },
      null,
      { cellsChanged: true, columnStatsChanged: true }
    );
  };

  var doReconMatchBestCandidates = function() {
    Refine.postCoreProcess(
      "recon-match-best-candidates",
      { columnName: column.name },
      null,
      { cellsChanged: true, columnStatsChanged: true }
    );
  };

  var doReconMarkNewTopics = function(shareNewTopics) {
    Refine.postCoreProcess(
      "recon-mark-new-topics",
      { columnName: column.name, shareNewTopics: shareNewTopics },
      null,
      { cellsChanged: true, columnStatsChanged: true }
    );
  };

  var doSearchToMatch = function() {
    var frame = DialogSystem.createDialog();
    frame.width("400px");

    var header = $('<div></div>').addClass("dialog-header").text($.i18n._('core-views')["search-match"]).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

    $('<p></p>').text($.i18n._('core-views')["search-fb-topic"]).appendTo(body);

    var input = $('<input />').appendTo($('<p></p>').appendTo(body));

    input.suggest({}).bind("fb-select", function(e, data) {
      var query = {
        "id" : data.id,
        "type" : []
      };
      var baseUrl = "https://www.googleapis.com/freebase/v1/mqlread?key=" + Freebase.API_KEY + "&";
      var url = baseUrl + $.param({ query: JSON.stringify(query) }) + "&callback=?";

      $.getJSON(
        url,
        null,
        function(o) {
          var types = "result" in o ? o.result.type : [];

          Refine.postCoreProcess(
            "recon-match-specific-topic-to-cells",
            {
              columnName: column.name,
              topicID: data.id,
              topicGUID: data.guid,
              topicName: data.name,
              types: types.join(",")
            },
            null,
            { cellsChanged: true, columnStatsChanged: true }
          );

          DialogSystem.dismissUntil(level - 1);
        }
      );
    });

    $('<button class="button"></button>').text($.i18n._('core-buttons')["cancel"]).click(function() {
      DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);

    var level = DialogSystem.showDialog(frame);
    input.focus().data("suggest").textchange();
  };

  var doCopyAcrossColumns = function() {
    var frame = $(DOM.loadHTML("core", "scripts/views/data-table/copy-recon-across-columns-dialog.html"));
    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n._('core-views')["copy-recon-judg"]+" " + column.name);
    
    elmts.or_views_copyToCol.text($.i18n._('core-views')["copy-to-col"]);
    elmts.or_views_copyOpt.text($.i18n._('core-views')["copy-opt"]);
    elmts.or_views_applyToCell.text($.i18n._('core-views')["apply-to-cell"]);
    elmts.or_views_whatToCopy.text($.i18n._('core-views')["what-to-copy"]);
    elmts.or_views_newRecon.text($.i18n._('core-views')["new-recon"]);
    elmts.or_views_matchRecon.text($.i18n._('core-views')["match-recon"]);
    elmts.okButton.text($.i18n._('core-buttons')["copy"]);
    elmts.cancelButton.text($.i18n._('core-buttons')["cancel"]);

    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
      var column2 = columns[i];
      if (column !== column2) {
        $('<option>').attr("value", column2.name).text(column2.name).appendTo(elmts.toColumnSelect);
      }
    }

    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

    elmts.cancelButton.click(dismiss);
    elmts.okButton.click(function() {
      var config = {
        fromColumnName: column.name,
        toColumnName: [],
        judgment: [],
        applyToJudgedCells: elmts.applyToJudgedCellsCheckbox[0].checked
      };

      if (elmts.newCheckbox[0].checked) {
        config.judgment.push("new");
      }
      if (elmts.matchCheckbox[0].checked) {
        config.judgment.push("matched");
      }
      elmts.toColumnSelect.find("option").each(function() {
        if (this.selected) {
          config.toColumnName.push(this.value);
        }
      });

      if (config.toColumnName.length === 0) {
        alert($.i18n._('core-views')["warning-other-col"]);
      } else if (config.judgment.length === 0) {
        alert($.i18n._('core-views')["warning-sel-judg"]);
      } else {
        Refine.postCoreProcess(
          "recon-copy-across-columns", 
          null,
          config,
          { rowsChanged: true }
        );
        dismiss();
      }
    });
  };

  MenuSystem.appendTo(menu, [ "core/reconcile" ], [
    {
      id: "core/reconcile",
      label: $.i18n._('core-views')["start-recon"]+'...',
      tooltip: $.i18n._('core-views')["recon-text-fb"],
      click: doReconcile
    },
    {},
    {
      id: "core/facets",
      label: $.i18n._('core-views')["facets"],
      submenu: [
        {
          id: "core/by-judgment",
          label: $.i18n._('core-views')["by-judg"],
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : column.name + ": judgment",
                  "columnName" : column.name, 
                  "expression" : 'forNonBlank(cell.recon.judgment, v, v, if(isNonBlank(value), "(unreconciled)", "(blank)"))'
                },
                {
                  "scroll" : false
                }
            );
          }
        },
        {},
        {
          id: "core/by-best-candidates-score",
          label: $.i18n._('core-views')["best-score"],
          click: function() {
            ui.browsingEngine.addFacet(
                "range", 
                {
                  "name" : column.name + ": "+$.i18n._('core-views')["best-cand-score"],
                  "columnName" : column.name, 
                  "expression" : "cell.recon.best.score",
                  "mode" : "range"
                },
                {
                }
            );
          }
        },
        {
          id: "core/by-best-candidates-type-match",
          label: $.i18n._('core-views')["best-type-match"],
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : column.name + ": "+$.i18n._('core-views')["best-cand-type-match"],
                  "columnName" : column.name, 
                  "expression" : 'forNonBlank(cell.recon.features.typeMatch, v, v, if(isNonBlank(value), "(unreconciled)", "(blank)"))'
                },
                {
                  "scroll" : false
                }
            );
          }
        },
        {
          id: "core/by-best-candidates-name-match",
          label: $.i18n._('core-views')["best-name"],
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : column.name + ": "+ $.i18n._('core-views')["best-cand-name"],
                  "columnName" : column.name, 
                  "expression" : 'forNonBlank(cell.recon.features.nameMatch, v, v, if(isNonBlank(value), "(unreconciled)", "(blank)"))'
                },
                {
                  "scroll" : false
                }
            );
          }
        },
        {},
        {
          id: "core/by-best-candidates-name-edit-distance",
          label: $.i18n._('core-views')["best-edit-dist"],
          click: function() {
            ui.browsingEngine.addFacet(
                "range", 
                {
                  "name" : column.name + ": "+$.i18n._('core-views')["best-cand-edit-dist"],
                  "columnName" : column.name, 
                  "expression" : "cell.recon.features.nameLevenshtein",
                  "mode" : "range"
                },
                {
                }
            );
          }
        },
        {
          id: "core/by-best-candidates-name-word-similarity",
          label: $.i18n._('core-views')["best-word-sim"],
          click: function() {
            ui.browsingEngine.addFacet(
                "range", 
                {
                  "name" : column.name + ": "+$.i18n._('core-views')["best-cand-word-sim"],
                  "columnName" : column.name, 
                  "expression" : "cell.recon.features.nameWordDistance",
                  "mode" : "range"
                },
                {
                }
            );
          }
        },
        {},
        {
          id: "core/by-best-candidates-types",
          label: $.i18n._('core-views')["best-type"],
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : column.name + ": best candidate's types",
                  "columnName" : column.name,
                  "expression" : 'forNonBlank(cell.recon.best.type, v, v, if(isNonBlank(value), "(unreconciled)", "(blank)"))'
                }
            );
          }
        }
      ]
    },
    {
      id: "core/qa-facets",
      label: $.i18n._('core-views')["qa-facets"],
      submenu: [
        {
          id: "core/by-qa-results",
          label: $.i18n._('core-views')["qa-results"],
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : column.name + " "+$.i18n._('core-views')["qa-results2"],
                  "columnName" : column.name, 
                  "expression" : "cell.recon.features.qaResult"
                }
            );
          }
        },
        {
          id: "core/by-judgment-actions",
          label: $.i18n._('core-views')["judg-actions"],
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : column.name + " "+$.i18n._('core-views')["judg-actions2"],
                  "columnName" : column.name, 
                  "expression" : "cell.recon.judgmentAction"
                }
            );
          }
        },
        {
          id: "core/by-judgment-history-entries",
          label: $.i18n._('core-views')["judg-hist"],
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : column.name + " "+$.i18n._('core-views')["hist-entries"],
                  "columnName" : column.name, 
                  "expression" : "cell.recon.judgmentHistoryEntry"
                }
            );
          }
        }
      ]
    },
    {
      id: "core/actions",
      label: $.i18n._('core-views')["actions"],
      submenu: [
        {
          id: "core/match-to-best-candidate",
          label: $.i18n._('core-views')["best-cand"],
          tooltip: $.i18n._('core-views')["best-cand2"],
          click: doReconMatchBestCandidates
        },
        {
          id: "core/match-to-new-topic",
          label: $.i18n._('core-views')["new-topic"],
          tooltip: $.i18n._('core-views')["new-topic2"],
          click: function() {
            doReconMarkNewTopics(false);
          }
        },
        {
          id: "core/match-similar-to-new-topic",
          label: $.i18n._('core-views')["one-topic"],
          tooltip: $.i18n._('core-views')["one-topic2"],
          click: function() {
            doReconMarkNewTopics(true);
          }
        },
        {
          id: "core/match-to-specific",
          label: $.i18n._('core-views')["filtered-cell"],
          tooltip: $.i18n._('core-views')["filtered-cell2"],
          click: doSearchToMatch
        },
        {},
        {
          id: "core/discard-judgments",
          label: $.i18n._('core-views')["discard-judg"],
          tooltip: $.i18n._('core-views')["discard-judg2"],
          click: doReconDiscardJudgments
        },
        {
          id: "core/clear-recon-data",
          label: $.i18n._('core-views')["clear-recon"],
          tooltip: $.i18n._('core-views')["clear-recon2"],
          click: doClearReconData
        }
      ]
    },
    {},
    {
      id: "core/copy-across-columns",
      label: $.i18n._('core-views')["copy-recon"],
      tooltip: $.i18n._('core-views')["copy-recon2"],
      click: doCopyAcrossColumns
    }
  ]);
});
