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
  var doTextTransform = function(columnName, expression, onError, repeat, repeatCount, callbacks) {
      callbacks = callbacks || {};
	    Refine.postCoreProcess(
	      "text-transform",
	      {
	        columnName: columnName,
	        onError: onError,
	        repeat: repeat,
	        repeatCount: repeatCount
	      },
	      { expression: expression },
	      { cellsChanged: true },
	      callbacks
	    );
	  };
	  
  var doAddColumn = function() {
    var frame = $(
        DOM.loadHTML("core", "scripts/views/data-table/add-column-dialog.html")
        .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));

    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n('core-views/add-col-col')+" " + column.name);
    
    elmts.or_views_newCol.text($.i18n('core-views/new-col-name'));
    elmts.or_views_onErr.text($.i18n('core-views/on-error'));
    elmts.or_views_setBlank.text($.i18n('core-views/set-blank'));
    elmts.or_views_storeErr.text($.i18n('core-views/store-err'));
    elmts.or_views_copyVal.text($.i18n('core-views/copy-val'));
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
      var columnName = jQueryTrim(elmts.columnNameInput[0].value);
      if (!columnName.length) {
        alert($.i18n('core-views/warning-col-name'));
        return;
      }

      Refine.postCoreProcess(
        "add-column", 
        {
          baseColumnName: column.name,  
          newColumnName: columnName, 
          columnInsertIndex: columnIndex + 1,
          onError: $('input[name="create-column-dialog-onerror-choice"]:checked')[0].value
        },
        { expression: previewWidget.getExpression(true) },
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
        .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml())
        .replace("$HTTP_HEADERS_WIDGET$", HttpHeadersDialog.generateWidgetHtml())
        );

    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n('core-views/add-col-fetch')+" " + column.name);
    
    elmts.or_views_newCol.text($.i18n('core-views/new-col-name'));
    elmts.or_views_throttle.text($.i18n('core-views/throttle-delay'));
    elmts.or_views_milli.text($.i18n('core-views/milli'));
    elmts.or_views_onErr.text($.i18n('core-views/on-error'));
    elmts.or_views_setBlank.text($.i18n('core-views/set-blank'));
    elmts.or_views_storeErr.text($.i18n('core-views/store-err'));
    elmts.or_views_cacheResponses.text($.i18n('core-views/cache-responses'));
    elmts.or_views_httpHeaders.text($.i18n('core-views/http-headers'));
    elmts.or_views_urlFetch.text($.i18n('core-views/url-fetch'));
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
      var columnName = jQueryTrim(elmts.columnNameInput[0].value);
      if (!columnName.length) {
        alert($.i18n('core-views/warning-col-name'));
        return;
      }
      let delay = Number.parseInt(elmts.throttleDelayInput[0].value);
      if (Number.isNaN(delay) || delay < 0) {
        alert($.i18n('core-views/warning-throttle-delay-input'));
        return;
      }
      Refine.postCoreProcess(
        "add-column-by-fetching-urls", 
        {
          baseColumnName: column.name, 
          urlExpression: previewWidget.getExpression(true), 
          newColumnName: columnName, 
          columnInsertIndex: columnIndex + 1,
          delay: delay,
          onError: $('input[name="dialog-onerror-choice"]:checked')[0].value,
          cacheResponses: $('input[name="dialog-cache-responses"]')[0].checked,
          httpHeaders: JSON.stringify(elmts.setHttpHeadersContainer.find("input").serializeArray())
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
    var frame = $(
        DOM.loadHTML("core", "scripts/views/data-table/rename-column.html"));

    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n('core-views/enter-col-name'));
    elmts.columnNameInput.text();
    elmts.columnNameInput.attr('aria-label',$.i18n('core-views/new-column-name'));
    elmts.columnNameInput[0].value = column.name;
    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.text($.i18n('core-buttons/cancel'));

    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };
    elmts.cancelButton.on('click',dismiss);
    elmts.form.on('submit',function(event) {
      event.preventDefault();
      var newColumnName = jQueryTrim(elmts.columnNameInput[0].value);
      if (newColumnName === column.name) {
        dismiss();
        return;
      }
      if (newColumnName.length > 0) {
        Refine.postCoreProcess(
            "rename-column",
            {
              oldColumnName: column.name,
              newColumnName: newColumnName
            },
            null,
            {modelsChanged: true},
            {
              onDone: function () {
                dismiss();
              }
            }
        );
      }
    });
    elmts.columnNameInput.trigger('focus').trigger('select');
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
    elmts.dialogHeader.text($.i18n('core-views/split-col', column.name));
    
    elmts.or_views_howSplit.text($.i18n('core-views/how-split'));
    elmts.or_views_bySep.text($.i18n('core-views/by-sep'));
    elmts.or_views_separator.text($.i18n('core-views/separator'));
    elmts.or_views_regExp.text($.i18n('core-views/reg-exp'));
    elmts.or_views_splitInto.text($.i18n('core-views/split-into'));
    elmts.or_views_colMost.text($.i18n('core-views/col-at-most'));
    elmts.or_views_fieldLen.text($.i18n('core-views/field-len'));
    elmts.or_views_listInt.text($.i18n('core-views/list-int'));
    elmts.or_views_afterSplit.text($.i18n('core-views/after-split'));
    elmts.or_views_guessType.text($.i18n('core-views/guess-cell'));
    elmts.or_views_removeCol.text($.i18n('core-views/remove-col'));
    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.text($.i18n('core-buttons/cancel'));

    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };
    
    elmts.separatorInput.trigger('focus').trigger('select');

    elmts.cancelButton.on('click',dismiss);
    elmts.okButton.on('click',function() {
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
          alert($.i18n('core-views/specify-sep'));
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
            alert($.i18n('core-views/warning-no-length'));
            return;
          }

          config.fieldLengths = JSON.stringify(lengths);
          
        } catch (e) {
          alert($.i18n('core-views/warning-format'));
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
  
  var doJoinColumns = function() {
    var self = this;
    var dialog = $(DOM.loadHTML("core","scripts/views/data-table/column-join.html"));
    var elmts = DOM.bind(dialog);
    var level = DialogSystem.showDialog(dialog);
    // Escape strings
    function escapeString(s,dontEscape) {
      var dontEscape = dontEscape || false;
      var temp = s;
      if (dontEscape) {
        // replace "\n" with newline and "\t" with tab
        temp = temp.replace(/\\n/g, '\n').replace(/\\t/g, '\t');
        // replace "\" with "\\"
        temp = temp.replace(/\\/g, '\\\\');
        // replace "\newline" with "\n" and "\tab" with "\t"
        temp = temp.replace(/\\\n/g, '\\n').replace(/\\\t/g, '\\t');
        // replace ' with \'
        temp = temp.replace(/'/g, "\\'");
      } 
      else {
     // escape \ and '
        temp = s.replace(/\\/g, '\\\\').replace(/'/g, "\\'") ; 
        // useless : .replace(/"/g, '\\"')
      }
      return temp;
    };
    // Close the dialog window
    var dismiss = function() {
       DialogSystem.dismissUntil(level - 1);
     };
     // Join the columns according to user input
    var transform = function() {
      // function called in a callback
      var deleteColumns = function() {
        if (deleteJoinedColumns) {
          console.log (theProject);
          var columnsToKeep = theProject.columnModel.columns
          .map (function (col) {return col.name;})
          .filter (function(colName) {
            // keep the selected column if it contains the result
            return (
                (columnsToJoin.indexOf (colName) == -1) ||
                ((writeOrCopy !="copy-to-new-column") && (colName == column.name)));
            }); 
          Refine.postCoreProcess(
              "reorder-columns",
              null,
              { "columnNames" : JSON.stringify(columnsToKeep) }, 
              { modelsChanged: true },
              { includeEngine: false }
          );
        }
      };
      // get options
      var onError = "keep-original" ;
      var repeat = false ;
      var repeatCount = "";
      var deleteJoinedColumns = elmts.delete_joined_columnsInput[0].checked;
      var writeOrCopy = $("input[name='write-or-copy']:checked")[0].value;
      var newColumnName = jQueryTrim(elmts.new_column_nameInput[0].value);
      var manageNulls = $("input[name='manage-nulls']:checked")[0].value;
      var nullSubstitute = elmts.null_substituteInput[0].value;
      var fieldSeparator = elmts.field_separatorInput[0].value;
      var dontEscape = elmts.dont_escapeInput[0].checked;
      // fix options if they are not consistent
      if (newColumnName != "") {
        writeOrCopy ="copy-to-new-column";
        } else
        {
          writeOrCopy ="write-selected-column";
        }
      if (nullSubstitute != "") {
          manageNulls ="replace-nulls";
      }   
      // build GREL expression
      var columnsToJoin = [];
      elmts.column_join_columnPicker
        .find('.column-join-column input[type="checkbox"]:checked')
        .each(function() {
            columnsToJoin.push (this.closest ('.column-join-column').getAttribute('column'));
         });
      expression = columnsToJoin.map (function (colName) {
        if (manageNulls == "skip-nulls") {
          return "cells['"+escapeString(colName) +"'].value";
        }
      else {
          return "coalesce(cells['"+escapeString(colName)+"'].value,'"+ escapeString(nullSubstitute,dontEscape) + "')";
        }
      }).join (',');
      expression = 'join ([' + expression + '],\'' + escapeString(fieldSeparator,dontEscape) + "')";
      // apply expression to selected column or new column
      if (writeOrCopy =="copy-to-new-column") {
        Refine.postCoreProcess(
          "add-column", 
          {
          baseColumnName: column.name,  
          newColumnName: newColumnName, 
          columnInsertIndex: columnIndex + 1,
          onError: onError
          },
          { expression: expression },
          { modelsChanged: true },
          { onFinallyDone: deleteColumns}
        );
      } 
      else {
        doTextTransform(
            column.name,
            expression,
            onError,
            repeat,
            repeatCount,
            { onFinallyDone: deleteColumns});
      }
    };
    // core of doJoinColumn
    elmts.dialogHeader.text($.i18n('core-views/column-join'));
    elmts.or_views_column_join_before_column_picker.text($.i18n('core-views/column-join-before-column-picker'));
    elmts.or_views_column_join_before_options.text($.i18n('core-views/column-join-before-options'));
    elmts.or_views_column_join_replace_nulls.text($.i18n('core-views/column-join-replace-nulls'));
    elmts.or_views_column_join_replace_nulls_advice.text($.i18n('core-views/column-join-replace-nulls-advice'));
    elmts.or_views_column_join_skip_nulls.text($.i18n('core-views/column-join-skip-nulls'));
    elmts.or_views_column_join_write_selected_column.text($.i18n('core-views/column-join-write-selected-column'));
    elmts.or_views_column_join_copy_to_new_column.text($.i18n('core-views/column-join-copy-to-new-column'));
    elmts.or_views_column_join_delete_joined_columns.text($.i18n('core-views/column-join-delete-joined-columns'));
    elmts.or_views_column_join_field_separator.text($.i18n('core-views/column-join-field-separator'));
    elmts.or_views_column_join_field_separator_advice.text($.i18n('core-views/column-join-field-separator-advice'));
    elmts.or_views_column_join_dont_escape.text($.i18n('core-views/column-join-dont-escape'));
    elmts.selectAllButton.html($.i18n('core-buttons/select-all'));
    elmts.deselectAllButton.html($.i18n('core-buttons/deselect-all'));
    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.html($.i18n('core-buttons/cancel'));
    /*
     * Populate column list.
     */
    for (var i = 0; i < theProject.columnModel.columns.length; i++) {
      var col = theProject.columnModel.columns[i];
      var colName = col.name;
      var div = $('<div>').
        addClass("column-join-column")
        .attr("column", colName)
        .appendTo(elmts.column_join_columnPicker);
      $('<input>').
        attr('type', 'checkbox')
        .attr("column", colName)
        .prop('checked',(i == columnIndex) ? true : false)
        .appendTo(div);
      $('<span>')
        .text(colName)
        .appendTo(div);
    }
    // Move the selected column on the top of the list
    if (columnIndex > 0) {
      selectedColumn = elmts.column_join_columnPicker
        .find('.column-join-column')
        .eq(columnIndex);
      selectedColumn.parent().prepend(selectedColumn);
     }
    // Make the list sortable
    elmts.column_join_columnPicker.sortable({});
   /*
    * Hook up event handlers.
    */
    elmts.column_join_columnPicker
      .find('.column-join-column')
      .on('click',function() {
        elmts.column_join_columnPicker
        .find('.column-join-column')
        .removeClass('selected');
        $(this).addClass('selected');
      });
    elmts.selectAllButton
      .on('click',function() {
        elmts.column_join_columnPicker
        .find('input[type="checkbox"]')
        .prop('checked',true);
       });
    elmts.deselectAllButton
      .on('click',function() {
        elmts.column_join_columnPicker
        .find('input[type="checkbox"]')
        .prop('checked',false);
      });
    elmts.okButton.on('click',function() {
      transform();
      dismiss();
    });
    elmts.cancelButton.on('click',function() {
      dismiss();
    });
    elmts.new_column_nameInput.on('change',function() {
      if (elmts.new_column_nameInput[0].value != "") {
        elmts.copy_to_new_columnInput.prop('checked',true);
      } else
        {
        elmts.write_selected_columnInput.prop('checked',true);
        }
    });
    elmts.null_substituteInput.on('change',function() {
        elmts.replace_nullsInput.prop('checked',true);
    });
  };

/*
 * Create global menu
 */
  MenuSystem.appendTo(menu, [ "core/edit-column" ], [
      {
        id: "core/split-column",
        label: $.i18n('core-views/split-into-col'),
        click: doSplitColumn
      },
      {
        id: "core/join-column",
        label: $.i18n('core-views/join-col'),
          click : doJoinColumns
        },
      {},
      {
        id: "core/add-column",
        label: $.i18n('core-views/add-based-col'),
        click: doAddColumn
      },
      {
        id: "core/add-column-by-fetching-urls",
        label: $.i18n('core-views/add-by-urls'),
        click: doAddColumnByFetchingURLs
      },
      {
        id: "core/add-column-by-reconciliation",
        label: $.i18n('core-views/add-col-recon-val'),
        click: doAddColumnByReconciliation
      },
      {},
      {
        id: "core/rename-column",
        label: $.i18n('core-views/rename-col'),
        click: doRenameColumn
      },
      {
        id: "core/remove-column",
        label: $.i18n('core-views/remove-col'),
        click: doRemoveColumn
      },
      {},
      {
        id: "core/move-column-to-beginning",
        label: $.i18n('core-views/move-to-beg'),
        click: function() { doMoveColumnTo(0); }
      },
      {
        id: "core/move-column-to-end",
        label: $.i18n('core-views/move-to-end'),
        click: function() { doMoveColumnTo(theProject.columnModel.columns.length - 1); }
      },
      {
        id: "core/move-column-to-left",
        label: $.i18n('core-views/move-to-left'),
        click: function() { doMoveColumnBy(-1);}
      },
      {
        id: "core/move-column-to-right",
        label: $.i18n('core-views/move-to-right'),
        click: function() { doMoveColumnBy(1); }
      }
    ]
  );
});
