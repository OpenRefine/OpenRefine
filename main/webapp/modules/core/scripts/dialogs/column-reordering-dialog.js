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

function ColumnReorderingDialog() {
    this._createDialog();
}

ColumnReorderingDialog.prototype._createDialog = function() {
    var self = this;
    var dialog = $(DOM.loadHTML("core", "scripts/dialogs/column-reordering-dialog.html"));
    this._elmts = DOM.bind(dialog);

    this._elmts.removeAllButton.on('click',function() { self._removeAll(); });
    this._elmts.addAllButton.on('click',function() { self._addAll() });
    this._elmts.cancelButton.on('click',function() { self._dismiss(); });
    this._elmts.okButton.on('click',function() { self._commit(); });
    
    this._elmts.dialogHeader.html($.i18n('core-dialogs/reorder-column'));
    this._elmts.or_dialog_dragCol.html($.i18n('core-dialogs/drag-column'));
    this._elmts.or_dialog_dropCol.html($.i18n('core-dialogs/drop-column'));
    this._elmts.addAllButton.html($.i18n('core-dialogs/add-all'));
    this._elmts.removeAllButton.html($.i18n('core-dialogs/remove-all'));
    this._elmts.okButton.html($.i18n('core-buttons/ok'));
    this._elmts.cancelButton.html($.i18n('core-buttons/cancel'));
    
    this._level = DialogSystem.showDialog(dialog);
    
    for (var i = 0; i < theProject.columnModel.columns.length; i++) {
        var column = theProject.columnModel.columns[i];
        var name = column.name;
        
        $('<div>')
            .addClass("column-reordering-dialog-column")
            .text(name)
            .attr("column", name)
            .appendTo(this._elmts.columnContainer);
    }
    
    dialog.find('.column-reordering-dialog-column-container')
        .sortable({
            connectWith: '.column-reordering-dialog-column-container'
        })
        .disableSelection();
};

ColumnReorderingDialog.prototype._removeAll = function() {
    this._elmts.columnContainer.children().appendTo(this._elmts.trashContainer);
};

ColumnReorderingDialog.prototype._addAll = function() {
    this._elmts.trashContainer.children().appendTo(this._elmts.columnContainer);
};

ColumnReorderingDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

ColumnReorderingDialog.prototype._commit = function() {
    var columnNames = this._elmts.columnContainer.find('div').map(function() { return this.getAttribute("column"); }).get();
    
    Refine.postCoreProcess(
        "reorder-columns",
        null,
        { "columnNames" : JSON.stringify(columnNames) }, 
        { modelsChanged: true },
        { includeEngine: false }
    );
    
    this._dismiss();
};
