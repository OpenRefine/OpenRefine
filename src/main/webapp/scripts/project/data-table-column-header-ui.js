function DataTableColumnHeaderUI(dataTableView, column, columnIndex, td) {
    this._dataTableView = dataTableView;
    this._column = column;
    this._columnIndex = columnIndex;
    this._td = td;
    
    this._render();
}

DataTableColumnHeaderUI.prototype._render = function() {
    var self = this;
    var td = this._td;
    
    var headerTable = document.createElement("table");
    $(headerTable).addClass("column-header-layout").attr("cellspacing", "0").attr("cellpadding", "0").attr("width", "100%").appendTo(td);
    
    var headerTableRow = headerTable.insertRow(0);
    var headerLeft = headerTableRow.insertCell(0);
    var headerRight = headerTableRow.insertCell(1);
    
    $('<span></span>').html(this._column.headerLabel).appendTo(headerLeft);
    
    $(headerRight).attr("width", "1%");
    $('<img src="/images/menu-dropdown.png" />').addClass("column-header-menu").appendTo(headerRight).click(function() {
        self._createMenuForColumnHeader(this);
    });
};

DataTableColumnHeaderUI.prototype._createMenuForColumnHeader = function(elmt) {
    self = this;
    MenuSystem.createAndShowStandardMenu([
        {
            label: "Edit",
            submenu: [
                {   "heading" : "Cell Content Transformations" },
                {
                    label: "To Titlecase",
                    click: function() { self._doTextTransform("toTitlecase(value)"); }
                },
                {
                    label: "To Uppercase",
                    click: function() { self._doTextTransform("toUppercase(value)"); }
                },
                {
                    label: "To Lowercase",
                    click: function() { self._doTextTransform("toLowercase(value)"); }
                },
                {},
                {
                    label: "Custom Expression ...",
                    click: function() { self._doTextTransformPrompt(); }
                },
                {   "heading" : "Column Operations" },
                {
                    label: "Add Column Based on This Column ...",
                    click: function() { self._doAddColumn("value"); }
                },
                {
                    label: "Remove This Column",
                    click: function() { self._doRemoveColumn(); }
                },
                {   "heading" : "Advanced Transformations" },
                {
                    label: "Split Multi-Value Cells ...",
                    click: function() { self._doSplitMultiValueCells(); }
                },
                {
                    label: "Join Multi-Value Cells ...",
                    click: function() { self._doJoinMultiValueCells(); }
                }
            ]
        },
        {
            label: "Filter",
            tooltip: "Filter rows by this column's cell content or characteristics",
            submenu: [
                {
                    label: "Text Facet",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : self._column.headerLabel,
                                "cellIndex" : self._column.cellIndex, 
                                "expression" : "value"
                            }
                        );
                    }
                },
                {
                    label: "Custom Text Facet ...",
                    click: function() { self._doFilterByExpressionPrompt("value", "list"); }
                },
                {},
                {
                    label: "Numeric Facet",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range", 
                            {
                                "name" : self._column.headerLabel,
                                "cellIndex" : self._column.cellIndex, 
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
                    label: "Custom Numeric Facet ...",
                    click: function() { self._doFilterByExpressionPrompt("value", "range"); }
                },
                {},
                {
                    label: "Text Search",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "text", 
                            {
                                "name" : self._column.headerLabel,
                                "cellIndex" : self._column.cellIndex, 
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
                                "name" : self._column.headerLabel + " (regex)",
                                "cellIndex" : self._column.cellIndex, 
                                "mode" : "regex",
                                "caseSensitive" : true
                            }
                        );
                    }
                }
            ]
        },
        {
            label: "View",
            tooltip: "Collapse/expand columns to make viewing the data more convenient",
            submenu: [
                {
                    label: "Collapse This Column",
                    click: function() {
                        theProject.columnModel.columns[self._columnIndex].collapsed = true;
                        self._dataTableView.render();
                    }
                },
                {
                    label: "Collapse All Other Columns",
                    click: function() {
                        for (var i = 0; i < theProject.columnModel.columns.length; i++) {
                            if (i != self._columnIndex) {
                                theProject.columnModel.columns[i].collapsed = true;
                            }
                        }
                        self._dataTableView.render();
                    }
                },
                {
                    label: "Collapse All Columns To Right",
                    click: function() {
                        for (var i = self._columnIndex + 1; i < theProject.columnModel.columns.length; i++) {
                            theProject.columnModel.columns[i].collapsed = true;
                        }
                        self._dataTableView.render();
                    }
                }
            ]
        },
        {},
        {
            label: "Reconcile",
            tooltip: "Match this column's cells to topics on Freebase",
            submenu: [
                {
                    label: "Start Reconciling ...",
                    tooltip: "Reconcile text in this column with topics on Freebase",
                    click: function() {
                        new ReconDialog(self._columnIndex);
                    }
                },
                {},
                {
                    label: "Approve Best Candidates",
                    tooltip: "Approve best reconciliaton candidate per cell in this column for all current filtered rows",
                    click: function() {
                        self._doApproveBestCandidates();
                    }
                },
                {
                    label: "Approve As New Topics",
                    tooltip: "Set to create new topics for cells in this column for all current filtered rows",
                    click: function() {
                        self._doApproveNewTopics();
                    }
                },
                {
                    label: "Discard Reconciliation Results",
                    tooltip: "Discard reconciliaton results in this column for all current filtered rows",
                    click: function() {
                        self._doDiscardReconResults();
                    }
                }
            ]
        },
        {
            label: "Reconcile Filter",
            tooltip: "Match this column's cells to topics on Freebase",
            submenu: [
                {
                    label: "By Judgment",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : self._column.headerLabel + ": judgment",
                                "cellIndex" : self._column.cellIndex, 
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
                                "name" : self._column.headerLabel + ": best candidate's relevance score",
                                "cellIndex" : self._column.cellIndex, 
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
                                "name" : self._column.headerLabel + ": best candidate's type match",
                                "cellIndex" : self._column.cellIndex, 
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
                                "name" : self._column.headerLabel + ": best candidate's name match",
                                "cellIndex" : self._column.cellIndex, 
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
                                "name" : self._column.headerLabel + ": best candidate's name edit distance",
                                "cellIndex" : self._column.cellIndex, 
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
                                "name" : self._column.headerLabel + ": best candidate's name word similarity",
                                "cellIndex" : self._column.cellIndex, 
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
                                "name" : self._column.headerLabel + ": best candidate's types",
                                "cellIndex" : self._column.cellIndex, 
                                "expression" : "cell.recon.best.type"
                            }
                        );
                    }
                }
            ]
        }
    ], elmt, { width: "120px", horizontal: false });
};

DataTableColumnHeaderUI.prototype._doFilterByExpressionPrompt = function(expression, type) {
    var self = this;
    DataTableView.promptExpressionOnVisibleRows(
        this._column,
        "Custom Filter on " + this._column.headerLabel, 
        expression,
        function(expression) {
            var config = {
                "name" : self._column.headerLabel + ": " + expression,
                "cellIndex" : self._column.cellIndex, 
                "expression" : expression
            };
            if (type == "range") {
                config.mode = "range";
            }

            ui.browsingEngine.addFacet(type, config);
        }
    );
};

DataTableColumnHeaderUI.prototype._doTextTransform = function(expression) {
    this._dataTableView.doPostThenUpdate(
        "do-text-transform",
        { cell: this._column.cellIndex, expression: expression }
    );
};

DataTableColumnHeaderUI.prototype._doTextTransformPrompt = function() {
    var self = this;
    DataTableView.promptExpressionOnVisibleRows(
        this._column,
        "Custom Transform on " + this._column.headerLabel, 
        "value",
        function(expression) {
            self._doTextTransform(expression);
        }
    );
};

DataTableColumnHeaderUI.prototype._doDiscardReconResults = function() {
    this._dataTableView.doPostThenUpdate(
        "discard-reconcile",
        { cell: this._column.cellIndex }
    );
};

DataTableColumnHeaderUI.prototype._doApproveBestCandidates = function() {
    this._dataTableView.doPostThenUpdate(
        "approve-reconcile",
        { cell: this._column.cellIndex }
    );
};

DataTableColumnHeaderUI.prototype._doApproveNewTopics = function() {
    this._dataTableView.doPostThenUpdate(
        "approve-new-reconcile",
        { cell: this._column.cellIndex }
    );
};

DataTableColumnHeaderUI.prototype._doAddColumn = function(initialExpression) {
    var self = this;
    DataTableView.promptExpressionOnVisibleRows(
        this._column,
        "Add Column Based on Column " + this._column.headerLabel, 
        initialExpression,
        function(expression) {
            var headerLabel = window.prompt("Enter header label for new column:");
            if (headerLabel != null) {
                self._dataTableView.doPostThenUpdate(
                    "add-column",
                    {
                        baseCellIndex: self._column.cellIndex, 
                        expression: expression, 
                        headerLabel: headerLabel, 
                        columnInsertIndex: self._columnIndex + 1 
                    },
                    true
                );
            }
        }
    );
};

DataTableColumnHeaderUI.prototype._doRemoveColumn = function() {
    this._dataTableView.doPostThenUpdate(
        "remove-column",
        { columnRemovalIndex: this._columnIndex },
        true
    );
};

DataTableColumnHeaderUI.prototype._doJoinMultiValueCells = function() {
    var separator = window.prompt("Enter separator to use between values", ", ");
    if (separator != null) {
        this._dataTableView.doPostThenUpdate(
            "join-multi-value-cells",
            {
                cellIndex: this._column.cellIndex,
                keyCellIndex: theProject.columnModel.keyCellIndex,
                separator: separator
            }
        );
    }
};

DataTableColumnHeaderUI.prototype._doSplitMultiValueCells = function() {
    var separator = window.prompt("What separator currently separates the values?", ", ");
    if (separator != null) {
        this._dataTableView.doPostThenUpdate(
            "split-multi-value-cells",
            {
                cellIndex: this._column.cellIndex,
                keyCellIndex: theProject.columnModel.keyCellIndex,
                separator: separator,
                mode: "plain"
            }
        );
    }
};
