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
    elmts.dialogHeader.text($.i18n("custom-text-trans")+" " + column.name);
    
    elmts.or_views_errorOn.text($.i18n("on-error"));
    elmts.or_views_keepOr.text($.i18n("keep-or"));
    elmts.or_views_setBlank.text($.i18n("set-blank"));
    elmts.or_views_storeErr.text($.i18n("store-err"));
    elmts.or_views_reTrans.text($.i18n("re-trans"));
    elmts.or_views_timesChang.text($.i18n("times-chang"));
    elmts.okButton.html($.i18n("ok"));
    elmts.cancelButton.text($.i18n("cancel"));    

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
    var separator = window.prompt($.i18n("enter-separator"), ", ");
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
    var separator = window.prompt($.i18n("what-separator"), ",");
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
      label: $.i18n("transform")+"...",
      click: function() { doTextTransformPrompt(); }
    },
    {
      id: "core/common-transforms",
      label: $.i18n("common-transform"),
      submenu: [
        {
          id: "core/trim-whitespace",
          label: $.i18n("trim-all"),
          click: function() { doTextTransform("value.trim()", "keep-original", false, ""); }
        },
        {
          id: "core/collapse-whitespace",
          label: $.i18n("collapse-white"),
          click: function() { doTextTransform("value.replace(/\\s+/,' ')", "keep-original", false, ""); }
        },
        {},
        {
          id: "core/unescape-html-entities",
          label: $.i18n("unescape-html"),
          click: function() { doTextTransform("value.unescape('html')", "keep-original", true, 10); }
        },
        {},
        {
          id: "core/to-titlecase",
          label: $.i18n("titlecase"),
          click: function() { doTextTransform("value.toTitlecase()", "keep-original", false, ""); }
        },
        {
          id: "core/to-uppercase",
          label: $.i18n("uppercase"),
          click: function() { doTextTransform("value.toUppercase()", "keep-original", false, ""); }
        },
        {
          id: "core/to-lowercase",
          label: $.i18n("lowercase"),
          click: function() { doTextTransform("value.toLowercase()", "keep-original", false, ""); }
        },
        {},
        {
          id: "core/to-number",
          label: $.i18n("to-number"),
          click: function() { doTextTransform("value.toNumber()", "keep-original", false, ""); }
        },
        {
          id: "core/to-date",
          label: $.i18n("to-date"),
          click: function() { doTextTransform("value.toDate()", "keep-original", false, ""); }
        },
        {
          id: "core/to-text",
          label: $.i18n("to-text"),
          click: function() { doTextTransform("value.toString()", "keep-original", false, ""); }
        },
        {},
        {
          id: "core/to-blank",
          label: $.i18n("blank-out"),
          click: function() { doTextTransform("null", "keep-original", false, ""); }
        }
      ]
    },
    {},
    {
      id: "core/fill-down",
      label: $.i18n("fill-down"),
      click: doFillDown
    },
    {
      id: "core/blank-down",
      label: $.i18n("blank-down"),
      click: doBlankDown
    },
    {},
    {
      id: "core/split-multi-valued-cells",
      label: $.i18n("split-cells")+"...",
      click: doSplitMultiValueCells
    },
    {
      id: "core/join-multi-valued-cells",
      label: $.i18n("join-cells")+"...",
      click: doJoinMultiValueCells
    },
    {},
    {
      id: "core/cluster",
      label: $.i18n("cluster-edit")+"...",
      click: function() { new ClusteringDialog(column.name, "value"); }
    }
  ]);

  var doTransposeColumnsIntoRows = function() {
    var dialog = $(DOM.loadHTML("core", "scripts/views/data-table/transpose-columns-into-rows.html"));

    var elmts = DOM.bind(dialog);
    var level = DialogSystem.showDialog(dialog);
    
    elmts.dialogHeader.html($.i18n("transp-cell"));
    elmts.or_views_fromCol.html($.i18n("from-col"));
    elmts.or_views_toCol.html($.i18n("to-col"));
    elmts.or_views_transpose.html($.i18n("transp-into"));
    elmts.or_views_twoCol.html($.i18n("two-new-col"));
    elmts.or_views_keyCol.html($.i18n("key-col"));
    elmts.or_views_containNames.html($.i18n("contain-names"));
    elmts.or_views_valCol.html($.i18n("val-col"));
    elmts.or_views_containOrig.html($.i18n("contain-val"));
    elmts.or_views_oneCol.html($.i18n("one-col"));
    elmts.or_views_prependName.html($.i18n("prepend-name"));
    elmts.or_views_followBy.html($.i18n("follow-by"));
    elmts.or_views_beforeVal.html($.i18n("before-val"));
    elmts.or_views_ignoreBlank.html($.i18n("ignore-blank"));
    elmts.or_views_fillOther.html($.i18n("fill-other"));
    elmts.okButton.html($.i18n("transpose"));
    elmts.cancelButton.html($.i18n("cancel"));
    
    var dismiss = function() {
      DialogSystem.dismissUntil(level - 1);
    };

    var columns = theProject.columnModel.columns;

    elmts.cancelButton.click(function() { dismiss(); });
    elmts.okButton.click(function() {
      var config = {
        startColumnName: elmts.fromColumnSelect[0].value,
        columnCount: elmts.toColumnSelect[0].value,
        ignoreBlankCells: elmts.ignoreBlankCellsCheckbox[0].checked,
        fillDown: elmts.fillDownCheckbox[0].checked
      };
      
      var mode = dialog.find('input[name="transpose-dialog-column-choices"]:checked')[0].value;
      if (mode == "2") {
        config.keyColumnName = $.trim(elmts.keyColumnNameInput[0].value);
        config.valueColumnName = $.trim(elmts.valueColumnNameInput[0].value);
        if (config.keyColumnName == "") {
          alert($.i18n("spec-new-name"));
          return;
        } else if (config.valueColumnName == "") {
          alert($.i18n("spec-new-val"));
          return;
        }
      } else {
        config.combinedColumnName = $.trim(elmts.combinedColumnNameInput[0].value);
        config.prependColumnName = elmts.prependColumnNameCheckbox[0].checked;
        config.separator = elmts.separatorInput[0].value;
        if (config.combinedColumnName == "") {
          alert($.i18n("spec-col-name"));
          return;
        } else if (config.prependColumnName && config.separator == "") {
          alert($.i18n("spec-separator"));
          return;
        }
      }

      Refine.postCoreProcess(
          "transpose-columns-into-rows", 
          config,
          null,
          { modelsChanged: true },
          {
            onDone: dismiss
          }
      );
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
        $('<option>').attr("value", k - j + 1).text(column2.name).appendTo(elmts.toColumnSelect);
      }
      
      $('<option>')
        .attr("value", "-1")
        .attr("selected", "true")
        .text("(last column)")
        .appendTo(elmts.toColumnSelect);
    };
    populateToColumn();

    elmts.fromColumnSelect.bind("change", populateToColumn);
  };

  var doTransposeRowsIntoColumns = function() {
    var rowCount = window.prompt($.i18n("how-many-rows"), "2");
    if (rowCount !== null) {
      try {
        rowCount = parseInt(rowCount,10);
      } catch (e) {
        // ignore
      }

      if (isNaN(rowCount) || rowCount < 2) {
        alert($.i18n("expect-two"));
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
  
  var doKeyValueColumnize = function() {
    var dialog = $(DOM.loadHTML("core", "scripts/views/data-table/key-value-columnize.html"));

    var elmts = DOM.bind(dialog);
    var level = DialogSystem.showDialog(dialog);
    
    elmts.dialogHeader.html($.i18n("columnize"));
    elmts.or_views_keyCol.html($.i18n("key-col"));
    elmts.or_views_valCol.html($.i18n("val-col"));
    elmts.or_views_noteCol.html($.i18n("note-col"));
    elmts.okButton.html($.i18n("ok"));
    elmts.cancelButton.html($.i18n("cancel"));
    
    var dismiss = function() {
      DialogSystem.dismissUntil(level - 1);
    };

    var columns = theProject.columnModel.columns;

    elmts.cancelButton.click(function() { dismiss(); });
    elmts.okButton.click(function() {
      var config = {
        keyColumnName: elmts.keyColumnSelect[0].value,
        valueColumnName: elmts.valueColumnSelect[0].value,
        noteColumnName: elmts.noteColumnSelect[0].value
      };
      if (config.keyColumnName == null ||
          config.valueColumnName == null ||
          config.keyColumnName == config.valueColumnName) {
        alert($.i18n("sel-col-val"));
        return;
      }
      
      var noteColumnName = elmts.noteColumnSelect[0].value;
      if (noteColumnName != null) {
        if (noteColumnName == config.keyColumnName ||
            noteColumnName == config.valueColumnName) {
          alert($.i18n("cannot-same"));
          return;
        }
        config.noteColumnName = noteColumnName;
      }

      Refine.postCoreProcess(
        "key-value-columnize", 
        config,
        null,
        { modelsChanged: true }
      );
      dismiss();
    });

    var valueColumnIndex = -1;
    for (var i = 0; i < columns.length; i++) {
      var column2 = columns[i];
      
      var keyOption = $('<option>').attr("value", column2.name).text(column2.name).appendTo(elmts.keyColumnSelect);
      if (column2.name == column.name) {
        keyOption.attr("selected", "true");
        valueColumnIndex = i + 1;
      }
      
      var valueOption = $('<option>').attr("value", column2.name).text(column2.name).appendTo(elmts.valueColumnSelect);
      if (i === valueColumnIndex) {
        valueOption.attr("selected", "true");
      }
      
      $('<option>').attr("value", column2.name).text(column2.name).appendTo(elmts.noteColumnSelect);
    }
  };

  MenuSystem.appendTo(menu, [ "core/transpose" ], [
      {
        id: "core/transpose-columns-into-rows",
        label: $.i18n("transp-cell-row")+"...",
        click: doTransposeColumnsIntoRows
      },
      {
        id: "core/transpose-rows-into-columns",
        label: $.i18n("transp-cell-col")+"...",
        click: doTransposeRowsIntoColumns
      },
      {},
      {
        id: "core/key-value-columnize",
        label: $.i18n("columnize-col")+"...",
        click: doKeyValueColumnize
      }
    ]
  );
});
