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
    this._elmts.dialogHeader.text("Reconcile column " + this._column.name);
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
                    self._selectService(record);
                });
            
            self._serviceRecords.push(record);
        };
    
        for (var i = 0; i < services.length; i++) {
            renderService(services[i]);
        }
    
        this._selectService(this._serviceRecords[0]);
    }
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
