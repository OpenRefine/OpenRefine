function ProcessWidget(div) {
    this._div = div;
    this._timerID = null;
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
    
    var bodyDiv = $('<div></div>').addClass("process-panel-inner").text("Testing").appendTo(this._div);
    if (this._data.processes.length == 0) {
        this._div.hide();
        return;
    }
    
    this._div.show();
    
    var hasPending = false;
    var renderProcess = function(process) {
        var div = $('<div></div>').addClass("process-panel-entry").appendTo(bodyDiv);
        
        if (process.status == "pending") {
            div.text(process.description + " (pending)");
        } else {
            div.text(process.description + " (" + process.progress + "%)");
            hasPending = true;
        }
    };
    
    var processes = this._data.processes;
    for (var i = 0; i < processes.length; i++) {
        renderProcess(processes[i]);
    }
    
    if (hasPending && this._timerID == null) {
        this._timerID = window.setTimeout(function() {
            self._timerID = null;
            self.update();
        }, 500);
    }
};
