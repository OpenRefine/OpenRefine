/*

Copyright 2024, OpenRefine contributors
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

function ColumnMappingDialog(operations, analyzedOperations) {
  var self = this;
  var frame = $(DOM.loadHTML("core", "scripts/dialogs/column-mapping-dialog.html"));
  var elmts = DOM.bind(frame);

  var columnDependencies = analyzedOperations.dependencies;
  var newColumns = analyzedOperations.newColumns;
  
  elmts.dialogHeader.text($.i18n('core-project/map-columns'));

  elmts.applyButton.val($.i18n('core-buttons/perform-op'));
  elmts.backButton.text($.i18n('core-buttons/previous'));

  var trHeader = $('<tr></tr>')
    .append($('<th></th>').text($.i18n('core-project/columns-in-the-recipe')))    
    .append($('<th></th>').text($.i18n('core-project/columns-in-the-project')))
  if (columnDependencies.length > 0) {
    elmts.dependenciesExplanation.text($.i18n('core-project/recipe-required-columns'));
    trHeader.clone()
      .appendTo(elmts.dependenciesTableHead);
  }
  if (newColumns.length > 0) {
    elmts.newColumnsExplanation.text($.i18n('core-project/recipe-created-columns'));
    trHeader
      .appendTo(elmts.newColumnsTableHead);
  }

  let columnExists = function(columnName) {
    return theProject.columnModel.columns.find(column => column.name === columnName) !== undefined;
  };

  var idx = 0;
  for (const columnName of columnDependencies) {
    var name = `column_${idx}`;
    var defaultValue = columnName;
    if (!columnExists(columnName)) {
      defaultValue = '';
    }
    let select = $('<select></select>')
      .attr('value', defaultValue)
      .data('originalName', columnName)
      .attr('name', name);
    if (defaultValue === '') {
      $('<option></option>')
        .attr('value', '')
        .attr('selected', 'true')
        .attr('disabled', 'true')
        .css('display', 'none')
        .appendTo(select);
    }
    for (const existingColumn of theProject.columnModel.columns) {
      let option = $('<option></option>')
        .attr('value', existingColumn.name)
        .text(existingColumn.name)
        .appendTo(select);
      if (existingColumn.name === defaultValue) {
        option.attr('selected', 'true');
      }
    }
    $('<tr></tr>')
      .append(
        $('<td></td>').append(
         $('<label></label>').attr("for", name).text(columnName))
      ).append(
        $('<td></td>').append(select)
      )
      .appendTo(elmts.dependenciesTableBody);
    idx++;
  }
  for (const columnName of newColumns) {
    var name = `column_${idx}`;
    var defaultValue = columnName;
    if (columnExists(columnName)) {
      defaultValue = '';
    }
    var tr = $('<tr></tr>')
      .append(
        $('<td></td>').append(
         $('<label></label>').attr("for", name).text(columnName))
      ).append(
        $('<td></td>').append(
          $('<input type="text" />')
             .attr('value', defaultValue)
             .data('originalName', columnName)
             .data('expectedToExist', false)
             .attr('name', name))
      );
    tr.appendTo(elmts.newColumnsTableBody);
    idx++;
  }

  var level = null;

  let runOperations = function(renames) {
      Refine.postCoreProcess(
        "apply-operations",
        {},
        {
          operations: JSON.stringify(operations),
          renames: JSON.stringify(renames)
        },
        { everythingChanged: true },
        {
          onDone: function(o) {
            if (o.code == "pending") {
              // Something might have already been done and so it's good to update
              Refine.update({ everythingChanged: true });
            }
            if (level !== null) {
              DialogSystem.dismissUntil(level - 1);
            }
          },
          onError: function(e) {
            elmts.errorContainer.text($.i18n('core-project/json-invalid', e.message));
          },
        }
    );
  }

  if (columnDependencies.length == 0 && newColumns.length == 0) {
    runOperations({});
    return;
  }

  level = DialogSystem.showDialog(frame);

  elmts.backButton.on('click',function() {
    DialogSystem.dismissUntil(level - 1);
    new ApplyOperationsDialog();
  });

  elmts.form.on('submit',function(e) {
    e.preventDefault();
    // collect the column mapping from the form
    var renames = {};
    var errorFound = false;

    elmts.columnMap.find('select').each(function(index, child) {
      let inputElem = $(child);
      let fromColumn = inputElem.data('originalName');
      let toColumn = inputElem.val();
      if (toColumn !== null && !columnExists(toColumn)) {
        errorFound = true;
        inputElem.addClass('invalid');
      } else {
        // still include nulls so we can filter out unused columns in the backend
        renames[fromColumn] = toColumn;
      }
    });
    elmts.columnMap.find('input').each(function(index, child) {
      let inputElem = $(child);
      let fromColumn = inputElem.data('originalName');
      let toColumn = inputElem.val();
      if (toColumn !== null && columnExists(toColumn)) {
        errorFound = true;
        inputElem.addClass('invalid');
        alert($.i18n('core-project/error-created-column', toColumn));
      } else {
        // still include nulls so we can filter out unused columns in the backend
        renames[fromColumn] = toColumn;
      }
    });
    
    if (!errorFound) {
      runOperations(renames);
    }
  });
}


