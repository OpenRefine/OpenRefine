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
    frame.width("700px");
    
    var header = $('<div></div>').addClass("dialog-header").text(title).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    var layoutTable = $('<table cellspacing="0" cellpadding="0" width="100%"><tr><td></td><td></td></tr></table>').appendTo(body);
    var mainColumn = layoutTable[0].rows[0].cells[0];
    var helpColumn = layoutTable[0].rows[0].cells[1];
    
    this._renderFooter($(footer));
    this._renderMainColumn($(mainColumn));
    this._renderHelpColumn($(helpColumn));
    
    this._level = DialogSystem.showDialog(frame);
    
    this._input[0].value = this._expression;
    this._input[0].focus();
    this._update();
};

ExpressionPreviewDialog.prototype._renderFooter = function(footer) {
    var self = this;
    
    $('<button></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
        self._onDone(self._expression);
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
};

ExpressionPreviewDialog.prototype._renderHelpColumn = function(helpColumn) {
    helpColumn.addClass("expression-preview-help-column").attr("width", "250").attr("height", "100%");
    
    var outer = $('<div></div>').addClass("expression-preview-help-outer").appendTo(helpColumn);
    var inner = $('<div></div>').addClass("expression-preview-help-inner").text("Loading expression language help info ...").appendTo(outer);
    var self = this;
    
    $.getJSON(
        "/command/get-expression-language-info",
        null,
        function(data) {
            self._renderHelp(inner, data);
        },
        "json"
    );
};

ExpressionPreviewDialog.prototype._renderHelp = function(elmt, data) {
    elmt.empty();
    
    $('<h3></h3>').text("Functions").appendTo(elmt);
    
    var ul = $('<ul></ul>').appendTo(elmt);
    for (var n in data.functions) {
        $('<li></li>').text(n).appendTo(ul);
    }
    
    $('<h3></h3>').text("Controls").appendTo(elmt);
    
    ul = $('<ul></ul>').appendTo(elmt);
    for (var n in data.controls) {
        $('<li></li>').text(n).appendTo(ul);
    }
};

ExpressionPreviewDialog.prototype._renderMainColumn = function(mainColumn) {
    var self = this;
    var p = $('<p></p>').text("Expression: ").appendTo(mainColumn);
    
    this._input = $('<input />').width("400px").keypress(function(){
        self._scheduleUpdate();
    }).appendTo(p);
    
    this._preview = $('<div></div>').addClass("expression-preview-container").appendTo(mainColumn);
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
    
    var table = $('<table width="100%"></table>').appendTo(container)[0];
    
    var tr = table.insertRow(0);
    $(tr.insertCell(0)).addClass("expression-preview-heading").text("row");
    $(tr.insertCell(1)).addClass("expression-preview-heading").text("value");
    $(tr.insertCell(2)).addClass("expression-preview-heading").text(expression);
    
    var renderValue = function(td, v) {
        if (v !== null && v !== undefined) {
            if ($.isArray(v)) {
                td.html(JSON.stringify(v));
            } else if (typeof v === "string" && v.length == 0) {
                $('<span>empty string</span>').addClass("expression-preview-empty").appendTo(td);
            } else {
                td.html(v.toString());
            }
        } else {
            $('<span>null</span>').addClass("expression-preview-empty").appendTo(td);
        }
    };
    
    for (var i = 0; i < this._values.length; i++) {
        var tr = table.insertRow(table.rows.length);
        
        $(tr.insertCell(0)).attr("width", "1%").html((this._rowIndices[i] + 1) + ".");
        
        renderValue($(tr.insertCell(1)), this._values[i]);
        
        var tdValue = $(tr.insertCell(2));
        if (this._results != null) {
            var v = this._results[i];
            renderValue(tdValue, v);
        } else {
            $('<span>error</span>').addClass("expression-preview-empty").appendTo(tdValue);
        }
    }
};