function ProcessWidget(div) {
    this._div = div;
    this._timerID = null;
    this._processCount = 0;
    this.update();
}

ProcessWidget.prototype.update = function() {
    if (this._timerID != null) {
        return;
    }
    
    var self = this;
    Ajax.chainGetJSON(
        "/command/get-processes?" + $.param({ project: theProject.id }), null,
        function(data) {
            self._data = data;
            self._render();
        }
    );
};

ProcessWidget.prototype._render = function() {
    var self = this;
    
    this._div.empty();
    
    var bodyDiv = $('<div></div>').addClass("process-panel-inner").appendTo(this._div);
    if (this._data.processes.length == 0) {
        this._div.hide();
        
        if (this._processCount > 0) {
            this._processCount = 0;
            
            ui.historyWidget.update();
            ui.dataTableView.update(true);
        }
        return;
    }
    this._processCount = this._data.processes.length;
    
    this._div.show();
    
    var renderProcess = function(process) {
        var div = $('<div></div>').addClass("process-panel-entry").appendTo(bodyDiv);
        
        if (process.status == "pending") {
            div.text(process.description + " (pending)");
        } else {
            div.text(process.description + " (" + process.progress + "%)");
        }
    };
    
    var processes = this._data.processes;
    for (var i = 0; i < processes.length; i++) {
        renderProcess(processes[i]);
    }
    
    if (processes.length > 0 && this._timerID == null) {
        this._timerID = window.setTimeout(function() {
            self._timerID = null;
            self.update();
        }, 500);
    }
};
