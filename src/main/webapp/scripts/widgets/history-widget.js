function HistoryWidget(div, tabHeader) {
    this._div = div;
    this._tabHeader = tabHeader;
    this.update();
}

HistoryWidget.prototype.resize = function() {
    var body = this._div.find(".history-panel-body");
    var footer = this._div.find(".history-panel-footer");
    var bodyPaddings = body.outerHeight(true) - body.height();
    
    body.css("height", (this._div.height() - footer.outerHeight(true) - bodyPaddings) + "px");
};

HistoryWidget.prototype.update = function(onDone) {
    var self = this;
    Ajax.chainGetJSON(
        "/command/get-history?" + $.param({ project: theProject.id }), null,
        function(data) {
            self._data = data;
            self._render();
            
            if (onDone) {
                onDone();
            }
        }
    );
};

HistoryWidget.prototype._render = function() {
    var self = this;
    
    this._tabHeader.html('Undo/Redo <span class="count">' + this._data.past.length + '</span>');
    
    this._div
        .empty()
        .unbind()
        .html(
            '<div class="history-panel-help" bind="helpDiv">' +
                '<h1>Don\'t worry ...</h1>' +
                '<p>about making mistakes. Every change you make will be shown here, and you can undo changes at any point.</p>' +
                '<p><a href="http://wiki.freebase.com/index.php?title=Gridworks" target="_blank">Learn more &raquo;</a></p>' +
            '</div>' +
            '<div class="history-panel-body" bind="bodyDiv">' +
                '<div class="history-past" bind="pastDiv"></div>' +
                '<div class="history-now" bind="nowDiv">done upto here</div>' +
                '<div class="history-future" bind="futureDiv"></div>' +
            '</div>' +
            '<div class="history-panel-footer" bind="footerDiv">' +
                '<a href="javascript:{}" bind="extractLink">extract</a> &bull; ' +
                '<a href="javascript:{}" bind="applyLink">apply</a>' +
            '</div>'
        );
    
    var elmts = DOM.bind(this._div);
    
    var renderEntry = function(container, entry, lastDoneID, title) {
        var a = $('<a href="javascript:{}"></a>').appendTo(container);
        a.addClass("history-entry").html(entry.description).attr("title", title).click(function(evt) {
            return self._onClickHistoryEntry(evt, entry, lastDoneID);
        });
        return a;
    };
    
    if (this._data.past.length > 0 || this._data.future.length > 0) {
        if (!this._data.past.length) {
            elmts.pastDiv.html('<div class="history-panel-message">No change to undo</div>');
        } else {
            for (var i = 0; i < this._data.past.length; i++) {
                var entry = this._data.past[i];
                renderEntry(elmts.pastDiv, entry, i === 0 ? 0 : this._data.past[i - 1].id, "Undo to here");
            }
        }
    
        if (!this._data.future.length) {
            elmts.futureDiv.html('<div class="history-panel-message">No change to redo</div>');
        } else {
            for (var i = 0; i < this._data.future.length; i++) {
                var entry = this._data.future[i];
                renderEntry(elmts.futureDiv, entry, entry.id, "Redo to here");
            }
        }
        
        elmts.helpDiv.hide();
    } else {
        elmts.bodyDiv.hide();
    }
    
    elmts.extractLink.click(function() { self._extractOperations(); });
    elmts.applyLink.click(function() { self._showApplyOperationsDialog(); });
    
    this.resize();
    
    elmts.bodyDiv[0].scrollTop = 
        elmts.nowDiv[0].offsetTop + 
        elmts.nowDiv[0].offsetHeight - 
        elmts.bodyDiv[0].offsetHeight;
};

HistoryWidget.prototype._onClickHistoryEntry = function(evt, entry, lastDoneID) {
    var self = this;
    
    Gridworks.postProcess(
        "undo-redo",
        { lastDoneID: lastDoneID },
        null,
        { everythingChanged: true }
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
    
    var html = $(
        '<div class="grid-layout layout-normal layout-full"><table>' +
            '<tr><td colspan="2">' +
                'The following JSON code encodes the operations you have done that can be abstracted. ' +
                'You can copy and save it in order to apply the same operations in the future.' +
            '</td></tr>' +
            '<tr>' +
                '<td width="50%" style="vertical-align: top">' +
                    '<div class="extract-operation-dialog-entries"><table cellspacing="5" bind="entryTable"></table></div>' +
                '</td>' +
                '<td width="50%" style="vertical-align: top">' +
                    '<div class="input-container"><textarea wrap="off" class="history-operation-json" bind="textarea" /></div>' +
                '</td>' +
            '</tr>' +
        '</table></div>'
    ).appendTo(body);
    
    var elmts = DOM.bind(html);
        
    var entryTable = elmts.entryTable[0];
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
    
    var updateJson = function() {
        var a = [];
        for (var i = 0; i < json.entries.length; i++) {
            var entry = json.entries[i];
            if ("operation" in entry && entry.selected) {
                a.push(entry.operation);
            }
        }
        elmts.textarea.text(JSON.stringify(a, null, 2));
    };
    updateJson();
    
    $('<button></button>').text("Close").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    var level = DialogSystem.showDialog(frame);
    
    textarea[0].select();
};

HistoryWidget.prototype._showApplyOperationsDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("800px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Apply Operations").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    var html = $(
        '<div class="grid-layout layout-normal layout-full"><table>' +
            '<tr><td>' +
                'Paste the JSON code encoding the operations to perform.' +
            '</td></tr>' +
            '<tr><td>' +
                '<div class="input-container"><textarea wrap="off" bind="textarea" class="history-operation-json" /></div>' +
            '</td></tr>' +
        '</table></div>'
    ).appendTo(body);
    
    var elmts = DOM.bind(html);
    
    $('<button></button>').text("Apply").click(function() {
        var json;
        
        try {
            json = JSON.parse(elmts.textarea[0].value);
        } catch (e) {
            alert("The JSON you pasted is invalid.");
            return;
        }
        
        Gridworks.postProcess(
            "apply-operations",
            {},
            { operations: JSON.stringify(json) },
            { everythingChanged: true },
            {
                onDone: function(o) {
                    if (o.code == "pending") {
                        // Something might have already been done and so it's good to update
                        Gridworks.update({ everythingChanged: true });
                    }
                }
            }
        );
        
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    var level = DialogSystem.showDialog(frame);
    
    textarea[0].focus();
};
