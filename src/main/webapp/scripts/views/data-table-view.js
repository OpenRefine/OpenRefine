function DataTableView(div) {
    this._div = div;
    this._pageSize = 20;
    this._showRecon = true;
    this._collapsedColumnNames = {};
    
    this._showRows(0);
}

DataTableView.prototype.resize = function() {
    var topHeight = this._div.find(".viewPanel-header").outerHeight(true);
    
    this._div.find(".data-table-container")
        .css("height", (this._div.innerHeight() - topHeight - 1) + "px")
        .css("display", "block");
};

DataTableView.prototype.update = function(onDone) {
    this._showRows(0, onDone);
};

DataTableView.prototype.render = function() {
    var self = this;
    
    var oldTableDiv = this._div.find(".data-table-container");
    var scrollLeft = (oldTableDiv.length > 0) ? oldTableDiv[0].scrollLeft : 0;
    
    var html = $(
        '<table class="viewPanel-header">' +
            '<tr>' +
                '<td bind="summary"></td>' +
                '<td bind="pageSizeControls" align="right"></td>' +
                '<td bind="pagingControls" align="right"></td>' +
            '</tr>' +
        '</table>' +
        '<div bind="dataTableContainer" class="data-table-container" style="display: none;"><table bind="table" class="data-table" cellspacing="0"></table></div>'
    );
    var elmts = DOM.bind(html);
    
    this._renderSummaryText(elmts.summary);
    this._renderPagingControls(elmts.pageSizeControls, elmts.pagingControls);
    this._renderDataTable(elmts.table[0]);
    
    this._div.empty().append(html);
    
    this.resize();
        
    elmts.dataTableContainer[0].scrollLeft = scrollLeft;
};

DataTableView.prototype._renderSummaryText = function(elmt) {
    var summaryText;
    
    var from = (theProject.rowModel.start + 1);
    var to = Math.min(theProject.rowModel.filtered, theProject.rowModel.start + theProject.rowModel.limit);
    if (theProject.rowModel.filtered == theProject.rowModel.total) {
        summaryText = from + ' &ndash; ' + to + ' of <span class="viewPanel-summary-row-count">' + (theProject.rowModel.total) + '</span> rows';
    } else {
        summaryText = from + ' &ndash; ' + to + ' of <span class="viewPanel-summary-row-count">' + 
            (theProject.rowModel.filtered) + '</span> matching rows (' + (theProject.rowModel.total) + ' total)';
    }
    $('<span>').html(summaryText).appendTo(elmt);
};

DataTableView.prototype._renderPagingControls = function(pageSizeControls, pagingControls) {
    var self = this;
    
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
    if (theProject.rowModel.start + theProject.rowModel.limit < theProject.rowModel.filtered) {
        nextPage.addClass("action").click(function(evt) { self._onClickNextPage(this, evt); });
        lastPage.addClass("action").click(function(evt) { self._onClickLastPage(this, evt); });
    } else {
        nextPage.addClass("inaction");
        lastPage.addClass("inaction");
    }
    
    $('<span>Show </span>').appendTo(pageSizeControls);
    var sizes = [ 10, 20, 25, 50 ];
    var renderPageSize = function(index) {
        var pageSize = sizes[index];
        var a = $('<a href="javascript:{}"></a>')
            .addClass("viewPanel-pagingControls-page")
            .appendTo(pageSizeControls);
        if (pageSize == self._pageSize) {
            a.text(pageSize).addClass("inaction");
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
    $('<span> rows</span>').appendTo(pageSizeControls);
};

DataTableView.prototype._renderDataTable = function(table) {
    var self = this;
    
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
            tr.insertCell(1); // flag
            tr.insertCell(2); // row index
            
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
            tr.insertCell(1); // flag
            tr.insertCell(2); // row index
            
            for (var c = 0; c < columns.length; c++) {
                var foundGroup = false;
                var columnGroup;
                
                for (var g = 0; g < groups.length; g++) {
                    columnGroup = groups[g];
                    if (columnGroup.startColumnIndex == c) {
                        foundGroup = true;
                        break;
                    }
                }
                
                var td = tr.insertCell(tr.cells.length);
                if (foundGroup) {
                    td.setAttribute("colspan", columnGroup.columnSpan);
                    td.style.background = "#FF6A00";
                    
                    if (columnGroup.keyColumnIndex >= 0) {
                        keys.push(columnGroup.keyColumnIndex);
                    }
                    
                    c += (columnGroup.columnSpan - 1);
                    
                    if ("subgroups" in columnGroup) {
                        nextLayer = nextLayer.concat(columnGroup.subgroups);
                    }
                }
            }
        }
        
        renderColumnKeys(keys);
        
        if (nextLayer.length > 0) {
            renderColumnGroups(nextLayer, []);
        }
    };
    
    if (columnGroups.length > 0) {
        renderColumnGroups(
            columnGroups, 
            [ theProject.columnModel.keyCellIndex ]
        );
    }    
    
    /*------------------------------------------------------------
     *  Column Headers with Menus
     *------------------------------------------------------------
     */
    
    var trHead = table.insertRow(table.rows.length);
    DOM.bind(
        $(trHead.insertCell(trHead.cells.length))
            .attr("colspan", "3")
            .addClass("column-header")
            .html(
                '<table class="column-header-layout"><tr>' +
                    '<td width="1%">' +
                        '<a class="column-header-menu" bind="dropdownMenu">&nbsp;</a>' +
                    '</td>' +
                    '<td>&nbsp;</td>' +
                '</tr></table>'
            )
    ).dropdownMenu.click(function() {
        self._createMenuForAllColumns(this);
    });
    
    var createColumnHeader = function(column, index) {
        var td = trHead.insertCell(trHead.cells.length);
        $(td).addClass("column-header");
        
        if (column.name in self._collapsedColumnNames) {
            $(td).html("&nbsp;").attr("title", column.name).click(function(evt) {
                delete self._collapsedColumnNames[column.name];
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

        var tdFlag = tr.insertCell(tr.cells.length);
        var flag = $('<a href="javascript:{}">&nbsp;</a>')
            .addClass(row.flagged ? "data-table-flag-on" : "data-table-flag-off")
            .appendTo(tdFlag)
            .click(function() {
                var newFlagged = !row.flagged;

                Gridworks.postProcess(
                    "annotate-one-row",
                    { row: row.i, flagged: newFlagged },
                    null,
                    {},
                    {   onDone: function(o) {
                            row.flagged = newFlagged;
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
        {   label: "Facet",
            submenu: [
                {
                    label: "Facet by Star",
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
                },
                {
                    label: "Facet by Flag",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : "Flagged Rows",
                                "columnName" : "", 
                                "expression" : "row.flagged"
                            },
                            {
                                "scroll" : false
                            }
                        );
                    }
                }
            ]
        },
        {   label: "Edit Rows",
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
                },
                {},
                {
                    label: "Flag Rows",
                    click: function() {
                        Gridworks.postProcess("annotate-rows", { "flagged" : "true" }, null, { rowMetadataChanged: true });
                    }
                },
                {
                    label: "Unflag Rows",
                    click: function() {
                        Gridworks.postProcess("annotate-rows", { "flagged" : "false" }, null, { rowMetadataChanged: true });
                    }
                },
                {},
                {
                    label: "Remove All Matching Rows",
                    click: function() {
                        Gridworks.postProcess("remove-rows", {}, null, { rowMetadataChanged: true });
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
                            self._collapsedColumnNames[theProject.columnModel.columns[i].name] = true;
                        }
                        self.render();
                    }
                },
                {
                    label: "Expand All Columns",
                    click: function() {
                        self._collapsedColumnNames = [];
                        self.render();
                    }
                }
            ]
        }
    ], elmt, { width: "80px", horizontal: false });
};

DataTableView.prototype._updateCell = function(rowIndex, cellIndex, cell) {
    var rows = theProject.rowModel.rows;
    for (var r = 0; r < rows.length; r++) {
        var row = rows[r];
        if (row.i === rowIndex) {
            while (cellIndex >= row.cells.length) {
                row.cells.push(null);
            }
            row.cells[cellIndex] = cell;
            break;
        }
    }
};

DataTableView.sampleVisibleRows = function(column) {
    var rowIndices = [];
    var values = [];
    
    var rows = theProject.rowModel.rows;
    for (var r = 0; r < rows.length; r++) {
        var row = rows[r];
        
        rowIndices.push(row.i);
        
        var v = null;
        if (column && column.cellIndex < row.cells.length) {
            var cell = row.cells[column.cellIndex];
            if (cell !== null) {
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


