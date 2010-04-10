function ScatterplotDialog() {
    this._createDialog();
}

ScatterplotDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("900px");
    
    var header = $('<div></div>').addClass("dialog-header").text('Scatterplot Matrix').appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $(
        '<div class="dialog-footer">' +
           '<table width="100%"><tr>' +
             '<td class="left" style="text-align: left"></td>' + 
             '<td class="right" style="text-align: right"></td>' +
           '</tr></table>' +
        '</div>'
    ).appendTo(frame);
    
    var html = $(
        '<div class="grid-layout layout-normal">' +
          '<div bind="tableContainer" class="scatterplot-dialog-table-container"></div>' +
        '</div>'
    ).appendTo(body);
    
    this._elmts = DOM.bind(html);
    
    var left_footer = footer.find(".left");    
    
    var right_footer = footer.find(".right");    
    $('<button></button>').text("Close").click(function() { self._dismiss(); }).appendTo(right_footer);
    
    this._renderMatrix(theProject.columnModel.columns);
    this._level = DialogSystem.showDialog(frame);
};

ScatterplotDialog.prototype._renderMatrix = function(columns) {
    var self = this;
    
    var container = this._elmts.tableContainer;
    
    if (columns.length > 0) {
        var table = $('<table></table>').addClass("scatterplot-matrix-table")[0];
        
        for (var i = 0; i < columns.length; i++) {
            var tr = table.insertRow(table.rows.length);
            for (var j = 0; j < i; j++) {
                var url = "/command/get-scatterplot?" + $.param({
                    project: theProject.id,
                    engine: JSON.stringify(ui.browsingEngine.getJSON()), 
                    plotter: JSON.stringify({ 
                        'cx' : columns[i].name, 
                        'cy' : columns[j].name,
                        'w' : 20,
                        'h' : 20
                    }) 
                });
                $(tr.insertCell(j)).html('<img class="scatterplot" title="' + columns[i].name + ' vs. ' + columns[j].name + '" src="' + url + '" />');
            }
            $(tr.insertCell(i)).text(columns[i]);
            for (var j = i + 1; j < columns.length; j++) {
                $(tr.insertCell(j)).text(" ");
            }
        }
        
        container.empty().append(table);
                
    } else {
        container.html(
            '<div style="margin: 2em;"><div style="font-size: 130%; color: #333;">There are no columns in this dataset</div></div>'
        );
    }
    
};

ScatterplotDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

