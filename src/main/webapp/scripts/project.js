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
        
        Gridworks.reinitializeProjectData(initializeUI);
    }
}
$(onLoad);

function initializeUI() {
    document.title = theProject.metadata.name + " - Gridworks";
    $('<span></span>').text(theProject.metadata.name).addClass("app-path-section").appendTo($("#path"));
    $('<span></span>').text(" project").appendTo($("#path"));
    
    var body = $("#body").empty();
    
    var table = document.createElement("table");
    $(table).attr("cellspacing", 20).css("width", "100%");
    body.append(table);
    
    var tr = table.insertRow(0);
    
    var tdLeft = tr.insertCell(0);
    var tdRight = tr.insertCell(1);
    tdLeft.setAttribute("width", "82%");
    tdRight.setAttribute("width", "18%");
    
    ui.viewPanel = $('<div></div>').appendTo(tdLeft);
    ui.facetPanel = $('<div></div>').appendTo(tdRight);
    ui.historyPanel = $('<div></div>').addClass("history-panel").appendTo(document.body);
    ui.processPanel = $('<div></div>').addClass("process-panel").appendTo(document.body);
    ui.menuBarPanel = $('<div></div>'); $("#header").after(ui.menuBarPanel);
    
    ui.browsingEngine = new BrowsingEngine(ui.facetPanel);
    ui.processWidget = new ProcessWidget(ui.processPanel);
    ui.historyWidget = new HistoryWidget(ui.historyPanel);
    ui.dataTableView = new DataTableView(ui.viewPanel);
    ui.menuBar = new MenuBar(ui.menuBarPanel);
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
    if (options["everythingChanged"] || options["modelsChanged"]) {
        pushFunction(Gridworks.reinitializeProjectData);
    }
    if (options["everythingChanged"] || options["modelsChanged"] || options["rowsChanged"] || options["cellsChanged"] || options["engineChanged"]) {
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
