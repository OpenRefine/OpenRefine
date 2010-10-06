function SummaryWidget(div) {
    this._div = div;
    this._initializeUI();
}

SummaryWidget.prototype._initializeUI = function() {
    
};

SummaryWidget.prototype.updateResultCount = function() {
    var summaryText;
    var units = theProject.rowModel.mode == "row-based" ? "rows" : "records";
    if (theProject.rowModel.filtered == theProject.rowModel.total) {
        summaryText = '<span class="summaryWidget-rowCount">' + (theProject.rowModel.total) + ' ' + units + '</span>';
    } else {
        summaryText = '<span class="summaryWidget-rowCount">' + 
            (theProject.rowModel.filtered) + ' matching ' + units + '</span>' + ' (' + (theProject.rowModel.total) + ' total)';
    }
    
    $('<span>').html(summaryText).appendTo(this._div.empty());
};
