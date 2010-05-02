function ScatterplotDialog(column) {
    this._column = column;
    this._plot_method = "lin";
    this._createDialog();
    this._active = true;
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
                        '<option selected="true" value="lin">linear</option>' +
                        '<option value="log">log-log</option>' +
                    '</select></span>' +
                    '<span class="clustering-dialog-controls">Plot Size: <input bind="plotSize" type="test" size="2" value=""> px</span>' +
                    '<span class="clustering-dialog-controls">Dot Size: <input bind="dotSize" type="test" size="2" value=""> px</span>' +
                    '<span class="clustering-dialog-controls">Rotation: <select bind="rotationSelector">' +
                    '<option selected="true" value="none">none</option>' +
                    '<option value="cw">45° clockwise</option>' +
                    '<option value="ccw">45° counter-clockwise</option>' +
                '</select></span>' +
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
        self._plot_method = $(this).find("option:selected").attr("value");
        self._renderMatrix();
    });

    this._elmts.rotationSelector.change(function() {
        self._rotation = $(this).find("option:selected").attr("value");
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
    $('<button></button>').text("Done").click(function() { self._dismiss(); }).appendTo(right_footer);
    
    this._level = DialogSystem.showDialog(frame);
    this._renderMatrix();
};

ScatterplotDialog.prototype._renderMatrix = function() {
    var self = this;
    
    var container = this._elmts.tableContainer.html(
        '<div style="margin: 1em; font-size: 130%; color: #888; background-color: white;">Processing... <img src="/images/small-spinner.gif"></div>'
    );

    if (theProject.columnModel.columns.length > 0) {
        var params = {
            project: theProject.id
        };
        $.getJSON("/command/get-columns-info?" + $.param(params),function(data) {
            if (data == null || typeof data.length == 'undefined') {
                container.html("Error calling 'get-columns-info'");
                return;
            }
                
            var columns = [];
            for (var i = 0; i < data.length; i++) {
                if (data[i].is_numeric) {
                    columns.push(data[i]);
                }
            }
                
            if (typeof self._plot_size == 'undefined') {
                self._plot_size = Math.max(Math.floor(500 / columns.length / 5) * 5,20);
                self._dot_size = 0.4;
                self._elmts.plotSize.val(self._plot_size);                
                self._elmts.dotSize.val(self._dot_size);                
            }
            
            var table = '<table class="scatterplot-matrix-table"><tbody>';
            
            var createScatterplot = function(cx, cy) {
                var title = cx + ' (x) vs. ' + cy + ' (y)';
                var link = '<a href="javascript:{}" title="' + title + '" cx="' + cx + '" cy="' + cy + '">';
                var plotter_params = { 
                    'cx' : cx, 
                    'cy' : cy,
                    'l' : self._plot_size,
                    'dot': self._dot_size,
                    'dim_x': self._plot_method,
                    'dim_y': self._plot_method,
                    'r': self._rotation
                };
                var params = {
                    project: theProject.id,
                    engine: JSON.stringify(ui.browsingEngine.getJSON()), 
                    plotter: JSON.stringify(plotter_params) 
                };
                var url = "/command/get-scatterplot?" + $.param(params);

                var attrs = [
                    'width="' + self._plot_size + '"',
                    'height="' + self._plot_size + '"',
                    'src2="' + url + '"'
                ];
                
                return link + '<img ' + attrs.join(' ') + ' /></a>';
            };
    
            for (var i = 0; i < columns.length; i++) {
                table += '<tr>';
                var div_class = "column_header";
                if (columns[i].name == self._column) div_class += " current_column";
                table += '<td class="' + div_class + '" colspan="' + (i + 1) + '">' + columns[i].name + '</td>'
                for (var j = i + 1; j < columns.length; j++) {
                    var cx = columns[i].name;
                    var cy = columns[j].name;
                    
                    var div_class = "scatterplot";
                    var current = cx == self._column || cy == self._column;
                    if (current) div_class += " current_column";
                    table += '<td><div class="' + div_class + '">' + createScatterplot(cx,cy) + '</div></td>';
                }
                table += '</tr>';
            }
    
            table += "</tbody></table>";
            
            var width = container.width();
            container.empty().css("width", width + "px").html(table);
            
            container.find("a").click(function() {
                var options = {
                    "name" : $(this).attr("title"),
                    "cx" : $(this).attr("cx"), 
                    "cy" : $(this).attr("cy"), 
                    "l" : 150,
                    "ex" : "value",
                    "ey" : "value",
                    "dot" : self._dot_size,
                    "dim_x" : self._plot_method,
                    "dim_y" : self._plot_method,
                    'r': self._rotation
                };
                ui.browsingEngine.addFacet("scatterplot", options);
                //self._dismiss();
            });

            var load_images = function(data) {
                if (self._active) {
                    data.batch = 0;
                    var end = Math.min(data.index + data.batch_size,data.images.length);
                    for (; data.index < end; data.index++) {
                        load_image(data);
                    }
                }
            };
            
            var load_image = function(data) {
                var img = $(data.images[data.index]);                
                var src2 = img.attr("src2");
                if (src2) {
                    img.attr("src", src2);
                    img.removeAttr("src2");
                    img.load(function() {
                        data.batch++;
                        if (data.batch == data.batch_size) {
                            load_images(data);
                        }
                    });
                }
            };
            
            load_images({
                index : 0,
                batch_size: 4,
                images : container.find(".scatterplot img")
            })
        });
    } else {
        container.html(
            '<div style="margin: 2em;"><div style="font-size: 130%; color: #333;">There are no columns in this dataset</div></div>'
        );
    }
    
};

ScatterplotDialog.prototype._dismiss = function() {
    this._active = false;
    DialogSystem.dismissUntil(this._level - 1);
};

