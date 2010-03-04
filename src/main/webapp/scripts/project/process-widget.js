function ProcessWidget(div) {
    this._div = div;
    this._timerID = null;
    this._processCount = 0;
    
    this._updateOptions = {};
    this._onDones = [];
    
    this.update({});
}

ProcessWidget.prototype.update = function(updateOptions, onDone) {
    for (var n in updateOptions) {
        if (updateOptions.hasOwnProperty(n)) {
            this._updateOptions[n] = updateOptions[n];
        }
    }
    if (onDone) {
        this._onDones.push(onDone);
    }
    
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

ProcessWidget.prototype._cancelAll = function() {
    $.post(
        "/command/cancel-processes?" + $.param({ project: theProject.id }), 
        null,
        function(o) {},
        "json"
    );
};

ProcessWidget.prototype._render = function() {
    var self = this;
    
    this._div.empty();
    
    if (this._data.processes.length == 0) {
        this._div.hide();
    } else {
        this._div.show();
        
        var innerDiv = $('<div></div>').addClass("process-panel-inner").appendTo(this._div);
        
        var headDiv = $('<div></div>').addClass("process-panel-head").appendTo(innerDiv);
        $('<img src="images/small-spinner.gif" />')
            .css("margin-right", "3px")
            .css("opacity", "0.3")
            .appendTo(headDiv);
        $('<a href="javascript:{}"></a>')
            .addClass("action")
            .text("cancel all")
            .click(function() {
                self._cancelAll();
                $(this).text("canceling all processes...").unbind();
            })
            .appendTo(headDiv);
            
        var bodyDiv = $('<div></div>').addClass("process-panel-body").appendTo(innerDiv);
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
    }
    
    if (this._data.processes.length > 0 && this._timerID == null) {
        this._timerID = window.setTimeout(function() {
            self._timerID = null;
            self.update();
        }, 500);
    } else {
    
        var updateOptions = this._updateOptions;
        var onDones = this._onDones;
        
        this._updateOptions = {};
        this._onDones = [];
        
        Gridworks.update(updateOptions, function() {
            for (var i = 0; i < onDones.length; i++) {
                try {
                    onDones[i]();
                } catch (e) {
                    Gridworks.reportException(e);
                }
            }
        });
    }
};
