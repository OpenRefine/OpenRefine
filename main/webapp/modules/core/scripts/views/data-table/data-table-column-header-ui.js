function DataTableColumnHeaderUI(dataTableView, column, columnIndex, td) {
    this._dataTableView = dataTableView;
    this._column = column;
    this._columnIndex = columnIndex;
    this._td = td;
    
    this._render();
}

DataTableColumnHeaderUI.prototype.getColumn = function() {
    return this._column;
};

DataTableColumnHeaderUI.prototype._render = function() {
    var self = this;
    var td = $(this._td);
    
    td.html(DOM.loadHTML("core", "scripts/views/data-table/column-header.html"));
    var elmts = DOM.bind(td);
    
    elmts.nameContainer.text(this._column.name);
    elmts.dropdownMenu.click(function() {
        self._createMenuForColumnHeader(this);
    });
    
    if ("reconStats" in this._column) {
        var stats = this._column.reconStats;
        if (stats.nonBlanks > 0) {
            var newPercent = Math.ceil(100 * stats.newTopics / stats.nonBlanks);
            var matchPercent = Math.ceil(100 * stats.matchedTopics / stats.nonBlanks);
            var unreconciledPercent = Math.ceil(100 * (stats.nonBlanks - stats.matchedTopics - stats.newTopics) / stats.nonBlanks);
            var title = matchPercent + "% matched, " + newPercent + "% new, " + unreconciledPercent + "% to be reconciled";
            
            var whole = $('<div>')
                .addClass("column-header-recon-stats-bar")
                .attr("title", title)
                .appendTo(elmts.reconStatsContainer.show());
            
            $('<div>')
                .addClass("column-header-recon-stats-blanks")
                .width(Math.round((stats.newTopics + stats.matchedTopics) * 100 / stats.nonBlanks) + "%")
                .appendTo(whole);
                
            $('<div>')
                .addClass("column-header-recon-stats-matched")
                .width(Math.round(stats.matchedTopics * 100 / stats.nonBlanks) + "%")
                .appendTo(whole);
        }
    }
};

DataTableColumnHeaderUI.prototype._createMenuForColumnHeader = function(elmt) {
    var self = this;
    MenuSystem.createAndShowStandardMenu([
        {
            label: "Facet",
            submenu: [
                {
                    label: "Text Facet",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : self._column.name,
                                "columnName" : self._column.name, 
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
                                "name" : self._column.name,
                                "columnName" : self._column.name, 
                                "expression" : "value",
                                "mode" : "range"
                            }
                        );
                    }
                },
                {
                    label: "Scatterplot Facet",
                    click: function() {
                        new ScatterplotDialog(self._column.name);
                    }
                },
                {},
                {
                    label: "Custom Text Facet ...",
                    click: function() { self._doFilterByExpressionPrompt(null, "list"); }
                },
                {
                    label: "Custom Numeric Facet ...",
                    click: function() { self._doFilterByExpressionPrompt(null, "range"); }
                },
                {
                    label: "Customized Facets",
                    submenu: [
                        {
                            label: "Word Facet",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "list", 
                                    {
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
                                        "expression" : "value.split(' ')"
                                    }
                                );
                            }
                        },
                        {},
                        {
                            label: "Numeric Log Facet",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "range", 
                                    {
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
                                        "expression" : "value.log()",
                                        "mode" : "range"
                                    }
                                );
                            }
                        },
                        {
                            label: "1-bounded Numeric Log Facet",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "range", 
                                    {
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
                                        "expression" : "log(max(1, value))",
                                        "mode" : "range"
                                    }
                                );
                            }
                        },
                        {},
                        {
                            label: "Text Length Facet",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "range", 
                                    {
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
                                        "expression" : "value.length()",
                                        "mode" : "range"
                                    }
                                );
                            }
                        },
                        {
                            label: "Log of Text Length Facet",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "range", 
                                    {
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
                                        "expression" : "value.length().log()",
                                        "mode" : "range"
                                    }
                                );
                            }
                        },
                        {
                            label: "Unicode Char-code Facet",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "range", 
                                    {
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
                                        "expression" : "value.unicode()",
                                        "mode" : "range"
                                    }
                                );
                            }
                        },
                        {},
                        {
                            label: "Facet by Error",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "list", 
                                    {
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
                                        "expression" : "isError(value)"
                                    }
                                );
                            }
                        },
                        {
                            label: "Facet by Blank",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "list", 
                                    {
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
                                        "expression" : "isBlank(value)"
                                    }
                                );
                            }
                        }
                    ]
                }
            ]
        },
        {
            label: "Text Filter",
            click: function() {
                ui.browsingEngine.addFacet(
                    "text", 
                    {
                        "name" : self._column.name,
                        "columnName" : self._column.name, 
                        "mode" : "text",
                        "caseSensitive" : false
                    }
                );
            }
        },
        {},
        {
            label: "Edit Cells",
            submenu: [
                {
                    label: "Transform ...",
                    click: function() { self._doTextTransformPrompt(); }
                },
                {
                    label: "Common Transforms",
                    submenu: [
                        {
                            label: "Unescape HTML entities",
                            click: function() { self._doTextTransform("value.unescape('html')", "store-blank", true, 10); }
                        },
                        {
                            label: "Collapse whitespace",
                            click: function() { self._doTextTransform("value.replaceRegexp('\\s+', ' ')", "store-blank", false, ""); }
                        },
                        {},
                        {
                            label: "To Titlecase",
                            click: function() { self._doTextTransform("toTitlecase(value)", "store-blank", false, ""); }
                        },
                        {
                            label: "To Uppercase",
                            click: function() { self._doTextTransform("toUppercase(value)", "store-blank", false, ""); }
                        },
                        {
                            label: "To Lowercase",
                            click: function() { self._doTextTransform("toLowercase(value)", "store-blank", false, ""); }
                        },
                        {
                            label: "To Blank",
                            click: function() { self._doTextTransform("null", "store-blank", false, ""); }
                        }
                    ]
                },
                {},
                {
                    label: "Fill Down",
                    click: function() { self._doFillDown(); }
                },
                {
                    label: "Blank Down",
                    click: function() { self._doBlankDown(); }
                },
                {},
                {
                    label: "Split Multi-Valued Cells ...",
                    click: function() { self._doSplitMultiValueCells(); }
                },
                {
                    label: "Join Multi-Valued Cells ...",
                    click: function() { self._doJoinMultiValueCells(); }
                },
                {},
                {
                    label: "Cluster & Edit ...",
                    click: function() { new ClusteringDialog(self._column.name, "value"); }
                }
            ]
        },
        {
            label: "Edit Column",
            submenu: [
                {
                    label: "Split into Several Columns",
                    click: function() { self._doSplitColumn(); }
                },
                {
                    label: "Add Column Based on This Column ...",
                    click: function() { self._doAddColumn("value"); }
                },
                {
                    label: "Add Columns From Freebase ...",
                    click: function() { self._doAddColumnFromFreebase(); }
                },
                {},
                {
                    label: "Rename This Column",
                    click: function() { self._doRenameColumn(); }
                },
                {
                    label: "Remove This Column",
                    click: function() { self._doRemoveColumn(); }
                },
                {},
                {
                    label: "Move Column to Beginning",
                    click: function() { self._doMoveColumnTo(0); }
                },
                {
                    label: "Move Column to End",
                    click: function() { self._doMoveColumnTo(theProject.columnModel.columns.length - 1); }
                },
                {
                    label: "Move Column Left",
                    click: function() { self._doMoveColumnBy(-1); }
                },
                {
                    label: "Move Column Right",
                    click: function() { self._doMoveColumnBy(1); }
                }
            ]
        },
        {
            label: "Transpose",
            submenu: [
                {
                    label: "Cells Across Columns into Rows",
                    click: function() { self._doTransposeColumnsIntoRows(); }
                },
                {
                    label: "Cells in Rows into Columns",
                    click: function() { self._doTransposeRowsIntoColumns(); }
                }
            ]
        },
        {},
        (
            this._dataTableView._getSortingCriterionForColumn(this._column.name) == null ?
                {
                    "label": "Sort ...",
                    "click": function() {
                        self._showSortingCriterion(null, self._dataTableView._getSortingCriteriaCount() > 0)
                    }
                } :
                {
                    label: "Sort",
                    submenu: this.createSortingMenu()
                }
        ),
        {
            label: "View",
            tooltip: "Collapse/expand columns to make viewing the data more convenient",
            submenu: [
                {
                    label: "Collapse This Column",
                    click: function() {
                        self._dataTableView._collapsedColumnNames[self._column.name] = true;
                        self._dataTableView.render();
                    }
                },
                {
                    label: "Collapse All Other Columns",
                    click: function() {
                        var collapsedColumnNames = {};
                        for (var i = 0; i < theProject.columnModel.columns.length; i++) {
                            if (i != self._columnIndex) {
                                collapsedColumnNames[theProject.columnModel.columns[i].name] = true;
                            }
                        }
                        self._dataTableView._collapsedColumnNames = collapsedColumnNames;
                        self._dataTableView.render();
                    }
                },
                {
                    label: "Collapse All Columns To Left",
                    click: function() {
                        for (var i = 0; i < self._columnIndex; i++) {
                            self._dataTableView._collapsedColumnNames[theProject.columnModel.columns[i].name] = true;
                        }
                        self._dataTableView.render();
                    }
                },
                {
                    label: "Collapse All Columns To Right",
                    click: function() {
                        for (var i = self._columnIndex + 1; i < theProject.columnModel.columns.length; i++) {
                            self._dataTableView._collapsedColumnNames[theProject.columnModel.columns[i].name] = true;
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
                    label: "Facets",
                    submenu: [
                        {
                            label: "By Judgment",
                            click: function() {
                                ui.browsingEngine.addFacet(
                                    "list", 
                                    {
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
                                        "expression" : "cell.recon.judgment",
                                        "omitError" : true
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
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
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
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
                                        "expression" : "cell.recon.features.typeMatch",
                                        "omitError" : true
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
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
                                        "expression" : "cell.recon.features.nameMatch",
                                        "omitError" : true
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
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
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
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
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
                                        "name" : self._column.name,
                                        "columnName" : self._column.name, 
                                        "expression" : "cell.recon.best.type",
                                        "omitError" : true
                                    }
                                );
                            }
                        }
                    ]
                },
                {
                    label: "Actions",
                    submenu: [
                        {
                            label: "Match Each Cell to Its Best Candidate",
                            tooltip: "Match each cell to its best candidate in this column for all current filtered rows",
                            click: function() {
                                self._doReconMatchBestCandidates();
                            }
                        },
                        {
                            label: "Create a New Topic for Each Cell",
                            tooltip: "Mark to create one new topic for each cell in this column for all current filtered rows",
                            click: function() {
                                self._doReconMarkNewTopics(false);
                            }
                        },
                        {},
                        {
                            label: "Create One New Topic for Similar Cells",
                            tooltip: "Mark to create one new topic for each group of similar cells in this column for all current filtered rows",
                            click: function() {
                                self._doReconMarkNewTopics(true);
                            }
                        },
                        {
                            label: "Match All Filtered Cells to ...",
                            tooltip: "Search for a topic to match all filtered cells to",
                            click: function() {
                                self._doSearchToMatch();
                            }
                        },
                        {},
                        {
                            label: "Discard Reconciliation Judgments",
                            tooltip: "Discard reconciliaton judgments in this column for all current filtered rows",
                            click: function() {
                                self._doReconDiscardJudgments();
                            }
                        }
                    ]
                }
            ]
        }
    ], elmt, { width: "120px", horizontal: false });
};

DataTableColumnHeaderUI.prototype.createSortingMenu = function() {
    var self = this;
    var criterion = this._dataTableView._getSortingCriterionForColumn(this._column.name);
    var criteriaCount = this._dataTableView._getSortingCriteriaCount();
    var hasOtherCriteria = criterion == null ? (criteriaCount > 0) : criteriaCount > 1;
    
    var items = [
        {
            "label": "Sort ...",
            "click": function() {
                self._showSortingCriterion(criterion, hasOtherCriteria)
            }
        }
    ];
    
    if (criterion != null) {
        items.push({
            "label": "Reverse",
            "click": function() {
                criterion.reverse = !criterion.reverse;
                self._dataTableView._addSortingCriterion(criterion);
            }
        });
        items.push({
            "label": "Un-sort",
            "click": function() {
                self._dataTableView._removeSortingCriterionOfColumn(criterion.column);
            }
        });
    }
    
    return items;
};

DataTableColumnHeaderUI.prototype._doFilterByExpressionPrompt = function(expression, type) {
    var self = this;
    DataTableView.promptExpressionOnVisibleRows(
        this._column,
        (type == "list" ? "Custom Facet on column " : "Custom Numeric Facet on column") + this._column.name, 
        expression,
        function(expression) {
            var config = {
                "name" : self._column.name,
                "columnName" : self._column.name, 
                "expression" : expression
            };
            if (type == "range") {
                config.mode = "range";
            }

            ui.browsingEngine.addFacet(type, config);
        }
    );
};

DataTableColumnHeaderUI.prototype._doTextTransform = function(expression, onError, repeat, repeatCount) {
    Gridworks.postProcess(
        "text-transform",
        {
            columnName: this._column.name, 
            expression: expression, 
            onError: onError,
            repeat: repeat,
            repeatCount: repeatCount
        },
        null,
        { cellsChanged: true }
    );
};

DataTableColumnHeaderUI.prototype._doTextTransformPrompt = function() {
    var self = this;
    var frame = $(
        DOM.loadHTML("core", "scripts/views/data-table/text-transform-dialog.html")
            .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));
            
    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text("Custom text transform on column " + this._column.name);
    
    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };
    
    elmts.cancelButton.click(dismiss);
    elmts.okButton.click(function() {
        self._doTextTransform(
            previewWidget.getExpression(true),
            $('input[name="text-transform-dialog-onerror-choice"]:checked')[0].value,
            elmts.repeatCheckbox[0].checked,
            elmts.repeatCountInput[0].value
        );
        dismiss();
    });
    
    var o = DataTableView.sampleVisibleRows(this._column);
    var previewWidget = new ExpressionPreviewDialog.Widget(
        elmts,
        this._column.cellIndex,
        o.rowIndices,
        o.values,
        null
    );
    previewWidget._prepareUpdate = function(params) {
        params.repeat = elmts.repeatCheckbox[0].checked;
        params.repeatCount = elmts.repeatCountInput[0].value;
    };
    elmts.repeatCheckbox.click(function() {
        previewWidget.update();
    });
};

DataTableColumnHeaderUI.prototype._doReconcile = function() {
    new ReconDialog(this._column);
};

DataTableColumnHeaderUI.prototype._doReconDiscardJudgments = function() {
    Gridworks.postProcess(
        "recon-discard-judgments",
        { columnName: this._column.name },
        null,
        { cellsChanged: true, columnStatsChanged: true }
    );
};

DataTableColumnHeaderUI.prototype._doReconMatchBestCandidates = function() {
    Gridworks.postProcess(
        "recon-match-best-candidates",
        { columnName: this._column.name },
        null,
        { cellsChanged: true, columnStatsChanged: true }
    );
};

DataTableColumnHeaderUI.prototype._doReconMarkNewTopics = function(shareNewTopics) {
    Gridworks.postProcess(
        "recon-mark-new-topics",
        { columnName: this._column.name, shareNewTopics: shareNewTopics },
        null,
        { cellsChanged: true, columnStatsChanged: true }
    );
};

DataTableColumnHeaderUI.prototype._doSearchToMatch = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("400px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Search for Match").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    $('<p></p>').text("Search Freebase for a topic to match all filtered cells:").appendTo(body);
    
    var input = $('<input />').appendTo($('<p></p>').appendTo(body));
    
    input.suggest({}).bind("fb-select", function(e, data) {
        var query = {
            "id" : data.id,
            "type" : []
        };
        var baseUrl = "http://api.freebase.com/api/service/mqlread";
        var url = baseUrl + "?" + $.param({ query: JSON.stringify({ query: query }) }) + "&callback=?";
        
        $.getJSON(
            url,
            null,
            function(o) {
                var types = "result" in o ? o.result.type : [];
                
                Gridworks.postProcess(
                    "recon-match-specific-topic-to-cells",
                    {
                        columnName: self._column.name,
                        topicID: data.id,
                        topicGUID: data.guid,
                        topicName: data.name,
                        types: types.join(",")
                    },
                    null,
                    { cellsChanged: true, columnStatsChanged: true }
                );
        
                DialogSystem.dismissUntil(level - 1);
            }
        );
    });
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    var level = DialogSystem.showDialog(frame);
    input.focus().data("suggest").textchange();
};

DataTableColumnHeaderUI.prototype._doAddColumn = function(initialExpression) {
    var self = this;
    var frame = $(
        DOM.loadHTML("core", "scripts/views/data-table/add-column-dialog.html")
            .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));
            
    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text("Add column based on column " + this._column.name);
    
    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };
    
    elmts.cancelButton.click(dismiss);
    elmts.okButton.click(function() {
        var columnName = $.trim(elmts.columnNameInput[0].value);
        if (!columnName.length) {
            alert("You must enter a column name.");
            return;
        }
        
        Gridworks.postProcess(
            "add-column", 
            {
                baseColumnName: self._column.name, 
                expression: previewWidget.getExpression(true), 
                newColumnName: columnName, 
                columnInsertIndex: self._columnIndex + 1,
                onError: $('input[name="create-column-dialog-onerror-choice"]:checked')[0].value
            },
            null,
            { modelsChanged: true }
        );
        dismiss();
    });
    
    var o = DataTableView.sampleVisibleRows(this._column);
    var previewWidget = new ExpressionPreviewDialog.Widget(
        elmts, 
        this._column.cellIndex,
        o.rowIndices,
        o.values,
        null
    );    
};

DataTableColumnHeaderUI.prototype._doAddColumnFromFreebase = function() {
    var o = DataTableView.sampleVisibleRows(this._column);
    var self = this;
    new ExtendDataPreviewDialog(
        this._column, 
        this._columnIndex, 
        o.rowIndices, 
        function(extension) {
            Gridworks.postProcess(
                "extend-data", 
                {
                    baseColumnName: self._column.name,
                    columnInsertIndex: self._columnIndex + 1
                },
                {
                    extension: JSON.stringify(extension)
                },
                { rowsChanged: true, modelsChanged: true }
            );
        }
    );
};

DataTableColumnHeaderUI.prototype._doRemoveColumn = function() {
    Gridworks.postProcess(
        "remove-column", 
        {
            columnName: this._column.name
        },
        null,
        { modelsChanged: true }
    );
};

DataTableColumnHeaderUI.prototype._doRenameColumn = function() {
    var newColumnName = window.prompt("Enter new column name", this._column.name);
    if (newColumnName !== null) {
        Gridworks.postProcess(
            "rename-column", 
            {
                oldColumnName: this._column.name,
                newColumnName: newColumnName
            },
            null,
            { modelsChanged: true }
        );
    }
};

DataTableColumnHeaderUI.prototype._doMoveColumnTo = function(index) {
    Gridworks.postProcess(
        "move-column", 
        {
            columnName: this._column.name,
            index: index
        },
        null,
        { modelsChanged: true }
    );
};

DataTableColumnHeaderUI.prototype._doMoveColumnBy = function(change) {
    Gridworks.postProcess(
        "move-column", 
        {
            columnName: this._column.name,
            index: Gridworks.columnNameToColumnIndex(this._column.name) + change
        },
        null,
        { modelsChanged: true }
    );
};

DataTableColumnHeaderUI.prototype._doFillDown = function() {
    Gridworks.postProcess(
        "fill-down", 
        {
            columnName: this._column.name
        },
        null,
        { modelsChanged: true }
    );
};

DataTableColumnHeaderUI.prototype._doBlankDown = function() {
    Gridworks.postProcess(
        "blank-down", 
        {
            columnName: this._column.name
        },
        null,
        { modelsChanged: true }
    );
};

DataTableColumnHeaderUI.prototype._doJoinMultiValueCells = function() {
    var separator = window.prompt("Enter separator to use between values", ", ");
    if (separator !== null) {
        Gridworks.postProcess(
            "join-multi-value-cells", 
            {
                columnName: this._column.name,
                keyColumnName: theProject.columnModel.keyColumnName,
                separator: separator
            },
            null,
            { rowsChanged: true }
        );
    }
};

DataTableColumnHeaderUI.prototype._doSplitMultiValueCells = function() {
    var separator = window.prompt("What separator currently separates the values?", ",");
    if (separator !== null) {
        Gridworks.postProcess(
            "split-multi-value-cells", 
            {
                columnName: this._column.name,
                keyColumnName: theProject.columnModel.keyColumnName,
                separator: separator,
                mode: "plain"
            },
            null,
            { rowsChanged: true }
        );
    }
};

DataTableColumnHeaderUI.prototype._doSplitColumn = function() {
    var self = this;
    var frame = $(DOM.loadHTML("core", "scripts/views/data-table/split-column-dialog.html"));
    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text("Split column " + this._column.name + " into several columns");
    
    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };
    
    elmts.cancelButton.click(dismiss);
    elmts.okButton.click(function() {
        var mode = $("input[name='split-by-mode']:checked")[0].value;
        var config = {
            columnName: self._column.name,
            mode: mode,
            guessCellType: elmts.guessCellTypeInput[0].checked,
            removeOriginalColumn: elmts.removeColumnInput[0].checked
        };
        if (mode == "separator") {
            config.separator = elmts.separatorInput[0].value;
            if (!(config.separator)) {
                alert("Please specify a separator.");
                return;
            }

            config.regex = elmts.regexInput[0].checked;
            
            var s = elmts.maxColumnsInput[0].value;
            if (s) {
                var n = parseInt(s,10);
                if (!isNaN(n)) {
                    config.maxColumns = n;
                }
            }
        } else {
            var s = "[" + elmts.lengthsTextarea[0].value + "]";
            try {
                var a = JSON.parse(s);
            } catch (e) {
                alert("The given field lengths are not properly formatted.");
                return;
            }
            
            var lengths = [];
            $.each(a, function(i,n) { if (typeof n == "number") lengths.push(n); });
            
            if (lengths.length === 0) {
                alert("No field length is specified.");
                return;
            }
            
            config.fieldLengths = JSON.stringify(lengths);
        }
        
        Gridworks.postProcess(
            "split-column", 
            config,
            null,
            { modelsChanged: true }
        );
        dismiss();
    });
};

DataTableColumnHeaderUI.prototype._doTransposeColumnsIntoRows = function() {
    var self = this;
    var dialog = $(DOM.loadHTML("core", "scripts/views/data-table/transpose-columns-into-rows.html"));

    var elmts = DOM.bind(dialog);
    elmts.dialogHeader.text('Transpose Cells Across Columns into Rows');
    
    var level = DialogSystem.showDialog(dialog);
    var dismiss = function() {
        DialogSystem.dismissUntil(level - 1);
    };
    
    var columns = theProject.columnModel.columns;
    
    elmts.cancelButton.click(function() { dismiss(); });
    elmts.okButton.click(function() {
        var config = {
            startColumnName: elmts.fromColumnSelect[0].value,
            columnCount: elmts.toColumnSelect[0].value,
            combinedColumnName: $.trim(elmts.combinedColumnNameInput[0].value),
            prependColumnName: elmts.prependColumnNameCheckbox[0].checked,
            separator: elmts.separatorInput[0].value,
            ignoreBlankCells: elmts.ignoreBlankCellsCheckbox[0].checked
        };
        
        Gridworks.postProcess(
            "transpose-columns-into-rows", 
            config,
            null,
            { modelsChanged: true }
        );
        dismiss();
    });
    
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        var option = $('<option>').attr("value", column.name).text(column.name).appendTo(elmts.fromColumnSelect);
        if (column.name == this._column.name) {
            option.attr("selected", "true");
        }
    }
    
    var populateToColumn = function() {
        elmts.toColumnSelect.empty();
        
        var toColumnName = elmts.fromColumnSelect[0].value;
        
        var j = 0;
        for (; j < columns.length; j++) {
            var column = columns[j];
            if (column.name == toColumnName) {
                break;
            }
        }
        
        for (var k = j + 1; k < columns.length; k++) {
            var column = columns[k];
            var option = $('<option>').attr("value", k - j + 1).text(column.name).appendTo(elmts.toColumnSelect);
            if (k == columns.length - 1) {
                option.attr("selected", "true");
            }
        }
    };
    populateToColumn();
    
    elmts.fromColumnSelect.bind("change", populateToColumn);
};

DataTableColumnHeaderUI.prototype._doTransposeRowsIntoColumns = function() {
    var rowCount = window.prompt("How many rows to transpose?", "2");
    if (rowCount != null) {
        try {
            rowCount = parseInt(rowCount);
        } catch (e) {
            // ignore
        }
        
        if (isNaN(rowCount) || rowCount < 2) {
            alert("Expected an integer at least 2.");
        } else {
            var config = {
                columnName: this._column.name,
                rowCount: rowCount
            };

            Gridworks.postProcess(
                "transpose-rows-into-columns", 
                config,
                null,
                { modelsChanged: true }
            );
        }
    }
};

DataTableColumnHeaderUI.prototype._showSortingCriterion = function(criterion, hasOtherCriteria) {
    criterion = criterion || {
        column: this._column.name,
        valueType: "string",
        caseSensitive: false,
        errorPosition: 1,
        blankPosition: 2
    };
    
    var self = this;
    var frame = $(DOM.loadHTML("core", "scripts/views/data-table/sorting-criterion-dialog.html"));
    var elmts = DOM.bind(frame);
    
    elmts.dialogHeader.text('Sort by ' + this._column.name);
    
    elmts.valueTypeOptions
        .find("input[type='radio'][value='" + criterion.valueType + "']")
        .attr("checked", "checked");
        
    var setValueType = function(valueType) {
        var forward = elmts.directionForwardLabel;
        var reverse = elmts.directionReverseLabel;
        if (valueType == "string") {
            forward.html("a - z");
            reverse.html("z - a");
        } else if (valueType == "number") {
            forward.html("smallest first");
            reverse.html("largest first");
        } else if (valueType == "date") {
            forward.html("earliest first");
            reverse.html("latest first");
        } else if (valueType == "boolean") {
            forward.html("false then true");
            reverse.html("true then false");
        }
    };
    elmts.valueTypeOptions
        .find("input[type='radio']")
        .change(function() {
            setValueType(this.value);
        });
       
    if (criterion.valueType == "string" && criterion.caseSensitive) {
        elmts.caseSensitiveCheckbox.attr("checked", "checked");
    }
    
    elmts.directionOptions
        .find("input[type='radio'][value='" + (criterion.reverse ? "reverse" : "forward") + "']")
        .attr("checked", "checked");

    if (hasOtherCriteria) {
        elmts.sortAloneContainer.show();
    }
    
    var validValuesHtml = '<li kind="value">Valid Values</li>';
    var blankValuesHtml = '<li kind="blank">Blanks</li>';
    var errorValuesHtml = '<li kind="error">Errors</li>';
    var positionsHtml;
    if (criterion.blankPosition < 0) {
        if (criterion.errorPosition > 0) {
            positionsHtml = [ blankValuesHtml, validValuesHtml, errorValuesHtml ];
        } else if (criterion.errorPosition < criterion.blankPosition) {
            positionsHtml = [ errorValuesHtml, blankValuesHtml, validValuesHtml ];
        } else {
            positionsHtml = [ blankValuesHtml, errorValuesHtml, validValuesHtml ];
        }
    } else {
        if (criterion.errorPosition < 0) {
            positionsHtml = [ errorValuesHtml, validValuesHtml, blankValuesHtml ];
        } else if (criterion.errorPosition < criterion.blankPosition) {
            positionsHtml = [ validValuesHtml, errorValuesHtml, blankValuesHtml  ];
        } else {
            positionsHtml = [ validValuesHtml, blankValuesHtml, errorValuesHtml ];
        }
    }
    elmts.blankErrorPositions.html(positionsHtml.join("")).sortable().disableSelection();
    
    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };
    
    setValueType(criterion.valueType); 
    
    elmts.cancelButton.click(dismiss);
    elmts.okButton.click(function() {
        var criterion2 = {
            column: self._column.name,
            valueType: elmts.valueTypeOptions.find("input[type='radio']:checked")[0].value,
            reverse: elmts.directionOptions.find("input[type='radio']:checked")[0].value == "reverse"
        };
        
        var valuePosition, blankPosition, errorPosition;
        elmts.blankErrorPositions.find("li").each(function(index, elmt) {
            var kind = this.getAttribute("kind");
            if (kind == "value") {
                valuePosition = index;
            } else if (kind == "blank") {
                blankPosition = index;
            } else if (kind == "error") {
                errorPosition = index;
            }
        });
        criterion2.blankPosition = blankPosition - valuePosition;
        criterion2.errorPosition = errorPosition - valuePosition;
        
        if (criterion2.valueType == "string") {
            criterion2.caseSensitive = elmts.caseSensitiveCheckbox[0].checked;
        }
        
        self._dataTableView._addSortingCriterion(
            criterion2, elmts.sortAloneContainer.find("input")[0].checked);
            
        dismiss();
    });
};