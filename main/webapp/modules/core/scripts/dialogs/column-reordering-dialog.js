function ColumnReorderingDialog() {
    this._createDialog();
}

ColumnReorderingDialog.prototype._createDialog = function() {
    var self = this;
    var dialog = $(DOM.loadHTML("core", "scripts/dialogs/column-reordering-dialog.html"));
    this._elmts = DOM.bind(dialog);
    this._elmts.cancelButton.click(function() { self._dismiss(); });
    this._elmts.okButton.click(function() { self._commit(); });
    
    this._level = DialogSystem.showDialog(dialog);
    
    for (var i = 0; i < theProject.columnModel.columns.length; i++) {
        var column = theProject.columnModel.columns[i];
        var name = column.name;
        
        $('<div>')
            .addClass("column-reordering-dialog-column")
            .text(name)
            .attr("column", name)
            .appendTo(this._elmts.columnContainer);
    }
    
    dialog.find('.column-reordering-dialog-column-container')
        .sortable({
            connectWith: '.column-reordering-dialog-column-container'
        })
        .disableSelection();
};

ColumnReorderingDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

ColumnReorderingDialog.prototype._commit = function() {
    var columnNames = this._elmts.columnContainer.find('div').map(function() { return this.getAttribute("column"); }).get();
    
    Refine.postCoreProcess(
        "reorder-columns",
        null,
        { "columnNames" : JSON.stringify(columnNames) }, 
        { modelsChanged: true },
        { includeEngine: false }
    );
    
    this._dismiss();
};
