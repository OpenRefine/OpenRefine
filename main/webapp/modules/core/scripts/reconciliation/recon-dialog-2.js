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

function ReconDialog2(column, selectedServiceRecordindex, serviceRecords, record, previousService) {
  this._column = column;
  this._serviceRecords = serviceRecords;
  this._selectedServiceRecordIndex = selectedServiceRecordindex;
  this._previousService = previousService;
  this._record = record;

  this._createDialog();
}

ReconDialog2.prototype._createDialog = function() {
  var self = this;
  var dialog = $(DOM.loadHTML("core", "scripts/reconciliation/recon-dialog-2.html"));

  this._elmts = DOM.bind(dialog);
  this._elmts.dialogHeader.text($.i18n('core-recon/recon-col',this._column.name ));
  this._elmts.reconcileButton.html($.i18n('core-buttons/start-recon'));
  this._elmts.cancelButton.html($.i18n('core-buttons/cancel'));
  this._elmts.backButton.html($.i18n('core-buttons/previous'));

  this._elmts.reconcileButton.on('click',function() { self._onOK(); });
  this._elmts.cancelButton.on('click',function() { self._dismiss(); });
  this._elmts.backButton.on('click',function() { self._previousDialog(self._column, self._previousService); });


  this._level = DialogSystem.showDialog(dialog);
  this._selectService(self._record);
};


ReconDialog2.prototype._previousDialog = function(column, previousService) {
  this._dismiss();
  new ReconDialog(column, previousService);
}


ReconDialog2.prototype._onOK = function() {

  if (this._selectedServiceRecordIndex >= 0) {
    var record = this._serviceRecords[this._selectedServiceRecordIndex];
    if (record.handler) {
      record.handler.start();
    }
  }
  this._dismiss();
};


ReconDialog2.prototype._dismiss = function() {
  for (var i = 0; i < this._serviceRecords.length; i++) {
    var record = this._serviceRecords[i];
    if (record.handler) {
      record.handler.dispose();
    }
  }
  this._serviceRecords = null;

  DialogSystem.dismissUntil(this._level - 1);
};


ReconDialog2.prototype._cleanDialog = function() {
  for (var i = 0; i < this._serviceRecords.length; i++) {
    var record = this._serviceRecords[i];
    if (record.handler) {
      record.handler.deactivate();
    }
    record.selector.remove();
  }
  this._serviceRecords = [];
  this._selectedServiceRecordIndex = -1;
};


ReconDialog2.prototype._selectService = function(record) {
  for (var i = 0; i < this._serviceRecords.length; i++) {
    if (record === this._serviceRecords[i]) {
       {  this._selectedServiceRecordIndex = i;
          if (record.handler) {
            record.handler.activate();
          } else {
          var handlerConstructor = eval(record.service.ui.handler);
          record.handler = new handlerConstructor(
              this._column, record.service, this._elmts.servicePanelContainer);
        }  
        return;
    }
  };   
};
}

ReconDialog2.prototype._refresh = function(newSelectIndex) {
  this._cleanDialog();
  this._populateDialog();
};
