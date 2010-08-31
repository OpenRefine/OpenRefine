DataTableColumnHeaderUI.extendMenu(function(column, columnHeaderUI, menu) {
    var doFilterByExpressionPrompt = function(expression, type) {
        DataTableView.promptExpressionOnVisibleRows(
            column,
            (type == "list" ? "Custom Facet on column " : "Custom Numeric Facet on column") + column.name, 
            expression,
            function(expression) {
                var config = {
                    "name" : column.name,
                    "columnName" : column.name, 
                    "expression" : expression
                };
                if (type == "range") {
                    config.mode = "range";
                }

                ui.browsingEngine.addFacet(type, config);
            }
        );
    };
    
    MenuSystem.appendTo(menu, [ "core/facet" ], [
        {
            id: "core/text-facet",
            label: "Text Facet",
            click: function() {
                ui.browsingEngine.addFacet(
                    "list",
                    {
                        "name": column.name,
                        "columnName": column.name,
                        "expression": "value"
                    }
                );
            }
        },
        {
            id: "core/numeric-facet",
            label: "Numeric Facet",
            click: function() {
                ui.browsingEngine.addFacet(
                    "range",
                    {
                        "name": column.name,
                        "columnName": column.name,
                        "expression": "value",
                        "mode": "range"
                    }
                );
            }
        },
        {
            id: "core/time-facet",
            label: "Timeline Facet",
            click: function() {
                ui.browsingEngine.addFacet(
                    "timerange",
                    {
                        "name": column.name,
                        "columnName": column.name,
                        "expression": "value",
                        "mode": "range"
                    }
                );
            }
        },
        {
            id: "core/scatterplot-facet",
            label: "Scatterplot Facet",
            click: function() {
                new ScatterplotDialog(column.name);
            }
        },
        {},
        {
            id: "core/custom-text-facet",
            label: "Custom Text Facet ...",
            click: function() {
                doFilterByExpressionPrompt(null, "list");
            }
        },
        {
            id: "core/custom-numeric-facet",
            label: "Custom Numeric Facet ...",
            click: function() {
                doFilterByExpressionPrompt(null, "range");
            }
        },
        {
            id: "core/customized-facets",
            label: "Customized Facets",
            submenu: [
                {
                    id: "core/word-facet",
                    label: "Word Facet",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list",
                            {
                                "name": column.name,
                                "columnName": column.name,
                                "expression": "value.split(' ')"
                            }
                        );
                    }
                },
                {},
                {
                    id: "core/numeric-log-facet",
                    label: "Numeric Log Facet",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range",
                            {
                                "name": column.name,
                                "columnName": column.name,
                                "expression": "value.log()",
                                "mode": "range"
                            }
                        );
                    }
                },
                {
                    id: "core/bounded-numeric-log-facet",
                    label: "1-bounded Numeric Log Facet",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range",
                            {
                                "name": column.name,
                                "columnName": column.name,
                                "expression": "log(max(1, value))",
                                "mode": "range"
                            }
                        );
                    }
                },
                {},
                {
                    id: "core/text-length-facet",
                    label: "Text Length Facet",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range",
                            {
                                "name": column.name,
                                "columnName": column.name,
                                "expression": "value.length()",
                                "mode": "range"
                            }
                        );
                    }
                },
                {
                    id: "core/log-text-length-facet",
                    label: "Log of Text Length Facet",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range",
                            {
                                "name": column.name,
                                "columnName": column.name,
                                "expression": "value.length().log()",
                                "mode": "range"
                            }
                        );
                    }
                },
                {
                    id: "core/unicode-charcode-facet",
                    label: "Unicode Char-code Facet",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range",
                            {
                                "name": column.name,
                                "columnName": column.name,
                                "expression": "value.unicode()",
                                "mode": "range"
                            }
                        );
                    }
                },
                {},
                {
                    id: "core/error-facet",
                    label: "Facet by Error",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list",
                            {
                                "name": column.name,
                                "columnName": column.name,
                                "expression": "isError(value)"
                            }
                        );
                    }
                },
                {
                    id: "core/blank-facet",
                    label: "Facet by Blank",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list",
                            {
                                "name": column.name,
                                "columnName": column.name,
                                "expression": "isBlank(value)"
                            }
                        );
                    }
                }
            ]
        }
    ]);
    
    MenuSystem.insertAfter(menu, [ "core/facet" ], [
        {
            label: "Text Filter",
            click: function() {
                ui.browsingEngine.addFacet(
                    "text", 
                    {
                        "name" : column.name,
                        "columnName" : column.name, 
                        "mode" : "text",
                        "caseSensitive" : false
                    }
                );
            }
        }
    ]);
});