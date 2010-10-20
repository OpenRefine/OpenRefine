/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

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