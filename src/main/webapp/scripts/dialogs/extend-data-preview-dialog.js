function ExtendDataPreviewDialog(column, rowIndices, onDone) {
    this._column = column;
    this._rowIndices = rowIndices;
    this._onDone = onDone;
    this._extension = { properties: [] };

    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("900px").addClass("extend-data-preview-dialog");
    
    var header = $('<div></div>').addClass("dialog-header").text("Add Columns from Freebase Based on Column " + column.name).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    var html = $(
        '<div class="grid-layout layout-normal layout-full"><table style="height: 600px">' +
            '<tr>' +
                '<td width="150" height="1">Add Property</td>' +
                '<td height="1">Preview</td>' +
            '</tr>' +
            '<tr>' +
                '<td height="1"><div class="input-container"><input bind="addPropertyInput" /></div></td>' +
                '<td height="100%" rowspan="3"><div class="preview-container" bind="previewContainer"></div></td>' +
            '</tr>' +
            '<tr>' +
                '<td height="1">Suggested Properties</td>' +
            '</tr>' +
            '<tr>' +
                '<td height="99%"><div class="suggested-property-container"></div></td>' +
            '</tr>' +
        '</table></div>'
    ).appendTo(body);
    
    this._elmts = DOM.bind(html);
    
    $('<button></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
        self._onDone(self._previewWidget.getExpression(true));
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
    
    this._level = DialogSystem.showDialog(frame);
    
    var suggestConfig = {
        type: '/type/property'
    };
    if ("reconConfig" in column) {
        suggestConfig.schema = column.reconConfig.type.id;
    }    
    this._elmts.addPropertyInput.suggestP(suggestConfig).bind("fb-select", function(evt, data) {
        self._addProperty(data);
    });
};

ExtendDataPreviewDialog.prototype._update = function() {
    this._elmts.previewContainer.empty().text("Querying Freebase ...");
    
    var self = this;
    var params = {
        project: theProject.id,
        columnName: this._column.name
    };
    
    $.post(
        "/command/preview-extend-data?" + $.param(params), 
        {
            rowIndices: JSON.stringify(this._rowIndices),
            extension: JSON.stringify(this._extension)
        },
        function(data) {
            self._renderPreview(data)
        },
        "json"
    );
};

ExtendDataPreviewDialog.prototype._addProperty = function(p) {
    this._extension.properties.push({
        id : p.id,
        name: p.name,
        expected: p["/type/property/expected_type"]
    });
    this._update();
};

ExtendDataPreviewDialog.prototype._renderPreview = function(data) {
    var container = this._elmts.previewContainer.empty();
    if (data.code == "error") {
        container.text("Error.");
        return;
    }
    
    var table = $('<table>')[0];
    
    for (var r = 0; r < data.rows.length; r++) {
        var tr = table.insertRow(table.rows.length);
        var row = data.rows[r];
        
        for (var c = 0; c < row.length; c++) {
            var td = tr.insertCell(tr.cells.length);
            var cell = row[c];
            if (cell != null) {
                if ($.isPlainObject(cell)) {
                    $('<a>').attr("href", "http://www.freebase.com/view" + cell.id).text(cell.name).appendTo(td);
                } else {
                    $('<span>').text(cell).appendTo(td);
                }
            }
        }
    }
    
    container.append(table);
}