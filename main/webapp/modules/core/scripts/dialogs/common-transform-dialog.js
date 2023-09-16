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

function commonTransformDialog(expression,label) {
    this._createDialog(expression,label);
    
}

commonTransformDialog.prototype._createDialog = function(expression,label) {
    var self = this;
    var dialog = $(DOM.loadHTML("core", "scripts/dialogs/common-transform-dialog.html"));
    this._elmts = DOM.bind(dialog);
    this._elmts.dialogHeader.html($.i18n(label));
    this._elmts.selectAllButton.html($.i18n('core-buttons/select-all'));
    this._elmts.deselectAllButton.html($.i18n('core-buttons/deselect-all'));
    this._elmts.okButton.html($.i18n('core-buttons/ok'));
    this._elmts.cancelButton.html($.i18n('core-buttons/cancel'));
    
    this._level = DialogSystem.showDialog(dialog);
    var container = this._elmts.columnContainer;
    
    for (var i = 0; i < theProject.columnModel.columns.length; i++) {
        var col = theProject.columnModel.columns[i];
        var colName = col.name;
        var columnIndex = Refine.columnNameToColumnIndex(col.name);
        var div = $('<div>').
          addClass("all-column-transform-dialog-container")
          .attr("column", colName)
          .appendTo(this._elmts.columnContainer);
        $('<input>').
          attr('type', 'checkbox')
          .attr("column", colName)
          .prop('checked',(i == columnIndex) ? true :false)
          .appendTo(div);
        $('<span>')
          .text(colName)
          .appendTo(div);
      }
      
      this._elmts.columnContainer
      .find('.all-column-transform-dialog-container')
      .on('click',function() {
        container
        .find('.all-column-transform-dialog-container')
        .removeClass('selected');
        $(this).addClass('selected');
      });
      this._elmts.selectAllButton
      .on('click',function() {
        container
        .find('input[type="checkbox"]')
        .prop('checked',true);
       });
       this._elmts.deselectAllButton
      .on('click',function() {
        container
        .find('input[type="checkbox"]')
        .prop('checked',false);
      });

    this._elmts.okButton.on('click',function() {
      self._commit(expression);
      self._dismiss();
    });
    this._elmts.cancelButton.on('click',function() {
      self._dismiss();
    });
  

};


commonTransformDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};


commonTransformDialog.prototype._commit = function(expression) {
      var doTextTransform = function(columnName, expression, onError, repeat, repeatCount) {
      Refine.postCoreProcess(
        "text-transform",
        {
          columnName: columnName, 
          expression: expression, 
          onError: onError,
          repeat: repeat,
          repeatCount: repeatCount
        },
        null,
        { cellsChanged: true }
      );
  };

	this._elmts.columnContainer.find('div').each(function() {
    if ($(this).find('input[type="checkbox"]')[0].checked) {
      var name = this.getAttribute('column');
	    doTextTransform(name,expression, "keep-original", false, "");
    }
  });
  
    
    this._dismiss();
    
};
