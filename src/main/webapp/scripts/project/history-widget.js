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
            if ("entries" in data) {
                self._showExtractOperationsDialog(data);
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
        
    var table = $('<table width="100%" cellspacing="0" cellpadding="0"><tr></tr></table>').appendTo(body)[0];
    var leftColumn = table.rows[0].insertCell(0);
    var rightColumn = table.rows[0].insertCell(1);
    $(leftColumn).width("50%");
    $(rightColumn).width("50%").css("padding-left", "20px");
    
    var entryDiv = $('<div>').height("400px").css("overflow", "auto").appendTo(leftColumn);
    var entryTable = $('<table cellspacing="5"></table>').appendTo(entryDiv)[0];
    var createEntry = function(entry) {
        var tr = entryTable.insertRow(entryTable.rows.length);
        var td0 = tr.insertCell(0);
        var td1 = tr.insertCell(1);
        td0.width = "1%";
        
        if ("operation" in entry) {
            entry.selected = true;
            
            $('<input type="checkbox" checked="true" />').appendTo(td0).click(function() {
                entry.selected = !entry.selected;
                updateJson();
            });
            
            $('<span>').text(entry.operation.description).appendTo(td1);
        } else {
            $('<span>').text(entry.description).css("color", "#888").appendTo(td1);
        }
    };
    for (var i = 0; i < json.entries.length; i++) {
        createEntry(json.entries[i]);
    }
        
    var textarea = $('<textarea />')
        .attr("wrap", "off")
        .css("white-space", "pre")
        .css("font-family", "monospace")
        .width("100%")
        .height("400px")
        .appendTo(rightColumn);
    var updateJson = function() {
        var a = [];
        for (var i = 0; i < json.entries.length; i++) {
            var entry = json.entries[i];
            if ("operation" in entry && entry.selected) {
                a.push(entry.operation);
            }
        }
        textarea.text(JSON.stringify(a, null, 2));
    };
    updateJson();
    
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
        
    var textarea = $('<textarea />')
        .attr("wrap", "off")
        .css("white-space", "pre")
        .css("font-family", "monospace")
        .width("100%")
        .height("400px")
        .appendTo(body);
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
