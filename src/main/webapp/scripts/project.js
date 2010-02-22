var theProject;
var ui = {};

function onLoad() {
    var params = URL.getParameters();
    if ("project" in params) {
        theProject = {
            id: parseInt(params.project)
        };
        
        reinitializeProjectData(initializeUI);
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

function reinitializeProjectData(f) {
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

function cellIndexToColumn(index) {
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        if (column.cellIndex == index) {
            return column;
        }
    }
    return null;
}