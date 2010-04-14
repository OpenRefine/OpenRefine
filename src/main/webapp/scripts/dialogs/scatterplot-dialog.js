function ScatterplotDialog(column) {
    this._column = column;
    this._plot_method = "lin";
    this._plot_size = Math.max(Math.floor("500" / theProject.columnModel.columns.length / 5) * 5,"20");
    this._dot_size = 0.3;
    this._createDialog();
}

ScatterplotDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("1100px");
    
    var header = $('<div></div>').addClass("dialog-header").text('Scatterplot Matrix' + ((typeof this._column == "undefined") ? "" : " (focusing on '" + this._column + "')")).appendTo(frame);
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
                    '<span class="clustering-dialog-controls">Plot Size: <input bind="plotSize" type="test" size="2" value="' + this._plot_size + '"> px</span>' +
                    '<span class="clustering-dialog-controls">Dot Size: <input bind="dotSize" type="test" size="2" value="' + this._dot_size + '"> px</span>' +
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
        var table = '<table class="scatterplot-matrix-table"><tbody>';
        
        var createScatterplot = function(cx, cy) {
            var title = cx + ' (x) vs. ' + cy + ' (y)';
            var link = '<a href="javascript:{}" title="' + title + '" cx="' + cx + '" cy="' + cy + '">';
            var plotter_params = { 
                'cx' : cx, 
                'cy' : cy,
                'w' : self._plot_size * 3,
                'h' : self._plot_size * 3,
                'dot': self._dot_size,
                'dim': self._plot_method
            };
            var params = {
                project: theProject.id,
                engine: JSON.stringify(ui.browsingEngine.getJSON()), 
                plotter: JSON.stringify(plotter_params) 
            };
            var url = "/command/get-scatterplot?" + $.param(params);
            return link + '<img src="' + url + '" width="' + self._plot_size + '" height="' + self._plot_size + '" /></a>';
        };

        for (var i = 0; i < columns.length; i++) {
            table += '<tr>';
            var div_class = "column_header";
            if (columns[i].name == this._column) div_class += " current_column";
            table += '<td class="' + div_class + '" colspan="' + (i + 1) + '">' + columns[i].name + '</td>'
            for (var j = i + 1; j < columns.length; j++) {
                var cx = columns[i].name;
                var cy = columns[j].name;
                
                var div_class = "scatterplot";
                if (cx == this._column || cy == this._column) div_class += " current_column";
                table += '<td><div class="' + div_class + '">' + createScatterplot(cx,cy) + '</div></td>';
            }
            table += '</tr>';
        }

        table += "</tbody></table>";
        
        var width = container.width();
        container.empty().css("width", width + "px").append($(table));
        
        container.find("a").click(function() {
            var options = {
                "name" : $(this).attr("title"),
                "x_columnName" : $(this).attr("cx"), 
                "y_columnName" : $(this).attr("cy"), 
                "x_expression" : "value",
                "y_expression" : "value",
                "dot" : self._dot_size,
                "dim" : self._plot_method
            };
            console.log(options);
            ui.browsingEngine.addFacet("scatterplot", options);
            //self._dismiss();
        });
    
        container.find(".scatterplot").hover(
            function() {                
                $(this).find('img').addClass("hover");
            } , function() {
                $(this).find('img').removeClass("hover");
            }
        );
    } else {
        container.html(
            '<div style="margin: 2em;"><div style="font-size: 130%; color: #333;">There are no columns in this dataset</div></div>'
        );
    }
    
};

ScatterplotDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

