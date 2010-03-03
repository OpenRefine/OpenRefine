function ExpressionPreviewDialog(title, cellIndex, rowIndices, values, expression, onDone) {
    this._onDone = onDone;

    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("700px");
    
    var header = $('<div></div>').addClass("dialog-header").text(title).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    var html = $(ExpressionPreviewDialog.generateWidgetHtml()).appendTo(body);
    
    this._elmts = DOM.bind(html);
    
    $('<button></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
        self._onDone(self._previewWidget.expression);
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
    
    this._level = DialogSystem.showDialog(frame);
    this._previewWidget = new ExpressionPreviewDialog.Widget(
        this._elmts, 
        cellIndex,
        rowIndices,
        values,
        expression
    );
};

ExpressionPreviewDialog.generateWidgetHtml = function() {
    return '<table class="expression-preview-layout">' +
            '<tr>' +
                '<td>Expression</td>' +
                '<td>Language</td>' +
            '</tr>' +
            '<tr>' +
                '<td rowspan="2"><textarea class="expression-preview-code" bind="expressionPreviewTextarea" /></td>' +
                '<td width="150">' +
                    '<select bind="expressionPreviewLanguageSelect">' +
                        '<option value="gel">Native expression language</option>' +
                        '<option value="jython">Jython</option>' +
                        '<option value="clojure">Clojure</option>' +
                    '</select>' +
                '</td>' +
            '</tr>' +
            '<tr>' +
                '<td class="expression-preview-error-container" bind="expressionPreviewErrorContainer" width="150"></td>' +
            '</tr>' +
            '<tr>' +
                '<td colspan="2">' +
                    '<div id="expression-preview-tabs">' +
                        '<ul>' +
                            '<li><a href="#expression-preview-tabs-preview">Preview</a></li>' +
                            '<li><a href="#expression-preview-tabs-history">History</a></li>' +
                            '<li><a href="#expression-preview-tabs-help">Help</a></li>' +
                        '</ul>' +
                        '<div id="expression-preview-tabs-preview">' +
                            '<div class="expression-preview-container" bind="expressionPreviewPreviewContainer"></div>' +
                        '</div>' +
                        '<div id="expression-preview-tabs-history">' +
                            '<div class="expression-preview-container" bind="expressionPreviewHistoryContainer"></div>' +
                        '</div>' +
                        '<div id="expression-preview-tabs-help" style="display: none;">' +
                            '<div class="expression-preview-help-container" bind="expressionPreviewHelpTabBody"></div>' +
                        '</div>' +
                    '</div>' +
                '</td>' +
            '</tr>' +
        '</table>';
};

ExpressionPreviewDialog.Widget = function(
    elmts, 
    cellIndex,
    rowIndices,
    values,
    expression
) {
    this._elmts = elmts;
    this._cellIndex = cellIndex;
    this._rowIndices = rowIndices;
    this._values = values;
    this.expression = expression;
    
    this._results = null;
    this._timerID = null;
    
    $("#expression-preview-tabs").tabs();
    $("#expression-preview-tabs-preview").css("display", "");
    $("#expression-preview-tabs-help").css("display", "");
        
    var self = this;
    this._elmts.expressionPreviewTextarea
        .attr("value", this.expression)
        .keyup(function(){
            self._scheduleUpdate();
        })
        .select()
        .focus();
    
    this._update();
    this._renderHelpTab();
};

ExpressionPreviewDialog.Widget.prototype._renderHelpTab = function() {
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

ExpressionPreviewDialog.Widget.prototype._renderHelp = function(data) {
    var elmt = this._elmts.expressionPreviewHelpTabBody.empty();
    
    $('<h3></h3>').text("Variables").appendTo(elmt);
    var varTable = $('<table width="100%" cellspacing="5"></table>').appendTo(elmt)[0];
    var vars = [
        {   name: "cell",
            description: "The current cell. It has a few fields: 'value' and 'recon'."
        },
        {   name: "value",
            description: "The current cell's value. This is a shortcut for 'cell.value'."
        },
        {   name: "row",
            description: "The current row. It has 4 fields: 'flagged', 'starred', 'index', and 'cells'."
        },
        {   name: "cells",
            description: "The cells of the current row. This is a shortcut for 'row.cells'. " +
                "A particular cell can be retrieved with 'cells.<column name>' if the <column name> is a single word, " +
                "or with 'cells[\"<column name>\"] otherwise."
        },
        {   name: "rowIndex",
            description: "The current row's index. This is a shortcut for 'row.index'."
        }
    ];
    for (var i = 0; i < vars.length; i++) {
        var variable = vars[i];
        var tr = varTable.insertRow(varTable.rows.length);
        $(tr.insertCell(0)).addClass("expression-preview-doc-item-title").text(variable.name);
        $(tr.insertCell(1)).addClass("expression-preview-doc-item-desc").text(variable.description);
    }
    
    var renderEntry = function(table, name, entry) {
        var tr0 = table.insertRow(table.rows.length);
        var tr1 = table.insertRow(table.rows.length);
        var tr2 = table.insertRow(table.rows.length);
        
        $(tr0.insertCell(0)).addClass("expression-preview-doc-item-title").text(name);
        $(tr0.insertCell(1)).addClass("expression-preview-doc-item-params").text("(" + entry.params + ")");
        
        $(tr1.insertCell(0));
        $(tr1.insertCell(1)).addClass("expression-preview-doc-item-returns").text("returns: " + entry.returns);
        
        $(tr2.insertCell(0));
        $(tr2.insertCell(1)).addClass("expression-preview-doc-item-desc").text(entry.description);
    };
    var renderEntries = function(table, map) {
        var names = [];
        for (var n in map) {
            if (map.hasOwnProperty(n)) {
                names.push(n);
            }
        }
        names.sort();
        
        for (var i = 0; i < names.length; i++) {
            var name = names[i];
            renderEntry(table, name, map[name]);
        }
    };
    
    $('<h3></h3>').text("Functions").appendTo(elmt);
    var functionTable = $('<table width="100%" cellspacing="5"></table>').appendTo(elmt)[0];
    renderEntries(functionTable, data.functions);
    
    $('<h3></h3>').text("Controls").appendTo(elmt);
    var controlTable = $('<table width="100%" cellspacing="5"></table>').appendTo(elmt)[0];
    renderEntries(controlTable, data.controls);
};

ExpressionPreviewDialog.Widget.prototype._scheduleUpdate = function() {
    if (this._timerID != null) {
        window.clearTimeout(this._timerID);
    }
    var self = this;
    this._timerID = window.setTimeout(function() { self._update(); }, 300);
};

ExpressionPreviewDialog.Widget.prototype._update = function() {
    var self = this;
    var expression = this.expression = $.trim(this._elmts.expressionPreviewTextarea[0].value);
    
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
            self._renderPreview(expression, data);
        },
        "json"
    );
};

ExpressionPreviewDialog.Widget.prototype._renderPreview = function(expression, data) {
    var container = this._elmts.expressionPreviewPreviewContainer.empty();
    var table = $('<table width="100%"></table>').appendTo(container)[0];
    
    var tr = table.insertRow(0);
    $(tr.insertCell(0)).addClass("expression-preview-heading").text("row");
    $(tr.insertCell(1)).addClass("expression-preview-heading").text("value");
    $(tr.insertCell(2)).addClass("expression-preview-heading").text(expression);
    
    var renderValue = function(td, v) {
        if (v !== null && v !== undefined) {
            if ($.isArray(v)) {
                td.html(JSON.stringify(v));
            } else if ($.isPlainObject(v)) {
                $('<span></span>').addClass("expression-preview-special-value").text("Error: " + v.message).appendTo(td);
            } else if (typeof v === "string" && v.length == 0) {
                $('<span>empty string</span>').addClass("expression-preview-special-value").appendTo(td);
            } else {
                td.html(v.toString());
            }
        } else {
            $('<span>null</span>').addClass("expression-preview-special-value").appendTo(td);
        }
    };
    
    if (this._results != null) {
        this._elmts.expressionPreviewErrorContainer.empty();
    } else {
        var message = (data.type == "parser") ? data.message : "Internal error";
        this._elmts.expressionPreviewErrorContainer.empty().text(message);
    }
    
    for (var i = 0; i < this._values.length; i++) {
        var tr = table.insertRow(table.rows.length);
        
        $(tr.insertCell(0)).attr("width", "1%").html((this._rowIndices[i] + 1) + ".");
        
        renderValue($(tr.insertCell(1)), this._values[i]);
        
        var tdValue = $(tr.insertCell(2));
        if (this._results != null) {
            var v = this._results[i];
            renderValue(tdValue, v);
        }
    }
};