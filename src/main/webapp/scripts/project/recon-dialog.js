function ReconDialog(columnIndex) {
    this._columnIndex = columnIndex;
    this._column = theProject.columnModel.columns[columnIndex];
    
    this._createDialog();
}

ReconDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("400px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Reconcile column " + this._column.headerLabel).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    $('<p></p>').text("Reconcile cell values to topics of type:").appendTo(body);
    
    var type = null;
    var input = $('<input />').appendTo($('<p></p>').appendTo(body));
    input.suggest({ type : '/type/type' }).bind("fb-select", function(e, data) {
        type = data.id;
    });
    
    $('<button></button>').text("Start Reconciling").click(function() {
        DialogSystem.dismissUntil(level - 1);
        $.post(
            "/command/reconcile?" + $.param({ project: theProject.id, cell: self._column.cellIndex, type: type }), 
            { engine: JSON.stringify(ui.browsingEngine.getJSON()) },
            function(data) {
                if (data.code != "error") {
                    ui.processWidget.update();
                } else {
                    alert(data.message);
                }
            },
            "json"
        );
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    var level = DialogSystem.showDialog(frame);
    
    input[0].focus();
};
