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
function ManagementDialog(title, column) {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.css("min-width", "700px");
    var header = $('<div></div>').addClass("dialog-header").text(title).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").css('justify-content', 'space-between').appendTo(frame);

    var html = $('<div class="functions-container" bind="functionsContainer"></div>').appendTo(body);
    this._elmts = DOM.bind(html);

    $('<button class="button"></button>').text($.i18n('core-buttons/add-function')).on('click', function() {
        ManagementDialog._addFunction(column);
    }).appendTo(footer);

    $('<button class="button"></button>').html($.i18n('core-buttons/ok')).on('click', function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);

    this._level = DialogSystem.showDialog(frame);
    this._manageWidget = new ManagementDialog.Widget(this._elmts);
}

ManagementDialog._addFunction = function(column) {
    var frame = $(
        DOM.loadHTML("core", "scripts/dialogs/add-function-dialog.html")
        .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));

    var elmts = DOM.bind(frame);
    elmts.dialogHeader.text($.i18n('core-dialogs/add-function'));

    elmts.newFunctionName.text($.i18n('core-dialogs/new-function-name'));
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
        var columnName = jQueryTrim(elmts.functionNameInput[0].value);
        if (!columnName.length) {
          alert($.i18n('core-views/warning-function-name'));
          return;
        }
  
        // post http request
        dismiss();
    });
};

ManagementDialog.Widget = function(elmts) {
    this._elmts = elmts;

    var container = this._elmts.functionsContainer.empty();
    
    var table = $('<table></table>').addClass("manage-functions-table").appendTo(container)[0];
        
    var tr = table.insertRow(0);
    $(tr.insertCell(0)).addClass("manage-functions-heading").text("Name");
    $(tr.insertCell(1)).addClass("manage-functions-heading").text("Action");
    
    // It's currently designed for simplicity but will be handled by the backend in the future.
    var data = [
        { name: "John Doe" },
        { name: "Jane Smith" }
    ];

    for (var i = 0; i < data.length; i++) {
        var newRow = $('<tr>');
        $('<td>').text(data[i].name).appendTo(newRow);
        var actionsCell = $('<td>').appendTo(newRow);
        $('<button>').text("Edit").on('click', this._editFunction()).appendTo(actionsCell);
        $('<button>').text("Remove").on('click', this._deleteFunction()).appendTo(actionsCell);
        newRow.appendTo(table);
    }
    
};

ManagementDialog.Widget.prototype._deleteFunction = function(name, expression) {
   // post http request
};

ManagementDialog.Widget.prototype._editFunction = function(name, expression) {
   // post http request
};
