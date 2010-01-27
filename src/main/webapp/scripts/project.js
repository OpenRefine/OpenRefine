var theProject;
var ui = {};

function onLoad() {
    var params = URL.getParameters();
    if ("project" in params) {
        theProject = {
            id: parseInt(params.project),
            view: {
                pageSize: 25
            }
        };
        
        Ajax.chainGetJSON(
            "/command/get-project-metadata?" + $.param({ project: theProject.id }), null,
            function(data) {
                theProject.metadata = data;
            },
            "/command/get-column-model?" + $.param({ project: theProject.id }), null,
            function(data) {
                theProject.columnModel = data;
                for (var i = 0; i < theProject.columnModel.columns.length; i++) {
                    theProject.columnModel.columns[i].collapsed = false;
                }
            },
            "/command/get-rows?" + $.param({ project: theProject.id, start: 0, limit: 25 }), null,
            function(data) {
                theProject.rowModel = data;
            },
            function() {
                initializeUI();
            }
        );
    }
}
$(onLoad);

function initializeUI() {
    document.title = theProject.metadata.name + " - Gridlock";
    $("#title").html(document.title);
    
    var body = $("#body").empty();
    
    var table = document.createElement("table");
    $(table).attr("cellspacing", 20).css("width", "100%");
    body.append(table);
    
    var tr = table.insertRow(0);
    
    var tdLeft = tr.insertCell(0);
    var tdRight = tr.insertCell(1);
    tdLeft.setAttribute("width", "75%");
    tdRight.setAttribute("width", "25%");
    
    ui.viewPanel = $('<div></div>').appendTo(tdLeft).css("width", tdLeft.offsetWidth + "px").css("overflow-x", "auto");
    ui.facetPanel = $('<div></div>').appendTo(tdRight);
    ui.historyPanel = $('<div></div>').addClass("history-panel").appendTo(document.body);
    
    ui.dataTableView = new DataTableView(ui.viewPanel);
    ui.historyWidget = new HistoryWidget(ui.historyPanel);
}
