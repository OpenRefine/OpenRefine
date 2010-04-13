function ScatterplotDialog() {
    this._createDialog();
    this._plot_method = "lin";
    this._plot_size = 20;
    this._dot_size = 0.1;
}

ScatterplotDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("1100px");
    
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
        '<div class="grid-layout layout-normal"><table width="100%">' +
            '<tr>' +
                '<td>' +
                    '<span class="clustering-dialog-controls">Plot type: <select bind="plotSelector">' +
                        '<option selected="true">linear</option>' +
                        '<option>log-log</option>' +
                    '</select></span>' +
                    '<span class="clustering-dialog-controls">Plot Size: <input bind="plotSize" type="test" size="2" value="20"> px</span>' +
                    '<span class="clustering-dialog-controls">Dot Size: <input bind="dotSize" type="test" size="2" value="0.1"> px</span>' +
                '</td>' +
            '</tr>' +
            '<tr>' +
                '<td>' +
                    '<div bind="tableContainer" class="scatterplot-dialog-table-container"></div>' +
                '</td>' +
            '</tr>' +
        '</table></div>'
    ).appendTo(body);
    
    
    this._elmts = DOM.bind(html);
    
    this._elmts.plotSelector.change(function() {
        var selection = $(this).find("option:selected").text();
        if (selection == 'linear') {
            self._plot_method = "lin";
        } else if (selection === 'log-log') {
            self._plot_method = "log";
        }
        self._renderMatrix();
    });

    this._elmts.plotSize.change(function() {
        try {
            self._plot_size = parseInt($(this).val())
            self._renderMatrix();
        } catch (e) {
            alert("Must be a number");
        }
    });

    this._elmts.dotSize.change(function() {
        try {
            self._dot_size = parseFloat($(this).val())
            self._renderMatrix();
        } catch (e) {
            alert("Must be a number");
        }
    });
    
    var left_footer = footer.find(".left");    
    
    var right_footer = footer.find(".right");    
    $('<button></button>').text("Close").click(function() { self._dismiss(); }).appendTo(right_footer);
    
    this._level = DialogSystem.showDialog(frame);
    this._renderMatrix();
};

ScatterplotDialog.prototype._renderMatrix = function() {
    var self = this;
    var columns = theProject.columnModel.columns;
    
    var container = this._elmts.tableContainer.html(
        '<div style="margin: 1em; font-size: 130%; color: #888;">Processing... <img src="/images/small-spinner.gif"></div>'
    );

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
                    'w' : self._plot_size,
                    'h' : self._plot_size,
                    'dot': self._dot_size,
                    'dim': self._plot_method
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

