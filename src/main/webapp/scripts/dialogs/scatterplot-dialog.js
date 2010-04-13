function ScatterplotDialog() {
    this._plot_method = "lin";
    this._plot_size = 20;
    this._dot_size = 0.1;
    this._createDialog();
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
        '<div class="grid-layout layout-normal layout-full"><table>' +
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
        
        var createScatterplot = function(i, j) {
            var cx = columns[i].name;
            var cy = columns[j].name;
            
            var name = cx + ' (x) vs. ' + cy + ' (y)';
            var link = $('<a href="javascript:{}"></a>')
                .attr("title", name)
                .click(function() {
                    var options = {
                        "name" : name,
                        "x_columnName" : cx, 
                        "y_columnName" : cy, 
                        "x_expression" : "value",
                        "y_expression" : "value",
                    };
                    ui.browsingEngine.addFacet("scatterplot", options);
                    //self._dismiss();
                });
                
            var plotter_params = { 
                'cx' : cx, 
                'cy' : cy,
                'w' : self._plot_size,
                'h' : self._plot_size,
                'dot': self._dot_size,
                'dim': self._plot_method
            };
            var params = {
                project: theProject.id,
                engine: JSON.stringify(ui.browsingEngine.getJSON()), 
                plotter: JSON.stringify(plotter_params) 
            };
            var url = "/command/get-scatterplot?" + $.param(params);
            var plot = $('<img src="' + url + '" />')
                .addClass("scatterplot")
                .attr("width", self._plot_size)
                .attr("height", self._plot_size)
                .appendTo(link);
            
            return link;
        };

        for (var i = 0; i < columns.length; i++) {
            var tr = table.insertRow(table.rows.length);
            for (var j = 0; j < i; j++) {
                $(tr.insertCell(j)).append(createScatterplot(i,j));
            }
            
            var tdColumnName = $(tr.insertCell(tr.cells.length));
            tdColumnName
                .text(columns[i].name)
                .css("text-align", "left")
                .attr("colspan", columns.length - i);
        }

        var width = container.width();
        container.empty().css("width", width + "px").append(table);
        
    } else {
        container.html(
            '<div style="margin: 2em;"><div style="font-size: 130%; color: #333;">There are no columns in this dataset</div></div>'
        );
    }
    
};

ScatterplotDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

