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

function ReconDialog(column, types) {
  this._column = column;
  this._serviceRecords = [];
  this._selectedServiceRecordIndex = -1;

  this._createDialog();
}

ReconDialog.prototype._createDialog = function() {
  var self = this;
  var dialog = $(DOM.loadHTML("core", "scripts/reconciliation/recon-dialog.html"));

  this._elmts = DOM.bind(dialog);
  this._elmts.dialogHeader.text($.i18n._('core-recon')["recon-col"]+' "' + this._column.name + '"');
  
  this._elmts.servicePanelMessage.html($.i18n._('core-recon')["pick-service"]);
  this._elmts.serviceListTitle.html($.i18n._('core-recon')["service-title"]);
  this._elmts.addStandardServiceButton.html($.i18n._('core-buttons')["add-std-svc"]+"...");
  this._elmts.reconcileButton.html($.i18n._('core-buttons')["start-recon"]);
  this._elmts.cancelButton.html($.i18n._('core-buttons')["cancel"]);

  this._elmts.addStandardServiceButton.click(function() { self._onAddStandardService(); });

  this._elmts.reconcileButton.click(function() { self._onOK(); });
  this._elmts.cancelButton.click(function() { self._dismiss(); });

  this._level = DialogSystem.showDialog(dialog);
  this._populateDialog();
};

ReconDialog.prototype._onOK = function() {
  if (this._selectedServiceRecordIndex >= 0) {
    var record = this._serviceRecords[this._selectedServiceRecordIndex];
    if (record.handler) {
      record.handler.start();
    }
  }
  this._dismiss();
};

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

ReconDialog.prototype._populateDialog = function() {
  var self = this;

  var services = ReconciliationManager.getAllServices();
  if (services.length > 0) {
    var renderService = function(service) {
      var record = {
          service: service,
          handler: null
      };

      record.selector = $('<a>')
      .attr("href", "javascript:{}")
      .addClass("recon-dialog-service-selector")
      .text(service.name)
      .appendTo(self._elmts.serviceList)
      .click(function() {
    	self._toggleServices();
        self._selectService(record);
      });

      $('<a>')
      .html("&nbsp;")
      .addClass("recon-dialog-service-selector-remove")
      .prependTo(record.selector)
      .click(function() {
        ReconciliationManager.unregisterService(service, function() {
          self._refresh(-1);
        });
      });

      self._serviceRecords.push(record);
    };

    for (var i = 0; i < services.length; i++) {
      renderService(services[i]);
    }
    

    $('.recon-dialog-service-opener').click(function() {
      self._toggleServices();
    });
  }
};

ReconDialog.prototype._toggleServices = function() {
  var self = this;
  self._toggleServiceTitle(500);
  self._toggleServiceList(500);
};

ReconDialog.prototype._toggleServiceTitle = function(duration) {
  var title = $('.recon-dialog-service-opener-title');
  title.animate({
	width : 'toggle'
	}, duration, 'swing', function() {
  });
};

ReconDialog.prototype._toggleServiceList = function(duration) {
  $(".recon-dialog-service-list").toggle("slide", duration);
};

ReconDialog.prototype._selectService = function(record) {
  for (var i = 0; i < this._serviceRecords.length; i++) {
    if (record === this._serviceRecords[i]) {
      if (i !== this._selectedServiceRecordIndex) {
        if (this._selectedServiceRecordIndex >= 0) {
          var oldRecord = this._serviceRecords[this._selectedServiceRecordIndex];
          if (oldRecord.handler) {
            oldRecord.selector.removeClass("selected");
            oldRecord.handler.deactivate();
          }
        }

        this._elmts.servicePanelMessage.hide();

        record.selector.addClass("selected");
        if (record.handler) {
          record.handler.activate();
        } else {
          var handlerConstructor = eval(record.service.ui.handler);

          record.handler = new handlerConstructor(
              this._column, record.service, this._elmts.servicePanelContainer);
        }

        this._selectedServiceRecordIndex = i;
        return;
      }
    }
  }
};

ReconDialog.prototype._refresh = function(newSelectIndex) {
  this._cleanDialog();
  this._populateDialog();
  if (newSelectIndex >= 0) {
    this._selectService(this._serviceRecords[newSelectIndex]);
  }
};

ReconDialog.prototype._onAddStandardService = function() {
  var self = this;
  var dialog = $(DOM.loadHTML("core", "scripts/reconciliation/add-standard-service-dialog.html"));
  var elmts = DOM.bind(dialog);

  elmts.dialogHeader.html($.i18n._('core-recon')["add-std-srv"]);
  elmts.or_recon_enterUrl.html($.i18n._('core-recon')["enter-url"]+":");
  elmts.addButton.html($.i18n._('core-buttons')["add-service"]);
  elmts.cancelButton.html($.i18n._('core-buttons')["cancel"]);
  
  var level = DialogSystem.showDialog(dialog);
  var dismiss = function() {
    DialogSystem.dismissUntil(level - 1);
  };

  elmts.cancelButton.click(dismiss);
  elmts.addButton.click(function() {
    var url = $.trim(elmts.input[0].value);
    if (url.length > 0) {
      ReconciliationManager.registerStandardService(url, function(index) {
        self._refresh(index);
      });
    }
    dismiss();
  });
  elmts.input.focus().select();
};


