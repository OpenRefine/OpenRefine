function DataTableView(div) {
    this._div = div;
    this._pageSize = 20;
    this._showRecon = true;
    this._collapsedColumnNames = {};
    
    this._initializeUI();
    this._showRows(0);
}

DataTableView.prototype._initializeUI = function() {
    this._div.addClass("view-panel");
    
    var self = this;
    $(window).resize(function() {
        var dataTableContainer = self._div.find(".data-table-container");
        dataTableContainer.hide().width(self._div.width() + "px").show();
    });
};

DataTableView.prototype.update = function(onDone) {
    this._showRows(0, onDone);
};

DataTableView.prototype.render = function() {
    var self = this;
    var container = this._div;
    
    var scrollLeft = 0;
    var oldTableDiv = container.find(".data-table-container");
    if (oldTableDiv.length > 0) {
        scrollLeft = oldTableDiv[0].scrollLeft;
    }
    
    container.empty();
    
    var divSummary = $('<div></div>').addClass("viewPanel-summary").appendTo(container);
    
    var summaryText;
    
    var from = (theProject.rowModel.start + 1);
    var to = Math.min(theProject.rowModel.filtered, theProject.rowModel.start + theProject.rowModel.limit);
    if (theProject.rowModel.filtered == theProject.rowModel.total) {
        summaryText = from + ' to ' + to + ' of <span class="viewPanel-summary-row-count">' + (theProject.rowModel.total) + '</span> rows';
    } else {
        summaryText = from + ' to ' + to + ' of <span class="viewPanel-summary-row-count">' + 
            (theProject.rowModel.filtered) + '</span> rows (filtered from ' + (theProject.rowModel.total) + ' rows total)';
    }
    $('<span>').html(summaryText).appendTo(divSummary);
    
    /*
     *  Paging controls
     */
    
    var pagingControls = $('<table width="100%"><tr><td align="right"></td><td align="right"></td></tr></table>').addClass("viewPanel-pagingControls").appendTo(container);
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
                self.update();
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
        .addClass("data-table");
    
    var columns = theProject.columnModel.columns;
    var columnGroups = theProject.columnModel.columnGroups;
    
    /*------------------------------------------------------------
     *  Column Group Headers
     *------------------------------------------------------------
     */
    
    var renderColumnKeys = function(keys) {
        if (keys.length > 0) {
            var tr = table.insertRow(table.rows.length);
            tr.insertCell(0); // star
            tr.insertCell(1); // row index
            
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
            tr.insertCell(0); // star
            tr.insertCell(1); // row index
            
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
    /*
    renderColumnGroups(
        columnGroups, 
        [ theProject.columnModel.keyCellIndex ]
    );
    */
    
    /*------------------------------------------------------------
     *  Column Headers with Menus
     *------------------------------------------------------------
     */
    
    var trHead = table.insertRow(table.rows.length);
    
    var tdHeadIndex = trHead.insertCell(trHead.cells.length);
    $(tdHeadIndex).attr("colspan", "2").addClass("column-header");
    $('<img src="/images/menu-dropdown.png" />').addClass("column-header-menu").appendTo(tdHeadIndex).click(function() {
        self._createMenuForAllColumns(this);
    });
    
    var createColumnHeader = function(column, index) {
        var td = trHead.insertCell(trHead.cells.length);
        $(td).addClass("column-header");
        
        if (column.name in self._collapsedColumnNames) {
            $(td).html("&nbsp;").attr("title", column.name).click(function(evt) {
                delete self._collapsedColumnNames[column.name]
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
    var renderRow = function(tr, r, row, even) {
        $(tr).empty();
        
        var cells = row.cells;
        
        var tdStar = tr.insertCell(tr.cells.length);
        var star = $('<a href="javascript:{}">&nbsp;</a>')
            .addClass(row.starred ? "data-table-star-on" : "data-table-star-off")
            .appendTo(tdStar)
            .click(function() {
                var newStarred = !row.starred;
                
                Gridworks.postProcess(
                    "annotate-one-row",
                    { row: row.i, starred: newStarred },
                    null,
                    {},
                    {   onDone: function(o) {
                            row.starred = newStarred;
                            renderRow(tr, r, row, even);
                        }
                    },
                    "json"
                );
            });
        
        var tdIndex = tr.insertCell(tr.cells.length);
        if ("j" in row) {
            $(tr).addClass("record");
            $('<div></div>').html((row.j + 1) + ".").appendTo(tdIndex);
        } else {
            $('<div></div>').html("&nbsp;").appendTo(tdIndex);
        }
        
        if ("contextual" in row && row.contextual) {
            $(tr).addClass("contextual");
        }
        
        $(tr).addClass(even ? "even" : "odd");
        
        for (var i = 0; i < columns.length; i++) {
            var column = columns[i];
            var td = tr.insertCell(tr.cells.length);
            if (column.name in self._collapsedColumnNames) {
                td.innerHTML = "&nbsp;";
            } else {
                var cell = (column.cellIndex < cells.length) ? cells[column.cellIndex] : null;
                new DataTableCellUI(self, cell, row.i, column.cellIndex, td);
            }
        }
    };
    
    var even = true;
    for (var r = 0; r < rows.length; r++) {
        var row = rows[r];
        var tr = table.insertRow(table.rows.length);
        if ("j" in row) {
            even = !even;
        }
        renderRow(tr, r, row, even);
    }
    
    /*
     *  Finally, inject the table into the DOM
     */
    $(table).appendTo(tableDiv);
    
    tableDiv[0].scrollLeft = scrollLeft;
};

DataTableView.prototype._showRows = function(start, onDone) {
    var self = this;
    Gridworks.fetchRows(start, this._pageSize, function() {
        self.render();
        
        if (onDone) {
            onDone();
        }
    });
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
        {   label: "Edit",
            submenu: [
                {
                    label: "Star Rows",
                    click: function() {
                        Gridworks.postProcess("annotate-rows", { "starred" : "true" }, null, { rowMetadataChanged: true });
                    }
                },
                {
                    label: "Unstar Rows",
                    click: function() {
                        Gridworks.postProcess("annotate-rows", { "starred" : "false" }, null, { rowMetadataChanged: true });
                    }
                }
            ]
        },
        {   label: "Filter",
            submenu: [
                {
                    label: "By Star",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : "Starred Rows",
                                "columnName" : "", 
                                "expression" : "row.starred"
                            },
                            {
                                "scroll" : false
                            }
                        );
                    }
                }
            ]
        },
        {   label: "View",
            submenu: [
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
            ]
        }
    ], elmt, { width: "80px", horizontal: false });
};

DataTableView.sampleVisibleRows = function(column) {
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
    
    return {
        rowIndices: rowIndices,
        values: values
    };
};

DataTableView.promptExpressionOnVisibleRows = function(column, title, expression, onDone) {
    var o = DataTableView.sampleVisibleRows(column);
    
    var self = this;
    new ExpressionPreviewDialog(
        title,
        column.cellIndex, 
        o.rowIndices, 
        o.values,
        expression,
        onDone
    );
};


