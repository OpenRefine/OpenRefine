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
    elmts.dialogHeader.text($.i18n._('core-views')["add-col-col"]+" " + column.name);
    
    elmts.or_views_newCol.text($.i18n._('core-views')["new-col-name"]);
    elmts.or_views_onErr.text($.i18n._('core-views')["addasdasd"]);
    elmts.or_views_setBlank.text($.i18n._('core-views')["set-blank"]);
    elmts.or_views_storeErr.text($.i18n._('core-views')["store-err"]);
    elmts.or_views_copyVal.text($.i18n._('core-views')["copy-val"]);
    elmts.okButton.html($.i18n._('core-buttons')["ok"]);
    elmts.cancelButton.text($.i18n._('core-buttons')["cancel"]);

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
    
    elmts.cancelButton.click(dismiss);
    elmts.okButton.click(function() {
      var columnName = $.trim(elmts.columnNameInput[0].value);
      if (!columnName.length) {
        alert($.i18n._('core-views')["warning-col-name"]);
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
  };

  var doAddColumnByFetchingURLs = function() {
    var frame = $(
        DOM.loadHTML("core", "scripts/views/data-table/add-column-by-fetching-urls-dialog.html")
        .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));

    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n._('core-views')["add-col-fetch"]+" " + column.name);
    
    elmts.or_views_newCol.text($.i18n._('core-views')["new-col-name"]);
    elmts.or_views_throttle.text($.i18n._('core-views')["throttle-delay"]);
    elmts.or_views_milli.text($.i18n._('core-views')["milli"]);
    elmts.or_views_onErr.text($.i18n._('core-views')["on-error"]);
    elmts.or_views_setBlank.text($.i18n._('core-views')["set-blank"]);
    elmts.or_views_storeErr.text($.i18n._('core-views')["store-err"]);
    elmts.or_views_cacheResponses.text($.i18n._('core-views')["cache-responses"]);
    elmts.or_views_urlFetch.text($.i18n._('core-views')["url-fetch"]);
    elmts.okButton.html($.i18n._('core-buttons')["ok"]);
    elmts.cancelButton.text($.i18n._('core-buttons')["cancel"]);

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
    
    elmts.cancelButton.click(dismiss);
    elmts.okButton.click(function() {
      var columnName = $.trim(elmts.columnNameInput[0].value);
      if (!columnName.length) {
        alert($.i18n._('core-views')["warning-col-name"]);
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
          onError: $('input[name="dialog-onerror-choice"]:checked')[0].value,
          cacheResponses: $('input[name="dialog-cache-responses"]')[0].checked,
        },
        null,
        { modelsChanged: true }
      );
      dismiss();
    });
  };

  var doAddColumnByReconciliation = function() {
    var columnIndex = Refine.columnNameToColumnIndex(column.name);
    var o = DataTableView.sampleVisibleRows(column);
    new ExtendReconciledDataPreviewDialog(
      column, 
      columnIndex, 
      o.rowIndices,
      function(extension, endpoint, identifierSpace, schemaSpace) {
        Refine.postProcess(
            "core",
            "extend-data", 
            {
              baseColumnName: column.name,
	      endpoint: endpoint,
              identifierSpace: identifierSpace,
              schemaSpace: schemaSpace,
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
    var newColumnName = window.prompt($.i18n._('core-views')["enter-col-name"], column.name);
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
    var newidx = Refine.columnNameToColumnIndex(column.name) + change;
    if (newidx >= 0) {
      Refine.postCoreProcess(
          "move-column", 
          {
            columnName: column.name,
            index: newidx
          },
          null,
          { modelsChanged: true }
      );
    }
  };

  var doSplitColumn = function() {
    var frame = $(DOM.loadHTML("core", "scripts/views/data-table/split-column-dialog.html"));
    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n._('core-views')["split-col"]+" " + column.name + " "+$.i18n._('core-views')["several-col"]);
    
    elmts.or_views_howSplit.text($.i18n._('core-views')["how-split"]);
    elmts.or_views_bySep.text($.i18n._('core-views')["by-sep"]);
    elmts.or_views_separator.text($.i18n._('core-views')["separator"]);
    elmts.or_views_regExp.text($.i18n._('core-views')["reg-exp"]);
    elmts.or_views_splitInto.text($.i18n._('core-views')["split-into"]);
    elmts.or_views_colMost.text($.i18n._('core-views')["col-at-most"]);
    elmts.or_views_fieldLen.text($.i18n._('core-views')["field-len"]);
    elmts.or_views_listInt.text($.i18n._('core-views')["list-int"]);
    elmts.or_views_afterSplit.text($.i18n._('core-views')["after-split"]);
    elmts.or_views_guessType.text($.i18n._('core-views')["guess-cell"]);
    elmts.or_views_removeCol.text($.i18n._('core-views')["remove-col"]);
    elmts.okButton.html($.i18n._('core-buttons')["ok"]);
    elmts.cancelButton.text($.i18n._('core-buttons')["cancel"]);

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
          alert($.i18n._('core-views')["specify-sep"]);
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
            alert($.i18n._('core-views')["warning-no-length"]);
            return;
          }

          config.fieldLengths = JSON.stringify(lengths);
          
        } catch (e) {
          alert($.i18n._('core-views')["warning-format"]);
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
        label: $.i18n._('core-views')["split-into-col"]+"...",
        click: doSplitColumn
      },
      {},
      {
        id: "core/add-column",
        label: $.i18n._('core-views')["add-based-col"]+"...",
        click: doAddColumn
      },
      {
        id: "core/add-column-by-fetching-urls",
        label: $.i18n._('core-views')["add-by-urls"]+"...",
        click: doAddColumnByFetchingURLs
      },
      {
        id: "core/add-column-by-reconciliation",
        label: $.i18n._('core-views')["add-col-recon-val"]+"...",
        click: doAddColumnByReconciliation
      },
      {},
      {
        id: "core/rename-column",
        label: $.i18n._('core-views')["rename-col"],
        click: doRenameColumn
      },
      {
        id: "core/remove-column",
        label: $.i18n._('core-views')["remove-col"],
        click: doRemoveColumn
      },
      {},
      {
        id: "core/move-column-to-beginning",
        label: $.i18n._('core-views')["move-to-beg"],
        click: function() { doMoveColumnTo(0); }
      },
      {
        id: "core/move-column-to-end",
        label: $.i18n._('core-views')["move-to-end"],
        click: function() { doMoveColumnTo(theProject.columnModel.columns.length - 1); }
      },
      {
        id: "core/move-column-to-left",
        label: $.i18n._('core-views')["move-to-left"],
        click: function() { doMoveColumnBy(-1); }
      },
      {
        id: "core/move-column-to-right",
        label: $.i18n._('core-views')["move-to-right"],
        click: function() { doMoveColumnBy(1); }
      }
    ]
  );
});
