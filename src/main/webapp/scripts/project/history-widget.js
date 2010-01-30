function HistoryWidget(div) {
    this._div = div;
    this._div.mouseover(function() {
        this.style.height = "300px";
    }).mouseout(function() {
        this.style.height = "100px";
    });
    
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
    var container = this._div.empty();
    
    $('<h3>History</h3>').appendTo(this._div);
    
    var renderEntry = function(container, entry, lastDoneID, title) {
        var a = $('<a href="javascript:{}"></a>').appendTo(container);
        a.addClass("history-entry").html(entry.description).attr("title", title).click(function(evt) {
            return self._onClickHistoryEntry(evt, entry, lastDoneID);
        });
    };
    
    var divPast = $('<div></div>').addClass("history-past").appendTo(this._div);
    for (var i = 0; i < this._data.past.length; i++) {
        var entry = this._data.past[i];
        renderEntry(divPast, entry, i == 0 ? 0 : this._data.past[i - 1].id, "Undo upto and including this change");
    }

    var divFuture = $('<div></div>').addClass("history-future").appendTo(this._div);
    for (var i = 0; i < this._data.future.length; i++) {
        var entry = this._data.future[i];
        renderEntry(divFuture, entry, entry.id, "Redo upto and including this change");
    }
    
};

HistoryWidget.prototype._onClickHistoryEntry = function(evt, entry, lastDoneID) {
    var self = this;
    $.post(
        "/command/undo-redo?" + $.param({ project: theProject.id, lastDoneID: lastDoneID }), 
        null,
        function(data) {
            if (data.code == "ok") {
                self.update();
                ui.dataTableView.update();
            } else {
                // update process UI
            }
        },
        "json"
    );
};
