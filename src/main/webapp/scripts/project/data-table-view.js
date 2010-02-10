function DataTableView(div) {
    this._div = div;
    this._pageSize = 20;
    this._showRecon = true;
    this._showRows(0);
}

DataTableView.prototype.update = function(reset) {
    if (reset) {
        var self = this;
        reinitializeProjectData(function() {
            self._showRows(0);
        });
    } else {
        this._showRows(theProject.rowModel.start);
    }
};

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
    
    /*============================================================
     *  Data Table
     *============================================================
     */
    var tableDiv = $('<div></div>')
        .addClass("data-table-container")
        .css("width", container.width() + "px")
        .appendTo(container);
    
    var table = document.createElement("table");
    $(table)
        .attr("cellspacing", "0")
        .addClass("data-table")
        .appendTo(tableDiv);
    
    var columns = theProject.columnModel.columns;
    var columnGroups = theProject.columnModel.columnGroups;
    
    /*------------------------------------------------------------
     *  Column Group Headers
     *------------------------------------------------------------
     */
    
    var renderColumnKeys = function(keys) {
        if (keys.length > 0) {
            var tr = table.insertRow(table.rows.length);
            tr.insertCell(0); // row index
            
            for (var c = 0; c < columns.length; c++) {
                var td = tr.insertCell(tr.cells.length);
                
                for (var k = 0; k < keys.length; k++) {
                    if (c == keys[k]) {
                        $('<img />').attr("src", "images/down-arrow.png").appendTo(td);
                        break;
                    }
                }
            }
        }
    };
    var renderColumnGroups = function(groups, keys) {
        var nextLayer = [];
        
        if (groups.length > 0) {
            var tr = table.insertRow(table.rows.length);
            tr.insertCell(0); // row index
            
            for (var c = 0; c < columns.length; c++) {
                var foundGroup = false;
                
                for (var g = 0; g < groups.length; g++) {
                    var columnGroup = groups[g];
                    if (columnGroup.startColumnIndex == c) {
                        foundGroup = true;
                        break;
                    }
                }
                
                var td = tr.insertCell(tr.cells.length);
                if (foundGroup) {
                    td.setAttribute("colspan", columnGroup.columnSpan);
                    td.style.background = "blue";
                    
                    if (columnGroup.keyColumnIndex >= 0) {
                        keys.push(columnGroup.keyColumnIndex);
                    }
                    
                    c += (columnGroup.columnSpan - 1);
                    
                    nextLayer = nextLayer.concat(columnGroup.subgroups);
                }
            }
        }
        
        renderColumnKeys(keys);
        
        if (nextLayer.length > 0) {
            renderColumnGroups(nextLayer, []);
        }
    };
    renderColumnGroups(
        columnGroups, 
        [ theProject.columnModel.keyCellIndex ]
    );
    
    /*------------------------------------------------------------
     *  Column Headers with Menus
     *------------------------------------------------------------
     */
    
    var trHead = table.insertRow(table.rows.length);
    
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
            new DataTableColumnHeaderUI(self, column, index, td);
        }
    };
    
    for (var i = 0; i < columns.length; i++) {
        createColumnHeader(columns[i], i);
    }
    
    /*------------------------------------------------------------
     *  Data Cells
     *------------------------------------------------------------
     */
    
    var rows = theProject.rowModel.rows;
    var even = true;
    for (var r = 0; r < rows.length; r++) {
        var row = rows[r];
        var cells = row.cells;
        
        var tr = table.insertRow(table.rows.length);
        
        var td = tr.insertCell(tr.cells.length);
        if ("j" in row) {
            even = !even;
            
            $(tr).addClass("record");
            $('<div></div>').html((row.j + 1) + ".").appendTo(td);
        } else {
            $('<div></div>').html("&nbsp;").appendTo(td);
        }
        
        if ("contextual" in row && row.contextual) {
            $(tr).addClass("contextual");
        }
        
        $(tr).addClass(even ? "even" : "odd");
        
        for (var i = 0; i < columns.length; i++) {
            var column = columns[i];
            var td = tr.insertCell(tr.cells.length);
            if (column.collapsed) {
                td.innerHTML = "&nbsp;";
            } else {
                var cell = (column.cellIndex < cells.length) ? cells[column.cellIndex] : null;
                new DataTableCellUI(this, cell, r, column.cellIndex, td);
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

DataTableView.prototype.createUpdateFunction = function(onBefore) {
    var self = this;
    return function(data) {
        if (data.code == "ok") {
            var onDone = function() {
                self.update();
                ui.historyWidget.update();
            };
        } else {
            var onDone = function() {
                ui.processWidget.update();
            }
        }
        
        if (onBefore) {
            onBefore(onDone);
        } else {
            onDone();
        }
    };
};

DataTableView.prototype.doPostThenUpdate = function(command, params, updateColumnModel) {
    params.project = theProject.id;
    $.post(
        "/command/" + command + "?" + $.param(params),
        { engine: JSON.stringify(ui.browsingEngine.getJSON()) },
        this.createUpdateFunction(updateColumnModel ? reinitializeProjectData : undefined),
        "json"
    );
};

DataTableView.promptExpressionOnVisibleRows = function(column, title, expression, onDone) {
    var rowIndices = [];
    var values = [];
    
    var rows = theProject.rowModel.rows;
    for (var r = 0; r < rows.length; r++) {
        var row = rows[r];
        
        rowIndices.push(row.i);
        
        var v = null;
        if (column.cellIndex < row.cells.length) {
            var cell = row.cells[column.cellIndex];
            if (cell != null) {
                v = cell.v;
            }
        }
        values.push(v);
    }
    
    var self = this;
    new ExpressionPreviewDialog(
        title,
        column.cellIndex, 
        rowIndices, 
        values,
        expression,
        onDone
    );
};


