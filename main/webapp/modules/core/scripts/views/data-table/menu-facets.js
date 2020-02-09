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
      (type == "list" ? $.i18n('core-views/custom-facet') : $.i18n('core-views/custom-numeric-label')) +" "+ column.name, 
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
      label: $.i18n('core-views/text-facet'),
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
    }
  ]);
});
