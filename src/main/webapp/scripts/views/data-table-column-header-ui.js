function DataTableColumnHeaderUI(dataTableView, column, columnIndex, td) {
    this._dataTableView = dataTableView;
    this._column = column;
    this._columnIndex = columnIndex;
    this._td = td;
    
    this._render();
}

DataTableColumnHeaderUI.prototype._render = function() {
    var self = this;
    
    var html = $(
        '<table class="column-header-layout">' +
            '<tr>' +
                '<td width="1%">' +
                    '<a class="column-header-menu" bind="dropdownMenu">&nbsp;</a>' +
                '</td>' +
                '<td bind="nameContainer"></td>' +
            '</tr>' +
        '</table>' +
        '<div style="display:none;" bind="reconStatsContainer"></div>'
    ).appendTo(this._td);
    
    var elmts = DOM.bind(html);
    
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
    self = this;
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
                    click: function() { self._doFilterByExpressionPrompt("value", "list"); }
                },
                {
                    label: "Custom Numeric Facet ...",
                    click: function() { self._doFilterByExpressionPrompt("value", "range"); }
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
                        }
                    ]
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
                }
            ]
        },
        {},
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
    var frame = DialogSystem.createDialog();
    frame.width("700px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Custom text transform on column " + this._column.name).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    body.html(
        '<div class="grid-layout layout-tight layout-full"><table>' +
            '<tr>' +
                '<td colspan="4">' + ExpressionPreviewDialog.generateWidgetHtml() + '</td>' +
            '</tr>' +
            '<tr style="white-space: pre;">' +
                '<td width="1%">' +
                    'On error' +
                '</td>' +
                '<td>' +
                    '<input type="radio" name="text-transform-dialog-onerror-choice" value="set-to-blank" checked /> set to blank<br/>' +
                    '<input type="radio" name="text-transform-dialog-onerror-choice" value="store-error" /> store error<br/>' +
                    '<input type="radio" name="text-transform-dialog-onerror-choice" value="keep-original" /> keep original' +
                '</td>' +
                '<td width="1%">' +
                    '<input type="checkbox" bind="repeatCheckbox" />' +
                '</td>' +
                '<td>' +
                    'Re-transform until no change<br/>' +
                    'up to <input bind="repeatCountInput" value="10" size="3" /> times' +
                '</td>' +
            '</tr>' +
        '</table>'
    );
    var bodyElmts = DOM.bind(body);
    
    footer.html(
        '<button bind="okButton">&nbsp;&nbsp;OK&nbsp;&nbsp;</button>' +
        '<button bind="cancelButton">Cancel</button>'
    );
    var footerElmts = DOM.bind(footer);
        
    var level = DialogSystem.showDialog(frame);
    var dismiss = function() {
        DialogSystem.dismissUntil(level - 1);
    };
    
    footerElmts.okButton.click(function() {
        self._doTextTransform(
            previewWidget.getExpression(true),
            $('input[name="text-transform-dialog-onerror-choice"]:checked')[0].value,
            bodyElmts.repeatCheckbox[0].checked,
            bodyElmts.repeatCountInput[0].value
        );
        dismiss();
    });
    footerElmts.cancelButton.click(dismiss);
    
    var o = DataTableView.sampleVisibleRows(this._column);
    var previewWidget = new ExpressionPreviewDialog.Widget(
        bodyElmts, 
        this._column.cellIndex,
        o.rowIndices,
        o.values,
        "value"
    );
    previewWidget._prepareUpdate = function(params) {
        params.repeat = bodyElmts.repeatCheckbox[0].checked;
        params.repeatCount = bodyElmts.repeatCountInput[0].value;
    };
    bodyElmts.repeatCheckbox.click(function() {
        previewWidget.update();
    });
};

DataTableColumnHeaderUI.prototype._doReconcile = function() {
    var self = this;
    var dismissBusy = DialogSystem.showBusy();
    $.post(
        "/command/guess-types-of-column?" + $.param({ project: theProject.id, columnName: this._column.name }), 
        null,
        function(data) {
            if (data.code != "ok") {
                dismissBusy();
                new ReconDialog(self._column, []);
            } else {
                data.types = data.types.slice(0, 20);
                
                var ids = $.map(data.types, function(elmt) { return elmt.id; });
                if (!ids.length) {
                    dismissBusy();
                    new ReconDialog(self._column, []);
                } else {
                    var query = [{
                        "id|=" : ids,
                        "id" : null,
                        "/freebase/type_profile/kind" : []
                    }];
                    $.getJSON(
                        "http://api.freebase.com/api/service/mqlread?" + $.param({ "query" : JSON.stringify({ "query" : query }) }) + "&callback=?",
                        null,
                        function(o) {
                            dismissBusy();
                            
                            var kindMap = {};
                            $.each(o.result, function() {
                                var m = kindMap[this.id] = {};
                                $.each(this["/freebase/type_profile/kind"], function() {
                                    m[this] = true;
                                });
                            });
                            
                            new ReconDialog(self._column, $.map(data.types, function(type) {
                                if (type.id in kindMap) {
                                    var m = kindMap[type.id];
                                    if (!("Role" in m) && !("Annotation" in m)) {
                                        return type;
                                    }
                                }
                                return null;
                            }));
                        },
                        "jsonp"
                    );
                }
            }
        },
        "json"
    );

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
    var frame = DialogSystem.createDialog();
    frame.width("700px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Add Column Based on Column " + this._column.name).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    body.html(
        '<div class="grid-layout layout-normal layout-full"><table cols="2">' +
            '<tr>' +
                '<td width="1%" style="white-space: pre;">' +
                    'New column name' +
                '</td>' +
                '<td>' +
                    '<input bind="columnNameInput" size="40" />' +
                '</td>' +
            '</tr>' +
            '<tr>' +
                '<td width="1%" style="white-space: pre;">' +
                    'On error' +
                '</td>' +
                '<td>' +
                    '<input type="radio" name="create-column-dialog-onerror-choice" value="set-to-blank" checked /> set to blank ' +
                    '<input type="radio" name="create-column-dialog-onerror-choice" value="store-error" /> store error ' +
                    '<input type="radio" name="create-column-dialog-onerror-choice" value="keep-original" /> keep original' +
                '</td>' +
            '</tr>' +
            '<tr>' +
                '<td colspan="2">' + ExpressionPreviewDialog.generateWidgetHtml() + '</td>' +
            '</tr>' +
        '</table></div>'
    );
    var bodyElmts = DOM.bind(body);
    
    footer.html(
        '<button bind="okButton">&nbsp;&nbsp;OK&nbsp;&nbsp;</button>' +
        '<button bind="cancelButton">Cancel</button>'
    );
    var footerElmts = DOM.bind(footer);
        
    var level = DialogSystem.showDialog(frame);
    var dismiss = function() {
        DialogSystem.dismissUntil(level - 1);
    };
    
    footerElmts.okButton.click(function() {
        var columnName = $.trim(bodyElmts.columnNameInput[0].value);
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
    footerElmts.cancelButton.click(dismiss);
    
    var o = DataTableView.sampleVisibleRows(this._column);
    var previewWidget = new ExpressionPreviewDialog.Widget(
        bodyElmts, 
        this._column.cellIndex,
        o.rowIndices,
        o.values,
        "value"
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
    var frame = DialogSystem.createDialog();
    frame.width("600px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Split Column " + this._column.name + " into Several Columns").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    body.html(
        '<div class="grid-layout layout-looser layout-full"><table><tr>' +
            '<td>' +
                '<div class="grid-layout layout-tighter"><table>' +
                    '<tr>' +
                        '<td colspan="3"><h3>How to Split Column</h3></td>' +
                    '</tr>' +
                    '<tr>' +
                        '<td width="1%"><input type="radio" checked="true" name="split-by-mode" value="separator" /></td>' +
                        '<td colspan="2">by separator</td>' +
                    '</tr>' +
                    '<tr><td></td>' +
                        '<td>Separator</td>' +
                        '<td style="white-space: pre;">' +
                            '<input size="15" value="," bind="separatorInput" /> ' +
                            '<input type="checkbox" bind="regexInput" /> regular expression' +
                        '</td>' +
                    '</tr>' +
                    '<tr><td></td>' +
                        '<td>Split into</td>' +
                        '<td style="white-space: pre;"><input size="3" bind="maxColumnsInput" /> ' +
                            'columns at most (leave blank for no limit)</td>' +
                    '</tr>' +
                    '<tr>' +
                        '<td width="1%"><input type="radio" name="split-by-mode" value="lengths" /></td>' +
                        '<td colspan="2">by field lengths</td>' +
                    '</tr>' +
                    '<tr><td></td>' +
                        '<td colspan="2">' +
                            '<textarea style="width: 100%;" bind="lengthsTextarea"></textarea>' +
                        '</td>' +
                    '</tr>' +
                    '<tr><td></td>' +
                        '<td colspan="2">' +
                            'List of integers separated by commas, e.g., 5, 7, 15' +
                        '</td>' +
                    '</tr>' +
                '</table></div>' +
            '</td>' +
            '<td>' +
                '<div class="grid-layout layout-tighter"><table>' +
                    '<tr>' +
                        '<td colspan="3"><h3>After Splitting</h3></td>' +
                    '</tr>' +
                    '<tr>' +
                        '<td width="1%"><input type="checkbox" checked="true" bind="guessCellTypeInput" /></td>' +
                        '<td colspan="2">Guess cell type</td>' +
                    '</tr>' +
                    '<tr>' +
                        '<td width="1%"><input type="checkbox" checked="true" bind="removeColumnInput" /></td>' +
                        '<td colspan="2">Remove this column</td>' +
                    '</tr>' +
                '</table></div>' +
            '</td>' +
        '</table></div>'
    );
    var bodyElmts = DOM.bind(body);
    
    footer.html(
        '<button bind="okButton">&nbsp;&nbsp;OK&nbsp;&nbsp;</button>' +
        '<button bind="cancelButton">Cancel</button>'
    );
    var footerElmts = DOM.bind(footer);
        
    var level = DialogSystem.showDialog(frame);
    var dismiss = function() {
        DialogSystem.dismissUntil(level - 1);
    };
    
    footerElmts.okButton.click(function() {
        var mode = $("input[name='split-by-mode']:checked")[0].value;
        var config = {
            columnName: self._column.name,
            mode: mode,
            guessCellType: bodyElmts.guessCellTypeInput[0].checked,
            removeOriginalColumn: bodyElmts.removeColumnInput[0].checked
        };
        if (mode == "separator") {
            config.separator = bodyElmts.separatorInput[0].value;
            if (!(config.separator)) {
                alert("Please specify a separator.");
                return;
            }

            config.regex = bodyElmts.regexInput[0].checked;
            
            var s = bodyElmts.maxColumnsInput[0].value;
            if (s) {
                var n = parseInt(s,10);
                if (!isNaN(n)) {
                    config.maxColumns = n;
                }
            }
        } else {
            var s = "[" + bodyElmts.lengthsTextarea[0].value + "]";
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
    
    footerElmts.cancelButton.click(dismiss);
};
