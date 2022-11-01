/*
 * Copyright (c) 2017, Tony Opara
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * Neither the name of Google nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

function SqlExporterDialog(options) {
    options = options || {
      format: 'sql',
      encoding: 'UTF-8',
      outputBlankRows: false,
      columns: null
    };

    this._columnOptionMap = {};
    this._createDialog(options);
  }


  SqlExporterDialog.uploadTargets = [];

  SqlExporterDialog.prototype._createDialog = function(options) {
    var self = this;

    this._dialog = $(DOM.loadHTML("core", "scripts/dialogs/sql-exporter-dialog.html"));
    this._elmts = DOM.bind(this._dialog);
    this._level = DialogSystem.showDialog(this._dialog);
    this._elmts.dialogHeader.html($.i18n('core-dialogs/sql-exporter'));
    this._elmts.or_dialog_content.html($.i18n('core-dialogs/content'));
    this._elmts.or_dialog_download.html($.i18n('core-dialogs/download'));

    this._elmts.fieldNameHeader.html($.i18n('core-dialogs/sql-exporter-field-name-header'));
    this._elmts.sqlTypeHeader.html($.i18n('core-dialogs/sql-exporter-sql-type-header'));
    this._elmts.sizeHeader.html($.i18n('core-dialogs/sql-exporter-size-header'));
    this._elmts.allowNullToggleCheckboxLabel.html($.i18n('core-dialogs/sql-exporter-allow-null-header'));
    this._elmts.defaultHeader.html($.i18n('core-dialogs/sql-exporter-default-header'));
    this._elmts.selectAllButton.html($.i18n('core-buttons/select-all'));
    this._elmts.deselectAllButton.html($.i18n('core-buttons/deselect-all'));

    this._elmts.downloadPreviewButton.html($.i18n('core-buttons/preview'));
    this._elmts.downloadButton.html($.i18n('core-buttons/download'));

    this._elmts.cancelButton.html($.i18n('core-buttons/cancel'));
//    this._elmts.nextButton.html($.i18n('core-buttons/next'));

    this._elmts.tableNameLabel.html($.i18n('core-dialogs/tableNameLabel'));
    this._elmts.includeStructureLabel.html($.i18n('core-dialogs/for-include-structure-checkbox'));
    this._elmts.includeDropStatementLabel.html($.i18n('core-dialogs/for-include-drop-statement-checkbox'));
    this._elmts.includeContentLabel.html($.i18n('core-dialogs/for-include-content-checkbox'));
    this._elmts.includeIfExistDropStatementLabel.html($.i18n('core-dialogs/for-include-if-exist-drop-stmt-checkbox'));

    this._elmts.nullCellValueToEmptyStringLabel.html($.i18n('core-dialogs/for-null-cell-value-to-empty-str-label'));

    this._elmts.sqlExportIgnoreFacetsLabel.html($.i18n('core-dialogs/sqlExporterIgnoreFacets'));
    this._elmts.sqlExportTrimAllColumnsLabel.html($.i18n('core-dialogs/sqlExporterTrimColumns'));
    this._elmts.sqlExportOutputEmptyRowsLabel.html($.i18n('core-dialogs/sqlExporterOutputEmptyRows'));

    $("#sql-exporter-tabs-content").css("display", "");
    $("#sql-exporter-tabs-download").css("display", "");
    $("#sql-exporter-tabs-download label").css("padding-right", ".25em");
    $("#sql-exporter-tabs").tabs();

    for (var i = 0; i < theProject.columnModel.columns.length; i++) {
        var column = theProject.columnModel.columns[i];
        var name = column.name;
        var rowId = "sql-exporter-dialog-row" + i;
        var selectBoxName = 'selectBoxRow' + i;
        var sizeInputName = 'sizeInputRow' + i;
        var applyAllBtnName = 'applyAllBtn' + i;

        var allowNullChkBoxName = 'allowNullChkBox' + i;
        var defaultValueTextBoxName = 'defaultValueTextBox' + i;


        var sel = $('<select>').appendTo('body');
        sel.append($("<option>").val('VARCHAR').text('VARCHAR'));
        sel.append($("<option>").val('TEXT').text('TEXT'));
        sel.append($("<option>").val('INT').text('INT'));
        sel.append($("<option>").val('NUMERIC').text('NUMERIC'));
        sel.append($("<option>").val('CHAR').text('CHAR'));
        sel.append($("<option>").val('DATE').text('DATE'));
        sel.append($("<option>").val('TIMESTAMP').text('TIMESTAMP'));

        sel.attr('id', selectBoxName);
        sel.attr('rowIndex', i);
        sel.addClass('typeSelectClass');
        sel.css({'width':'95%'});

        $(sel).on('change', function() {
              var rowIndex = this.getAttribute('rowIndex');
              if (this.value === 'VARCHAR' || this.value === 'CHAR' || this.value === 'NUMERIC' || this.value === 'INT') {
                  $('#sizeInputRow'+ rowIndex).prop("disabled", false);
              }else{
                  $('#sizeInputRow'+ rowIndex).val("");
                  $('#sizeInputRow'+ rowIndex).prop("disabled", true);
              }
        });
         //create and add Row
         var row = $('<tr>')
              .addClass("sql-exporter-dialog-row")
              .attr('id', rowId)
              .attr("column", name)
              .attr("rowIndex", i)
              .appendTo(this._elmts.columnListTable);

         //create and add column
          var columnCell =  $('<td>')
          .attr('width', '150px')
          .appendTo(row);
          $('<input>')
            .attr('type', 'checkbox')
            .prop('checked', true)
            .addClass("columnNameCheckboxStyle")
            .appendTo(columnCell);
            $('<span>')
                .text(name)
                .appendTo(columnCell);

          var typeCell =  $('<td>')
          .attr('width', '150px')
          .appendTo(row);
           sel.appendTo(typeCell);

          var sizeCell =  $('<td>')
           .attr('width', '20px')
           .appendTo(row);
            $('<input>')
               .attr('type', 'text')
               .attr('size', '8px')
               .attr('id', sizeInputName)
               .addClass("sql-exporter-dialog-input")
               .appendTo(sizeCell);

            var applyAllCell =  $('<td>')
            .attr('width', '60px')
            .appendTo(row);
              $('<input>')
              .attr('type', 'button')
              .val($.i18n('core-buttons/apply-all'))
              .attr('id', applyAllBtnName)
              .attr("rowIndex", i)
              .appendTo(applyAllCell);

          var nullableCell =  $('<td>')
            .attr('width', '90px')
            .addClass("allowNullCellStyle")
            .appendTo(row);
            $('<input>')
              .attr('type', 'checkbox')
              .prop('checked', true)
              .attr('id', allowNullChkBoxName)
              .attr("rowIndex", i)
              .addClass("allowNullCheckboxStyle")
              .appendTo(nullableCell);

          var defValueCell =  $('<td>')
            .attr('width', '30px')
            .appendTo(row);
             $('<input>')
                .attr('type', 'text')
                .attr('size', '8px')
                .attr('id', defaultValueTextBoxName)
                .attr("rowIndex", i)
                .addClass("defaultValueTextBoxStyle")
                .appendTo(defValueCell);



            $('#' + applyAllBtnName).on('click', function() {
                var rowIndex = this.getAttribute('rowIndex');
                var typeValue = $("#selectBoxRow" + rowIndex).val();
                var sizeValue = $("#sizeInputRow" + rowIndex).val();

                $('select.typeSelectClass').each(function() {
                    //alert("Value:" + this.value + " RowIndex:" + rowIndex + " TypeValue:" + typeValue + "" + this.value);
                    var rowId = this.getAttribute('rowIndex');
                    var id = this.getAttribute('id');
                    if(rowIndex !== rowId){
                        $("#" + id).val(typeValue);
                    }

                });
                $('input.sql-exporter-dialog-input').each(function() {
                    var rowId = this.getAttribute('rowIndex');
                    var id = this.getAttribute('id');
                    if(rowIndex !== rowId){
                        $("#" + id).val(sizeValue);
                    }

                });

            });

            $('#' + allowNullChkBoxName).on('click', function() {
                var rowIndex = this.getAttribute('rowIndex');
                var id = this.getAttribute('id');

                if(!this.checked){
                    $('#defaultValueTextBox'+ rowIndex).prop("disabled", false);
                }else{
                    $('#defaultValueTextBox'+ rowIndex).val("");
                    $('#defaultValueTextBox'+ rowIndex).prop("disabled", true);
                }
            });

          this._columnOptionMap[name] = {
            name: name,
            type: '',
            size: ''

          };
    }

    this._elmts.allowNullToggleCheckbox.on('click',function() {
        if(this.checked){
            $("input:checkbox[class=allowNullCheckboxStyle]").each(function () {
                $(this).prop('checked', true);
            });
            $("input:text[class=defaultValueTextBoxStyle]").each(function () {
                $(this).prop('disabled', true);
            });

        }else{
            $("input:checkbox[class=allowNullCheckboxStyle]").each(function () {
                $(this).prop('checked', false);
            });
            $("input:text[class=defaultValueTextBoxStyle]").each(function () {
                $(this).prop('disabled', false);
            });
        }

    });

    this._elmts.selectAllButton.on('click',function() {
       $("input:checkbox[class=columnNameCheckboxStyle]").each(function () {
           $(this).prop('checked', true);
        });
      self._updateOptionCode();
    });
    this._elmts.deselectAllButton.on('click',function() {
        $("input:checkbox[class=columnNameCheckboxStyle]").each(function () {
           $(this).prop('checked', false);
        });
       self._updateOptionCode();
    });

    this._elmts.includeStructureCheckbox.on('click', function() {
        self._elmts.includeDropStatementCheckbox.prop("disabled", !this.checked);

        if (this.checked) {
            if (self._elmts.includeDropStatementCheckbox.prop("checked")) {
                self._elmts.includeIfExistDropStatementCheckbox.prop("disabled", !this.checked);
            }
        } else {
            self._elmts.includeIfExistDropStatementCheckbox.prop("disabled", !this.checked);
        }
    });

    this._elmts.includeDropStatementCheckbox.on('click',function () {
        self._elmts.includeIfExistDropStatementCheckbox.prop("disabled", !this.checked);
    });

    this._elmts.includeContentCheckbox.on('click',function() {
        self._elmts.nullCellValueToEmptyStringCheckbox.prop("disabled", !this.checked);
    });


    this._elmts.cancelButton.on('click',function() { self._dismiss(); });
    this._elmts.downloadButton.on('click',function() { self._download(); });
    this._elmts.downloadPreviewButton.on('click',function(evt) { self._previewDownload(); });
    this._configureUIFromOptionCode(options);
    this._updateOptionCode();
  };

  SqlExporterDialog.prototype._configureUIFromOptionCode = function(options) {

      this._elmts.tableNameTextBox.val(theProject.metadata.name.replace(/\W/g, ' ').replace(/\s+/g, '_'));
      this._elmts.sqlExportOutputEmptyRowsCheckbox.prop('checked', true);
      this._elmts.sqlExportTrimAllColumnsCheckbox.prop('checked', true);
      this._elmts.nullCellValueToEmptyStringLabel.prop('checked', true);

      $("input:text[class=defaultValueTextBoxStyle]").each(function () {
          $(this).prop("disabled", true);
       });


  };

  SqlExporterDialog.prototype._dismiss = function() {
      DialogSystem.dismissUntil(this._level - 1);
  };

  SqlExporterDialog.prototype._previewDownload = function() {
    this._postExport(true);
  };

  SqlExporterDialog.prototype._download = function() {
    var result = this._postExport(false);
   // alert("result::" + result);
    if(result == true){
        this._dismiss();
    }

  };

  SqlExporterDialog.prototype._postExport = function(preview) {
    var self = this;
    var exportAllRowsCheckbox = this._elmts.sqlExportAllRowsCheckbox[0].checked;
    var options = this._getOptionCode();

    if(options.columns == null || options.columns.length == 0){
        alert("Please select at least one column...");
        return false;
    }

    var name = ExporterManager.stripNonFileChars(theProject.metadata.name);

    var format = options.format;
    var encoding = options.encoding;

    delete options.format;
    delete options.encoding;
    if (preview) {
      options.limit = 10;
    }

  // var ext = SqlExporterDialog.formats[format].extension;
    var form = self._prepareSqlExportRowsForm(format, !exportAllRowsCheckbox, "sql");
    if (preview) {
      $(form).attr("target", "refine-export");
    }
    $('<input />')
    .attr("name", "options")
    .val(JSON.stringify(options))
    .appendTo(form);
    if (encoding) {
      $('<input />')
      .attr("name", "encoding")
      .val(encoding)
      .appendTo(form);
    }
    $('<input />')
    .attr("name", "preview")
    .val(preview)
    .appendTo(form);

    document.body.appendChild(form);

    if (preview) {
      window.open(" ", "refine-export");
    }
    form.submit();

    document.body.removeChild(form);
    return true;
  };

  SqlExporterDialog.prototype._prepareSqlExportRowsForm = function(format, includeEngine, ext) {
      var name = ExporterManager.stripNonFileChars(theProject.metadata.name);
      var form = document.createElement("form");
      $(form)
      .css("display", "none")
      .attr("method", "post")
      .attr("action", "command/core/export-rows/" + name + ((ext) ? ("." + ext) : ""));

      $('<input />')
      .attr("name", "project")
      .val(theProject.id)
      .appendTo(form);
      $('<input />')
      .attr("name", "format")
      .val(format)
      .appendTo(form);
      if (includeEngine) {
        $('<input />')
        .attr("name", "engine")
        .val(JSON.stringify(ui.browsingEngine.getJSON()))
        .appendTo(form);
      }

      return form;
   };

  SqlExporterDialog.prototype._selectColumn = function(columnName) {

    this._elmts.columnNameSpan.text(columnName);
    var columnOptions = this._columnOptionMap[columnName];
    alert("in _selectColumn:column type::" + columnOptions.type);

  };

  SqlExporterDialog.prototype._updateCurrentColumnOptions = function() {
//    var selectedColumnName = this._elmts.columnList.find('.sql-exporter-dialog-column.selected').attr('column');
//    //alert("_updateCurrentColumnOptions::" + selectedColumnName);
//    var columnOptions = this._columnOptionMap[selectedColumnName];
//    columnOptions.type= this._elmts.columnOptionPane.find('input[name="sql-exporter-type"]:checked').val();

  };

  SqlExporterDialog.prototype._updateOptionCode = function() {
   this._elmts.optionCodeInput.val(JSON.stringify(this._getOptionCode(), null, 2));
  };


  SqlExporterDialog.prototype._getOptionCode = function() {
    var options = {
      //format: this._dialog.find('input[name="sql-exporter-download-format"]:checked').val()
    };
    var unescapeJavascriptString = function(s) {
      try {
        return JSON.parse('"' + s + '"');
      } catch (e) {
        // We're not handling the case where the user doesn't escape double quotation marks.
        return s;
      }
    };

    options.format = 'sql';
    options.separator = ';';
    options.encoding = 'UTF-8';

    options.outputBlankRows = this._elmts.sqlExportOutputEmptyRowsCheckbox[0].checked;
    options.includeStructure = this._elmts.includeStructureCheckbox[0].checked;
    options.includeDropStatement = this._elmts.includeDropStatementCheckbox[0].checked;
    options.includeContent = this._elmts.includeContentCheckbox[0].checked;
    options.tableName = jQueryTrim(this._elmts.tableNameTextBox.val().replace(/\W/g, ' ')).replace(/\s+/g, '_');
    options.trimColumnNames = this._elmts.sqlExportTrimAllColumnsCheckbox[0].checked;

    options.convertNulltoEmptyString = this._elmts.nullCellValueToEmptyStringCheckbox[0].checked;
    options.includeIfExistWithDropStatement = this._elmts.includeIfExistDropStatementCheckbox[0].checked;


    options.columns = [];

    var self = this;
    this._elmts.columnListTable.find('.sql-exporter-dialog-row').each(function() {
      if ($(this).find('input[type="checkbox"]')[0].checked) {
        // TODO: Ideally we'd like to let the user preview the names in their cleaned SQL-legal form
        var name = this.getAttribute('column');
        var rowIndex = this.getAttribute('rowIndex');

        var selectedValue = $('#selectBoxRow' + rowIndex).val();

        var typeSize = 0;
        if(selectedValue === 'VARCHAR' || selectedValue === 'CHAR' || selectedValue === 'INT' || selectedValue === 'NUMERIC'){
            typeSize = $('#sizeInputRow' + rowIndex).val();
           // alert("typeSize::" + typeSize);
        }

        var allowNullChkBoxName = $('#allowNullChkBox' + rowIndex).prop('checked');
        var defaultValueTextBoxName = $('#defaultValueTextBox' + rowIndex).val();
       // alert("allowNullChkBoxName::" + allowNullChkBoxName);
       // alert("defaultValueTextBoxName::" + defaultValueTextBoxName);

        var fullColumnOptions = self._columnOptionMap[name];
        var columnOptions = {
          name: name,
          type: selectedValue,
          size: typeSize,
          allowNull: allowNullChkBoxName,
          defaultValue: defaultValueTextBoxName,
          nullValueToEmptyStr: options.convertNulltoEmptyString

        };

      //  alert('checked type ' + columnIndex + ' =' + check_value);

        options.columns.push(columnOptions);
      }
    });
    //alert('options:' + options);
    return options;
  };

