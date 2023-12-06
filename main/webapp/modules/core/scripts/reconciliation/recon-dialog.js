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

function ReconDialog(column, previousServiceURL) {
  this._column = column;
  this._serviceRecords = [];
  this._selectedServiceRecordIndex = -1;
  this._record = null;
  this.previousRecordURL = previousServiceURL;

  this._createDialog();
}

ReconDialog.prototype._createDialog = function() {
  var self = this;
  var dialog = $(DOM.loadHTML("core", "scripts/reconciliation/recon-dialog.html"));

  this._elmts = DOM.bind(dialog);
  this._elmts.dialogHeader.text($.i18n('core-recon/recon-col',this._column.name));

  this._elmts.servicePanelMessage.html($.i18n('core-recon/pick-service'));
  this._elmts.addStandardServiceButton.html($.i18n('core-buttons/add-std-svc')+"...");
  this._elmts.cancelButton.html($.i18n('core-buttons/cancel'));
  this._elmts.discoverServicesButton.html($.i18n('core-buttons/discover-services'));
  this._elmts.nextButton.html($.i18n('core-buttons/next'));
  this._elmts.popUpMessage.html($.i18n('core-recon/disable-next-button'));

  this._elmts.addStandardServiceButton.on('click',function() { self._onAddStandardService(); });

  this._elmts.cancelButton.on('click',function() { self._dismiss(); });
  this._elmts.nextButton.on('click',function() { 
    if(self._record){
    self._nextDialog(self._column, self._selectedServiceRecordIndex, self._serviceRecords, self._record, self.previousRecord);
    }
    else{
      var message = document.getElementById('popup-message');
        message.classList.add('show');
        setTimeout(function() {
            message.classList.remove('show');
        }, 1000); 

    }
   });

  this._level = DialogSystem.showDialog(dialog);
  this._populateDialog(self.previousRecordURL);

};


ReconDialog.prototype._nextDialog = function(column, selectedServiceRecordIndex, serviceRecords, record, previousService) {
  this._dismiss();
  new ReconDialog2(column, selectedServiceRecordIndex, serviceRecords, record, previousService);
}

ReconDialog.prototype._dismiss = function() {
  for (var i = 0; i < this._serviceRecords.length; i++) {
    var record = this._serviceRecords[i];
    if (record.handler) {
      record.handler.dispose();
    }
  }
  this._serviceRecords = null;

  DialogSystem.dismissUntil(this._level - 1);
};

ReconDialog.prototype._cleanDialog = function() {
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

ReconDialog.prototype._populateDialog = function(selectedURL) {
  var self = this;
  self._elmts.serviceList.empty();

  var services = ReconciliationManager.getAllServices();
  if (services.length > 0) {
    
    var renderService = function(service) {
      var record = {
          service: service,
          handler: null
      };

      var label = $('<label>')
        .addClass('recon-dialog-service-list-element')
        .appendTo(self._elmts.serviceList);

        
      record.selector = $('<input type="radio" name="service-choice">')
        .addClass("recon-dialog-service-selector")
        .val(service.name) 
        .appendTo(label);

      if(selectedURL === service.url) {
        record.selector.prop('checked',true);
        self._record=record;
      }

      var mainSpan=$('<span>')
        .addClass("recon-service-entry")
        .appendTo(label);

      var divElement=$('<div>')
        .text(service.name)
        .appendTo(mainSpan);

      var divElementURL = $('<div>')
        .addClass("recon-dialog-url-element")
        .text(service.url)
        .appendTo(mainSpan);
      
       label.on('click', function() {
       self._record = record;
      
});

      $('<a>')
      .html("&nbsp;")
      .addClass("recon-dialog-service-selector-remove")
      .prependTo(label)
      .on('click',function(event) {
        $(this).parents('label').remove();
        ReconciliationManager.unregisterService(service, function() {
          self._record = null;
        });

        event.stopImmediatePropagation();
      });

      self._serviceRecords.push(record);
      
    };

    for (var i = 0; i < services.length; i++) {
      renderService(services[i]);
    }
    
  }
};


ReconDialog.prototype._refresh = function(selectedURL) {
  this._cleanDialog();
  this._populateDialog(selectedURL);
};

ReconDialog.prototype._onAddStandardService = function() {
  var self = this;
  var dialog = $(DOM.loadHTML("core", "scripts/reconciliation/add-standard-service-dialog.html"));
  var elmts = DOM.bind(dialog);

  elmts.dialogHeader.html($.i18n('core-recon/add-std-srv'));
  elmts.or_recon_enterUrl.html($.i18n('core-recon/enter-url'));
  elmts.addButton.html($.i18n('core-buttons/add-service'));
  elmts.cancelButton.html($.i18n('core-buttons/cancel'));
  
  var level = DialogSystem.showDialog(dialog);
  var dismiss = function() {
    DialogSystem.dismissUntil(level - 1);
  };

  elmts.cancelButton.on('click',dismiss);
  elmts.form.on('submit',function() {
    var url = jQueryTrim(elmts.input[0].value);
    if (url.length > 0) {
      ReconciliationManager.registerStandardService(url, function(index) {
        self._refresh(url);
      });
    }
    dismiss();
  });
  elmts.input.trigger('focus').trigger('select');
};


