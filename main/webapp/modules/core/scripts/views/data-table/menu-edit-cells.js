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
    var doTextTransform = function(expression, onError, repeat, repeatCount) {
        Refine.postCoreProcess(
            "text-transform",
            {
                columnName: column.name, 
                expression: expression, 
                onError: onError,
                repeat: repeat,
                repeatCount: repeatCount
            },
            null,
            { cellsChanged: true }
        );
    };
    
    var doTextTransformPrompt = function() {
        var frame = $(
            DOM.loadHTML("core", "scripts/views/data-table/text-transform-dialog.html")
                .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));

        var elmts = DOM.bind(frame);
        elmts.dialogHeader.text("Custom text transform on column " + column.name);

        var level = DialogSystem.showDialog(frame);
        var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

        elmts.cancelButton.click(dismiss);
        elmts.okButton.click(function() {
            doTextTransform(
                previewWidget.getExpression(true),
                $('input[name="text-transform-dialog-onerror-choice"]:checked')[0].value,
                elmts.repeatCheckbox[0].checked,
                elmts.repeatCountInput[0].value
            );
            dismiss();
        });

        var o = DataTableView.sampleVisibleRows(column);
        var previewWidget = new ExpressionPreviewDialog.Widget(
            elmts,
            column.cellIndex,
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
    
    var doFillDown = function() {
        Refine.postCoreProcess(
            "fill-down", 
            {
                columnName: column.name
            },
            null,
            { modelsChanged: true }
        );
    };

    var doBlankDown = function() {
        Refine.postCoreProcess(
            "blank-down", 
            {
                columnName: column.name
            },
            null,
            { modelsChanged: true }
        );
    };

   var doJoinMultiValueCells = function() {
        var separator = window.prompt("Enter separator to use between values", ", ");
        if (separator !== null) {
            Refine.postCoreProcess(
                "join-multi-value-cells", 
                {
                    columnName: column.name,
                    keyColumnName: theProject.columnModel.keyColumnName,
                    separator: separator
                },
                null,
                { rowsChanged: true }
            );
        }
    };

    var doSplitMultiValueCells = function() {
        var separator = window.prompt("What separator currently separates the values?", ",");
        if (separator !== null) {
            Refine.postCoreProcess(
                "split-multi-value-cells", 
                {
                    columnName: column.name,
                    keyColumnName: theProject.columnModel.keyColumnName,
                    separator: separator,
                    mode: "plain"
                },
                null,
                { rowsChanged: true }
            );
        }
    };
    
    MenuSystem.appendTo(menu, [ "core/edit-cells" ], [
        {
            id: "core/text-transform",
            label: "Transform...",
            click: function() { doTextTransformPrompt(); }
        },
        {
            id: "core/common-transforms",
            label: "Common transforms",
            submenu: [
                {
                    id: "core/trim-whitespace",
                    label: "Trim leading and trailing whitespace",
                    click: function() { doTextTransform("value.trim()", "store-blank", false, ""); }
                },
                {
                    id: "core/collapse-whitespace",
                    label: "Collapse consecutive whitespace",
                    click: function() { doTextTransform("value.replace(/\\s+/,' ')", "store-blank", false, ""); }
                },
                {},
                {
                    id: "core/unescape-html-entities",
                    label: "Unescape HTML entities",
                    click: function() { doTextTransform("value.unescape('html')", "store-blank", true, 10); }
                },
                {},
                {
                    id: "core/to-titlecase",
                    label: "To titlecase",
                    click: function() { doTextTransform("value.toTitlecase()", "store-blank", false, ""); }
                },
                {
                    id: "core/to-uppercase",
                    label: "To uppercase",
                    click: function() { doTextTransform("value.toUppercase()", "store-blank", false, ""); }
                },
                {
                    id: "core/to-lowercase",
                    label: "To lowercase",
                    click: function() { doTextTransform("value.toLowercase()", "store-blank", false, ""); }
                },
                {},
                {
                    id: "core/to-number",
                    label: "To number",
                    click: function() { doTextTransform("value.toNumber()", "store-blank", false, ""); }
                },
                {
                    id: "core/to-date",
                    label: "To date",
                    click: function() { doTextTransform("value.toDate()", "store-blank", false, ""); }
                },
                {
                    id: "core/to-text",
                    label: "To text",
                    click: function() { doTextTransform("value.toString()", "store-blank", false, ""); }
                },
                {},
                {
                    id: "core/to-blank",
                    label: "Blank out cells",
                    click: function() { doTextTransform("null", "store-blank", false, ""); }
                }
            ]
        },
        {},
        {
            id: "core/fill-down",
            label: "Fill down",
            click: doFillDown
        },
        {
            id: "core/blank-down",
            label: "Blank down",
            click: doBlankDown
        },
        {},
        {
            id: "core/split-multi-valued-cells",
            label: "Split multi-valued cells...",
            click: doSplitMultiValueCells
        },
        {
            id: "core/join-multi-valued-cells",
            label: "Join multi-valued cells...",
            click: doJoinMultiValueCells
        },
        {},
        {
            id: "core/cluster",
            label: "Cluster and edit...",
            click: function() { new ClusteringDialog(column.name, "value"); }
        }
    ]);
    
    var doTransposeColumnsIntoRows = function() {
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

            Refine.postCoreProcess(
                "transpose-columns-into-rows", 
                config,
                null,
                { modelsChanged: true }
            );
            dismiss();
        });

        for (var i = 0; i < columns.length; i++) {
            var column2 = columns[i];
            var option = $('<option>').attr("value", column2.name).text(column2.name).appendTo(elmts.fromColumnSelect);
            if (column2.name == column.name) {
                option.attr("selected", "true");
            }
        }

        var populateToColumn = function() {
            elmts.toColumnSelect.empty();

            var toColumnName = elmts.fromColumnSelect[0].value;

            var j = 0;
            for (; j < columns.length; j++) {
                var column2 = columns[j];
                if (column2.name == toColumnName) {
                    break;
                }
            }

            for (var k = j + 1; k < columns.length; k++) {
                var column2 = columns[k];
                var option = $('<option>').attr("value", k - j + 1).text(column2.name).appendTo(elmts.toColumnSelect);
                if (k == columns.length - 1) {
                    option.attr("selected", "true");
                }
            }
        };
        populateToColumn();

        elmts.fromColumnSelect.bind("change", populateToColumn);
    };

    var doTransposeRowsIntoColumns = function() {
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
                    columnName: column.name,
                    rowCount: rowCount
                };

                Refine.postCoreProcess(
                    "transpose-rows-into-columns", 
                    config,
                    null,
                    { modelsChanged: true }
                );
            }
        }
    };
    
    MenuSystem.appendTo(menu, [ "core/transpose" ], [
        {
            id: "core/transpose-columns-into-rows",
            label: "Cells across columns into rows...",
            click: doTransposeColumnsIntoRows
        },
        {
            id: "core/transpose-rows-into-columns",
            label: "Cells in rows into columns...",
            click: doTransposeRowsIntoColumns
        }
    ]);
});