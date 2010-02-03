function ExpressionPreviewDialog(title, cellIndex, rowIndices, values, expression, onDone) {
    this._cellIndex = cellIndex;
    this._rowIndices = rowIndices;
    this._values = values;
    this._results = null;
    
    this._expression = expression;
    this._onDone = onDone;
    
    this._timerID = null;
    this._createDialog(title);
}

ExpressionPreviewDialog.prototype._createDialog = function(title) {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("600px");
    
    var header = $('<div></div>').addClass("dialog-header").text(title).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    $('<p></p>').text("Expression:").appendTo(body);
    
    this._input = $('<input />').width("400px").keypress(function(){
        self._scheduleUpdate();
    }).appendTo($('<p></p>').appendTo(body));
    this._preview = $('<div></div>').addClass("expression-preview-container").appendTo(body);
    
    $('<button></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        DialogSystem.dismissUntil(level - 1);
        self._onDone(self._expression);
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    var level = DialogSystem.showDialog(frame);
    
    this._input[0].value = this._expression;
    this._input[0].focus();
    this._renderPreview(this._expression);
};

ExpressionPreviewDialog.prototype._scheduleUpdate = function() {
    if (this._timerID != null) {
        window.clearTimeout(this._timerID);
    }
    var self = this;
    this._timerID = window.setTimeout(function() { self._update(); }, 300);
};

ExpressionPreviewDialog.prototype._update = function() {
    var self = this;
    var expression = this._expression = $.trim(this._input[0].value);
    
    $.post(
        "/command/preview-expression?" + $.param({ project: theProject.id, expression: expression, cellIndex: this._cellIndex }), 
        {
            rowIndices: JSON.stringify(this._rowIndices) 
        },
        function(data) {
            if (data.code != "error") {
                self._results = data.results;
            } else {
                self._results = null;
            }
            self._renderPreview(expression);
        },
        "json"
    );
};

ExpressionPreviewDialog.prototype._renderPreview = function(expression) {
    var container = this._preview.empty();
    
    var table = $('<table></table>').appendTo(container)[0];
    var tr = table.insertRow(0);
    $(tr.insertCell(0)).addClass("expression-preview-heading").text("value");
    $(tr.insertCell(1)).addClass("expression-preview-heading").text(expression);
    
    for (var i = 0; i < this._values.length; i++) {
        var tr = table.insertRow(table.rows.length);
        
        $(tr.insertCell(0)).html(this._values[i]);
        if (this._results != null) {
            var v = this._results[i];
            if (v != null) {
                if ($.isArray(v)) {
                    v = JSON.stringify(v);
                }
                $(tr.insertCell(1)).html(v);
            }
        }
    }
};