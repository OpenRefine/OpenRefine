function ProcessWidget(div) {
    this._div = div;
    this._timerID = null;
    this._processCount = 0;
    
    this._updateOptions = {};
    this._onDones = [];
    this._latestHistoryEntry = null;
    
    var self = this;
    $(window).keypress(function(evt) {
        if (evt.ctrlKey || evt.metaKey) {
            var t = evt.target;
            if (t) {
                var tagName = t.tagName.toLowerCase();
                if (tagName == "textarea" || tagName == "input") {
                    return;
                }
            }
            self.undo();
        }
    });
    
    this.update({});
}

ProcessWidget.prototype.resize = function() {
};

ProcessWidget.prototype.update = function(updateOptions, onDone) {
    this._latestHistoryEntry = null;
    
    for (var n in updateOptions) {
        if (updateOptions.hasOwnProperty(n)) {
            this._updateOptions[n] = updateOptions[n];
        }
    }
    if (onDone) {
        this._onDones.push(onDone);
    }
    
    if (this._timerID !== null) {
        return;
    }
    
    var self = this;
    Ajax.chainGetJSON(
        "/command/get-processes?" + $.param({ project: theProject.id }), null,
        function(data) {
            self._latestHistoryEntry = null;
            self._render(data);
        }
    );
};

ProcessWidget.prototype.showUndo = function(historyEntry) {
    var self = this;
    
    this._latestHistoryEntry = historyEntry;

    this._div.empty().html(
        '<div class="process-panel-inner"><div class="process-panel-undo">' +
            '<a href="javascript:{}" bind="undo">Undo</a> <span bind="description"></span>' +
        '</div><div style="text-align: right; padding-right: 1em;"><a href="javascript:{}" bind="close">close</a></div></div>'
    ).slideDown().delay(5000).slideUp(); ;
    var elmts = DOM.bind(this._div);
        
    elmts.description.text(historyEntry.description);
    elmts.undo.click(function() { self.undo(); });
    elmts.close.click(function() { $(".process-panel-inner").slideUp(); });
};

ProcessWidget.prototype.undo = function() {
    if (this._latestHistoryEntry !== null) {
        Gridworks.postProcess(
            "undo-redo",
            { undoID: this._latestHistoryEntry.id },
            null,
            { everythingChanged: true }
        );
    }
};

ProcessWidget.prototype._cancelAll = function() {
    var self = this;
    $.post(
        "/command/cancel-processes?" + $.param({ project: theProject.id }), 
        null,
        function(o) {
            self._data = null;
            self._runOnDones();
        },
        "json"
    );
};

ProcessWidget.prototype._render = function(newData) {
    var self = this;
    var newProcessMap = {};
    
    this._div.empty();
    
    if (!newData.processes.length) {
        Gridworks.setTitle();
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
                Gridworks.setTitle(process.progress + "%");
                div.text(process.description + " (" + process.progress + "%)");
            }
        };
        
        var processes = newData.processes;
        for (var i = 0; i < processes.length; i++) {
            var process = processes[i];
            renderProcess(process);
            if ("onDone" in process) {
                newProcessMap[process.id] = process;
            }
        }
    }
    
    if ((this._data) && this._data.processes.length > 0) {
        var oldProcesses = this._data.processes;
        for (var i = 0; i < oldProcesses.length; i++) {
            var process = oldProcesses[i];
            if ("onDone" in process && !(process.id in newProcessMap)) {
                this._perform(process.onDone);
            }
        }
    }
    this._data = newData;
    
    if (this._data.processes.length && !this._timerID) {
        this._timerID = window.setTimeout(function() {
            self._timerID = null;
            self.update();
        }, 500);
    } else {
        this._runOnDones();
    }
};

ProcessWidget.prototype._perform = function(jobs) {
    for (var i = 0; i < jobs.length; i++) {
        var job = jobs[i];
        if (job.action == "createFacet") {
            try {
                ui.browsingEngine.addFacet(
                    job.facetType,
                    job.facetConfig,
                    job.facetOptions
                );
            } catch (e) {
                //
            }
        }
    }
};

ProcessWidget.prototype._runOnDones = function() {
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
};
