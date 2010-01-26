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
            },
            "/command/get-rows?" + $.param({ project: theProject.id, start: 0, limit: 25 }), null,
            function(data) {
                theProject.rowModel = data;
            },
            function() {
                initializeUI();
                renderView();
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
}

function renderView() {
    ui.viewPanel.empty();
    
    var divSummary = $('<div></div>').addClass("viewPanel-summary").appendTo(ui.viewPanel);
    $('<span>' + 
            (theProject.rowModel.start + 1) + " to " + 
            (theProject.rowModel.start + theProject.rowModel.limit) + " of " + 
            (theProject.rowModel.total) + 
            " rows total" + 
        '</span>'
    ).appendTo(divSummary);
    
    var pagingControls = $('<div></div>').addClass("viewPanel-pagingControls").appendTo(ui.viewPanel);
    var firstPage = $('<a href="javascript:{}">&laquo; first</a>').appendTo(pagingControls);
    var previousPage = $('<a href="javascript:{}">&laquo; previous</a>').appendTo(pagingControls);
    if (theProject.rowModel.start > 0) {
        firstPage.addClass("action").click(onClickFirstPage);
        previousPage.addClass("action").click(onClickPreviousPage);
    } else {
        firstPage.addClass("inaction");
        previousPage.addClass("inaction");
    }
    $('<span> &bull; </span>').appendTo(pagingControls);
    var nextPage = $('<a href="javascript:{}">next page &raquo;</a>').appendTo(pagingControls);
    var lastPage = $('<a href="javascript:{}">last &raquo;</a>').appendTo(pagingControls);
    if (theProject.rowModel.start + theProject.rowModel.limit < theProject.rowModel.total) {
        nextPage.addClass("action").click(onClickNextPage);
        lastPage.addClass("action").click(onClickLastPage);
    } else {
        nextPage.addClass("inaction");
        lastPage.addClass("inaction");
    }
    
    var table = document.createElement("table");
    table.className = "data-table";
    ui.viewPanel.append(table);
    
    var trHead = table.insertRow(0);
    var td = trHead.insertCell(trHead.cells.length);
        
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        var td = trHead.insertCell(trHead.cells.length);
        $(td).html(column.headerLabel);
    }
    
    var rows = theProject.rowModel.rows;
    for (var r = 0; r < rows.length; r++) {
        var row = rows[r];
        var cells = row.cells;
        
        var tr = table.insertRow(table.rows.length);
        tr.className = (r % 2) == 1 ? "odd" : "even";
        
        var td = tr.insertCell(tr.cells.length);
        $(td).html((theProject.rowModel.start + r + 1) + ".");
        
        for (var i = 0; i < columns.length; i++) {
            var column = columns[i];
            var td = tr.insertCell(tr.cells.length);
            
            if (column.cellIndex < cells.length) {
                var cell = cells[column.cellIndex];
                if (cell.v != null) {
                    $(td).html(cell.v);
                }
            }
        }
    }
}

function showRows(start, onDone) {
    Ajax.chainGetJSON(
        "/command/get-rows?" + $.param({ project: theProject.id, start: start, limit: theProject.view.pageSize }), null,
        function(data) {
            theProject.rowModel = data;
            renderView();
        }
    );
}

function onClickPreviousPage() {
    showRows(theProject.rowModel.start - theProject.view.pageSize);
}

function onClickNextPage() {
    showRows(theProject.rowModel.start + theProject.view.pageSize);
}

function onClickFirstPage() {
    showRows(0, theProject.view.pageSize);
}

function onClickLastPage() {
    showRows(Math.floor(theProject.rowModel.total / theProject.view.pageSize) * theProject.view.pageSize);
}
