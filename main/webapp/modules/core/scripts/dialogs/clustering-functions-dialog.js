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
function ClusteringFunctionsDialog(title, clusteringDialog) {
    var self = this;
    self._column = clusteringDialog._column;
    self._columnName = clusteringDialog._columnName;
    
    var frame = DialogSystem.createDialog();
    frame.css("width", "700px");
    var header = $('<div></div>').addClass("dialog-header").text(title).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").css('justify-content', 'space-between').appendTo(frame);

    var html = $(DOM.loadHTML("core", "scripts/dialogs/clustering-functions-dialog.html")).appendTo(body);

    this._elmts = DOM.bind(html);
    this._elmts.or_dialog_descr.html($.i18n('core-dialogs/custom-cluster-descr'));
    this._elmts.or_dialog_findMore.html($.i18n('core-dialogs/find-more'));

    $('<button class="button" id="add-new-functions"></button>').text($.i18n("core-buttons/add-keying-function")).on('click', function () {
        self._addFunction(self._column);
    }).appendTo(footer);

    $('<button class="button"></button>').html($.i18n('core-buttons/ok')).on('click', function () {
        DialogSystem.dismissUntil(self._level - 1);
        clusteringDialog._renderClusteringFunctions();
    }).appendTo(footer);

    this._level = DialogSystem.showDialog(frame);
    this._renderTable();
}

ClusteringFunctionsDialog.prototype._renderTable = function () {
    var self = this;

    $("#clustering-functions-tabs").tabs({
        activate: function (event, ui) {
            if (ui.newTab.text() == $.i18n('core-dialogs/keying-functions')) {
                $("#add-new-functions").text($.i18n('core-buttons/add-keying-function'));
            } else {
                $("#add-new-functions").text($.i18n('core-buttons/add-distance-function'));
            }
        }
    });

    this._elmts.or_dialog_keying.html($.i18n('core-dialogs/keying-functions'));
    this._elmts.or_dialog_distance.html($.i18n('core-dialogs/distance-functions'));

    var renderKeyingFunctions = function () {
        $.ajax({
            url: "command/core/get-preference?" + $.param({
                name: "ui.clustering.customKeyingFunctions"
            }),
            success: function (data) {
                var functions = data.value == null ? [] : JSON.parse(data.value);
                self._renderFunctions("Keying", functions);
            },
            dataType: "json",
        });
    }

    var renderDistanceFunctions = function () {
        $.ajax({
            url: "command/core/get-preference?" + $.param({
                name: "ui.clustering.customDistanceFunctions"
            }),
            success: function (data) {
                var functions = data.value == null ? [] : JSON.parse(data.value);
                self._renderFunctions("Distance", functions);
            },
            dataType: "json",
        });
    }

    renderKeyingFunctions();
    renderDistanceFunctions();
};

ClusteringFunctionsDialog.prototype._renderFunctions = function (functionsType, functions) {
    var self = this;
    var container;
    if (functionsType == "Keying") {
        container = this._elmts.keyingFunctionsContainer.empty();
    } else {
        container = this._elmts.distanceFunctionsContainer.empty();
    }

    if (functions.length > 0) {
        var table = $('<table></table>')
            .addClass("clustering-functions-table")
            .appendTo($('<div>').addClass("clustering-functions-table-wrapper").appendTo(container))[0];

        var tr = table.insertRow(0);
        $(tr.insertCell(0)).attr("id", "clustering-functions-heading").text($.i18n('core-dialogs/name'));
        $(tr.insertCell(1)).attr("id", "clustering-functions-heading").text($.i18n('core-dialogs/action'));

        for (var i = 0; i < functions.length; i++) {
            var newRow = $('<tr>');

            var td = $("<td></td>").css({
                "border-right": "none"
            });
            var name = $("<div></div>").addClass("main-text").text(functions[i].name);
            var expression = $("<div></div>").addClass("sub-text").text(functions[i].expression);
            td.append(name).append(expression).appendTo(newRow);

            var actionsCell = $('<td>').css({
                "border-left": "none"
            }).appendTo(newRow);

            (function (index) {
                $('<button>').text($.i18n('core-buttons/edit')).addClass("button")
                    .on('click', function () {
                        self._editFunction(self._column, functionsType, index);
                    }).appendTo(actionsCell);

                $('<button>').text($.i18n('core-buttons/remove')).addClass("button")
                    .on('click', function () {
                        self._deleteFunction(index);
                    })
                    .appendTo(actionsCell);
            })(i);

            newRow.appendTo(table);
        }
    } else {
        container.html(
            '<div style="margin: 1em;"><div style="font-size: 130%; color: #333;">' + $.i18n('core-index/no-custom-functions') + '</div><div style="padding-top: 5px; font-size: 110%; color: #888;">' + $.i18n('core-index/add-custom-function') + '</div></div>'
        );
    }
}

ClusteringFunctionsDialog.prototype._addFunction = function (column) {
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
    var dismiss = function () { DialogSystem.dismissUntil(level - 1); };

    var activeTabName = $("#clustering-functions-tabs").find(".ui-tabs-active a").text().split(' ')[0];

    var o = DataTableView.sampleVisibleRows(column);
    var previewWidget = new ExpressionPreviewDialog.Widget(
        elmts,
        column.cellIndex,
        o.rowIndices,
        o.values,
        activeTabName === "Distance" ? "grel:levenshteinDistance(value1, value2)" : null,
        self._columnName
    );

    elmts.cancelButton.on('click', dismiss);
    elmts.form.on('submit', function (event) {
        event.preventDefault();
        var functionName = jQueryTrim(elmts.functionNameInput[0].value);
        if (!functionName.length) {
            alert($.i18n('core-views/warning-function-name'));
            return;
        }

        var add = function () {
            $.ajax({
                url: "command/core/get-preference?" + $.param({
                    name: "ui.clustering.custom" + activeTabName + "Functions"
                }),
                success: function (data1) {
                    var langAndExpr = previewWidget.getExpression();
                    var colonIndex = langAndExpr.indexOf(':');
                    var lang = langAndExpr.substring(0, colonIndex);
                    var fullExpr = langAndExpr.substring(colonIndex + 1);

                    var _functions = data1.value == null ? [] : JSON.parse(data1.value);
                    _functions.push({
                        name: functionName,
                        expressionLang: lang,
                        expression: fullExpr
                    });

                    Refine.wrapCSRF(function (token) {
                        $.ajax({
                            type: "POST",
                            url: "command/core/set-preference?" + $.param({
                                name: "ui.clustering.custom" + activeTabName + "Functions"
                            }),
                            data: {
                                "value": JSON.stringify(_functions),
                                csrf_token: token
                            },
                            success: function (data) {
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

ClusteringFunctionsDialog.prototype._editFunction = function (column, functionsType, index) {
    var self = this;
    var frame = $(
        DOM.loadHTML("core", "scripts/dialogs/add-function-dialog.html")
            .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));

    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n('core-dialogs/edit-function'));

    elmts.newFunctionName.text($.i18n('core-dialogs/edit-name'));
    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.text($.i18n('core-buttons/cancel'));

    var level = DialogSystem.showDialog(frame);
    var dismiss = function () { DialogSystem.dismissUntil(level - 1); };

    var previewWidget;
    $.ajax({
        url: "command/core/get-preference?" + $.param({
            name: "ui.clustering.custom" + functionsType + "Functions"
        }),
        success: function (data) {
            var _functions = data.value == null ? [] : JSON.parse(data.value);
            elmts.functionNameInput[0].value = _functions[index].name;

            var o = DataTableView.sampleVisibleRows(column);
            previewWidget = new ExpressionPreviewDialog.Widget(
                elmts,
                column.cellIndex,
                o.rowIndices,
                o.values,
                _functions[index].expressionLang + ':' + _functions[index].expression,
                self._columnName
            );
        },
        dataType: "json",
    });

    elmts.cancelButton.on('click', dismiss);
    elmts.form.on('submit', function (event) {
        event.preventDefault();
        var functionName = jQueryTrim(elmts.functionNameInput[0].value);
        if (!functionName.length) {
            alert($.i18n('core-views/warning-function-name'));
            return;
        }

        var activeTabName = $("#clustering-functions-tabs").find(".ui-tabs-active a").text().split(' ')[0];
        var edit = function () {
            $.ajax({
                url: "command/core/get-preference?" + $.param({
                    name: "ui.clustering.custom" + activeTabName + "Functions"
                }),
                success: function (data1) {
                    var langAndExpr = previewWidget.getExpression();
                    var colonIndex = langAndExpr.indexOf(':');
                    var lang = langAndExpr.substring(0, colonIndex);
                    var fullExpr = langAndExpr.substring(colonIndex + 1);

                    var _functions = data1.value == null ? [] : JSON.parse(data1.value);
                    _functions[index].name = functionName;
                    _functions[index].expressionLang = lang;
                    _functions[index].expression = fullExpr;

                    Refine.wrapCSRF(function (token) {
                        $.ajax({
                            type: "POST",
                            url: "command/core/set-preference?" + $.param({
                                name: "ui.clustering.custom" + activeTabName + "Functions"
                            }),
                            data: {
                                "value": JSON.stringify(_functions),
                                csrf_token: token
                            },
                            success: function (data) {
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

ClusteringFunctionsDialog.prototype._deleteFunction = function (index) {
    var self = this;
    var result = confirm($.i18n('core-views/warning-delete-functions'));
    if (result) {
        var activeTabName = $("#clustering-functions-tabs").find(".ui-tabs-active a").text().split(' ')[0];
        var remove = function () {
            $.ajax({
                url: "command/core/get-preference?" + $.param({
                    name: "ui.clustering.custom" + activeTabName + "Functions"
                }),
                success: function (data1) {
                    var _functions = data1.value == null ? [] : JSON.parse(data1.value);
                    _functions.splice(index, 1);
                    Refine.wrapCSRF(function (token) {
                        $.ajax({
                            type: "POST",
                            url: "command/core/set-preference?" + $.param({
                                name: "ui.clustering.custom" + activeTabName + "Functions"
                            }),
                            data: {
                                "value": JSON.stringify(_functions),
                                csrf_token: token
                            },
                            success: function (data) {
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
