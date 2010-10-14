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
        summaryText = (theProject.rowModel.total) + ' ' + units;
    } else {
        summaryText = (theProject.rowModel.filtered) + ' matching ' + units + ' <span id="summary-total">(' + (theProject.rowModel.total) + ' total)</span>';
    }
    
    $('<span>').html(summaryText).appendTo(this._div.empty());
};
