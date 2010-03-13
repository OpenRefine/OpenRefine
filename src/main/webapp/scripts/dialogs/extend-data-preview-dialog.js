function ExtendDataPreviewDialog(columnName, rowIndices, onDone) {
    this._onDone = onDone;

    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("900px").addClass("extend-data-preview-dialog");
    
    var header = $('<div></div>').addClass("dialog-header").text("Add Columns from Freebase Based on Column " + columnName).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    var html = $(
        '<div class="grid-layout layout-normal layout-full"><table style="height: 600px">' +
            '<tr>' +
                '<td width="150" height="1">Add Property</td>' +
                '<td height="100%" rowspan="4"><div class="preview-container" bind="previewDiv"></div></td>' +
            '</tr>' +
            '<tr>' +
                '<td height="1"><div class="input-container"><input /></div></td>' +
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
};

ExtendDataPreviewDialog.prototype.update = function() {
    
};