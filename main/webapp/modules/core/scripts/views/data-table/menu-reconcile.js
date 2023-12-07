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
  var columnIndex = Refine.columnNameToColumnIndex(column.name);
  var serviceUrl = null;
  var service = null;
  if (column.reconConfig) {
    serviceUrl = column.reconConfig.service;
  }
  if (serviceUrl) {
    service = ReconciliationManager.getServiceFromUrl(serviceUrl);
  }
  var doReconcile = function() {
      new ReconDialog(column, serviceUrl);
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

  function successCallBack(columnName,dismissDialog)
  {
      var serviceUrl = null;
      var service = null;
      if (column.reconConfig) {
        serviceUrl = column.reconConfig.service;
      }
      if (serviceUrl) {
        service = ReconciliationManager.getServiceFromUrl(serviceUrl);
      }
      if (service && service.view){
        
        Refine.postCoreProcess
        (
          "add-column",
          {
            baseColumnName: column.name,
            newColumnName: columnName,
            columnInsertIndex: columnIndex + 1,
            onError: "set-to-blank"
          },
          { expression: 'if(cell.recon.match!=null,"' + service.view.url + '".replace("{{id}}",escape(cell.recon.match.id,"url")),null)' },
          { modelsChanged: true },
          { onDone: dismissDialog},
        );
      } else {
        alert($.i18n('core-views/service-does-not-associate-URLs-to-the-entities-it-contains'));
      }
    }


  var doAddColumnWithUrlOfMatchedEntities = function () {

    promptForColumn(successCallBack,'core-views/add-entity-URL-col');

  }


  function promptForColumn(successCallBack,dialogName){
    var frame = $(
      DOM.loadHTML("core", "scripts/views/data-table/add-q-column-dialog.html"));

    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n(dialogName, column.name));
    elmts.or_views_newCol.text($.i18n('core-views/new-col-name'));
    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.text($.i18n('core-buttons/cancel'));

    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

    var o = DataTableView.sampleVisibleRows(column);

    elmts.cancelButton.on('click',dismiss);

    elmts.form.on('submit',function(event) {
      event.preventDefault();
      var columnName = jQueryTrim(elmts.columnNameInput[0].value);
      if (!columnName.length) {
        alert($.i18n('core-views/warning-col-name'));
        return;
      }
      var dismissDialog=function(o) {
        dismiss();
      };
      successCallBack(columnName,dismissDialog);

    });
  }

  var doReconMatchBestCandidates = function() {
    Refine.postCoreProcess(
      "recon-match-best-candidates",
      { columnName: column.name },
      null,
      { cellsChanged: true, columnStatsChanged: true }
    );
  };

  var doReconMarkNewTopics = function(shareNewTopics) {
    var headerText = $.i18n(shareNewTopics ? 'core-views/one-topic/header' : 'core-views/new-topic/header');
    var explanationText = $.i18n('core-views/recon-mark-new-warning');
    var onSelect = function(service, identifierSpace, schemaSpace) {
      Refine.postCoreProcess(
        "recon-mark-new-topics",
        {
          columnName: column.name,
          shareNewTopics: shareNewTopics,
          service: service,
          identifierSpace: identifierSpace,
          schemaSpace: schemaSpace
        },
        null,
        { cellsChanged: true, columnStatsChanged: true }
      );
    };

    // if this column is already partly reconciled,
    // we do not need to prompt the user for a reconciliation service,
    // as we can use the existing one.
    if (column.reconConfig != null && column.reconConfig.service != null) {
      // we do not pass the service, identifierSpace and schemaSpace to the operation
      // so that the existing ReconConfig is kept by the operation
      onSelect(null, null, null);
    } else {
      createReconServiceSelectionDialog(headerText, explanationText, onSelect);
    }
  };
  
  var doSearchToMatch = function() {
    var serviceUrl = null;
    var service = null;
    var suggestOptions = {};
    if (column.reconConfig) {
        serviceUrl = column.reconConfig.service;
    }
    if (serviceUrl) {
        service = ReconciliationManager.getServiceFromUrl(serviceUrl);
    }
    if (service && service.suggest && service.suggest.entity) {
       suggestOptions = $.extend({}, service.suggest.entity);
       suggestOptions.query_param_name = "prefix";

       // CORS / JSONP support
       if (service.ui && service.ui.access) {
          suggestOptions.access = service.ui.access;
       }

       if ('view' in service && 'url' in service.view && !('view_url' in suggestOptions)) {
          suggestOptions.formatter_url = service.view.url;
       }
    }
    
    var frame = DialogSystem.createDialog();
    frame.width("400px");

    var header = $('<div></div>').addClass("dialog-header").text($.i18n('core-views/search-match')).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

    $('<p></p>').text($.i18n('core-views/search-fb-topic')).appendTo(body);

    var input = $('<input />').appendTo($('<p></p>').appendTo(body));
    
    input.suggest(sanitizeSuggestOptions(suggestOptions)).on("fb-select", function(e, data) {
        var types = data.notable ? data.notable : [];
      
        Refine.postCoreProcess(
        "recon-match-specific-topic-to-cells",
        {
            columnName: column.name,
            topicID: data.id,
            topicName: data.name,
            types: types.join(","),
            identifierSpace: service.identifierSpace,
            schemaSpace: service.schemaSpace
        },
        null,
        { cellsChanged: true, columnStatsChanged: true }
        );

        DialogSystem.dismissUntil(level - 1);
    });

    $('<button class="button"></button>').text($.i18n('core-buttons/cancel')).on('click',function() {
      DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);

    var level = DialogSystem.showDialog(frame);
    input.trigger('focus').data("suggest").textchange();
  };

  var createReconServiceSelectionDialog = function(headerText, explanationText, onSelect) {
    var frame = DialogSystem.createDialog();
    frame.width("400px");
    frame.addClass('recon-service-selection-dialog');

    var header = $('<div></div>').addClass("dialog-header").text(headerText).appendTo(frame);
    var form = $('<form></form>').appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(form);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(form);
    
    $('<p></p>')
      .text(explanationText)
      .appendTo(body);
    $('<label></label>')
      .attr('for', 'dialog-recon-service-select')
      .text($.i18n('core-views/choose-reconciliation-service')).appendTo(body);
    var select = $('<select></select>')
      .attr('id', 'dialog-recon-service-select')
      .attr('name', 'dialog-recon-service-select')
      .appendTo(body);
    var services = ReconciliationManager.getAllServices();
    for (var i = 0; i < services.length; i++) {
        var service = services[i];
        $('<option></option>').val(service.url)
           .text(service.name)
           .appendTo(select);
    }

    $('<button class="button"></button>')
      .text($.i18n('core-buttons/cancel'))
      .on('click',function() {
         DialogSystem.dismissUntil(level - 1);
      })
      .appendTo(footer);

    $('<button class="button"></button>')
      .attr('type', 'submit')
      .html($.i18n('core-buttons/ok'))
      .appendTo(footer);

    form.on('submit', function() {
          var service = select.val();
          var identifierSpace = null;
          var schemaSpace = null;
          for(var i = 0; i < services.length; i++) {
            if(services[i].url === service) {
                identifierSpace = services[i].identifierSpace;
                schemaSpace = services[i].schemaSpace;
            }
          }
          if (identifierSpace === null) {
              alert($.i18n('core-views/choose-reconciliation-service-alert'));
          } else {
              onSelect(service, identifierSpace, schemaSpace);
              DialogSystem.dismissUntil(level - 1);
          }
      });

    var level = DialogSystem.showDialog(frame);
  };

  var doUseValuesAsIdentifiers = function() {
    var headerText = $.i18n('core-views/use-values-as-identifiers/header');
    var explanationText = $.i18n('core-views/use-values-as-identifiers-note');
    var onSelect = function(service, identifierSpace, schemaSpace) {
      Refine.postCoreProcess(
        "recon-use-values-as-identifiers",
        {
          columnName: column.name,
          service: service,
          identifierSpace: identifierSpace,
          schemaSpace: schemaSpace
        },
        null,
        { cellsChanged: true, columnStatsChanged: true }
      );
    };

    createReconServiceSelectionDialog(headerText, explanationText, onSelect);
  };


  function successCallBackForAddingIdColumn(columnName,dismissDialog)
  {
    Refine.postCoreProcess(
      "add-column", 
      {
        baseColumnName: column.name,  
        newColumnName: columnName, 
        columnInsertIndex: columnIndex + 1,
        onError: "set-to-blank"
      },
      { expression: "cell.recon.match.id" },
      { modelsChanged: true },
      { onDone: dismissDialog },
        );
  }

  var doAddIdcolumn = function() {
    
   promptForColumn(successCallBackForAddingIdColumn,'core-views/add-id-col');

      
  };


  var doCopyAcrossColumns = function() {
    var frame = $(DOM.loadHTML("core", "scripts/views/data-table/copy-recon-across-columns-dialog.html"));
    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n('core-views/copy-recon-judg')+" " + column.name);
    
    elmts.or_views_copyToCol.text($.i18n('core-views/copy-to-col'));
    elmts.or_views_copyOpt.text($.i18n('core-views/copy-opt'));
    elmts.or_views_applyToCell.text($.i18n('core-views/apply-to-cell'));
    elmts.or_views_whatToCopy.text($.i18n('core-views/what-to-copy'));
    elmts.or_views_newRecon.text($.i18n('core-views/new-recon'));
    elmts.or_views_matchRecon.text($.i18n('core-views/match-recon'));
    elmts.okButton.text($.i18n('core-buttons/copy'));
    elmts.cancelButton.text($.i18n('core-buttons/cancel'));

    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
      var column2 = columns[i];
      if (column !== column2) {
        $('<option>').val(column2.name).text(column2.name).appendTo(elmts.toColumnSelect);
      }
    }

    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

    elmts.cancelButton.on('click',dismiss);
    elmts.okButton.on('click',function() {
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
        alert($.i18n('core-views/warning-other-col'));
      } else if (config.judgment.length === 0) {
        alert($.i18n('core-views/warning-sel-judg'));
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
      label: $.i18n('core-views/start-recon'),
      tooltip: $.i18n('core-views/recon-text-fb'),
      click: doReconcile
    },
    {},
    {
      id: "core/facets",
      label: $.i18n('core-views/facets'),
      submenu: [
        {
          id: "core/by-judgment",
          label: $.i18n('core-views/by-judg'),
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : $.i18n("core-views/judgment", column.name),
                  "columnName" : column.name, 
                  "expression" : 'forNonBlank(cell.recon.judgment, v, v, if(isNonBlank(value), "(unreconciled)", "(blank)"))'
                },
                {
                  "scroll" : true
                }
            );
          }
        },
                {
          id: "core/by-judgment-actions",
          label: $.i18n('core-views/judg-actions'),
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : $.i18n('core-views/judg-actions2', column.name),
                  "columnName" : column.name, 
                  "expression" : "cell.recon.judgmentAction"
                }
            );
          }
        },
        {
          id: "core/by-judgment-history-entries",
          label: $.i18n('core-views/judg-hist'),
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : $.i18n('core-views/hist-entries', column.name),
                  "columnName" : column.name, 
                  "expression" : "cell.recon.judgmentHistoryEntry"
                }
            );
          }
        },
        {},
        {
          id: "core/by-best-candidates-score",
          label: $.i18n('core-views/best-score'),
          click: function() {
            ui.browsingEngine.addFacet(
                "range", 
                {
                  "name" : $.i18n('core-views/best-cand-score', column.name),
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
          label: $.i18n('core-views/best-type-match'),
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : $.i18n('core-views/best-cand-type-match', column.name),
                  "columnName" : column.name, 
                  "expression" : 'forNonBlank(cell.recon.features.typeMatch, v, v, if(isNonBlank(value), if(cell.recon != null, "(no type)", "(unreconciled)"), "(blank)"))'
                },
                {
                  "scroll" : true
                }
            );
          }
        },
        {
          id: "core/by-best-candidates-name-match",
          label: $.i18n('core-views/best-name'),
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : $.i18n('core-views/best-cand-name', column.name),
                  "columnName" : column.name, 
                  "expression" : 'forNonBlank(cell.recon.features.nameMatch, v, v, if(isNonBlank(value), "(unreconciled)", "(blank)"))'
                },
                {
                  "scroll" : true
                }
            );
          }
        },
        {},
        {
          id: "core/by-best-candidates-name-edit-distance",
          label: $.i18n('core-views/best-edit-dist'),
          click: function() {
            ui.browsingEngine.addFacet(
                "range", 
                {
                  "name" : $.i18n('core-views/best-cand-edit-dist', column.name),
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
          label: $.i18n('core-views/best-word-sim'),
          click: function() {
            ui.browsingEngine.addFacet(
                "range", 
                {
                  "name" :  $.i18n('core-views/best-cand-word-sim', column.name),
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
          label: $.i18n('core-views/best-type'),
          click: function() {
            ui.browsingEngine.addFacet(
                "list", 
                {
                  "name" : $.i18n("core-views/best-cand-types", column.name),
                  "columnName" : column.name,
                  "expression" : 'forNonBlank(cell.recon.best.type, v, v, if(isNonBlank(value), "(unreconciled)", "(blank)"))'
                }
            );
          }
        }
      ]
    },
    {
      id: "core/actions",
      label: $.i18n('core-views/actions'),
      submenu: [
        {
          id: "core/match-to-best-candidate",
          label: $.i18n('core-views/best-cand'),
          tooltip: $.i18n('core-views/best-cand2'),
          click: doReconMatchBestCandidates
        },
        {
          id: "core/match-to-new-topic",
          label: $.i18n('core-views/new-topic'),
          tooltip: $.i18n('core-views/new-topic2'),
          click: function() {
            doReconMarkNewTopics(false);
          }
        },
        {
          id: "core/match-similar-to-new-topic",
          label: $.i18n('core-views/one-topic'),
          tooltip: $.i18n('core-views/one-topic2'),
          click: function() {
            doReconMarkNewTopics(true);
          }
        },
        {
          id: "core/match-to-specific",
          label: $.i18n('core-views/filtered-cell'),
          tooltip: $.i18n('core-views/filtered-cell2'),
          click: doSearchToMatch
        },
        {},
        {
          id: "core/discard-judgments",
          label: $.i18n('core-views/discard-judg'),
          tooltip: $.i18n('core-views/discard-judg2'),
          click: doReconDiscardJudgments
        },
        {
          id: "core/clear-recon-data",
          label: $.i18n('core-views/clear-recon'),
          tooltip: $.i18n('core-views/clear-recon2'),
          click: doClearReconData
        }
      ]
    },
    {},
    {
      id: "core/copy-across-columns",
      label: $.i18n('core-views/copy-recon'),
      tooltip: $.i18n('core-views/copy-recon2'),
      click: doCopyAcrossColumns
    },
    {
      id: "core/use-values-as-identifiers",
      label: $.i18n('core-views/use-values-as-identifiers'),
      tooltip: $.i18n('core-views/use-values-as-identifiers2'),
      click: doUseValuesAsIdentifiers
    },
    {},
    {
      id: "core/Add-column-with-URLs-of-matched-entities",
      label: $.i18n("core-views/add-column-with-URLs-of-matched-entities"),
      tooltip: $.i18n("core-views/add-column-with-URLs-of-matched-entities-tooltip"),
      click: doAddColumnWithUrlOfMatchedEntities
    },
    {
      id: "core/add-id-column",
      label: $.i18n('core-views/add-id-column'),
      tooltip: $.i18n('core-views/add-id-column2'),
      click: doAddIdcolumn
    }
  ]);
});
