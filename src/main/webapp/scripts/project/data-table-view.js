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
    var tableDiv = $('<div></div>').addClass("data-table-container").css("width", container.width() + "px").appendTo(container);
    
    var table = document.createElement("table");
    table.className = "data-table";
    tableDiv.append(table);
    
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
    
    for (var i = 0; i < columns.length; i++) {
        createColumnHeader(columns[i], i);
    }
    
    /*------------------------------------------------------------
     *  Data Cells
     *------------------------------------------------------------
     */
    
    var renderCell = function(cell, td) {
        if (cell == null || cell.v == null) {
            return;
        }
        
        if (!("r" in cell) || cell.r == null) {
            $(td).html(cell.v);
        } else {
            var r = cell.r;
            if (r.j == "new") {
                $(td).html(cell.v + " (new)");
            } else if (r.j == "approve" && "m" in r && r.m != null) {
                var match = cell.r.m;
                $('<a></a>')
                    .attr("href", "http://www.freebase.com/view" + match.id)
                    .attr("target", "_blank")
                    .text(match.name)
                    .appendTo(td);
            } else {
                $(td).html(cell.v);
                if (self._showRecon && "c" in r && r.c.length > 0) {
                    var candidates = r.c;
                    var ul = $('<ul></ul>').appendTo(td);
                    
                    for (var i = 0; i < candidates.length; i++) {
                        var candidate = candidates[i];
                        var li = $('<li></li>').appendTo(ul);
                        $('<a></a>')
                            .attr("href", "http://www.freebase.com/view" + candidate.id)
                            .attr("target", "_blank")
                            .text(candidate.name)
                            .appendTo(li);
                        $('<span></span>').addClass("recon-score").text("(" + Math.round(candidate.score) + ")").appendTo(li);
                    }
                }
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
        },
        {},
        {
            label: "Export Filtered Rows",
            click: function() { self._doExportRows(); }
        }
    ], elmt);
};

DataTableView.prototype._createMenuForColumnHeader = function(column, index, elmt) {
    self = this;
    MenuSystem.createAndShowStandardMenu([
        {
            label: "Filter",
            tooltip: "Filter rows by this column's cell content or characteristics",
            submenu: [
                {   "heading" : "On Cell Content" },
                {
                    label: "Text Facet",
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
                    label: "Numeric Facet",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range", 
                            {
                                "name" : column.headerLabel,
                                "cellIndex" : column.cellIndex, 
                                "expression" : "value",
                                "mode" : "range",
                                "min" : 0,
                                "max" : 1
                            },
                            {
                            }
                        );
                    }
                },
                {
                    label: "Custom Text Facet ...",
                    click: function() { self._doFilterByExpressionPrompt(column); }
                },
                {},
                {
                    label: "Text Search",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "text", 
                            {
                                "name" : column.headerLabel,
                                "cellIndex" : column.cellIndex, 
                                "mode" : "text",
                                "caseSensitive" : false
                            }
                        );
                    }
                },
                {
                    label: "Regular Expression Search",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "text", 
                            {
                                "name" : column.headerLabel + " (regex)",
                                "cellIndex" : column.cellIndex, 
                                "mode" : "regex",
                                "caseSensitive" : true
                            }
                        );
                    }
                },
                {   "heading" : "By Reconciliation Features" },
                {
                    label: "By Judgment",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.headerLabel + ": judgment",
                                "cellIndex" : column.cellIndex, 
                                "expression" : "cell.recon.judgment"
                            },
                            {
                                "scroll" : false
                            }
                        );
                    }
                },
                {},
                {
                    label: "Best Candidate's Relevance Score",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range", 
                            {
                                "name" : column.headerLabel + ": best candidate's relevance score",
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
                    label: "Best Candidate's Type Match",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.headerLabel + ": best candidate's type match",
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
                    label: "Best Candidate's Name Match",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.headerLabel + ": best candidate's name match",
                                "cellIndex" : column.cellIndex, 
                                "expression" : "cell.recon.features.nameMatch"
                            },
                            {
                                "scroll" : false
                            }
                        );
                    }
                },
                {},
                {
                    label: "Best Candidate's Name Edit Distance",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range", 
                            {
                                "name" : column.headerLabel + ": best candidate's name edit distance",
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
                    label: "Best Candidate's Name Word Similarity",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range", 
                            {
                                "name" : column.headerLabel + ": best candidate's name word similarity",
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
                {},
                {
                    label: "Best Candidate's Types",
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
        },
        {
            label: "Collapse/Expand",
            tooltip: "Collapse/expand columns to make viewing the data more convenient",
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
            label: "Add Column Based on This Column",
            click: function() { self._doAddColumn(column, index, "value"); }
        },
        {
            label: "Remove This Column",
            click: function() { self._doRemoveColumn(column, index); }
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
                    click: function() { self._doTextTransformPrompt(column); }
                }
            ]
        },
        {
            label: "Reconcile",
            tooltip: "Match this column's cells to topics on Freebase",
            submenu: [
                {
                    label: "Start Reconciling ...",
                    tooltip: "Reconcile text in this column with topics on Freebase",
                    click: function() {
                        new ReconDialog(index);
                    }
                },
                {},
                {
                    label: "Approve Best Candidates",
                    tooltip: "Approve best reconciliaton candidate per cell in this column for all current filtered rows",
                    click: function() {
                        self._doApproveBestCandidates(column);
                    }
                },
                {
                    label: "Approve As New Topics",
                    tooltip: "Set to create new topics for cells in this column for all current filtered rows",
                    click: function() {
                        self._doApproveNewTopics(column);
                    }
                },
                {
                    label: "Discard Reconciliation Results",
                    tooltip: "Discard reconciliaton results in this column for all current filtered rows",
                    click: function() {
                        self._doDiscardReconResults(column);
                    }
                }
            ]
        }
    ], elmt);
};

DataTableView.prototype._doFilterByExpressionPrompt = function(column, expression) {
    var self = this;
    DataTableView.promptExpressionOnVisibleRows(
        column,
        "Custom Filter on " + column.headerLabel, 
        "value",
        function(expression) {
            ui.browsingEngine.addFacet(
                "list", 
                {
                    "name" : column.headerLabel + ": " + expression,
                    "cellIndex" : column.cellIndex, 
                    "expression" : expression
                }
            );
        }
    );
};

DataTableView.prototype._createUpdateFunction = function(onBefore) {
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

DataTableView.prototype._doPostThenUpdate = function(command, params, updateColumnModel) {
    params.project = theProject.id;
    $.post(
        "/command/" + command + "?" + $.param(params),
        { engine: JSON.stringify(ui.browsingEngine.getJSON()) },
        this._createUpdateFunction(updateColumnModel ? reinitializeProjectData : undefined),
        "json"
    );
};

DataTableView.prototype._doTextTransform = function(column, expression) {
    this._doPostThenUpdate(
        "do-text-transform",
        { cell: column.cellIndex, expression: expression }
    );
};

DataTableView.prototype._doTextTransformPrompt = function(column) {
    var self = this;
    DataTableView.promptExpressionOnVisibleRows(
        column,
        "Custom Transform on " + column.headerLabel, 
        "value",
        function(expression) {
            self._doTextTransform(column, expression);
        }
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

DataTableView.prototype._doDiscardReconResults = function(column) {
    this._doPostThenUpdate(
        "discard-reconcile",
        { cell: column.cellIndex }
    );
};

DataTableView.prototype._doApproveBestCandidates = function(column) {
    this._doPostThenUpdate(
        "approve-reconcile",
        { cell: column.cellIndex }
    );
};

DataTableView.prototype._doApproveNewTopics = function(column) {
    this._doPostThenUpdate(
        "approve-new-reconcile",
        { cell: column.cellIndex }
    );
};

DataTableView.prototype._doAddColumn = function(column, index, initialExpression) {
    var self = this;
    DataTableView.promptExpressionOnVisibleRows(
        column,
        "Add Column Based on Column " + column.headerLabel, 
        initialExpression,
        function(expression) {
            var headerLabel = window.prompt("Enter header label for new column:");
            if (headerLabel != null) {
                self._doPostThenUpdate(
                    "add-column",
                    {
                        baseCellIndex: column.cellIndex, 
                        expression: expression, 
                        headerLabel: headerLabel, 
                        columnInsertIndex: index + 1 
                    },
                    true
                );
            }
        }
    );
};

DataTableView.prototype._doRemoveColumn = function(column, index) {
    this._doPostThenUpdate(
        "remove-column",
        { columnRemovalIndex: index },
        true
    );
};

DataTableView.prototype._doExportRows = function() {
    var form = document.createElement("form");
    $(form)
        .css("display", "none")
        .attr("method", "post")
        .attr("action", "/command/export-rows?project=" + theProject.id)
        .attr("target", "gridworks-export");

    $('<input />')
        .attr("name", "engine")
        .attr("value", JSON.stringify(ui.browsingEngine.getJSON()))
        .appendTo(form);
    
    document.body.appendChild(form);

    window.open("about:blank", "gridworks-export");
    form.submit();
    
    document.body.removeChild(form);
};
