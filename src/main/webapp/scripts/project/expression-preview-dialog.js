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
    
    var html = $(
        '<div class="dialog-body">' +
            '<p>Expression: <input bind="expressionInput" /></p>' +
            '<div id="expression-preview-tabs">' +
                '<ul>' +
                    '<li><a href="#expression-preview-tabs-preview">Preview</a></li>' +
                    '<li><a href="#expression-preview-tabs-help">Help</a></li>' +
                '</ul>' +
                '<div id="expression-preview-tabs-preview">' +
                    '<div class="expression-preview-container" bind="previewContainer"></div>' +
                '</div>' +
                '<div id="expression-preview-tabs-help" style="display: none;">' +
                    '<div class="expression-preview-help-container" bind="helpTabBody"></div>' +
                '</div>' +
            '</div>' +
        '</div>'
    ).appendTo(body);

    this._elmts = DOM.bind(html);
    
    $('<button></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
        self._onDone(self._expression);
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
    
    this._elmts.expressionInput
        .width("400px")
        .attr("value", this._expression)
        .keyup(function(){
            self._scheduleUpdate();
        })
        .focus();
    
    this._level = DialogSystem.showDialog(frame);
    $("#expression-preview-tabs").tabs();
    $("#expression-preview-tabs-preview").css("display", "");
    $("#expression-preview-tabs-help").css("display", "");
        
    this._update();
    this._renderHelpTab();
};

ExpressionPreviewDialog.prototype._renderHelpTab = function() {
    var self = this;
    $.getJSON(
        "/command/get-expression-language-info",
        null,
        function(data) {
            self._renderHelp(data);
        },
        "json"
    );
};

ExpressionPreviewDialog.prototype._renderHelp = function(data) {
    var elmt = this._elmts.helpTabBody.empty();
    
    var renderEntry = function(table, name, entry) {
        var tr0 = table.insertRow(table.rows.length);
        var tr1 = table.insertRow(table.rows.length);
        var tr2 = table.insertRow(table.rows.length);
        
        $(tr0.insertCell(0)).text(name);
        $(tr0.insertCell(1)).text("(" + entry.params + ")");
        
        $(tr1.insertCell(0));
        $(tr1.insertCell(1)).text("returns: " + entry.returns);
        
        $(tr2.insertCell(0));
        $(tr2.insertCell(1)).text(entry.description);
    };
    
    $('<h3></h3>').text("Functions").appendTo(elmt);
    var functionTable = $('<table width="100%"></table>').appendTo(elmt)[0];
    
    $('<h3></h3>').text("Controls").appendTo(elmt);
    var controlTable = $('<table width="100%"></table>').appendTo(elmt)[0];
    
    for (var n in data.functions) {
        renderEntry(functionTable, n, data.functions[n]);
    }
    for (var n in data.controls) {
        renderEntry(controlTable, n, data.controls[n]);
    }
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
    var expression = this._expression = $.trim(this._elmts.expressionInput[0].value);
    
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
    var container = this._elmts.previewContainer.empty();
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