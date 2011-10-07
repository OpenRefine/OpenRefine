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
        { modelsChanged: true },
        {
          onDone: function(o) {
            dismiss();
          }
        }
      );
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

          var lengths = [];
          $.each(a, function(i,n) { 
            if (typeof n == "number") {
              lengths.push(n); 
            }
          });

          if (lengths.length === 0) {
            alert("No field length is specified.");
            return;
          }

          config.fieldLengths = JSON.stringify(lengths);
          
        } catch (e) {
          alert("The given field lengths are not properly formatted.");
          return;
        }
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
        id: "core/split-column",
        label: "Split into several columns...",
        click: doSplitColumn
      },
      {},
      {
        id: "core/add-column",
        label: "Add column based on this column...",
        click: doAddColumn
      },
      {
        id: "core/add-column-by-fetching-urls",
        label: "Add column by fetching URLs...",
        click: doAddColumnByFetchingURLs
      },
      {},
      {
        id: "core/rename-column",
        label: "Rename this column",
        click: doRenameColumn
      },
      {
        id: "core/remove-column",
        label: "Remove this column",
        click: doRemoveColumn
      },
      {},
      {
        id: "core/move-column-to-beginning",
        label: "Move column to beginning",
        click: function() { doMoveColumnTo(0); }
      },
      {
        id: "core/move-column-to-end",
        label: "Move column to end",
        click: function() { doMoveColumnTo(theProject.columnModel.columns.length - 1); }
      },
      {
        id: "core/move-column-to-left",
        label: "Move column left",
        click: function() { doMoveColumnBy(-1); }
      },
      {
        id: "core/move-column-to-right",
        label: "Move column right",
        click: function() { doMoveColumnBy(1); }
      }
    ]
  );
});
