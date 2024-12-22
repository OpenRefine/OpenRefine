/*

Copyright 2024, OpenRefine.
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

function RemoveDuplicateRowsDialog() {
    this._createDialog();
}

RemoveDuplicateRowsDialog.prototype._createDialog = function() {
    var self = this;
    var dialog = $(DOM.loadHTML("core", "scripts/dialogs/remove-duplicate-rows-dialog.html"));
    this._elmts = DOM.bind(dialog);

    this._elmts.cancelButton.on('click',function() { self._dismiss(); });
    this._elmts.okButton.on('click',function() { self._commit(); });
    this._elmts.selectAllButton.on('click',function() { self._columnSelectToggle(true); });
    this._elmts.deselectAllButton.on('click',function() { self._columnSelectToggle(false); });
    
    this._elmts.dialogHeader.html($.i18n('core-dialogs/remove-duplicate-rows'));
    this._elmts.or_dialog_columnsList.html($.i18n('core-dialogs/remove-duplicate-rows-col-select'));
    this._elmts.okButton.html($.i18n('core-buttons/ok'));
    this._elmts.cancelButton.html($.i18n('core-buttons/cancel'));
    this._elmts.selectAllButton.html($.i18n('core-buttons/select-all'));
    this._elmts.deselectAllButton.html($.i18n('core-buttons/deselect-all'));
    
    this._level = DialogSystem.showDialog(dialog);
    var columnList = this._elmts.columnContainer;

    for (var i = 0; i < theProject.columnModel.columns.length; i++) {
        var column = theProject.columnModel.columns[i];
        var name = column.name;
        var columnCheckbox = $('<div></div>');
        var checkbox = $('<input>', {
            type: 'checkbox',
            class: 'column-checkbox',
            id: 'column-' + i,
            value: name,
            checked: true
        });
        var label = $('<label>', {
            for: 'column-' + i,
        }).text(name);
        columnCheckbox.append(checkbox).append(label);
        columnList.append(columnCheckbox);
    }

    $('#select-all').on('change', function () {
        var isChecked = $(this).is(':checked');
        $('.column-checkbox').prop('checked', isChecked);
    });
};

RemoveDuplicateRowsDialog.prototype._columnSelectToggle = function(value) {
        $('.column-checkbox').prop('checked', value);
}

RemoveDuplicateRowsDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

RemoveDuplicateRowsDialog.prototype._commit = function() {
    var self = this;
    var selectedColumns = $('.column-checkbox:checked').map(function () {
        return $(this).val();
    }).get();

    if ( selectedColumns.length <= 0 ) {
        window.alert($.i18n('core-buttons/no-criteria-selected'));
        return;
    }

    Refine.postCoreProcess(
        "remove-duplicate-rows",
        null,
        { "criteria" : JSON.stringify(selectedColumns) },
        {
          includeEngine: true,
          modelsChanged: true
        },
        {
          "onDone": function() { self._dismiss(); },
          "onError": (o) => window.alert(`Error: ${o.message}`)
        }
    );
};
