DataTableColumnHeaderUI.extendMenu(function(column, columnHeaderUI, menu) {
    var columnIndex = Refine.columnNameToColumnIndex(column.name);
    var doAddColumn = function() {
        var frame = $(
            DOM.loadHTML("core", "scripts/views/data-table/add-column-dialog.html")
                .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));

        var elmts = DOM.bind(frame);
        elmts.dialogHeader.text("Add column based on column " + column.name);

        var level = DialogSystem.showDialog(frame);
        var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

        elmts.cancelButton.click(dismiss);
        elmts.okButton.click(function() {
            var columnName = $.trim(elmts.columnNameInput[0].value);
            if (!columnName.length) {
                alert("You must enter a column name.");
                return;
            }

            Refine.postCoreProcess(
                "add-column", 
                {
                    baseColumnName: column.name, 
                    expression: previewWidget.getExpression(true), 
                    newColumnName: columnName, 
                    columnInsertIndex: columnIndex + 1,
                    onError: $('input[name="create-column-dialog-onerror-choice"]:checked')[0].value
                },
                null,
                { modelsChanged: true }
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
    };

    var doAddColumnByFetchingURLs = function() {
        var frame = $(
            DOM.loadHTML("core", "scripts/views/data-table/add-column-by-fetching-urls-dialog.html")
                .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));

        var elmts = DOM.bind(frame);
        elmts.dialogHeader.text("Add column by fetching URLs based on column " + column.name);

        var level = DialogSystem.showDialog(frame);
        var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

        elmts.cancelButton.click(dismiss);
        elmts.okButton.click(function() {
            var columnName = $.trim(elmts.columnNameInput[0].value);
            if (!columnName.length) {
                alert("You must enter a column name.");
                return;
            }

            Refine.postCoreProcess(
                "add-column-by-fetching-urls", 
                {
                    baseColumnName: column.name, 
                    urlExpression: previewWidget.getExpression(true), 
                    newColumnName: columnName, 
                    columnInsertIndex: columnIndex + 1,
                    delay: elmts.throttleDelayInput[0].value,
                    onError: $('input[name="dialog-onerror-choice"]:checked')[0].value
                },
                null,
                { modelsChanged: true }
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
    };
    
    var doAddColumnFromFreebase = function() {
        var o = DataTableView.sampleVisibleRows(column);
        new ExtendDataPreviewDialog(
            column, 
            columnIndex, 
            o.rowIndices, 
            function(extension) {
                Refine.postCoreProcess(
                    "extend-data", 
                    {
                        baseColumnName: column.name,
                        columnInsertIndex: columnIndex + 1
                    },
                    {
                        extension: JSON.stringify(extension)
                    },
                    { rowsChanged: true, modelsChanged: true }
                );
            }
        );
    };

    var doRemoveColumn = function() {
        Refine.postCoreProcess(
            "remove-column", 
            {
                columnName: column.name
            },
            null,
            { modelsChanged: true }
        );
    };

    var doRenameColumn = function() {
        var newColumnName = window.prompt("Enter new column name", column.name);
        if (newColumnName !== null) {
            Refine.postCoreProcess(
                "rename-column", 
                {
                    oldColumnName: column.name,
                    newColumnName: newColumnName
                },
                null,
                { modelsChanged: true }
            );
        }
    };

    var doMoveColumnTo = function(index) {
        Refine.postCoreProcess(
            "move-column", 
            {
                columnName: column.name,
                index: index
            },
            null,
            { modelsChanged: true }
        );
    };

    var doMoveColumnBy = function(change) {
        Refine.postCoreProcess(
            "move-column", 
            {
                columnName: column.name,
                index: Refine.columnNameToColumnIndex(column.name) + change
            },
            null,
            { modelsChanged: true }
        );
    };

    var doSplitColumn = function() {
        var frame = $(DOM.loadHTML("core", "scripts/views/data-table/split-column-dialog.html"));
        var elmts = DOM.bind(frame);
        elmts.dialogHeader.text("Split column " + column.name + " into several columns");

        var level = DialogSystem.showDialog(frame);
        var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

        elmts.cancelButton.click(dismiss);
        elmts.okButton.click(function() {
            var mode = $("input[name='split-by-mode']:checked")[0].value;
            var config = {
                columnName: column.name,
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

            Refine.postCoreProcess(
                "split-column", 
                config,
                null,
                { modelsChanged: true }
            );
            dismiss();
        });
    };
    
    MenuSystem.appendTo(menu, [ "core/edit-column" ], [
        {
            label: "Split into Several Columns ...",
            click: doSplitColumn
        },
        {},
        {
            label: "Add Column Based on This Column ...",
            click: doAddColumn
        },
        {
            label: "Add Columns From Freebase ...",
            click: doAddColumnFromFreebase
        },
        {
            label: "Add Column By Fetching URLs ...",
            click: doAddColumnByFetchingURLs
        },
        {},
        {
            label: "Rename This Column",
            click: doRenameColumn
        },
        {
            label: "Remove This Column",
            click: doRemoveColumn
        },
        {},
        {
            label: "Move Column to Beginning",
            click: function() { doMoveColumnTo(0); }
        },
        {
            label: "Move Column to End",
            click: function() { doMoveColumnTo(theProject.columnModel.columns.length - 1); }
        },
        {
            label: "Move Column Left",
            click: function() { doMoveColumnBy(-1); }
        },
        {
            label: "Move Column Right",
            click: function() { doMoveColumnBy(1); }
        }
    ]);
});
