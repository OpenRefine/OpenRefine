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

    var header = $('<div></div>').addClass("dialog-header").text("Search for Match").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

    $('<p></p>').text("Search Freebase for a topic to match all filtered cells:").appendTo(body);

    var input = $('<input />').appendTo($('<p></p>').appendTo(body));

    input.suggest({}).bind("fb-select", function(e, data) {
      var query = {
        "id" : data.id,
        "type" : []
      };
      var baseUrl = "http://api.freebase.com/api/service/mqlread";
      var url = baseUrl + "?" + $.param({ query: JSON.stringify({ query: query }) }) + "&callback=?";

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

    $('<button class="button"></button>').text("Cancel").click(function() {
      DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);

    var level = DialogSystem.showDialog(frame);
    input.focus().data("suggest").textchange();
  };

  var doCopyAcrossColumns = function() {
    var frame = $(DOM.loadHTML("core", "scripts/views/data-table/copy-recon-across-columns-dialog.html"));
    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text("Copy recon judgments from column " + column.name);

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
        alert("Please select some other column to copy to.");
      } else if (config.judgment.length === 0) {
        alert("Please select at least one kind of judgment to copy.");
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
      label: "Start reconciling...",
      tooltip: "Reconcile text in this column with topics on Freebase",
      click: doReconcile
    },
    {},
    {
      id: "core/facets",
      label: "Facets",
      submenu: [
        {
          id: "core/by-judgment",
          label: "By judgment",
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
          label: "Best candidate's score",
          click: function() {
            ui.browsingEngine.addFacet(
                "range", 
                {
                  "name" : column.name + ": best candidate's score",
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
          label: "Best candidate's type match",
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : column.name + ": best candidate's types match?",
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
          label: "Best candidate's name match",
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : column.name + ": best candidate's name match?",
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
          label: "Best candidate's name edit distance",
          click: function() {
            ui.browsingEngine.addFacet(
                "range", 
                {
                  "name" : column.name + ": best candidate's name edit distance",
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
          label: "Best candidate's name word similarity",
          click: function() {
            ui.browsingEngine.addFacet(
                "range", 
                {
                  "name" : column.name + ": best candidate's name word similarity",
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
          label: "Best candidate's types",
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
      label: "QA facets",
      submenu: [
        {
          id: "core/by-qa-results",
          label: "QA results",
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : column.name + " QA Results",
                  "columnName" : column.name, 
                  "expression" : "cell.recon.features.qaResult"
                }
            );
          }
        },
        {
          id: "core/by-judgment-actions",
          label: "Judgment actions",
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : column.name + " Judgment Actions",
                  "columnName" : column.name, 
                  "expression" : "cell.recon.judgmentAction"
                }
            );
          }
        },
        {
          id: "core/by-judgment-history-entries",
          label: "Judgment history entries",
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : column.name + " History Entries",
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
      label: "Actions",
      submenu: [
        {
          id: "core/match-to-best-candidate",
          label: "Match each cell to its best candidate",
          tooltip: "Match each cell to its best candidate in this column for all current filtered rows",
          click: doReconMatchBestCandidates
        },
        {
          id: "core/match-to-new-topic",
          label: "Create a new topic for each cell",
          tooltip: "Mark to create one new topic for each cell in this column for all current filtered rows",
          click: function() {
            doReconMarkNewTopics(false);
          }
        },
        {},
        {
          id: "core/match-similar-to-new-topic",
          label: "Create one new topic for similar cells",
          tooltip: "Mark to create one new topic for each group of similar cells in this column for all current filtered rows",
          click: function() {
            doReconMarkNewTopics(true);
          }
        },
        {
          id: "core/match-to-specific",
          label: "Match all filtered cells to...",
          tooltip: "Search for a topic to match all filtered cells to",
          click: doSearchToMatch
        },
        {},
        {
          id: "core/discard-judgments",
          label: "Discard reconciliation judgments",
          tooltip: "Discard reconciliation judgments in this column for all current filtered rows",
          click: doReconDiscardJudgments
        },
        {
          id: "core/clear-recon-data",
          label: "Clear reconciliation data",
          tooltip: "Clear reconciliation data in this column for all current filtered rows",
          click: doClearReconData
        }
      ]
    },
    {},
    {
      id: "core/copy-across-columns",
      label: "Copy reconciliation data...",
      tooltip: "Copy this column's reconciliation data to other columns",
      click: doCopyAcrossColumns
    }
  ]);
});