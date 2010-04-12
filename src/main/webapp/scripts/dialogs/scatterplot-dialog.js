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
                var cx = columns[i];
                var cy = columns[j];
                var plotter_params = { 
                    'cx' : cx.name, 
                    'cy' : cy.name,
                    'w' : 20,
                    'h' : 20
                };
                var params = {
                    project: theProject.id,
                    engine: JSON.stringify(ui.browsingEngine.getJSON()), 
                    plotter: JSON.stringify(plotter_params) 
                }                
                var url = "/command/get-scatterplot?" + $.param(params);
                var name = cx.name + '(x) vs. ' + cy.name + '(y)';
                var cell = $(tr.insertCell(j));
                var link = $('<a href="javascript:{}"></a>').attr("title",name).click(function() {
                    ui.browsingEngine.addFacet(
                        "scatterplot", 
                        {
                            "name" : name,
                            "x_column" : cx.name, 
                            "y_column" : cy.name, 
                            "expression" : "value",
                            "mode" : "scatterplot"
                        }
                    );
                    //self._dismiss();
                });
                var plot = $('<img src="' + url + '" />').addClass("scatterplot").appendTo(link);
                link.appendTo(cell);
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

