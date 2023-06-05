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

var theProject;
var thePreferences;
var ui = {};

var Refine = {};

I18NUtil.init("core");

Refine.wrapCSRF = CSRFUtil.wrapCSRF;
Refine.postCSRF = CSRFUtil.postCSRF;

Refine.reportException = function(e) {
  if (window.console) {
    console.log(e);
  }
};

function resize() {
  var leftPanelWidth = JSON.parse(Refine.getPreference("ui.browsing.facetsHistoryPanelWidth", 350));
  if(typeof leftPanelWidth != "number" || leftPanelWidth < 200 || leftPanelWidth > 500) { 
    leftPanelWidth = 350; 
  }

  var width = $(window).width();
  var top = $("#header").outerHeight();
  var height = $(window).height() - top;
  
  if (ui.leftPanelDiv.css('display') == "none") { leftPanelWidth = 0; }

  var leftPanelPaddings = ui.leftPanelDiv.outerHeight(true) - ui.leftPanelDiv.height();
  ui.leftPanelDiv
  .css("top", top + "px")
  .css("left", "0px")
  .css("height", (height - leftPanelPaddings) + "px")
  .css("width", leftPanelWidth + "px");

  var leftPanelTabsPaddings = ui.leftPanelTabs.outerHeight(true) - ui.leftPanelTabs.height();
  ui.leftPanelTabs.height(ui.leftPanelDiv.height() - leftPanelTabsPaddings);

  var rightPanelVPaddings = ui.rightPanelDiv.outerHeight(true) - ui.rightPanelDiv.height();
  var rightPanelHPaddings = ui.rightPanelDiv.outerWidth(true) - ui.rightPanelDiv.width();
  ui.rightPanelDiv
  .css("top", top + "px")
  .css("left", leftPanelWidth + "px")
  .css("height", (height - rightPanelVPaddings) + "px")
  .css("width", (width - leftPanelWidth - rightPanelHPaddings) + "px");

  ui.viewPanelDiv.height((height - ui.toolPanelDiv.outerHeight() - rightPanelVPaddings) + "px");

  var notificationContainerWidth = 400;
  ui.notificationContainer
  .css("width", notificationContainerWidth + "px")
  .css("left", Math.floor((width - notificationContainerWidth) / 2) + "px");
}

function resizeTabs() {
  var totalHeight = ui.leftPanelDiv.height();
  var headerHeight = ui.leftPanelTabs.find(".ui-tabs-nav").outerHeight(true);

  var visibleTabPanels = ui.leftPanelTabs.find(".ui-tabs-panel:not(.ui-tabs-hide)");
  var paddings = visibleTabPanels.outerHeight(true) - visibleTabPanels.height();

  var allTabPanels = ui.leftPanelTabs.find(".ui-tabs-panel");
  allTabPanels.height(totalHeight - headerHeight - paddings - 1);
}

function resizeAll() {
  resize();
  resizeTabs();
  ui.extensionBar.resize();
  ui.browsingEngine.resize();
  ui.processPanel.resize();
  ui.historyPanel.resize();
  ui.dataTableView.resize();
  if (SchemaAlignment) {
    SchemaAlignment.resize();
  }
}

function initializeUI(uiState) {
  $("#loading-message").hide();
  $("#notification-container").hide();
  $("#body").show();
  
  $("#or-proj-open").text($.i18n('core-project/open')+"...");
  $("#project-permalink-button").text($.i18n('core-project/permalink'));
  $("#project-name-button").attr("title",$.i18n('core-project/proj-name'));
  $("#or-proj-export").text($.i18n('core-project/export'));
  $("#or-proj-help").text($.i18n('core-project/help'));
  $("#or-proj-starting").text($.i18n('core-project/starting')+"...");
  $("#or-proj-facFil").text($.i18n('core-project/facet-filter'));
  $("#or-proj-undoRedo").text($.i18n('core-project/undo-redo'));
  $("#or-proj-process").text($.i18n('core-project/processes'));
  $("#or-proj-ext").text($.i18n('core-project/extensions'));

  $('#project-name-button').on('click',Refine._renameProject);
  $('#project-permalink-button').on('focus',function() {
    this.href = Refine.getPermanentLink();
  });

  $('#app-home-button').attr('title', $.i18n('core-index/navigate-home'));

  Refine.setTitle();
  Refine.clearAjaxInProgress();

  ui = DOM.bind($("#body"));

  ui.extensionBar = new ExtensionBar(ui.extensionBarDiv); // construct the menu first so we can resize everything else
  ui.exporterManager = new ExporterManager($("#export-button"));

  ui.leftPanelTabs.tabs();
  resize();
  resizeTabs();

  $('<button>').attr("id", "hide-left-panel-button")
    .addClass("visibility-panel-button")
    .attr("aria-label", $.i18n('core-index/hide-panel'))
    .on('click',function() { Refine._showHideLeftPanel(); })
    .prependTo(ui.leftPanelTabs);

  $('<button>').attr("id", "show-left-panel-button")
    .addClass("visibility-panel-button")
    .attr("aria-label", $.i18n('core-index/show-panel'))
    .on('click',function() { Refine._showHideLeftPanel(); })
    .prependTo(ui.toolPanelDiv);
  
  ui.summaryBar = new SummaryBar(ui.summaryBarDiv);
  ui.browsingEngine = new BrowsingEngine(ui.facetPanelDiv, uiState.facets || []);
  ui.processPanel = new ProcessPanel(ui.notificationContainer, ui.processPanelDiv, ui.processTabHeader);
  ui.historyPanel = new HistoryPanel(ui.historyPanelDiv, ui.historyTabHeader);
  ui.dataTableView = new DataTableView(ui.viewPanelDiv);

  ui.leftPanelTabs.on('tabsactivate', function(event, tabs) {
    tabs.newPanel.trigger('resize');
  });

  $(window).on("resize", resizeAll);

  if (uiState.facets) {
    Refine.update({ engineChanged: true });
  }
}

Refine._showHideLeftPanel = function() {
  $('div#body').toggleClass("hide-left-panel");
  resizeAll();
};

Refine.showLeftPanel = function() {
  $('div#body').removeClass("hide-left-panel");
  if(ui.browsingEngine == undefined || ui.browsingEngine.resize == undefined) return;
  resizeAll();
};

Refine.activateLeftPanelTab = function(tab) {
  var index = 0;
  if (tab === 'facets') {
    index = 0;
  } else if (tab === 'undoRedo') {
    index = 1;
  } else if (tab === 'process') {
    index = 2;
  }
  ui.leftPanelTabs.tabs({ active: index });
};

Refine.setTitle = function(status) {
  var title = theProject.metadata.name + " - OpenRefine";
  if (status) {
    title = status + " - " + title;
  }
  document.title = title;

  $("#project-name-button").text(theProject.metadata.name);
};

Refine.reinitializeProjectData = function(f, fError) {
  $.getJSON(
    "command/core/get-project-metadata?" + $.param({ project: theProject.id }), null,
    function(data) {
      if (data.status == "error") {
        alert(data.message);
        if (fError) {
          fError();
        }
      } else {
        theProject.metadata = data;
        $.getJSON(
          "command/core/get-models?" + $.param({ project: theProject.id }), null,
          function(data) {
            if (data.status == "error") {
              alert(data.message);
              if (fError) {
                fError();
              }
            } else {
              for (var n in data) {
                if (data.hasOwnProperty(n)) {
                  theProject[n] = data[n];
                }
              }
              $.post(
                "command/core/get-all-preferences", null,
                function(preferences) {
                  if (preferences.status == "error") {
                    alert(preferences.message);
                    if (fError) {
                      fError();
                    }
                  } else {
                    if (preferences != null) {
                      thePreferences = preferences;
                    }
                    f();
                  }
                },
                'json'
              );
            }
          },
          'json'
        );
      }
    },
    'json'
  );
};

Refine.getPreference = function(key, defaultValue) { 
  if(!thePreferences.hasOwnProperty(key)) { return defaultValue; }

  return thePreferences[key];
}

Refine.setPreference = function(key, newValue) { 
  thePreferences[key] = newValue;

  Refine.wrapCSRF(function(token) {
    $.ajax({
      async: false,
      type: "POST",
      url: "command/core/set-preference?" + $.param({ name: key }),
      data: {
        "value" : JSON.stringify(newValue), 
        csrf_token: token
      },
      success: function(data) { },
      dataType: "json"
    });
  });
}

Refine._renameProject = function() {
  var name = window.prompt($.i18n('core-index/new-proj-name'), theProject.metadata.name);
  if (name === null) {
    return;
  }

  name = jQueryTrim(name);
  if (theProject.metadata.name == name || name.length === 0) {
    return;
  }

  Refine.postCSRF(
    "command/core/rename-project",
    { "project" : theProject.id, "name" : name },
    function (data) {
      if (data && typeof data.code != "undefined" && data.code == "ok") {
        theProject.metadata.name = name;
        Refine.setTitle();
      } else {
        alert($.i18n('core-index/error-rename')+" " + data.message);
      }
    },
    "json"
  );
};

/*
 *  Utility state functions
 */

Refine.customUpdateCallbacks = [];

Refine.createUpdateFunction = function(options, onFinallyDone) {
  var functions = [];
  var pushFunction = function(f) {
    var index = functions.length;
    functions.push(function() {
      f(functions[index + 1]);
    });
  };

  pushFunction(function(onDone) {
    ui.historyPanel.update(onDone);
  });
  if (options.everythingChanged || options.modelsChanged || options.columnStatsChanged) {
    pushFunction(Refine.reinitializeProjectData);
  }
  if (options.everythingChanged || options.modelsChanged || options.rowsChanged || options.rowMetadataChanged || options.cellsChanged || options.engineChanged) {
    var preservePage = options.rowIdsPreserved && (options.recordIdsPreserved || ui.browsingEngine.getMode() === "row-based");
    pushFunction(function(onDone) {
      ui.dataTableView.update(onDone, preservePage);
    });
    pushFunction(function(onDone) {
      ui.browsingEngine.update(onDone);
    });
  }
  if (options.everythingChanged || options.processesChanged) {
    pushFunction(function(onDone) {
      ui.processPanel.update(onDone);
    });
  }

  // run the callbacks registered by extensions, passing them
  // the options
  pushFunction(function(onDone) {
    for(var i = 0; i != Refine.customUpdateCallbacks.length; i++) {
        Refine.customUpdateCallbacks[i](options);
    }
    onDone();
  });

  functions.push(onFinallyDone || function() {});

  return functions[0];
};

/*
 * Registers a callback function to be called after each update.
 * This is provided for extensions which need to run some code when
 * the project is updated. This was introduced for the Wikidata 
 * extension as a means to avoid monkey-patching Refine's core
 * methods (which was the solution adopted for GOKb, as they had
 * no way to change Refine's code directly).
 *
 * The function will be called with an "options" object as above
 * describing which change has happened, so that the code can run
 * fine-grained updates.
 */
Refine.registerUpdateFunction = function(callback) {
   Refine.customUpdateCallbacks.push(callback);
}

Refine.update = function(options, onFinallyDone) {
  var done = false;
  var dismissBusy = null;

  Refine.setAjaxInProgress();

  Refine.createUpdateFunction(options, function() {
    Refine.clearAjaxInProgress();

    done = true;
    if (dismissBusy) {
      dismissBusy();
    }
    if (onFinallyDone) {
      onFinallyDone();
    }
  })();

  window.setTimeout(function() {
    if (!done) {
      dismissBusy = DialogSystem.showBusy();
    }
  }, 500);
};

Refine.postOperation = function(operation, updateOptions, callbacks) {
  Refine.postOperations([ operation ], updateOptions, callbacks);
};

Refine.postOperations = function(operations, updateOptions, callbacks) {
  callbacks = callbacks || {};
  updateOptions = updateOptions || {};

  // add engine options to each operation
  if (!("includeEngine" in updateOptions) || updateOptions.includeEngine) {
    var engineConfig = ("engineConfig" in updateOptions ?
            updateOptions.engineConfig :
            ui.browsingEngine.getJSON());

    for (let operation of operations) {
      operation.engineConfig = engineConfig;
    }
  }

  Refine.postCoreProcess(
    "apply-operations",
    {},
    { operations: JSON.stringify(operations) },
    updateOptions,
    {
      onDone: function(o) {
        // show pill notification for the last operation to have been successfully applied,
        // and compute whether all operations successfully applied preserved rows / records
        var latestOperationResult = null;
        updateOptions.rowIdsPreserved = true;
        updateOptions.recordIdsPreserved = true;
        for (let operationResult of o.results) {
          if (operationResult.historyEntry) {
            latestOperationResult = operationResult;
            updateOptions.rowIdsPreserved = updateOptions.rowIdsPreserved && latestOperationResult.historyEntry.gridPreservation !== 'no-row-preservation';
            updateOptions.recordIdsPreserved = updateOptions.recordIdsPreserved && latestOperationResult.historyEntry.gridPreservation === 'preserves-records'; 
            updateOptions.processesChanged = true;
            if (latestOperationResult.changeResult.createdFacets) {
              for (let facetConfig of latestOperationResult.changeResult.createdFacets) {
                let facetType = facetConfig.type;
                ui.browsingEngine.addFacet(facetType.indexOf('/') != -1 ? facetType.split('/')[1] : facetType, facetConfig, {});
              }
            }
          }
        }
        if (latestOperationResult && latestOperationResult.historyEntry) {
          ui.processPanel.showUndo(latestOperationResult.historyEntry);
        }
        if (callbacks.onDone) {
          callbacks.onDone(latestOperationResult);
        }
      },
      onError: function(o) {
        var operationsApplied = o.results.length - 1;
        var errorMessage = o.results[o.results.length - 1].error.message;
        if (operationsApplied) {
          errorMessage = $.i18n('core-project/some-operations-applied-but-error', operationsApplied, errorMessage);
          Refine.update({ everythingChanged: true });
        }
        if (callbacks.onError) {
          callbacks.onError(errorMessage);
        } else {
          alert(errorMessage);
        }
      }
    }
  );
};

Refine.postCoreProcess = function(command, params, body, updateOptions, callbacks) {
  Refine.postProcess("core", command, params, body, updateOptions, callbacks);
};

Refine.postProcess = function(moduleName, command, params, body, updateOptions, callbacks) {
  updateOptions = updateOptions || {};
  callbacks = callbacks || {};

  params = params || {};
  params.project = theProject.id;

  body = body || {};
  if (!("includeEngine" in updateOptions) || updateOptions.includeEngine) {
    body.engine = JSON.stringify(
        "engineConfig" in updateOptions ?
            updateOptions.engineConfig :
              ui.browsingEngine.getJSON()
    );
  }

  var done = false;
  var dismissBusy = null;

  function onDone(o) {
    done = true;
    if (dismissBusy) {
      dismissBusy();
    }

    Refine.clearAjaxInProgress();

    if (o.code == "error") {
      if ("onError" in callbacks) {
        try {
          callbacks.onError(o);
        } catch (e) {
          Refine.reportException(e);
        }
      } else {
        alert(o.message);
      }
    } else {
      if ("onDone" in callbacks) {
        try {
          callbacks.onDone(o);
        } catch (e) {
          Refine.reportException(e);
        }
      }

      if (o.code == "ok") {
        Refine.update(updateOptions, callbacks.onFinallyDone);
      }
    }
  }

  var runChange = function() {
    Refine.setAjaxInProgress();

    Refine.postCSRF(
        "command/" + moduleName + "/" + command + "?" + $.param(params),
        body,
        onDone,
        "json"
    );

    window.setTimeout(function() {
        if (!done) {
        dismissBusy = DialogSystem.showBusy();
        }
    }, 500);
  }

  var undoneChanges = ui.historyPanel.undoneChanges();
  if (Refine.getPreference("ui.history.warnAgainstDeletion", 'true') === 'true' && undoneChanges.length > 0 && (!("warnAgainstHistoryErasure" in updateOptions) || updateOptions.warnAgainstHistoryErasure)) {
    Refine._confirmHistoryErasure(undoneChanges, runChange);
  } else {
    runChange();
  }
};

Refine.setAjaxInProgress = function() {
  $(document.body).attr("ajax_in_progress", "true");
};

Refine.clearAjaxInProgress = function() {
  $(document.body).attr("ajax_in_progress", "false");
};

/**
 * Confirmation of erasure of history, when applying an operation with changes undone
 */
Refine._confirmHistoryErasure = function(entries, onDone) {
  var self = this;
  var frame = $(DOM.loadHTML("core", "scripts/util/confirm-history-erasure-dialog.html"));
  var elmts = DOM.bind(frame);
  var level = DialogSystem.showDialog(frame);
  
  elmts.dialogHeader.text($.i18n('core-project/confirm-erasure-of-project-history'));
  elmts.warningText.text($.i18n('core-project/applying-change-erases-entries', entries.length));
  elmts.doNotWarnText.text($.i18n('core-project/do-not-warn'));
  elmts.cancelButton.text($.i18n('core-buttons/cancel'));
  elmts.okButton.text($.i18n('core-buttons/apply-anyway'));

  // populate the history entries
  for (let entry of entries) {
    var entryDom = $(DOM.loadHTML("core", "scripts/project/history-entry.html")).appendTo(elmts.entryList);
    var entryElmts = DOM.bind(entryDom);
    entryElmts.entryDescription.text(entry.description);
  }

  var updateWarnPreferences = function () {
    var doNotWarnCheckBox = elmts.doNotWarnCheckbox.is(':checked');
    if (doNotWarnCheckBox) {
      Refine.setPreference('ui.history.warnAgainstDeletion', 'false');
    }
  };
  
  elmts.form.on('submit', function() {
    DialogSystem.dismissUntil(level - 1);
    updateWarnPreferences();
    onDone();
  });
  elmts.cancelButton.on('click',function() {
    updateWarnPreferences();
    DialogSystem.dismissUntil(level - 1);
  });
};

/*
 *  Utility model functions
 */

Refine.cellIndexToColumn = function(cellIndex) {
  var columns = theProject.columnModel.columns;
  if (0 <= cellIndex && cellIndex < columns.length) {
     return columns[cellIndex];
  }
};
Refine.columnNameToColumn = function(columnName) {
  var columns = theProject.columnModel.columns;
  for (var i = 0; i < columns.length; i++) {
    var column = columns[i];
    if (column.name == columnName) {
      return column;
    }
  }
  return null;
};
Refine.columnNameToColumnIndex = function(columnName) {
  var columns = theProject.columnModel.columns;
  for (var i = 0; i < columns.length; i++) {
    var column = columns[i];
    if (column.name == columnName) {
      return i;
    }
  }
  return -1;
};

/*
  Fetch rows after or before a given row id. The engine configuration can also
  be used to set filters (facets) or switch between rows/records mode.
*/
Refine.fetchRows = function(paginationOptions, limit, onDone, sorting) {
  var body = {
    engine: JSON.stringify(ui.browsingEngine.getJSON())
  };
  if (sorting) {
    body.sorting = JSON.stringify(sorting);
  }

  var fullSettings = {
     engine: ui.browsingEngine.getJSON(),
     paginationOptions,
     limit,
     sorting
  };
  
  Refine._lastRequestedPaginationSettings = fullSettings;
  $.post(
    "command/core/get-rows?" + $.param({ ...paginationOptions, project: theProject.id, limit: limit }),
    body,
    function(data) {
      if (fullSettings === Refine._lastRequestedPaginationSettings) {
        if (data.code !== "error") {
          theProject.rowModel = data;
        }

        if (onDone) {
          onDone();
        }
      }
    },
    "json"
  );
};

Refine.getPermanentLink = function() {
  var params = [
    "project=" + encodeURIComponent(theProject.id),
    "ui=" + encodeURIComponent(JSON.stringify({
      facets: ui.browsingEngine.getFacetUIStates()
    }))
  ];
  return "project?" + params.join("&");
};

/*
 * Loader
 */

function onLoad() {
  var params = URL.getParameters();
  if ("project" in params) {
    var uiState = {};
    if ("ui" in params) {
      try {
        uiState = JSON.parse(decodeURIComponent(params.ui));
      } catch (e) {
      }
    }

    Refine.reinitializeProjectData(
      function() {
        initializeUI(uiState);
      },
      function() {
        $("#loading-message").hide();
      }
    );
  }
}

$(onLoad);
