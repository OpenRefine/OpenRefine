function DataTableView(div) {
    this._div = div;
    this.render();
}

DataTableView.prototype.render = function() {
    var self = this;
    var container = this._div.empty();
    
    var divSummary = $('<div></div>').addClass("viewPanel-summary").appendTo(container);
    $('<span>' + 
            (theProject.rowModel.start + 1) + " to " + 
            (theProject.rowModel.start + theProject.rowModel.limit) + " of " + 
            (theProject.rowModel.total) + 
            " rows total" + 
        '</span>'
    ).appendTo(divSummary);
    
    var pagingControls = $('<div></div>').addClass("viewPanel-pagingControls").appendTo(container);
    var firstPage = $('<a href="javascript:{}">&laquo; first</a>').appendTo(pagingControls);
    var previousPage = $('<a href="javascript:{}">&laquo; previous</a>').appendTo(pagingControls);
    if (theProject.rowModel.start > 0) {
        firstPage.addClass("action").click(function(evt) { self._onClickFirstPage(this, evt); });
        previousPage.addClass("action").click(function(evt) { self._onClickPreviousPage(this, evt); });
    } else {
        firstPage.addClass("inaction");
        previousPage.addClass("inaction");
    }
    $('<span> &bull; </span>').appendTo(pagingControls);
    var nextPage = $('<a href="javascript:{}">next page &raquo;</a>').appendTo(pagingControls);
    var lastPage = $('<a href="javascript:{}">last &raquo;</a>').appendTo(pagingControls);
    if (theProject.rowModel.start + theProject.rowModel.limit < theProject.rowModel.total) {
        nextPage.addClass("action").click(function(evt) { self._onClickNextPage(this, evt); });
        lastPage.addClass("action").click(function(evt) { self._onClickLastPage(this, evt); });
    } else {
        nextPage.addClass("inaction");
        lastPage.addClass("inaction");
    }
    
    var table = document.createElement("table");
    table.className = "data-table";
    container.append(table);
    
    var trHead = table.insertRow(0);
    
    var td = trHead.insertCell(trHead.cells.length);
    $('<img src="/images/menu-dropdown.png" />').addClass("column-header-menu").appendTo(td).click(function() {
        self._createMenuForAllColumns(this);
    });
    
    var createColumnHeader = function(column, index) {
        var td = trHead.insertCell(trHead.cells.length);
        $(td).addClass("column-header");
        
        if (column.collapsed) {
            $(td).html("&nbsp;").attr("title", column.headerLabel).click(function(evt) {
                column.collapsed = false;
                self.render();
            });
        } else {
            var headerTable = document.createElement("table");
            $(headerTable).addClass("column-header-layout").attr("cellspacing", "0").attr("cellpadding", "0").attr("width", "100%").appendTo(td);
            
            var headerTableRow = headerTable.insertRow(0);
            var headerLeft = headerTableRow.insertCell(0);
            var headerRight = headerTableRow.insertCell(1);
            
            $('<span></span>').html(column.headerLabel).appendTo(headerLeft);
            
            $(headerRight).attr("width", "1%");
            $('<img src="/images/menu-dropdown.png" />').addClass("column-header-menu").appendTo(headerRight).click(function() {
                self._createMenuForColumnHeader(column, index, this);
            });
        }
    };
    
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        createColumnHeader(columns[i], i);
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
            if (column.collapsed) {
                td.innerHTML = "&nbsp;";
            } else if (column.cellIndex < cells.length) {
                var cell = cells[column.cellIndex];
                if (cell.v != null) {
                    $(td).html(cell.v);
                }
            }
        }
    }
};

DataTableView.prototype._showRows = function(start, onDone) {
    var self = this;
    Ajax.chainGetJSON(
        "/command/get-rows?" + $.param({ project: theProject.id, start: start, limit: theProject.view.pageSize }), null,
        function(data) {
            theProject.rowModel = data;
            self.render();
        }
    );
};

DataTableView.prototype._onClickPreviousPage = function(elmt, evt) {
    this._showRows(theProject.rowModel.start - theProject.view.pageSize);
};

DataTableView.prototype._onClickNextPage = function(elmt, evt) {
    this._showRows(theProject.rowModel.start + theProject.view.pageSize);
};

DataTableView.prototype._onClickFirstPage = function(elmt, evt) {
    this._showRows(0, theProject.view.pageSize);
};

DataTableView.prototype._onClickLastPage = function(elmt, evt) {
    this._showRows(Math.floor(theProject.rowModel.total / theProject.view.pageSize) * theProject.view.pageSize);
};

DataTableView.prototype._createMenuForAllColumns = function(elmt) {
    self = this;
    MenuSystem.createAndShowStandardMenu([
        {
            label: "Collapse All Columns",
            click: function() {
                for (var i = 0; i < theProject.columnModel.columns.length; i++) {
                    theProject.columnModel.columns[i].collapsed = true;
                }
                self.render();
            }
        },
        {
            label: "Expand All Columns",
            click: function() {
                for (var i = 0; i < theProject.columnModel.columns.length; i++) {
                    theProject.columnModel.columns[i].collapsed = false;
                }
                self.render();
            }
        }
    ], elmt);
};

DataTableView.prototype._createMenuForColumnHeader = function(column, index, elmt) {
    self = this;
    MenuSystem.createAndShowStandardMenu([
        {
            label: "Filter",
            submenu: [
                {
                    label: "By Nominal Choices",
                    click: function() {}
                },
                {
                    label: "By Simple Text Search",
                    click: function() {}
                },
                {
                    label: "By Regular Expression",
                    click: function() {}
                },
                {
                    label: "By Reconciliation Features",
                    click: function() {}
                }
            ]
        },
        {
            label: "Collapse/Expand",
            submenu: [
                {
                    label: "Collapse This Column",
                    click: function() {
                        theProject.columnModel.columns[index].collapsed = true;
                        self.render();
                    }
                },
                {
                    label: "Collapse All Other Columns",
                    click: function() {
                        for (var i = 0; i < theProject.columnModel.columns.length; i++) {
                            if (i != index) {
                                theProject.columnModel.columns[i].collapsed = true;
                            }
                        }
                        self.render();
                    }
                },
                {
                    label: "Collapse All Columns To Right",
                    click: function() {
                        for (var i = index + 1; i < theProject.columnModel.columns.length; i++) {
                            theProject.columnModel.columns[i].collapsed = true;
                        }
                        self.render();
                    }
                }
            ]
        },
        {},
        {
            label: "Text Transform",
            submenu: [
                {
                    label: "To Titlecase",
                    click: function() { self._doTextTransform(column, "toTitlecase(this.value)"); }
                },
                {
                    label: "To Uppercase",
                    click: function() { self._doTextTransform(column, "toUppercase(this.value)"); }
                },
                {
                    label: "To Lowercase",
                    click: function() { self._doTextTransform(column, "toLowercase(this.value)"); }
                },
                {},
                {
                    label: "Custom Expression ...",
                    click: function() {
                        var expression = window.prompt("Enter expression", 'replace(this.value,"","")');
                        if (expression != null) {
                            self._doTextTransform(column, expression);
                        }
                    }
                }
            ]
        },
        {
            label: "Reconcile",
            submenu: [
                {
                    label: "Initiate on Filtered Rows...",
                    click: function() {}
                },
                {},
                {
                    label: "Approve Filtered Rows",
                    click: function() {}
                }
            ]
        }
    ], elmt);
};

DataTableView.prototype._doTextTransform = function(column, expression) {
    var self = this;
    $.post(
        "/command/do-text-transform?" + $.param({ project: theProject.id, cell: column.cellIndex, expression: expression }), 
        null,
        function(data) {
            if (data.code == "ok") {
                self.update();
                ui.historyWidget.update();
            } else {
                // update process UI
            }
        },
        "json"
    );
};

DataTableView.prototype.update = function() {
    this._showRows(theProject.rowModel.start);
};
