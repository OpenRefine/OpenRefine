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
      (type == "list" ? $.i18n._('core-views')["custom-facet"] : $.i18n._('core-views')["custom-numeric-label"]) +" "+ column.name, 
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
      label: $.i18n._('core-views')["text-facet"],
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
      label:  $.i18n._('core-views')["numeric-facet"],
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
      label: $.i18n._('core-views')["timeline-facet"],
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
      label: $.i18n._('core-views')["scatterplot-facet"],
      click: function() {
        new ScatterplotDialog(column.name);
      }
    },
    {},
    {
      id: "core/custom-text-facet",
      label: $.i18n._('core-views')["custom-text-facet"]+'...',
      click: function() {
        doFilterByExpressionPrompt(null, "list");
      }
    },
    {
      id: "core/custom-numeric-facet",
      label: $.i18n._('core-views')["custom-numeric"]+'...',
      click: function() {
        doFilterByExpressionPrompt(null, "range");
      }
    },
    {
      id: "core/customized-facets",
      label: $.i18n._('core-views')["custom-facets"],
      submenu: [
        {
          id: "core/word-facet",
          label: $.i18n._('core-views')["word-facet"],
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
          id: "core/duplicates-facet",
          label: $.i18n._('core-views')["duplicates-facet"],
          click: function() {
            ui.browsingEngine.addFacet(
                "list",
                {
                  "name": column.name,
                  "columnName": column.name,
                  "expression": "facetCount(value, 'value', '" +
                  column.name + "') > 1"
                }
            );
          }
        },
        {},
        {
          id: "core/numeric-log-facet",
          label: $.i18n._('core-views')["numeric-log-facet"],
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
          label: $.i18n._('core-views')["bounded-log-facet"],
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
          label: $.i18n._('core-views')["text-length-facet"],
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
          label: $.i18n._('core-views')["log-length-facet"],
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
          label: $.i18n._('core-views')["unicode-facet"],
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
          label: $.i18n._('core-views')["facet-error"],
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
          label: $.i18n._('core-views')["facet-blank"],
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
      label: $.i18n._('core-views')["text-filter"],
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