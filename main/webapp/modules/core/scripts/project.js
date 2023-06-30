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
  var leftPanelWidth = JSON.parse(Refine.getPreference("ui.browsing.facetsHistoryPanelWidth", 300));
  if(typeof leftPanelWidth != "number" || leftPanelWidth < 200 || leftPanelWidth > 500) {
    leftPanelWidth = 300;
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

  var processPanelWidth = 400;
  ui.processPanelDiv
  .css("width", processPanelWidth + "px")
  .css("left", Math.floor((width - processPanelWidth) / 2) + "px");
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
  $("#or-proj-ext").text($.i18n('core-project/extensions'));

  $('#project-name-button').on('click',Refine._renameProject);
  $('#project-permalink-button').on('focus',function() {
    this.href = Refine.getPermanentLink();
  });

  $('#app-home-button').attr('title', $.i18n('core-index/navigate-home'));

  Refine.setTitle();

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
  ui.processPanel = new ProcessPanel(ui.processPanelDiv);
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

Refine.setTitle = function(status) {
  var title = theProject.metadata.name + " - OpenRefine";
  if (status) {
    title = status + " - " + title;
  }
  document.title = title;

  $("#project-name-button").text(theProject.metadata.name);
};

Refine.reinitializeProjectData = function(f, fError) {
  function handleError(status, message, fError) {
    if (status === "error") {
      alert(message);
      if (fError) {
        fError();
      }
      return true;
    }
    return false;
  }

  $.when(
    $.getJSON("command/core/get-project-metadata?" + $.param({ project: theProject.id }), null),
    $.getJSON("command/core/get-models?" + $.param({ project: theProject.id }), null),
    $.getJSON("command/core/get-all-preferences", null),
  ).done(function(metadata, models, preferences) {
    metadata = metadata[0], models = models[0], preferences = preferences[0];
    if (
      handleError(metadata.status, metadata.message, fError) ||
      handleError(models.status, models.message, fError) ||
      handleError(preferences.status, preferences.message, fError)
    ) {
      return;
    }

    theProject.metadata = metadata;

    for (var n in models) {
      if (models.hasOwnProperty(n)) {
        theProject[n] = models[n];
      }
    }

    if (preferences) {
      thePreferences = preferences;
    }

    f();
  });
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
    pushFunction(function(onDone) {
      ui.dataTableView.update(onDone);
    });
    pushFunction(function(onDone) {
      ui.browsingEngine.update(onDone);
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

        if ("historyEntry" in o) {
          ui.processPanel.showUndo(o.historyEntry);
        }
      } else if (o.code == "pending") {
        if ("onPending" in callbacks) {
          try {
            callbacks.onPending(o);
          } catch (e) {
            Refine.reportException(e);
          }
        }
        ui.processPanel.update(updateOptions, callbacks.onFinallyDone);
      }
    }
  }

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
};

Refine.setAjaxInProgress = function() {
  $(document.body).attr("ajax_in_progress", "true");
};

Refine.clearAjaxInProgress = function() {
  $(document.body).attr("ajax_in_progress", "false");
};

/*
 *  Utility model functions
 */

Refine.cellIndexToColumn = function(cellIndex) {
  var columns = theProject.columnModel.columns;
  for (var i = 0; i < columns.length; i++) {
    var column = columns[i];
    if (column.cellIndex == cellIndex) {
      return column;
    }
  }
  return null;
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

Refine.fetchRows = function(start, limit, onDone, sorting) {
  var body = {
    engine: JSON.stringify(ui.browsingEngine.getJSON())
  };
  if (sorting) {
    body.sorting = JSON.stringify(sorting);
  }

  $.post(
    "command/core/get-rows?" + $.param({ project: theProject.id, start: start, limit: limit }),
    body,
    function(data) {
      if(data.code === "error") {
        data = theProject.rowModel;
      }
      theProject.rowModel = data;

      // Un-pool objects
      for (var r = 0; r < data.rows.length; r++) {
        var row = data.rows[r];
        for (var c = 0; c < row.cells.length; c++) {
          var cell = row.cells[c];
          if ((cell) && ("r" in cell)) {
            cell.r = data.pool.recons[cell.r];
          }
        }
      }

      if (onDone) {
        onDone();
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
  var params = URLUtil.getParameters();
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
