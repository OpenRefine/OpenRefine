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
function ManagementDialog(title, clusteringDialog) {
    var self = this;
    self._column = clusteringDialog._column;
    var frame = DialogSystem.createDialog();
    frame.css("min-width", "700px");
    var header = $('<div></div>').addClass("dialog-header").text(title).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").css('justify-content', 'space-between').appendTo(frame);

    var html = $(DOM.loadHTML("core", "scripts/dialogs/management-dialog.html")).appendTo(body);

    this._elmts = DOM.bind(html);

    $('<button class="button" id="add-new-functions"></button>').text("Add new keying function").on('click', function() {
        self._addFunction(self._column);
    }).appendTo(footer);

    $('<button class="button"></button>').html($.i18n('core-buttons/ok')).on('click', function() {
        // TO ITERATE ON NEXT WEEK
        // var functions = Refine.getPreference("functions",[]); // to edit
        // for(var i = 0; i < functions.length; i++){
        //     $('<option></option>')
        //      .val(functions[i].name)
        //      .text(functions[i].name)
        //      .appendTo(clusteringDialog._elmts.keyingFunctionSelector);
        // }ui.newTab.text().trim();
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);

    this._level = DialogSystem.showDialog(frame);
    this._renderTable();
}

ManagementDialog.prototype._renderTable = function() {
    var self = this;
    
    $("#management-tabs").tabs({
        activate: function(event, ui) {
            if(ui.newTab.text() == "Keying functions"){
                $("#add-new-functions").text("Add new keying function");
            } else {
                $("#add-new-functions").text("Add new distance function");
            }
        }
    });

    this._elmts.or_dialog_keying.html("Keying functions");
    this._elmts.or_dialog_distance.html("Distance functions");
    
    var renderKeyingFunctions = function() {
        $.ajax({
            url: "command/core/get-preference?" + $.param({
              name: "keying functions"
            }),
            success: function(data) {
                functions = JSON.parse(data.value);
                self._renderFunctions("keying", functions);
            },
            dataType: "json",
        });
    }

    var renderDistanceFunctions = function() {
        $.ajax({
            url: "command/core/get-preference?" + $.param({
              name: "distance functions"
            }),
            success: function(data) {
                functions = JSON.parse(data.value);
                self._renderFunctions("distance", functions);
            },
            dataType: "json",
        });
    }

    renderKeyingFunctions();
    renderDistanceFunctions();
};

ManagementDialog.prototype._renderFunctions = function(functionsType, functions) {
    var self = this;
    var container;
    if(functionsType == "keying"){
        container = this._elmts.keyingFunctionsContainer.empty();
    } else {
        container = this._elmts.distanceFunctionsContainer.empty();
    }

    if(functions.length > 0){
        var table = $('<table></table>')
        .addClass("manage-functions-table")
        .appendTo($('<div>').addClass("management-table-wrapper").appendTo(container))[0];

        var tr = table.insertRow(0);
        $(tr.insertCell(0)).attr("id", "manage-functions-heading").text("Name");
        $(tr.insertCell(1)).attr("id", "manage-functions-heading").text("Action");
        
        for (var i = 0; i < functions.length; i++) {
            var newRow = $('<tr>');

            var td = $("<td></td>").css({
                "border-right" : "none"
            });
            var name = $("<div></div>").addClass("main-text").text(functions[i].name);
            var expression = $("<div></div>").addClass("sub-text").text(functions[i].expression);
            td.append(name).append(expression).appendTo(newRow);
            
            var actionsCell = $('<td>').css({
                "border-left" : "none"
            }).appendTo(newRow);
    
            (function (index){
                $('<button>').text("Edit").addClass("button")
                .on('click', function (){
                    self._editFunction(self._column, functionsType, index);
                }).appendTo(actionsCell);
    
                $('<button>').text("Remove").addClass("button")
                .on('click', function (){
                    self._deleteFunction(index);
                })
                .appendTo(actionsCell);
            })(i);
            
            newRow.appendTo(table);
        }
    } else {
        container.html(
            '<div style="margin: 1em;"><div style="font-size: 130%; color: #333;">'+"No functions were found currently"+'</div><div style="padding-top: 5px; font-size: 110%; color: #888;">'+"Try adding a new functions below"+'</div></div>'
        );
    }
}

ManagementDialog.prototype._addFunction = function(column) {
    var self = this;
    var frame = $(
        DOM.loadHTML("core", "scripts/dialogs/add-function-dialog.html")
        .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));

    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n('core-dialogs/add-function'));

    elmts.newFunctionName.text($.i18n('core-dialogs/new-function-name'));
    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.text($.i18n('core-buttons/cancel'));

    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

    var o = DataTableView.sampleVisibleRows(column);
    var previewWidget = new ExpressionPreviewDialog.Widget(
      elmts, 
      column.cellIndex,
      o.rowIndices,
      o.values,
      null
    );
    
    elmts.cancelButton.on('click',dismiss);
    elmts.form.on('submit',function(event) {
        event.preventDefault();
        var columnName = jQueryTrim(elmts.functionNameInput[0].value);
        if (!columnName.length) {
          alert($.i18n('core-views/warning-function-name'));
          return;
        }

        var activeTabName = $("#management-tabs").find(".ui-tabs-active a").text();
        var add = function() {
            $.ajax({
                url: "command/core/get-preference?" + $.param({
                  name: activeTabName.toLowerCase()
                }),
                success: function(data1) {
                    var _functions = JSON.parse(data1.value);
                    var langAndExpr = previewWidget.getExpression().split(':');
                    _functions.push({
                        name:  columnName,
                        expressionLang: langAndExpr[0],
                        expression: langAndExpr[1]
                    });
            
                    Refine.wrapCSRF(function(token) {
                        $.ajax({
                          type: "POST",
                          url: "command/core/set-preference?" + $.param({ name: activeTabName.toLowerCase() }),
                          data: {
                            "value" : JSON.stringify(_functions),
                            csrf_token: token
                          },
                          success: function(data) { 
                             self._renderTable();
                          },
                          dataType: "json"
                        });
                    });
                },
                dataType: "json",
            });
        }

        add();
        dismiss();
    });
};

ManagementDialog.prototype._editFunction = function(column, functionsType, index) {
    var self = this;
    var frame = $(
        DOM.loadHTML("core", "scripts/dialogs/add-function-dialog.html")
        .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));

    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text("Edit function name and expression");

    elmts.newFunctionName.text("Function name");
    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.text($.i18n('core-buttons/cancel'));

    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

    var previewWidget;
    $.ajax({
        url: "command/core/get-preference?" + $.param({
          name: functionsType + " functions"
        }),
        success: function(data) {
            var _functions = JSON.parse(data.value);
            elmts.functionNameInput[0].value = _functions[index].name;

            var o = DataTableView.sampleVisibleRows(column);
            previewWidget = new ExpressionPreviewDialog.Widget(
                elmts, 
                column.cellIndex,
                o.rowIndices,
                o.values,
                _functions[index].expressionLang + ':' + _functions[index].expression
            );
        },
        dataType: "json",
    });

    elmts.cancelButton.on('click',dismiss);
    elmts.form.on('submit',function(event) {
        event.preventDefault();
        var columnName = jQueryTrim(elmts.functionNameInput[0].value);
        if (!columnName.length) {
          alert($.i18n('core-views/warning-function-name'));
          return;
        }

        var activeTabName = $("#management-tabs").find(".ui-tabs-active a").text();
        var edit = function() {
            $.ajax({
                url: "command/core/get-preference?" + $.param({
                  name: activeTabName.toLowerCase()
                }),
                success: function(data1) {
                    var _functions = JSON.parse(data1.value);
                    var langAndExpr = previewWidget.getExpression().split(':');
                    _functions[index].name = columnName;
                    _functions[index].expressionLang = langAndExpr[0];
                    _functions[index].expression = langAndExpr[1];
            
                    Refine.wrapCSRF(function(token) {
                        $.ajax({
                          type: "POST",
                          url: "command/core/set-preference?" + $.param({ name: activeTabName.toLowerCase() }),
                          data: {
                            "value" : JSON.stringify(_functions),
                            csrf_token: token
                          },
                          success: function(data) { 
                             self._renderTable();
                          },
                          dataType: "json"
                        });
                    });
                },
                dataType: "json",
            });
        }

        edit();
        dismiss();
    });
};

ManagementDialog.prototype._deleteFunction = function(index) {
    var self = this;
    var result = confirm("Are you sure you want to delete this function?");
    if (result) {
        var activeTabName = $("#management-tabs").find(".ui-tabs-active a").text();
        var remove = function() {
            $.ajax({
                url: "command/core/get-preference?" + $.param({
                  name: activeTabName.toLowerCase()
                }),
                success: function(data1) {
                    var _functions = JSON.parse(data1.value);
                    _functions.splice(index, 1);
                    Refine.wrapCSRF(function(token) {
                        $.ajax({
                          type: "POST",
                          url: "command/core/set-preference?" + $.param({ name: activeTabName.toLowerCase() }),
                          data: {
                            "value" : JSON.stringify(_functions),
                            csrf_token: token
                          },
                          success: function(data) { 
                             self._renderTable();
                          },
                          dataType: "json"
                        });
                    });
                },
                dataType: "json",
            });
        }
        remove();
    }
 };
