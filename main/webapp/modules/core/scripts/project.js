var theProject;
var ui = {};

var Refine = {
    refineHelperService: "http://1.refine-helper.stefanomazzocchi.user.dev.freebaseapps.com"
};

Refine.reportException = function(e) {
    if (window.console) {
        console.log(e);
    }
};

function resize() {
    var header = $("#header");
    
    var leftPanelWidth = 300;
    var width = $(window).width();
    var top = $("#header").outerHeight();
    var height = $(window).height() - top;
    
    var leftPanelPaddings = ui.leftPanel.outerHeight(true) - ui.leftPanel.height();
    ui.leftPanel
        .css("top", top + "px")
        .css("left", "0px")
        .css("height", (height - leftPanelPaddings) + "px")
        .css("width", leftPanelWidth + "px");
        
    var leftPanelTabsPaddings = ui.leftPanelTabs.outerHeight(true) - ui.leftPanelTabs.height();
    ui.leftPanelTabs.height(ui.leftPanel.height() - leftPanelTabsPaddings);
    
    var rightPanelVPaddings = ui.rightPanel.outerHeight(true) - ui.rightPanel.height();
    var rightPanelHPaddings = ui.rightPanel.outerWidth(true) - ui.rightPanel.width();
    ui.rightPanel
        .css("top", top + "px")
        .css("left", leftPanelWidth + "px")
        .css("height", (height - rightPanelVPaddings) + "px")
        .css("width", (width - leftPanelWidth - rightPanelHPaddings) + "px");
    
    ui.viewPanel.height((height - ui.toolPanel.outerHeight() - rightPanelVPaddings) + "px");
    
    var processPanelWidth = 400;
    ui.processPanel
        .css("width", processPanelWidth + "px")
        .css("left", Math.floor((width - processPanelWidth) / 2) + "px");
}

function resizeTabs() {
    var totalHeight = ui.leftPanel.height();
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
    ui.processWidget.resize();
    ui.historyWidget.resize();
    ui.dataTableView.resize();
}

function initializeUI(uiState) {
    $("#loading-message").hide();
    $("#project-title").show();
    $("#project-controls").show();
    $("#body").show();
    
    $('#project-name-button').click(Refine._renameProject);
    $('#project-permalink-button').mouseenter(function() {
        this.href = Refine.getPermanentLink();
    });
    
    Refine.setTitle();

    ui = DOM.bind($("#body"));
    
    ui.extensionBar = new ExtensionBar(ui.extensionBar); // construct the menu first so we can resize everything else
    ui.exporterManager = new ExporterManager($("#export-button"));
    
    ui.leftPanelTabs.tabs({ selected: 0 });
    resize();
    resizeTabs();
    
    ui.summaryWidget = new SummaryWidget(ui.summaryBar);
    ui.browsingEngine = new BrowsingEngine(ui.facetPanel, uiState.facets || []);
    ui.processWidget = new ProcessWidget(ui.processPanel);
    ui.historyWidget = new HistoryWidget(ui.historyPanel, ui.historyTabHeader);
    ui.dataTableView = new DataTableView(ui.viewPanel);
    
    ui.leftPanelTabs.bind('tabsshow', function(event, tabs) {
        if (tabs.index === 0) {
            ui.browsingEngine.resize();
        } else if (tabs.index === 1) {
            ui.historyWidget.resize();
        }
    });
    
    $(window).bind("resize", resizeAll);
    
    if (uiState.facets) {
        Refine.update({ engineChanged: true });
    }
}

Refine.setTitle = function(status) {
    var title = theProject.metadata.name + " - Google Refine";
    if (status) {
        title = status + " - " + title;
    }
    document.title = title;
    
    $("#project-name-button").text(theProject.metadata.name);
};

Refine.reinitializeProjectData = function(f) {
    Ajax.chainGetJSON(
        "/command/core/get-project-metadata?" + $.param({ project: theProject.id }), null,
        function(data) {
            theProject.metadata = data;
        },
        "/command/core/get-models?" + $.param({ project: theProject.id }), null,
        function(data) {
            for (var n in data) {
                if (data.hasOwnProperty(n)) {
                    theProject[n] = data[n];
                }
            }
        },
        f
    );
};

Refine._renameProject = function() {
    var name = window.prompt("New project name:", theProject.metadata.name);
    if (name == null) {
        return;
    }

    name = $.trim(name);
    if (theProject.metadata.name == name || name.length == 0) {
        return;
    }

    $.ajax({
        type: "POST",
        url: "/command/core/rename-project",
        data: { "project" : theProject.id, "name" : name },
        dataType: "json",
        success: function (data) {
            if (data && typeof data.code != 'undefined' && data.code == "ok") {
                theProject.metadata.name = name;
                Refine.setTitle();
            } else {
                alert("Failed to rename project: " + data.message);
            }
        }
    });
};

/*
 *  Utility state functions
 */
 
Refine.createUpdateFunction = function(options, onFinallyDone) {
    var functions = [];
    var pushFunction = function(f) {
        var index = functions.length;
        functions.push(function() {
            f(functions[index + 1]);
        });
    };
    
    pushFunction(function(onDone) {
        ui.historyWidget.update(onDone);
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
    
    functions.push(onFinallyDone || function() {});
    
    return functions[0];
};

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
                    ui.processWidget.showUndo(o.historyEntry);
                }
            } else if (o.code == "pending") {
                if ("onPending" in callbacks) {
                    try {
                        callbacks.onPending(o);
                    } catch (e) {
                        Refine.reportException(e);
                    }
                }
                ui.processWidget.update(updateOptions, callbacks.onFinallyDone);
            }
        }
    }
    
    Refine.setAjaxInProgress();
    
    $.post(
        "/command/" + moduleName + "/" + command + "?" + $.param(params),
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

Refine.preparePool = function(pool) {
    for (var id in pool.recons) {
        if (pool.recons.hasOwnProperty(id)) {        
            var recon = pool.recons[id];
            if (recon.m) {
                recon.m = pool.reconCandidates[recon.m];
            }
            if (recon.c) {
                for (var j = 0; j < recon.c.length; j++) {
                    recon.c[j] = pool.reconCandidates[recon.c[j]];
                }
            }
        }
    }
};

Refine.fetchRows = function(start, limit, onDone, sorting) {
    var body = {
        engine: JSON.stringify(ui.browsingEngine.getJSON())
    };
    if (sorting) {
        body.sorting = JSON.stringify(sorting)
    }
    
    $.post(
        "/command/core/get-rows?" + $.param({ project: theProject.id, start: start, limit: limit }) + "&callback=?",
        body,
        function(data) {
            theProject.rowModel = data;
            
            // Un-pool objects
            Refine.preparePool(data.pool);
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
        "jsonp"
    );
};

Refine.getPermanentLink = function() {
    var params = [
        "project=" + escape(theProject.id),
        "ui=" + escape(JSON.stringify({
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
                uiState = JSON.parse(params.ui);
            } catch (e) {
            }
        }
        
        Refine.reinitializeProjectData(function() {
            initializeUI(uiState);
        });
    }
}

$(onLoad);
