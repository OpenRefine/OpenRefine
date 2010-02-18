function HistoryWidget(div) {
    this._div = div;
    this.update();
}

HistoryWidget.prototype.update = function(onDone) {
    var self = this;
    Ajax.chainGetJSON(
        "/command/get-history?" + $.param({ project: theProject.id }), null,
        function(data) {
            self._data = data;
            self._render();
        }
    );
};

HistoryWidget.prototype._render = function() {
    var self = this;
    
    this._div.empty();
    this._div.unbind();
    
    $('<h3>Undo/Redo History</h3>').appendTo(this._div);
    
    var bodyDiv = $('<div></div>').addClass("history-panel-body").appendTo(this._div);
    bodyDiv.mouseenter(function(evt) {
        bodyDiv.addClass("history-panel-body-expanded");
    });
    
    this._div.mouseenter(function(evt) {
        if (self._timerID != null) {
            window.clearTimeout(self._timerID);
            self._timerID = null;
        }
    }).mouseleave(function(evt) {
        self._timerID = window.setTimeout(function() {
            self._timerID = null;
            bodyDiv.removeClass("history-panel-body-expanded");
            autoscroll();
        }, 1000);
    });
    
    var renderEntry = function(container, entry, lastDoneID, title) {
        var a = $('<a href="javascript:{}"></a>').appendTo(container);
        a.addClass("history-entry").html(entry.description).attr("title", title).click(function(evt) {
            return self._onClickHistoryEntry(evt, entry, lastDoneID);
        });
        return a;
    };
    
    var divPast = $('<div></div>').addClass("history-past").appendTo(bodyDiv);
    if (this._data.past.length == 0) {
        $('<div></div>').addClass("history-panel-message").text("No change to undo").appendTo(divPast);
    } else {
        for (var i = 0; i < this._data.past.length; i++) {
            var entry = this._data.past[i];
            renderEntry(divPast, entry, i == 0 ? 0 : this._data.past[i - 1].id, "Undo to here");
        }
    }
    
    var divNow = $('<div></div>').text("done upto here").addClass("history-now").appendTo(bodyDiv);
    
    var divFuture = $('<div></div>').addClass("history-future").appendTo(bodyDiv);
    if (this._data.future.length == 0) {
        $('<div></div>').addClass("history-panel-message").text("No change to redo").appendTo(divFuture);
    } else {
        for (var i = 0; i < this._data.future.length; i++) {
            var entry = this._data.future[i];
            renderEntry(divFuture, entry, entry.id, "Redo to here");
        }
    }
    
    var autoscroll = function() {
        bodyDiv[0].scrollTop = divNow[0].offsetTop + divNow[0].offsetHeight - bodyDiv[0].offsetHeight;
    };
    autoscroll();
    
    
    var footerDiv = $('<div></div>').addClass("history-panel-footer").appendTo(this._div);
    $('<a href="javascript:{}"></a>').text("extract").appendTo(footerDiv).click(function() {
        self._extractOperations();
    });
    $('<span> &bull; </span>').appendTo(footerDiv);
    $('<a href="javascript:{}"></a>').text("apply").appendTo(footerDiv).click(function() {
        self._showApplyOperationsDialog();
    });
};

HistoryWidget.prototype._onClickHistoryEntry = function(evt, entry, lastDoneID) {
    var self = this;
    $.post(
        "/command/undo-redo?" + $.param({ project: theProject.id, lastDoneID: lastDoneID }), 
        null,
        function(data) {
            if (data.code == "ok") {
                self.update();
                ui.dataTableView.update(true);
            } else {
                // update process UI
            }
        },
        "json"
    );
};

HistoryWidget.prototype._extractOperations = function() {
    var self = this;
    $.getJSON(
        "/command/get-operations?" + $.param({ project: theProject.id }), 
        null,
        function(data) {
            if ("operations" in data) {
                self._showExtractOperationsDialog(data.operations);
            }
        },
        "jsonp"
    );
};

HistoryWidget.prototype._showExtractOperationsDialog = function(json) {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("800px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Extract Operations").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    $('<p></p>').text(
        "The following JSON code encodes the operations you have done that can be abstracted. " + 
        "You can copy and save it in order to apply the same operations in the future.").appendTo(body);
        
    var textarea = $('<textarea />').attr("wrap", "hard").width("100%").height("400px").appendTo(body);
    textarea.text(JSON.stringify(json, null, 2));
    
    $('<button></button>').text("Done").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    var level = DialogSystem.showDialog(frame);
    
    textarea[0].select();
};

HistoryWidget.prototype._showApplyOperationsDialog = function(json) {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("800px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Apply Operations").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    $('<p></p>').text(
        "Paste the JSON code encoding the operations to perform.").appendTo(body);
        
    var textarea = $('<textarea />').attr("wrap", "hard").width("100%").height("400px").appendTo(body);
    textarea.text(JSON.stringify(json, null, 2));
    
    $('<button></button>').text("Apply").click(function() {
        try {
            var json = JSON.parse(textarea[0].value);
        } catch (e) {
            alert("The JSON you pasted is invalid.");
            return;
        }
        
        DialogSystem.dismissUntil(level - 1);
        
        $.post(
            "/command/apply-operations?" + $.param({ project: theProject.id }),
            { operations: JSON.stringify(json) },
            function(data) {
                self.update();
                ui.dataTableView.update(true);
                ui.processWidget.update();
            },
            "json"
        );
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    var level = DialogSystem.showDialog(frame);
    
    textarea[0].focus();
};
