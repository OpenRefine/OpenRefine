function DataTableCellUI(dataTableView, cell, rowIndex, cellIndex, td) {
    this._dataTableView = dataTableView;
    this._cell = cell;
    this._rowIndex = rowIndex;
    this._cellIndex = cellIndex;
    this._td = td;
    
    this._render();
};

DataTableCellUI.prototype._render = function() {
    var cell = this._cell;
    var td = this._td;
    
    if (cell == null || cell.v == null) {
        return;
    }
    
    if (!("r" in cell) || cell.r == null) {
        $(td).html(cell.v);
    } else {
        var r = cell.r;
        if (r.j == "new") {
            $(td).html(cell.v + " (new)");
        } else if (r.j == "approve" && "m" in r && r.m != null) {
            var match = cell.r.m;
            $('<a></a>')
                .attr("href", "http://www.freebase.com/view" + match.id)
                .attr("target", "_blank")
                .text(match.name)
                .appendTo(td);
        } else {
            $(td).html(cell.v);
            if (this._dataTableView._showRecon && "c" in r && r.c.length > 0) {
                var candidates = r.c;
                var ul = $('<ul></ul>').appendTo(td);
                
                for (var i = 0; i < candidates.length; i++) {
                    var candidate = candidates[i];
                    var li = $('<li></li>').appendTo(ul);
                    $('<a></a>')
                        .attr("href", "http://www.freebase.com/view" + candidate.id)
                        .attr("target", "_blank")
                        .text(candidate.name)
                        .appendTo(li);
                    $('<span></span>').addClass("recon-score").text("(" + Math.round(candidate.score) + ")").appendTo(li);
                }
            }
        }
    }
};