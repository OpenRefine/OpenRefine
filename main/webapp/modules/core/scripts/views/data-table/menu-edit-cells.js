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
        onError: onError,
        repeat: repeat,
        repeatCount: repeatCount
      },
      { expression: expression },
      { cellsChanged: true }
    );
  };

  var doTextTransformPrompt = function() {
    var frame = $(
        DOM.loadHTML("core", "scripts/views/data-table/text-transform-dialog.html")
        .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));

    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n('core-views/custom-text-trans')+" " + column.name);

    elmts.or_views_errorOn.text($.i18n('core-views/on-error'));
    elmts.or_views_keepOr.text($.i18n('core-views/keep-or'));
    elmts.or_views_setBlank.text($.i18n('core-views/set-blank'));
    elmts.or_views_storeErr.text($.i18n('core-views/store-err'));
    elmts.or_views_reTrans.text($.i18n('core-views/re-trans'));
    elmts.or_views_timesChang.text($.i18n('core-views/times-chang'));
    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.text($.i18n('core-buttons/cancel'));

    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

    elmts.cancelButton.on('click',dismiss);
    elmts.okButton.on('click',function() {
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
    elmts.repeatCheckbox.on('click',function() {
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

  var doJoinMultiValueCells = function(separator) {
    var defaultValue = Refine.getPreference("ui.cell.rowSplitDefaultSeparator", ",");
    var separator = window.prompt($.i18n('core-views/enter-separator'), defaultValue);
    if (separator !== null) {
      Refine.postCoreProcess(
        "join-multi-value-cells",
        {
          columnName: column.name,
          keyColumnName: theProject.columnModel.keyColumnName,
          separator
        },
        null,
        { rowsChanged: true }
      );
      Refine.setPreference("ui.cell.rowSplitDefaultSeparator", separator);
    }
  };

  var doReplace = function() {
    function isValidRegexPattern(p) {
      // check if a string can be used as a regexp pattern
      // parameters :
      // p : a string without beginning and trailing "/"

      // we need a manual check for unescaped /
      // the GREL replace() function cannot contain a pattern with unescaped /
      // but javascript Regexp accepts it and auto escape it
      var pos = p.replace(/\\\//g,'').indexOf("/");
      if (pos != -1) {
        alert($.i18n('core-views/warning-regex',p));
        return 0;}
      try {
        var pattern = new RegExp(p);
        return 1;
        } catch (e) {
          alert($.i18n('core-views/warning-regex', p));
        return 0;}
    }
    function escapeInputString(s) {
      // if the input is used as a plain string
      // user input                | result after escaping
      // 4 characters : A\tA       | 5 characters : A\\tA
      // 4 characters : A\nA       | 5 characters : A\\nA
      // 1 characters : \          | 2 characters : \\
      // 1 character : '           | 2 characters : \'
      // 1 character : "           | 2 characters : \"
      // new line or tab           | nothing
      // other non printable char  | nothing
      // parameters :
      // s : a string from an HTML input
      // Note: if the input field is replaced with a textarea, it could be imaginable to escape new lines and tabs as \n and \t
      if (typeof s != 'string') {
        return "";
      }
      // delete new lines, tabs and other non printable characters, and escape \ ' and "
      return s.replace(/[\x00-\x1F\x80-\x9F]/g, '').replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '\\"')
    }
    function escapeInputRegex(s) {
      // if the HTML input is used as a regex pattern
      // delete new lines, tabs and other non printable characters
      // no need to escape \ or /
      // parameters :
      // s : a string from a HTML input
      return (typeof s == 'string') ? s.replace(/[\x00-\x1F\x80-\x9F]/g, '')  : ""
      }
    function escapedStringToRegex(s) {
      // converts a plain string (beforehand escaped with escapeInputString) to regex, in order to use the i flag
      // escaping is needed to force the regex processor to interpret every character litteraly
      // parameters :
      // s : a string from a HTML input, preprocessed with escapeInputString
      if (typeof s != 'string') {
        return "";
      }
      // we MUST NOT escape \ : already escaped by escapeInputString
      var EscapeCharsRegex = /[-|{}()[\]^$+*?.]/g;
      // FIXME escaping of / directly by adding / or \/ or // in EscapeCharsRegex don't work...
      return s.replace(EscapeCharsRegex, '\\$&').replace(/\//g, '\\/');
    }
    function escapeReplacementString(dont_escape,s) {
    // in replacement string, the GREL replace function handle in a specific way the Java special escape sequences
    // cf https://docs.oracle.com/javase/tutorial/java/data/characters.html
    // Escape Sequence  | Java documentation  | GREL replace function
    // \t               | tab                 | tabs
    // \n               | new line            | new line
    // \r               |	carriage return     | <blank>
    // \b	              | backspace           | b
    // \f               | formfeed            | f
    // \'	              | '                   | ''
    // \"	              | "                   | ""
    // \\               | \                   | \
    // GREL replace function returns an error if a \ is not followed by an other character
    // it could be unexpected for the user, so the replace menu escape the replacement string, but gives the possibility to choose the default behavior
    // if dont_escape = 0, every \ are escaped as \\
    // if dont_escape = 1, \t and \n are not escaped, but  \r, \b, \f are escaped as \\r, \\b, \\f
    // in both case, new lines and tabs manually typed in the input are converted into \n and \t, and non printable characters are deleted
    // parameters :
    // replace_dont_escape : 0 or 1
    // s : a string from a HTML input
    if (typeof s != 'string') {
      return "";
    }
    var temp = s;
    if (dont_escape == 0) {
      temp = temp.replace(/\\/g, '\\\\');
    }
    else {
      // replace "\n" with newline and "\t" with tab
      temp = temp.replace(/\\n/g, '\n').replace(/\\t/g, '\t');
      // replace "\" with "\\"
      temp = temp.replace(/\\/g, '\\\\');
      // replace "\newline" with "\n" and "\tab" with "\t"
      temp = temp.replace(/\\\n/g, '\\n').replace(/\\\t/g, '\\t');
    }
    // replace newline with "\n" and tab with "\t"
    return temp.replace(/\n/g, '\\n').replace(/\t/g, '\\t').replace(/[\x00-\x1F\x80-\x9F]/g,'');
  }
    var frame = $(DOM.loadHTML("core", "scripts/views/data-table/replace-dialog.html"));
    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n('core-views/replace/header'));
    elmts.or_views_text_to_find.text($.i18n('core-views/text-to-find'));
    elmts.or_views_replacement.text($.i18n('core-views/replacement-text'));
    elmts.or_views_finding_info1.text($.i18n('core-views/finding-info1'));
    elmts.or_views_finding_info2.text($.i18n('core-views/finding-info2'));
    elmts.or_views_replacement_info.text($.i18n('core-views/replacement-info'));
    elmts.or_views_find_regExp.text($.i18n('core-views/reg-exp'));
    elmts.or_views_find_case_insensitive.text($.i18n('core-views/case-insensitive'));
    elmts.or_views_find_whole_word.text($.i18n('core-views/whole-word'));
    elmts.or_views_replace_dont_escape.text($.i18n('core-views/replace-dont-escape'));
    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.text($.i18n('core-buttons/cancel'));
    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };
    elmts.cancelButton.on('click',dismiss);
    elmts.text_to_findInput.trigger('focus');
    elmts.okButton.on('click',function() {
      var text_to_find = elmts.text_to_findInput[0].value;
      var replacement_text = elmts.replacement_textInput[0].value;
      var replace_dont_escape = elmts.replace_dont_escapeInput[0].checked;
      var find_regex = elmts.find_regexInput[0].checked;
      var find_case_insensitive = elmts.find_case_insensitiveInput[0].checked;
      var find_whole_word = elmts.find_whole_wordInput[0].checked;
      replacement_text = escapeReplacementString(replace_dont_escape, replacement_text)
      if (find_regex) {
        text_to_find = escapeInputRegex(text_to_find);
      }
      else {
        text_to_find = escapeInputString(text_to_find);
      }
      if (!find_regex && (find_case_insensitive || find_whole_word)) {
        text_to_find = escapedStringToRegex(text_to_find);
      }
      if (find_regex || find_case_insensitive || find_whole_word ) {
        if (!isValidRegexPattern (text_to_find)) {
          return;
        }
        if (find_whole_word) {
          text_to_find = "\\b"+text_to_find+"\\b";
        }
        text_to_find = "/"+text_to_find+"/";
        if (find_case_insensitive) {
          text_to_find = text_to_find+"i";
        }
      }
      else {
        text_to_find = '"'+text_to_find+'"';
      }
      expression = 'value.replace('+text_to_find+',"'+replacement_text+'")';
      doTextTransform(expression, "keep-original", false, "");
      dismiss();
    });
  };

  var doSplitMultiValueCells = function() {

    var frame = $(DOM.loadHTML("core", "scripts/views/data-table/split-multi-valued-cells-dialog.html"));
    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n('core-views/split-cells/header'));

    elmts.or_views_howSplit.text($.i18n('core-views/how-split-cells'));
    elmts.or_views_bySep.text($.i18n('core-views/by-sep'));
    elmts.or_views_separator.text($.i18n('core-views/separator'));
    elmts.or_views_regExp.text($.i18n('core-views/reg-exp'));

    elmts.or_views_fieldLen.text($.i18n('core-views/field-len'));
    elmts.or_views_listInt.text($.i18n('core-views/list-int'));

    elmts.or_views_byCase.text($.i18n('core-views/by-case'));
    elmts.or_views_byNumber.text($.i18n('core-views/by-number'));
    elmts.or_views_revCase.text($.i18n('core-views/by-rev'));
    elmts.or_views_revNum.text($.i18n('core-views/by-rev'));
    elmts.or_views_caseExample.text($.i18n('core-views/by-case-example'));
    elmts.or_views_caseReverseExample.text($.i18n('core-views/by-case-rev-example'));
    elmts.or_views_numberExample.text($.i18n('core-views/by-number-example'));
    elmts.or_views_numberReverseExample.text($.i18n('core-views/by-number-rev-example'));

    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.text($.i18n('core-buttons/cancel'));

    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };
    
    var defaultValue = Refine.getPreference("ui.cell.rowSplitDefaultSeparator", ",");
    elmts.separatorInput[0].value = defaultValue;
    elmts.separatorInput.trigger('focus').trigger('select');
    
    elmts.cancelButton.on('click',dismiss);
    elmts.okButton.on('click',function() {
      var mode = $("input[name='split-by-mode']:checked")[0].value;
      var config = {
        columnName: column.name,
        keyColumnName: theProject.columnModel.keyColumnName,
        mode
      };
      if (mode === "separator") {
        config.separator = elmts.separatorInput[0].value;
        if (!(config.separator)) {
          alert($.i18n('core-views/specify-sep'));
          return;
        }

        config.regex = elmts.regexInput[0].checked;
        Refine.setPreference("ui.cell.rowSplitDefaultSeparator", config.separator);
      } else if (mode === "lengths") {
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
      } else if (mode === "cases") {
        if(elmts.reverseTransitionCases[0].checked) {
          config.separator = "(?<=\\p{Upper}|[\\p{Upper}][\\s])(?=\\p{Lower})";
        } else {
          config.separator = "(?<=\\p{Lower}|[\\p{Lower}][\\s])(?=\\p{Upper})";
        }
        config.regex = true;
      } else if (mode === "number") {
        if(elmts.reverseTransitionNumbers[0].checked) {
          config.separator = "(?<=\\p{L}|[\\p{L}][\\s])(?=\\p{Digit})";
        } else {
          config.separator = "(?<=\\p{Digit}|[\\p{Digit}][\\s])(?=\\p{L})";
        }
        config.regex = true;
      }

      Refine.postCoreProcess(
        "split-multi-value-cells",
        config,
        null,
        { rowsChanged: true }
      );

      dismiss();
    });
  };

  MenuSystem.appendTo(menu, [ "core/edit-cells" ], [
    {
      id: "core/text-transform",
      label: $.i18n('core-views/transform'),
      click: function() { doTextTransformPrompt(); }
    },
    {
      id: "core/common-transforms",
      label: $.i18n('core-views/common-transform'),
      submenu: [
        {
          id: "core/trim-whitespace",
          label: $.i18n('core-views/trim-all/single'),
          click: function() { doTextTransform("value.trim()", "keep-original", false, ""); }
        },
        {
          id: "core/collapse-whitespace",
          label: $.i18n('core-views/collapse-white/single'),
          click: function() { doTextTransform("value.replace(/[\\p{Zs}\\s]+/,' ')", "keep-original", false, ""); }
        },
        {},
        {
          id: "core/unescape-html-entities",
          label: $.i18n('core-views/unescape-html/single'),
          click: function() { doTextTransform("value.unescape('html')", "keep-original", true, 10); }
        },
        {
          id: "core/replace-smartquotes",
          label: $.i18n('core-views/replace-smartquotes/single'),
          click: function() { doTextTransform("value.replace(/[\u2018\u2019\u201A\u201B\u2039\u203A\u201A]/,\"\\\'\").replace(/[\u201C\u201D\u00AB\u00BB\u201E]/,\"\\\"\")", "keep-original", false, ""); }
        },
        {},
        {
          id: "core/to-titlecase",
          label: $.i18n('core-views/titlecase/single'),
          click: function() { doTextTransform("value.toTitlecase()", "keep-original", false, ""); }
        },
        {
          id: "core/to-uppercase",
          label: $.i18n('core-views/uppercase/single'),
          click: function() { doTextTransform("value.toUppercase()", "keep-original", false, ""); }
        },
        {
          id: "core/to-lowercase",
          label: $.i18n('core-views/lowercase/single'),
          click: function() { doTextTransform("value.toLowercase()", "keep-original", false, ""); }
        },
        {},
        {
          id: "core/to-number",
          label: $.i18n('core-views/to-number/single'),
          click: function() { doTextTransform("value.toNumber()", "keep-original", false, ""); }
        },
        {
          id: "core/to-date",
          label: $.i18n('core-views/to-date/single'),
          click: function() { doTextTransform("value.toDate()", "keep-original", false, ""); }
        },
        {
          id: "core/to-text",
          label: $.i18n('core-views/to-text/single'),
          click: function() { doTextTransform("value.toString()", "keep-original", false, ""); }
        },
        {},
        {
          id: "core/to-blank",
          label: $.i18n('core-views/blank-out/single'),
          click: function() { doTextTransform("null", "keep-original", false, ""); }
        },
        {
          id: "core/to-empty",
          label: $.i18n('core-views/blank-out-empty/single'),
          click: function() { doTextTransform("\"\"", "keep-original", false, ""); }
        }
      ]
    },
    {},
    {
      id: "core/fill-down",
      label: $.i18n('core-views/fill-down'),
      click: function () {
        if (columnHeaderUI._dataTableView._getSortingCriteriaCount() > 0) {
           columnHeaderUI._dataTableView._createPendingSortWarningDialog(doFillDown);
        }
        else {
           doFillDown();
        }
      }
    },
    {
      id: "core/blank-down",
      label: $.i18n('core-views/blank-down'),
      click: function () {
        if (columnHeaderUI._dataTableView._getSortingCriteriaCount() > 0) {
           columnHeaderUI._dataTableView._createPendingSortWarningDialog(doBlankDown);
        }
        else {
           doBlankDown();
        }
      }
    },
    {},
    {
      id: "core/split-multi-valued-cells",
      label: $.i18n('core-views/split-cells'),
      click: doSplitMultiValueCells
    },
    {
      id: "core/join-multi-valued-cells",
      label: $.i18n('core-views/join-cells'),
      click: doJoinMultiValueCells
    },
    {},
    {
      id: "core/cluster",
      label: $.i18n('core-views/cluster-edit'),
      click: function() { new ClusteringDialog(column.name, "value"); }
    },
    {},
    {
      id: "core/replace",
      label: $.i18n('core-views/replace'),
      click: doReplace
    }
  ]);

  var doTransposeColumnsIntoRows = function() {
    var dialog = $(DOM.loadHTML("core", "scripts/views/data-table/transpose-columns-into-rows.html"));

    var elmts = DOM.bind(dialog);
    var level = DialogSystem.showDialog(dialog);

    elmts.dialogHeader.html($.i18n('core-views/transp-cell'));
    elmts.or_views_fromCol.html($.i18n('core-views/from-col'));
    elmts.or_views_toCol.html($.i18n('core-views/to-col'));
    elmts.or_views_transpose.html($.i18n('core-views/transp-into'));
    elmts.or_views_twoCol.html($.i18n('core-views/two-new-col'));
    elmts.or_views_keyCol.html($.i18n('core-views/key-col'));
    elmts.or_views_containNames.html($.i18n('core-views/contain-names'));
    elmts.or_views_valCol.html($.i18n('core-views/val-col'));
    elmts.or_views_containOrig.html($.i18n('core-views/contain-val'));
    elmts.or_views_oneCol.html($.i18n('core-views/one-col'));
    elmts.or_views_prependName.html($.i18n('core-views/prepend-name'));
    elmts.or_views_followBy.html($.i18n('core-views/follow-by'));
    elmts.or_views_beforeVal.html($.i18n('core-views/before-val'));
    elmts.or_views_ignoreBlank.html($.i18n('core-views/ignore-blank'));
    elmts.or_views_fillOther.html($.i18n('core-views/fill-other'));
    elmts.okButton.html($.i18n('core-buttons/transpose'));
    elmts.cancelButton.html($.i18n('core-buttons/cancel'));

    var dismiss = function() {
      DialogSystem.dismissUntil(level - 1);
    };

    var columns = theProject.columnModel.columns;

    elmts.cancelButton.on('click',function() { dismiss(); });
    elmts.okButton.on('click',function() {
      var config = {
        startColumnName: elmts.fromColumnSelect[0].value,
        columnCount: elmts.toColumnSelect[0].value,
        ignoreBlankCells: elmts.ignoreBlankCellsCheckbox[0].checked,
        fillDown: elmts.fillDownCheckbox[0].checked
      };

      var mode = dialog.find('input[name="transpose-dialog-column-choices"]:checked')[0].value;
      if (mode == "2") {
        config.keyColumnName = jQueryTrim(elmts.keyColumnNameInput[0].value);
        config.valueColumnName = jQueryTrim(elmts.valueColumnNameInput[0].value);
        if (config.keyColumnName == "") {
          alert($.i18n('core-views/spec-new-name'));
          return;
        } else if (config.valueColumnName == "") {
          alert($.i18n('core-views/spec-new-val'));
          return;
        }
      } else {
        config.combinedColumnName = jQueryTrim(elmts.combinedColumnNameInput[0].value);
        config.prependColumnName = elmts.prependColumnNameCheckbox[0].checked;
        config.separator = elmts.separatorInput[0].value;
        if (config.combinedColumnName == "") {
          alert($.i18n('core-views/spec-col-name'));
          return;
        } else if (config.prependColumnName && config.separator == "") {
          alert($.i18n('core-views/spec-separator'));
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
      var option = $('<option>').val(column2.name).text(column2.name).appendTo(elmts.fromColumnSelect);
      if (column2.name == column.name) {
        option.prop("selected", "true");
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
        $('<option>').val(k - j + 1).text(column2.name).appendTo(elmts.toColumnSelect);
      }

      $('<option>')
        .val("-1")
        .prop("selected", "true")
        .text("(last column)")
        .appendTo(elmts.toColumnSelect);
    };
    populateToColumn();

    elmts.fromColumnSelect.on("change", populateToColumn);
  };

  var doTransposeRowsIntoColumns = function() {
    var rowCount = window.prompt($.i18n('core-views/how-many-rows'), "2");
    if (rowCount !== null) {
      try {
        rowCount = parseInt(rowCount,10);
      } catch (e) {
        // ignore
      }

      if (isNaN(rowCount) || rowCount < 2) {
        alert($.i18n('core-views/expect-two'));
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

    elmts.dialogHeader.html($.i18n('core-views/columnize'));
    elmts.or_views_keyCol.html($.i18n('core-views/key-col'));
    elmts.or_views_valCol.html($.i18n('core-views/val-col'));
    elmts.or_views_noteCol.html($.i18n('core-views/note-col'));
    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.html($.i18n('core-buttons/cancel'));

    var dismiss = function() {
      DialogSystem.dismissUntil(level - 1);
    };

    var columns = theProject.columnModel.columns;

    elmts.cancelButton.on('click',function() { dismiss(); });
    elmts.okButton.on('click',function() {
      var config = {
        keyColumnName: elmts.keyColumnSelect[0].value,
        valueColumnName: elmts.valueColumnSelect[0].value,
        noteColumnName: elmts.noteColumnSelect[0].value
      };
      if (config.keyColumnName == null ||
          config.valueColumnName == null ||
          config.keyColumnName == config.valueColumnName) {
        alert($.i18n('core-views/sel-col-val'));
        return;
      }

      var noteColumnName = elmts.noteColumnSelect[0].value;
      if (noteColumnName != null) {
        if (noteColumnName == config.keyColumnName ||
            noteColumnName == config.valueColumnName) {
          alert($.i18n('core-views/cannot-same'));
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

      var keyOption = $('<option>').val(column2.name).text(column2.name).appendTo(elmts.keyColumnSelect);
      if (column2.name == column.name) {
        keyOption.prop("selected", "true");
        valueColumnIndex = i + 1;
      }

      var valueOption = $('<option>').val(column2.name).text(column2.name).appendTo(elmts.valueColumnSelect);
      if (i === valueColumnIndex) {
        valueOption.prop("selected", "true");
      }

      $('<option>').val(column2.name).text(column2.name).appendTo(elmts.noteColumnSelect);
    }

    var currentHeight = dialog.outerHeight();
    var currentWidth = dialog.outerWidth();
    dialog.resizable({
      alsoResize: ".dialog-border .dialog-body",
      handles: "e, w, se",
      minHeight: currentHeight,
      maxHeight: currentHeight,
      minWidth: currentWidth
    });
  };

  MenuSystem.appendTo(menu, [ "core/transpose" ], [
      {
        id: "core/transpose-columns-into-rows",
        label: $.i18n('core-views/transp-cell-row'),
        click: doTransposeColumnsIntoRows
      },
      {
        id: "core/transpose-rows-into-columns",
        label: $.i18n('core-views/transp-cell-col'),
        click: doTransposeRowsIntoColumns
      },
      {},
      {
        id: "core/key-value-columnize",
        label: $.i18n('core-views/columnize-col'),
        click: doKeyValueColumnize
      }
    ]
  );
});
