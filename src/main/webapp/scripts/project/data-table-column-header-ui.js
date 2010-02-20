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
            label: "Edit Cells",
            submenu: [
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
                {
                    label: "Custom Transform ...",
                    click: function() { self._doTextTransformPrompt(); }
                },
                {},
                {
                    label: "Split Multi-Valued Cells ...",
                    click: function() { self._doSplitMultiValueCells(); }
                },
                {
                    label: "Join Multi-Valued Cells ...",
                    click: function() { self._doJoinMultiValueCells(); }
                }
            ]
        },
        {
            label: "Edit Column",
            submenu: [
                {
                    label: "Add Column Based on This Column ...",
                    click: function() { self._doAddColumn("value"); }
                },
                {
                    label: "Remove This Column",
                    click: function() { self._doRemoveColumn(); }
                },
            ]
        },
        {},
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
                                "columnName" : self._column.headerLabel, 
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
                                "columnName" : self._column.headerLabel, 
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
                                "columnName" : self._column.headerLabel, 
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
                                "columnName" : self._column.headerLabel, 
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
                        self._doReconcile();
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
                },
                {},
                {
                    label: "Match Filtered Cells to ...",
                    tooltip: "Search for a topic to match all filtered cells to",
                    click: function() {
                        self._doSearchToMatch();
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
                                "columnName" : self._column.headerLabel, 
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
                                "columnName" : self._column.headerLabel, 
                                "expression" : "cell.recon.best.score",
                                "mode" : "range"
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
                                "columnName" : self._column.headerLabel, 
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
                                "columnName" : self._column.headerLabel, 
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
                                "columnName" : self._column.headerLabel, 
                                "expression" : "cell.recon.features.nameLevenshtein",
                                "mode" : "range"
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
                                "columnName" : self._column.headerLabel, 
                                "expression" : "cell.recon.features.nameWordDistance",
                                "mode" : "range"
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
                                "columnName" : self._column.headerLabel, 
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
                "columnName" : self._column.headerLabel, 
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
        { columnName: this._column.headerLabel, expression: expression }
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

DataTableColumnHeaderUI.prototype._doReconcile = function() {
    var self = this;
    var dismissBusy = DialogSystem.showBusy();
    $.post(
        "/command/guess-types-of-column?" + $.param({ project: theProject.id, columnName: this._column.headerLabel }), 
        null,
        function(data) {
            dismissBusy();
            new ReconDialog(self._column, data.code == "ok" ? data.types : []);
        },
        "json"
    );

};

DataTableColumnHeaderUI.prototype._doDiscardReconResults = function() {
    this._dataTableView.doPostThenUpdate(
        "recon-discard-judgments",
        { columnName: this._column.headerLabel }
    );
};

DataTableColumnHeaderUI.prototype._doApproveBestCandidates = function() {
    this._dataTableView.doPostThenUpdate(
        "recon-approve-best-matches",
        { columnName: this._column.headerLabel }
    );
};

DataTableColumnHeaderUI.prototype._doApproveNewTopics = function() {
    this._dataTableView.doPostThenUpdate(
        "recon-mark-new-topics",
        { columnName: this._column.headerLabel }
    );
};

DataTableColumnHeaderUI.prototype._doSearchToMatch = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("200px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Search for Match").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    $('<p></p>').text("Search Freebase for a topic to match all filtered cells:").appendTo(body);
    
    var input = $('<input />').appendTo($('<p></p>').appendTo(body));
    
    input.suggest({}).bind("fb-select", function(e, data) {
        self._dataTableView.doPostThenUpdate(
            "recon-match-specific-topic-to-cells",
            {
                columnName: self._column.headerLabel,
                topicID: data.id,
                topicGUID: data.guid,
                topicName: data.name,
                types: $.map(data.type, function(elmt) { return elmt.id; }).join(",")
            }
        );
        DialogSystem.dismissUntil(level - 1);
    });
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    var level = DialogSystem.showDialog(frame);
    input[0].focus();
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
                        baseColumnName: self._column.headerLabel, 
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
        { columnName: this._column.headerLabel },
        true
    );
};

DataTableColumnHeaderUI.prototype._doJoinMultiValueCells = function() {
    var separator = window.prompt("Enter separator to use between values", ", ");
    if (separator != null) {
        this._dataTableView.doPostThenUpdate(
            "join-multi-value-cells",
            {
                columnName: this._column.headerLabel,
                keyColumnName: theProject.columnModel.keyColumnName,
                separator: separator
            }
        );
    }
};

DataTableColumnHeaderUI.prototype._doSplitMultiValueCells = function() {
    var separator = window.prompt("What separator currently separates the values?", ",");
    if (separator != null) {
        this._dataTableView.doPostThenUpdate(
            "split-multi-value-cells",
            {
                columnName: this._column.headerLabel,
                keyColumnName: theProject.columnModel.keyColumnName,
                separator: separator,
                mode: "plain"
            }
        );
    }
};
