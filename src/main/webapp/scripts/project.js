var theProject;
var ui = {};

var Gridworks = {
};

Gridworks.reportException = function(e) {
    if (window.console) {
        console.log(e);
    }
};

function onLoad() {
    var params = URL.getParameters();
    if ("project" in params) {
        theProject = {
            id: parseInt(params.project)
        };
        
        var uiState = {};
        if ("ui" in params) {
            try {
                uiState = JSON.parse(params.ui);
            } catch (e) {
            }
        }
        
        Gridworks.reinitializeProjectData(function() {
            initializeUI(uiState);
        });
    }
}
$(onLoad);

function initializeUI(uiState) {
    document.title = theProject.metadata.name + " - Gridworks";
    
    var path = $("#path");
    $('<span></span>').text(theProject.metadata.name).addClass("app-path-section").appendTo(path);
    $('<span></span>').text(" project").appendTo(path);
    
    $('<span>').html(" &raquo; ").appendTo(path);
    $('<a href="javascript:{}"></a>')
        .addClass("app-path-section")
        .text("current view")
        .mouseenter(function() {
            this.href = Gridworks.getPermanentLink();
        })
        .appendTo(path);
    
    var body = $("#body").empty().html(
        '<div bind="viewPanel" class="view-panel"></div>' +
        '<div bind="facetPanel" class="facet-panel"></div>' +
        '<div bind="historyPanel" class="history-panel"></div>' +
        '<div bind="processPanel" class="process-panel"></div>' +
        '<div class="menu-bar-container" bind="menuBarContainer"><div bind="menuBarPanel" class="menu-bar"></div></div>'
    );
    ui = DOM.bind(body);
    
    ui.menuBarContainer.css("top", $("#header").outerHeight() + "px");
    ui.menuBar = new MenuBar(ui.menuBarPanel); // construct the menu first so we can resize everything else
    
    resize();

    ui.browsingEngine = new BrowsingEngine(ui.facetPanel, uiState.facets || []);
    ui.processWidget = new ProcessWidget(ui.processPanel);
    ui.historyWidget = new HistoryWidget(ui.historyPanel);
    ui.dataTableView = new DataTableView(ui.viewPanel);
    
    $(window).bind("resize", resizeAll);
}

function resize() {
    var header = $("#header");
    var footer = $("#footer");
    
    ui.menuBarContainer.css("top", header.outerHeight() + "px");

    var facetPanelWidth = 250;
    var width = $(window).width();
    var top = ui.menuBarContainer.offset().top + ui.menuBarContainer.outerHeight();
    var height = footer.offset().top - top;
    
    ui.viewPanel
        .css("top", top + "px")
        .css("height", height + "px")
        .css("left", "0px")
        .css("width", (width - facetPanelWidth) + "px");
        
    ui.facetPanel
        .css("top", top + "px")
        .css("height", height + "px")
        .css("left", (width - facetPanelWidth) + "px")
        .css("width", facetPanelWidth + "px");
}

function resizeAll() {
    resize();
    
    ui.menuBar.resize();
    ui.browsingEngine.resize();
    ui.processWidget.resize();
    ui.historyWidget.resize();
    ui.dataTableView.resize();
}

Gridworks.reinitializeProjectData = function(f) {
    Ajax.chainGetJSON(
        "/command/get-project-metadata?" + $.param({ project: theProject.id }), null,
        function(data) {
            theProject.metadata = data;
        },
        "/command/get-models?" + $.param({ project: theProject.id }), null,
        function(data) {
            theProject.columnModel = data.columnModel;
            theProject.protograph = data.protograph;
            
            for (var i = 0; i < theProject.columnModel.columns.length; i++) {
                theProject.columnModel.columns[i].collapsed = false;
            }
        },
        f
    );
}

/*
 *  Utility state functions
 */
 
Gridworks.createUpdateFunction = function(options, onFinallyDone) {
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
    if (options["everythingChanged"] || options["modelsChanged"] || options["columnStatsChanged"]) {
        pushFunction(Gridworks.reinitializeProjectData);
    }
    if (options["everythingChanged"] || options["modelsChanged"] || options["rowsChanged"] || options["rowMetadataChanged"] || options["cellsChanged"] || options["engineChanged"]) {
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

Gridworks.update = function(options, onFinallyDone) {
    var done = false;
    var dismissBusy = null;
    
    Gridworks.createUpdateFunction(options, function() {
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

Gridworks.postProcess = function(command, params, body, updateOptions, callbacks) {
    params = params || {};
    params.project = theProject.id;
    
    body = body || {};
    body.engine = JSON.stringify(ui.browsingEngine.getJSON());
    
    updateOptions = updateOptions || {};
    callbacks = callbacks || {};
    
    var done = false;
    var dismissBusy = null;
    
    function onDone(o) {
        done = true;
        if (dismissBusy) {
            dismissBusy();
        }
        
        if (o.code == "error") {
            if ("onError" in callbacks) {
                try {
                    callbacks["onError"](o);
                } catch (e) {
                    Gridworks.reportException(e);
                }
            }
        } else {
            if ("onDone" in callbacks) {
                try {
                    callbacks["onDone"](o);
                } catch (e) {
                    Gridworks.reportException(e);
                }
            }
            
            if (o.code == "ok") {
                Gridworks.update(updateOptions, callbacks["onFinallyDone"]);
                
                if ("historyEntry" in o) {
                    ui.processWidget.showUndo(o.historyEntry);
                }
            } else if (o.code == "pending") {
                if ("onPending" in callbacks) {
                    try {
                        callbacks["onPending"](o);
                    } catch (e) {
                        Gridworks.reportException(e);
                    }
                }
                ui.processWidget.update(updateOptions, callbacks["onFinallyDone"]);
            }
        }
    }
    
    $.post(
        "/command/" + command + "?" + $.param(params),
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

/*
 *  Utility model functions
 */
 
Gridworks.cellIndexToColumn = function(cellIndex) {
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        if (column.cellIndex == cellIndex) {
            return column;
        }
    }
    return null;
};

Gridworks.fetchRows = function(start, limit, onDone) {
    $.post(
        "/command/get-rows?" + $.param({ project: theProject.id, start: start, limit: limit }),
        { engine: JSON.stringify(ui.browsingEngine.getJSON()) },
        function(data) {
            theProject.rowModel = data;
            if (onDone) {
                onDone();
            }
        },
        "json"
    );
};

Gridworks.getPermanentLink = function() {
    var params = [
        "project=" + escape(theProject.id),
        "ui=" + escape(JSON.stringify({
            facets: ui.browsingEngine.getFacetUIStates()
        }))
    ];
    return "project.html?" + params.join("&");
};
