/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

function ScatterplotDialog(column) {
    this._column = column;
    this._plot_method = "lin";
    this._createDialog();
    this._active = true;
}

ScatterplotDialog.prototype._createDialog = function() {
    var self = this;
    var dialog = $(DOM.loadHTML("core", "scripts/dialogs/scatterplot-dialog.html"));
    this._elmts = DOM.bind(dialog);
    this._elmts.dialogHeader.text(
            $.i18n('core-dialogs/scatterplot-matrix') + 
            ((typeof this._column == "undefined") ? "" : $.i18n('core-dialogs/focusing-on-column', this._column)));

    this._elmts.closeButton.on('click',function() { self._dismiss(); });
    this._elmts.or_dialog_linplot.attr("title", $.i18n('core-dialogs/linear-plot'));
    this._elmts.or_dialog_logplot.attr("title", $.i18n('core-dialogs/logarithmic-plot'));
    this._elmts.or_dialog_counter.attr("title", $.i18n('core-dialogs/rotated-counter-clock'));
    this._elmts.or_dialog_norot.attr("title", $.i18n('core-dialogs/no-rotation'));
    this._elmts.or_dialog_clock.attr("title", $.i18n('core-dialogs/rotated-clock'));
    this._elmts.or_dialog_smallDot.attr("title", $.i18n('core-dialogs/small-dot'));
    this._elmts.or_dialog_regularDot.attr("title", $.i18n('core-dialogs/regular-dot'));
    this._elmts.or_dialog_bigDot.attr("title", $.i18n('core-dialogs/big-dot'));
    this._elmts.closeButton.text($.i18n('core-buttons/close'));
    
    this._elmts.plotSelector.buttonset().on('change',function() {
        self._plot_method = $(this).find("input:checked").val();
        self._renderMatrix();
    });

    this._elmts.rotationSelector.buttonset().on('change',function() {
        self._rotation = $(this).find("input:checked").val();
        self._renderMatrix();
    });
    
    this._elmts.dotSelector.buttonset().on('change',function() {
        var dot_size = $(this).find("input:checked").val();
        if (dot_size == "small") {
            self._dot_size = 0.4;
        } else if (dot_size == "big") {
            self._dot_size = 1.4;
        } else {
            self._dot_size = 0.8;
        }
        self._renderMatrix();
    });
    
    this._level = DialogSystem.showDialog(dialog);
    this._renderMatrix();
    //the function buttonset() groups the input buttons into one but in doing so it creates icon on the input button
    //the icon is created using checkboxradio() 
    //to get rid of the icon a class "no-icon" is directly applied to input button and checkboxradio() is called again with option :- icon=false  
    $(".no-icon").checkboxradio("option", "icon", false);
    //this function only works after initialisation
};

ScatterplotDialog.prototype._renderMatrix = function() {
    var self = this;
    
    var container = this._elmts.tableContainer.html(
        '<div style="margin: 1em; font-size: 130%; color: #888; background-color: white;">'+$.i18n('core-dialogs/focusing-on')+' <img src="images/small-spinner.gif"></div>'
    );

    if (theProject.columnModel.columns.length > 0) {
        var params = {
            project: theProject.id
        };
        $.getJSON("command/core/get-columns-info?" + $.param(params),function(data) {
            if (data === null || typeof data.length == 'undefined') {
                container.html($.i18n('core-dialogs/error-getColumnInfo'));
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
                self._dot_size = 0.8;
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
                var url = "command/core/get-scatterplot?" + $.param(params);

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
                table += '<td class="' + div_class + '" colspan="' + (i + 1) + '">' + columns[i].name + '</td>';
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
            
            container.find("a").on('click',function() {
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
                self._dismiss();
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
                    img.on("load", function() {
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
            });
        });
    } else {
        container.html(
            '<div style="margin: 2em;"><div style="font-size: 130%; color: #333;">'+$.i18n('core-dialogs/no-column-dataset')+ '</div></div>'
        );
    }
    
};

ScatterplotDialog.prototype._dismiss = function() {
    this._active = false;
    DialogSystem.dismissUntil(this._level - 1);
};

