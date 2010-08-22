function TemplatingExporterDialog() {
    this._timerID = null;
    this._createDialog();
    this._updatePreview();
}

TemplatingExporterDialog.prototype._createDialog = function() {
    var self = this;
    var dialog = $(DOM.loadHTML("core", "scripts/dialogs/templating-exporter-dialog.html"));
    this._elmts = DOM.bind(dialog);
    this._elmts.controls.find("textarea").keyup(function() { self._scheduleUpdate(); });
    
    this._elmts.prefixTextarea[0].value = '{\n  "rows" : [\n';
    this._elmts.suffixTextarea[0].value = '\n  ]\n}';
    this._elmts.separatorTextarea[0].value = ',\n';
    this._elmts.templateTextarea[0].value = '    {' +
        $.map(theProject.columnModel.columns, function(column, i) {
            return '\n      "' + column.name + '" : {{jsonize(cells["' + column.name + '"].value)}}';
        }).join(',') + '\n    }';
    
    this._elmts.exportButton.click(function() { self._export(); self._dismiss(); });
    this._elmts.cancelButton.click(function() { self._dismiss(); });
    
    this._level = DialogSystem.showDialog(dialog);
};

TemplatingExporterDialog.prototype._scheduleUpdate = function() {
    var self = this;
    
    if (this._timerID) {
        window.clearTimeout(this._timerID);
    }
    
    this._elmts.previewTextarea[0].value = "Idling...";
    this._timerID = window.setTimeout(function() {
        self._timerID = null;
        self._elmts.previewTextarea[0].value = "Updating...";
        self._updatePreview();
    }, 1000);
};

TemplatingExporterDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

TemplatingExporterDialog.prototype._updatePreview = function() {
    var self = this;
    $.post(
        "/command/core/export-rows/preview.txt",
        {
            "project" : theProject.id, 
            "format" : "template",
            "engine" : JSON.stringify(ui.browsingEngine.getJSON()),
            "sorting" : JSON.stringify(ui.dataTableView.getSorting()),
            "prefix" : this._elmts.prefixTextarea[0].value,
            "suffix" : this._elmts.suffixTextarea[0].value,
            "separator" : this._elmts.separatorTextarea[0].value,
            "template" : this._elmts.templateTextarea[0].value,
            "limit" : "20"
        },
        function (data) {
            self._elmts.previewTextarea[0].value = data;
        },
        "text"
    );
};

TemplatingExporterDialog.prototype._export = function() {
    var name = $.trim(theProject.metadata.name.replace(/\W/g, ' ')).replace(/\s+/g, '-');
    var form = document.createElement("form");
    $(form)
        .css("display", "none")
        .attr("method", "post")
        .attr("action", "/command/core/export-rows/" + name + ".txt")
        .attr("target", "gridworks-export");
        
    var appendField = function(name, value) {
        $('<textarea />')
            .attr("name", name)
            .attr("value", value)
            .appendTo(form);
    }

    appendField("engine", JSON.stringify(ui.browsingEngine.getJSON()));
    appendField("project", theProject.id);
    appendField("format", "template");
    appendField("sorting", JSON.stringify(ui.dataTableView.getSorting()));
    appendField("prefix", this._elmts.prefixTextarea[0].value);
    appendField("suffix", this._elmts.suffixTextarea[0].value);
    appendField("separator", this._elmts.separatorTextarea[0].value);
    appendField("template", this._elmts.templateTextarea[0].value);

    document.body.appendChild(form);

    window.open("about:blank", "gridworks-export");
    form.submit();

    document.body.removeChild(form);
};