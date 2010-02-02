function DataTableView(div) {
    this._div = div;
    this._pageSize = 20;
    this._showRecon = true;
    this._showRows(0);
}

DataTableView.prototype.render = function() {
    var self = this;
    var container = this._div.empty();
    
    var divSummary = $('<div></div>').addClass("viewPanel-summary").appendTo(container);
    $('<span>' + 
            (theProject.rowModel.start + 1) + " to " + 
            Math.min(theProject.rowModel.filtered, theProject.rowModel.start + theProject.rowModel.limit) + " of " + 
            (theProject.rowModel.filtered) + " filtered rows, " +
            (theProject.rowModel.total) + " total rows" + 
        '</span>'
    ).appendTo(divSummary);
    
    /*
     *  Paging controls
     */
    
    var pagingControls = $('<table width="100%"><tr><td align="center"></td><td align="center"></td></tr></table>').addClass("viewPanel-pagingControls").appendTo(container);
    var pagingControls0 = pagingControls[0].rows[0].cells[0];
    var pagingControls1 = pagingControls[0].rows[0].cells[1];
    
    var firstPage = $('<a href="javascript:{}">&laquo; first</a>').appendTo(pagingControls0);
    var previousPage = $('<a href="javascript:{}">&laquo; previous</a>').appendTo(pagingControls0);
    if (theProject.rowModel.start > 0) {
        firstPage.addClass("action").click(function(evt) { self._onClickFirstPage(this, evt); });
        previousPage.addClass("action").click(function(evt) { self._onClickPreviousPage(this, evt); });
    } else {
        firstPage.addClass("inaction");
        previousPage.addClass("inaction");
    }
    $('<span> &bull; </span>').appendTo(pagingControls0);
    var nextPage = $('<a href="javascript:{}">next page &raquo;</a>').appendTo(pagingControls0);
    var lastPage = $('<a href="javascript:{}">last &raquo;</a>').appendTo(pagingControls0);
    if (theProject.rowModel.start + theProject.rowModel.limit < theProject.rowModel.filtered) {
        nextPage.addClass("action").click(function(evt) { self._onClickNextPage(this, evt); });
        lastPage.addClass("action").click(function(evt) { self._onClickLastPage(this, evt); });
    } else {
        nextPage.addClass("inaction");
        lastPage.addClass("inaction");
    }
    
    $('<span>page size: </span>').appendTo(pagingControls1);
    var sizes = [ 5, 10, 15, 20, 25, 50 ];
    var renderPageSize = function(index) {
        var pageSize = sizes[index];
        var a = $('<a href="javascript:{}"></a>').appendTo(pagingControls1)
        if (pageSize == self._pageSize) {
            a.text("[" + pageSize + "]").addClass("inaction");
        } else {
            a.text(pageSize).addClass("action").click(function(evt) {
                self._pageSize = pageSize;
                self.update(true);
            });
        }
    };
    for (var i = 0; i < sizes.length; i++) {
        renderPageSize(i);
    }
    
    /*
     *  Data table
     */
    var table = document.createElement("table");
    table.className = "data-table";
    container.append(table);
    
    var trHead = table.insertRow(0);
    
    var td = trHead.insertCell(trHead.cells.length);
    $(td).addClass("column-header");
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
    
    var renderCell = function(cell, td) {
        if (cell.v == null) {
            return;
        }
        
        $(td).html(cell.v);
            
        if ("r" in cell && self._showRecon) {
            var candidates = cell.r.c;
            var ul = $('<ul></ul>').appendTo(td);
            
            for (var i = 0; i < candidates.length; i++) {
                var candidate = candidates[i];
                var li = $('<li></li>').appendTo(ul);
                $('<a></a>')
                    .attr("href", "http://www.freebase.com/view" + candidate.id)
                    .attr("target", "_blank")
                    .text(candidate.name)
                    .appendTo(li);
            }
        }
    };
    
    var rows = theProject.rowModel.rows;
    for (var r = 0; r < rows.length; r++) {
        var row = rows[r];
        var cells = row.cells;
        
        var tr = table.insertRow(table.rows.length);
        tr.className = (r % 2) == 1 ? "odd" : "even";
        
        var td = tr.insertCell(tr.cells.length);
        $(td).html((row.i + 1) + ".");
        
        for (var i = 0; i < columns.length; i++) {
            var column = columns[i];
            var td = tr.insertCell(tr.cells.length);
            if (column.collapsed) {
                td.innerHTML = "&nbsp;";
            } else if (column.cellIndex < cells.length) {
                var cell = cells[column.cellIndex];
                renderCell(cell, td);
            }
        }
    }
};

DataTableView.prototype._showRows = function(start, onDone) {
    var self = this;
    
    $.post(
        "/command/get-rows?" + $.param({ project: theProject.id, start: start, limit: this._pageSize }),
        { engine: JSON.stringify(ui.browsingEngine.getJSON()) },
        function(data) {
            theProject.rowModel = data;
            self.render();
            
            if (onDone) {
                onDone();
            }
        },
        "json"
    );
};

DataTableView.prototype._onClickPreviousPage = function(elmt, evt) {
    this._showRows(theProject.rowModel.start - this._pageSize);
};

DataTableView.prototype._onClickNextPage = function(elmt, evt) {
    this._showRows(theProject.rowModel.start + this._pageSize);
};

DataTableView.prototype._onClickFirstPage = function(elmt, evt) {
    this._showRows(0);
};

DataTableView.prototype._onClickLastPage = function(elmt, evt) {
    this._showRows(Math.floor(theProject.rowModel.filtered / this._pageSize) * this._pageSize);
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
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.headerLabel,
                                "cellIndex" : column.cellIndex, 
                                "expression" : "value"
                            }
                        );
                    }
                },
                {
                    label: "By Simple Text Search",
                    click: function() {}
                },
                {
                    label: "By Custom Expression",
                    click: function() {
                        var expression = window.prompt("Enter expression", 'value');
                        if (expression != null) {
                            self._doFilterByExpression(column, expression);
                        }                    
                    }
                },
                {
                    label: "By Reconciliation Features",
                    submenu: [
                        {
                            label: "By Type Match",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "list", 
                                    {
                                        "name" : column.headerLabel + ": recon type match",
                                        "cellIndex" : column.cellIndex, 
                                        "expression" : "cell.recon.features.typeMatch"
                                    },
                                    {
                                        "scroll" : false
                                    }
                                );
                            }
                        },
                        {
                            label: "By Name Match",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "list", 
                                    {
                                        "name" : column.headerLabel + ": recon name match",
                                        "cellIndex" : column.cellIndex, 
                                        "expression" : "cell.recon.features.nameMatch"
                                    },
                                    {
                                        "scroll" : false
                                    }
                                );
                            }
                        },
                        {
                            label: "By Best Candidate's Score",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "range", 
                                    {
                                        "name" : column.headerLabel + ": best candidate's score",
                                        "cellIndex" : column.cellIndex, 
                                        "expression" : "cell.recon.best.score",
                                        "mode" : "range",
                                        "min" : 0,
                                        "max" : 200
                                    },
                                    {
                                    }
                                );
                            }
                        },
                        {
                            label: "By Name's Edit Distance",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "range", 
                                    {
                                        "name" : column.headerLabel + ": name's edit distance",
                                        "cellIndex" : column.cellIndex, 
                                        "expression" : "cell.recon.features.nameLevenshtein",
                                        "mode" : "range",
                                        "min" : 0,
                                        "max" : 1,
                                        "step" : 0.1
                                    },
                                    {
                                    }
                                );
                            }
                        },
                        {
                            label: "By Name's Word Similarity",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "range", 
                                    {
                                        "name" : column.headerLabel + ": name's word similarity",
                                        "cellIndex" : column.cellIndex, 
                                        "expression" : "cell.recon.features.nameWordDistance",
                                        "mode" : "range",
                                        "min" : 0,
                                        "max" : 1,
                                        "step" : 0.1
                                    },
                                    {
                                    }
                                );
                            }
                        },
                        {
                            label: "By Best Candidate's Types",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "list", 
                                    {
                                        "name" : column.headerLabel + ": best candidate's types",
                                        "cellIndex" : column.cellIndex, 
                                        "expression" : "cell.recon.best.type"
                                    }
                                );
                            }
                        }
                    ]
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
                    click: function() { self._doTextTransform(column, "toTitlecase(value)"); }
                },
                {
                    label: "To Uppercase",
                    click: function() { self._doTextTransform(column, "toUppercase(value)"); }
                },
                {
                    label: "To Lowercase",
                    click: function() { self._doTextTransform(column, "toLowercase(value)"); }
                },
                {},
                {
                    label: "Custom Expression ...",
                    click: function() {
                        var expression = window.prompt("Enter expression", 'replace(value, "", "")');
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
                    click: function() {
                        new ReconDialog(index);
                    }
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
        { engine: JSON.stringify(ui.browsingEngine.getJSON()) },
        function(data) {
            if (data.code == "ok") {
                self.update();
                ui.historyWidget.update();
            } else {
                ui.processWidget.update();
            }
        },
        "json"
    );
};

DataTableView.prototype._doFilterByExpression = function(column, expression) {
    ui.browsingEngine.addFacet(
        "list", 
        {
            "name" : column.headerLabel + ": " + expression,
            "cellIndex" : column.cellIndex, 
            "expression" : expression
        }
    );
};

DataTableView.prototype.update = function(reset) {
    this._showRows(reset ? 0 : theProject.rowModel.start);
};

