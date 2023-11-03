/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

function ExpressionPreviewDialog(title, cellIndex, rowIndices, values, expression, onDone) {
    this._onDone = onDone;

    var self = this;
    var frame = DialogSystem.createDialog();
    frame.css("min-width", "700px")
    var header = $('<div></div>').addClass("dialog-header").text(title).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    var html = $(ExpressionPreviewDialog.generateWidgetHtml()).appendTo(body);
    
    this._elmts = DOM.bind(html);
    
    $('<button class="button"></button>').html($.i18n('core-buttons/ok')).on('click',function() {
        DialogSystem.dismissUntil(self._level - 1);
        self._onDone(self._previewWidget.getExpression(true));
    }).appendTo(footer);
    
    $('<button class="button"></button>').text($.i18n('core-buttons/cancel')).on('click',function() {
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
}

ExpressionPreviewDialog.generateWidgetHtml = function() {
    var html = DOM.loadHTML("core", "scripts/dialogs/expression-preview-dialog.html");
    
    var languageOptions = [];
    for (var prefix in theProject.scripting) {
        if (theProject.scripting.hasOwnProperty(prefix)) {
            var info = theProject.scripting[prefix];
            languageOptions.push('<option value="' + prefix + '">' + info.name + '</option>');
        }
    }
    
    return html.replace("$LANGUAGE_OPTIONS$", languageOptions.join(""));
};

ExpressionPreviewDialog.Widget = function(
    elmts, 
    cellIndex,
    rowIndices,
    values,
    expression
) {
    var language = "grel";
    if (!(expression)) {
        language = Cookies.get("scripting.lang");
        if (language == "gel") { // backward compatible
            language = "grel";
        }
        
        if (!(language) || !(language.toLowerCase() in theProject.scripting)) {
            language = "grel";
        }
        this.expression = theProject.scripting[language].defaultExpression;
    } else {
        this.expression = expression;
        
        var colon = expression.indexOf(":");
        if (colon > 0) {
            var l = expression.substring(0, colon);
            if (l.toLowerCase() in theProject.scripting) {
                this.expression = expression.substring(colon + 1);
                language = l;
            }
        }
    }
    
    this._elmts = elmts;
    this._cellIndex = cellIndex;
    this._rowIndices = rowIndices;
    this._values = values;
    
    this._results = null;
    this._timerID = null;
    
    $("#expression-preview-tabs").tabs();
    
    this._elmts.expressionPreviewLanguageSelect[0].value = language;
    this._elmts.expressionPreviewLanguageSelect.on("change", function() {
        Cookies.set("scripting.lang", this.value, {"SameSite" : "Lax"});
        self.update();
    });
        
    var self = this;
    this._elmts.expressionPreviewTextarea
        .val(this.expression)
        .on("keyup change input",function(){
            self._scheduleUpdate();
        })
        .trigger('select')
        .trigger('focus');

    this._elmts.or_dialog_expr.html($.i18n('core-dialogs/expression'));
    this._elmts.or_dialog_lang.html($.i18n('core-dialogs/language'));
    this._elmts.or_dialog_preview.html($.i18n('core-dialogs/preview'));
    this._elmts.or_dialog_history.html($.i18n('core-dialogs/history'));
    this._elmts.or_dialog_starred.html($.i18n('core-dialogs/starred'));
    this._elmts.or_dialog_help.html($.i18n('core-dialogs/help'));
    
    this.update();
    this._renderExpressionHistoryTab();
    this._renderStarredExpressionsTab();
    this._renderHelpTab();
};

ExpressionPreviewDialog.Widget.prototype.getExpression = function(commit) {
    var s = jQueryTrim(this.expression || "");
    if (!s.length) {
        return null;
    }
    
    s = this._getLanguage() + ":" + s;
    if (commit) {
        Refine.postCSRF(
            "command/core/log-expression?" + $.param({ project: theProject.id }),
            { expression: s },
            function(data) {
            },
            "json"
        );
    }
    
    return s;
};

ExpressionPreviewDialog.Widget.prototype._getLanguage = function() {
    return this._elmts.expressionPreviewLanguageSelect[0].value;
};

ExpressionPreviewDialog.Widget.prototype._renderHelpTab = function() {
    var self = this;
    $.getJSON(
        "command/core/get-expression-language-info",
        null,
        function(data) {
            self._renderHelp(data);
        },
        "json"
    );
};

ExpressionPreviewDialog.Widget.prototype._renderHelp = function(data) {
    var elmt = this._elmts.expressionPreviewHelpTabBody.empty();
    $("<a />", {
        href: "https://openrefine.org/docs/manual/grelfunctions",
        text: $.i18n('core-dialogs/help/grelreference'),
        target: "_blank",
      }).appendTo(elmt);
    $('<h3></h3>').text("Variables").appendTo(elmt);
    var varTable = $('<table cellspacing="5"></table>').appendTo(elmt)[0];
    var vars = [
        {   name: "cell",
            description: $.i18n('core-dialogs/cell-fields')
        },
        {   name: "value",
            description: $.i18n('core-dialogs/cell-value')
        },
        {   name: "row",
            description: $.i18n('core-dialogs/row-fields')
        },
        {   name: "cells",
            description: $.i18n('core-dialogs/cells-of-row')
        },
        {   name: "rowIndex",
            description: $.i18n('core-dialogs/row-index')
        },
        {   name: "record",
            description: $.i18n('core-dialogs/record-fields')
        }
    ];
    for (var i = 0; i < vars.length; i++) {
        var variable = vars[i];
        var tr = varTable.insertRow(varTable.rows.length);
        $(tr.insertCell(0)).addClass("expression-preview-doc-item-title").text(variable.name);
        $(tr.insertCell(1)).addClass("expression-preview-doc-item-desc").html(variable.description);
    }
    
    var renderEntry = function(table, name, entry) {
        var tr0 = table.insertRow(table.rows.length);
        var tr1 = table.insertRow(table.rows.length);
        var tr2 = table.insertRow(table.rows.length);
        
        $(tr0.insertCell(0)).addClass("expression-preview-doc-item-title").text(name);
        $(tr0.insertCell(1)).addClass("expression-preview-doc-item-params").text("(" + entry.params + ")");
        
        $(tr1.insertCell(0));
        $(tr1.insertCell(1)).addClass("expression-preview-doc-item-returns").text($.i18n('core-dialogs/returns')+": " + entry.returns);
        
        $(tr2.insertCell(0));
        $(tr2.insertCell(1)).addClass("expression-preview-doc-item-desc").html(entry.description);
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

ExpressionPreviewDialog.Widget.prototype._renderExpressionHistoryTab = function() {
    var self = this;
    $.getJSON(
        "command/core/get-expression-history?" + $.param({ project: theProject.id }),
        null,
        function(data) {
            self._renderExpressionHistory(data);
        },
        "json"
    );
};

ExpressionPreviewDialog.Widget.prototype._renderExpressionHistory = function(data) {
    var self = this;
    var elmt = this._elmts.expressionPreviewHistoryContainer.empty();
    
    var table = $(
        '<table>' +
            '<tr><th></th><th></th><th>'+$.i18n('core-dialogs/from')+'</th><th colspan="2">'+$.i18n('core-dialogs/expression')+'</th><th></th></tr>' +
        '</table>'
    ).appendTo($('<div>').addClass("expression-preview-table-wrapper").appendTo(elmt))[0];
    
    var renderEntry = function(self,tr,entry) {
        $(tr).empty();
        var o = Scripting.parse(entry.code);
        $('<a href="javascript:{}">&nbsp;</a>')
                .addClass(entry.starred ? "data-table-star-on" : "data-table-star-off")
                .appendTo(tr.insertCell(0))
                .on('click',function() {
                    Refine.postCSRF(
                        "command/core/toggle-starred-expression",
                        {
                            expression: entry.code
                        },
                        function(data) {
                            entry.starred = !entry.starred;
                            renderEntry(self,tr,entry);
                            self._renderStarredExpressionsTab();
                        },
                        ""
                    );
                });
        
        $('<a href="javascript:{}">'+$.i18n('core-dialogs/reuse')+'</a>').appendTo(tr.insertCell(1)).on('click',function() {
            self._elmts.expressionPreviewTextarea[0].value = o.expression;
            self._elmts.expressionPreviewLanguageSelect[0].value = o.language;
            
            $("#expression-preview-tabs").tabs();
            
            self._elmts.expressionPreviewTextarea.trigger('select').trigger('focus');
            
            self.update();
        });
        
        
        $(tr.insertCell(2)).html(entry.global ? "Other&nbsp;projects" : "This&nbsp;project");
        $(tr.insertCell(3)).text(o.language + ":");
        $(tr.insertCell(4)).text(o.expression);
    };
    
    for (var i = 0; i < data.expressions.length; i++) {
        var tr = table.insertRow(table.rows.length);
        var entry = data.expressions[i];
        renderEntry(self,tr,entry);
    }
   
};

ExpressionPreviewDialog.Widget.prototype._renderStarredExpressionsTab = function() {
    var self = this;
    $.getJSON(
        "command/core/get-starred-expressions",
        null,
        function(data) {
            self._renderStarredExpressions(data);
        },
        ""
    );
};

ExpressionPreviewDialog.Widget.prototype._renderStarredExpressions = function(data) {
    var self = this;
    var elmt = this._elmts.expressionPreviewStarredContainer.empty();
    
    var table = $(
        '<table>' +
            '<tr><th></th><th></th><th colspan="2">'+$.i18n('core-dialogs/expression')+'</th><th></th></tr>' +
        '</table>'
    ).appendTo($('<div>').addClass("expression-preview-table-wrapper").appendTo(elmt))[0];
    
    var renderEntry = function(entry) {
        var tr = table.insertRow(table.rows.length);
        var o = Scripting.parse(entry.code);
        
        $('<a href="javascript:{}">'+$.i18n('core-dialogs/remove')+'</a>').appendTo(tr.insertCell(0)).on('click',function() {
            var removeExpression = DialogSystem.createDialog();
                removeExpression.width("250px");
            var removeExpressionHead = $('<div></div>').addClass("dialog-header").text($.i18n('core-dialogs/unstar-expression'))
                .appendTo(removeExpression);
            var removeExpressionFooter = $('<div></div>').addClass("dialog-footer").appendTo(removeExpression);

            $('<button class="button"></button>').html($.i18n('core-buttons/ok')).on('click',function() {
                Refine.postCSRF(
                    "command/core/toggle-starred-expression",
                    { expression: entry.code, returnList: true },
                    function(data) {
                        self._renderStarredExpressions(data);
                        self._renderExpressionHistoryTab();
                    },
                    "json"
                );
                DialogSystem.dismissUntil(DialogSystem._layers.length - 1);
            }).appendTo(removeExpressionFooter);

            $('<button class="button" style="float:right;"></button>').text($.i18n('core-buttons/cancel')).on('click',function() {
                DialogSystem.dismissUntil(DialogSystem._layers.length - 1);
            }).appendTo(removeExpressionFooter);

            this._level = DialogSystem.showDialog(removeExpression);
        });
        
        $('<a href="javascript:{}">Reuse</a>').appendTo(tr.insertCell(1)).on('click',function() {
            self._elmts.expressionPreviewTextarea[0].value = o.expression;
            self._elmts.expressionPreviewLanguageSelect[0].value = o.language;
            
            $("#expression-preview-tabs").tabs();
            
            self._elmts.expressionPreviewTextarea.trigger('select').trigger('focus');
            
            self.update();
        });
        
        $(tr.insertCell(2)).text(o.language + ":");
        $(tr.insertCell(3)).text(o.expression);
    };
    
    for (var i = 0; i < data.expressions.length; i++) {
        var entry = data.expressions[i];
        renderEntry(entry);
    }
};

ExpressionPreviewDialog.Widget.prototype._scheduleUpdate = function() {
    if (this._timerID !== null) {
        window.clearTimeout(this._timerID);
    }
    var self = this;
    this._timerID = window.setTimeout(function() { self.update(); }, 300);
};

ExpressionPreviewDialog.Widget.prototype.update = function() {
    var self = this;
    var expression = this.expression = jQueryTrim(this._elmts.expressionPreviewTextarea[0].value);
    var params = {
        project: theProject.id,
        cellIndex: this._cellIndex
    };
    this._prepareUpdate(params);
    
    $.post(
        "command/core/preview-expression?" + $.param(params), 
        {
        	expression: this._getLanguage() + ":" + expression,
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

ExpressionPreviewDialog.Widget.prototype._prepareUpdate = function(params) {
};

ExpressionPreviewDialog.Widget.prototype._renderPreview = function(expression, data) {
    var container = this._elmts.expressionPreviewPreviewContainer.empty();
    
    var table = $('<table></table>').appendTo(
        $('<div>').addClass("expression-preview-table-wrapper").appendTo(container))[0];
    
    var truncExpression = expression.length > 30 ? expression.substring(0, 30) + ' ...' : expression; 
    
    var tr = table.insertRow(0);
    $(tr.insertCell(0)).addClass("expression-preview-heading").text("row");
    $(tr.insertCell(1)).addClass("expression-preview-heading").text("value");
    $(tr.insertCell(2)).addClass("expression-preview-heading").text(truncExpression);
    
    var renderValue = function(td, v) {
        if (v !== null && v !== undefined) {
            if ($.isPlainObject(v)) {
                $('<span></span>').addClass("expression-preview-special-value").text($.i18n('core-dialogs/error')+": " + v.message).appendTo(td);
            } else {
                td.text(v);
            }
        } else {
            $('<span>null</span>').addClass("expression-preview-special-value").appendTo(td);
        }
    };
    
    if (this._results !== null) {
        this._elmts.expressionPreviewParsingStatus.empty().removeClass("error").text($.i18n('core-dialogs/no-syntax-err')+".");
    } else {
        var message = (data.type == "parser") ? data.message : $.i18n('core-dialogs/internal-err');
        this._elmts.expressionPreviewParsingStatus.empty().addClass("error").text(message);
    }
    
    for (var i = 0; i < this._values.length; i++) {
        var tr = table.insertRow(table.rows.length);
        
        $(tr.insertCell(0)).attr("width", "1%").html((this._rowIndices[i] + 1) + ".");
        
        renderValue($(tr.insertCell(1)).addClass("expression-preview-value"), this._values[i]);
        
        var tdValue = $(tr.insertCell(2)).addClass("expression-preview-value");
        if (this._results !== null) {
            var v = this._results[i];
            renderValue(tdValue, v);
        }
    }
};
