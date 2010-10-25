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

function ClusteringDialog(columnName, expression) {
    this._columnName = columnName;
    this._expression = expression;
    this._method = "binning";
    this._function = "fingerprint";
    this._params = {};
    
    this._facets = [];
    
    this._createDialog();
    this._cluster();
}

ClusteringDialog.prototype._createDialog = function() {
    var self = this;
    var dialog = $(DOM.loadHTML("core", "scripts/dialogs/clustering-dialog.html"));

    this._elmts = DOM.bind(dialog);
    this._elmts.dialogHeader.text('Cluster & Edit column "' + this._columnName + '"');

    this._elmts.methodSelector.change(function() {
        var selection = $(this).find("option:selected").text();
        if (selection == 'key collision') {
            dialog.find(".binning-controls").show();
            dialog.find(".knn-controls").hide();
            self._method = "binning";
            self._elmts.keyingFunctionSelector.change();
        } else if (selection === 'nearest neighbor') {
            dialog.find(".binning-controls").hide();
            dialog.find(".knn-controls").show();
            self._method = "knn";
            self._elmts.distanceFunctionSelector.change();
        }
    });

    var changer = function() {
        self._function = $(this).find("option:selected").text();
        $(".function-params").hide();
        $("#" + self._function + "-params").show();
        params_changer();
    };
    
    this._elmts.keyingFunctionSelector.change(changer);
    this._elmts.distanceFunctionSelector.change(changer);
    
    var params_changer = function() {
        self._params = {};
        $(".dialog-body input.param:visible").each(function() {
            var e = $(this);
            var name = e.attr('name');
            var datatype = e.attr('datatype') || 'string';
            var value = e.val(); 
            if (datatype == 'int') {
                value = parseInt(value,10);
            } else if (datatype == 'float') {
                value = parseFloat(value);
            }   
            self._params[name] = value;
        });
        self._cluster();
    };
    
    this._elmts.ngramSize.change(params_changer);
    this._elmts.radius.change(params_changer);
    this._elmts.ngramBlock.change(params_changer);
    
    this._elmts.selectAllButton.click(function() { self._selectAll(); });
    this._elmts.deselectAllButton.click(function() { self._deselectAll(); });
    this._elmts.applyReClusterButton.click(function() { self._onApplyReCluster(); });
    this._elmts.applyCloseButton.click(function() { self._onApplyClose(); });
    this._elmts.closeButton.click(function() { self._dismiss(); });
    
    this._level = DialogSystem.showDialog(dialog);
};

ClusteringDialog.prototype._renderTable = function(clusters) {
    var self = this;
    
    var container = this._elmts.tableContainer;
    
    if (clusters.length > 0) {
        var table = $('<table></table>').addClass("clustering-dialog-entry-table")[0];
        
        var trHead = table.insertRow(table.rows.length);
        trHead.className = "header";
        $(trHead.insertCell(0)).text("Cluster Size");
        $(trHead.insertCell(1)).text("Row Count");
        $(trHead.insertCell(2)).text("Values in Cluster");
        $(trHead.insertCell(3)).text("Merge?");
        $(trHead.insertCell(4)).text("New Cell Value");

        var renderCluster = function(cluster) {
            var tr = table.insertRow(table.rows.length);
            tr.className = table.rows.length % 2 === 0 ? "odd" : "even";
            
            $(tr.insertCell(0)).text(cluster.choices.length);
            
            $(tr.insertCell(1)).text(cluster.rowCount);

            var facet = {
                "c": {
                    "type":"list",
                    "name": self._columnName,
                    "columnName": self._columnName,
                    "expression":"value"
                },
                "o":{
                    "sort":"name"
                },
                "s":[
                ]
            };
            
            var ul = $('<ul></ul>');
            var choices = cluster.choices;
            var rowCount = 0;
            var onClick = function() {
              var parent = $(this).closest("tr");
              var value = $(this).text();
              cluster.value = value;
              
              parent.find("input[type='text']").val(value);
              parent.find("input:not(:checked)").attr('checked', true).change();
              return false;
            };
            for (var c = 0; c < choices.length; c++) {
                var choice = choices[c];
                var li = $('<li></li>');
                $('<a href="javascript:{}" title="Use this value"></a>').text(choice.v).click(onClick).appendTo(li);
                $('<span></span>').text("(" + choice.c + " rows)").addClass("clustering-dialog-entry-count").appendTo(li);
                rowCount += choice.c;
                facet.s[c] = {
                    "v": {
                        "v":choice.v,
                        "l":choice.v
                    }
                };
                li.appendTo(ul);
            }
            
            var params = [
                "project=" + escape(theProject.id),
                "ui=" + escape(JSON.stringify({
                    "facets" : [ facet ]
                }))
            ];
            var url = "project?" + params.join("&");
                            
            var div = $('<div></div>').addClass("clustering-dialog-value-focus");
            
            var browseLink = $('<a target="_new" title="Browse only these values">Browse this cluster</a>')
                .addClass("clustering-dialog-browse-focus")
                .attr("href",url)
                .css("visibility","hidden")
                .appendTo(div);

            $(tr.insertCell(2))
                .mouseenter(function() { browseLink.css("visibility", "visible"); })
                .mouseleave(function() { browseLink.css("visibility", "hidden"); })
                .append(ul)
                .append(div);
            
            var editCheck = $('<input type="checkbox" />')
                .change(function() {
                    cluster.edit = !cluster.edit;
                }).appendTo(tr.insertCell(3));

            if (cluster.edit) {
                editCheck.attr("checked", "true");
            }
            
            var input = $('<input size="25" />')
                .attr("value", cluster.value)
                .keyup(function() {
                    cluster.value = this.value;
                }).appendTo(tr.insertCell(4));
        };
        
        for (var i = 0; i < clusters.length; i++) {
            renderCluster(clusters[i]);
        }
        
        container.empty().append(table);
        
        this._elmts.resultSummary.html(
            (clusters.length === this._clusters.length) ?
                ("<b>" + this._clusters.length + "</b> cluster" + ((this._clusters.length != 1) ? "s" : "") + " found") :
                ("<b>" + clusters.length + "</b> cluster" + ((clusters.length != 1) ? "s" : "") + " filtered from <b>" + this._clusters.length + "</b> total")
        );
        
    } else {
        container.html(
            '<div style="margin: 2em;"><div style="font-size: 130%; color: #333;">No clusters were found with the selected method</div><div style="padding-top: 1em; font-size: 110%; color: #888;">Try selecting another method above or changing its parameters</div></div>'
        );
    }
};

ClusteringDialog.prototype._cluster = function() {
    var self = this;
    
    var container = this._elmts.tableContainer.html(
        '<div style="margin: 1em; font-size: 130%; color: #888;">Clustering... <img src="/images/small-spinner.gif"></div>'
    );
    
    this._elmts.resultSummary.empty();

    $.post(
        "/command/core/compute-clusters?" + $.param({ project: theProject.id }),
        { 
            engine: JSON.stringify(ui.browsingEngine.getJSON()), 
            clusterer: JSON.stringify({ 
                'type' : this._method, 
                'function' : this._function,
                'column' : this._columnName,
                'params' : this._params
            }) 
        },
        function(data) {
            self._updateData(data);
        },
        "json"
    );
};

ClusteringDialog.prototype._updateData = function(data) {
    var clusters = [];
    $.each(data, function() {
        var cluster = {
            edit: false,
            choices: this,
            value: this[0].v,
            size: this.length
        };
        
        var sum = 0;
        var sumSquared = 0;
        var rowCount = 0;
        $.each(cluster.choices, function() {
            rowCount += this.c;
        
            var l = this.v.length; 
            sum += l;
            sumSquared += l * l;
        });
        
        cluster.rowCount = rowCount;
        cluster.avg = sum / cluster.choices.length;
        cluster.variance = Math.sqrt(sumSquared / cluster.choices.length - cluster.avg * cluster.avg);
        
        clusters.push(cluster);
    });
    this._clusters = clusters;
    
    this._resetFacets();
    this._updateAll();
};

ClusteringDialog.prototype._selectAll = function() {
    $(".clustering-dialog-entry-table input:not(:checked)").attr('checked', true).change();
};

ClusteringDialog.prototype._deselectAll = function() {
    $(".clustering-dialog-entry-table input:checked").attr('checked', false).change();
};

ClusteringDialog.prototype._onApplyClose = function() {
    var self = this;        
    this._apply(function() {
        self._dismiss();
    });
};

ClusteringDialog.prototype._onApplyReCluster = function() {
    var self = this;        
    this._apply(function() {
        self._cluster();
    });
};

ClusteringDialog.prototype._apply = function(onDone) {
    var clusters = this._getRestrictedClusters();
    var edits = [];
    for (var i = 0; i < clusters.length; i++) {
        var cluster = clusters[i];
        if (cluster.edit) {
            var values = [];
            for (var j = 0; j < cluster.choices.length; j++) {
                values.push(cluster.choices[j].v);
            }
            
            edits.push({
                from: values,
                to: cluster.value
            });
        }
    }
    
    if (edits.length > 0) {
        Refine.postCoreProcess(
            "mass-edit",
            {},
            {
                columnName: this._columnName,
                expression: this._expression,
                edits: JSON.stringify(edits)
            },
            { cellsChanged: true },
            {
                onError: function(o) {
                    alert("Error: " + o.message);
                },
                onDone: onDone
            }
        );
    } else {
        alert("You must check some Edit? checkboxes for your edits to be applied.");
    }
};

ClusteringDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

ClusteringDialog.prototype._getBaseClusters = function() {
    return [].concat(this._clusters);
};

ClusteringDialog.prototype._getRestrictedClusters = function(except) {
    var clusters = this._getBaseClusters();
    for (var i = 0; i < this._facets.length; i++) {
        var facet = this._facets[i].facet;
        if (except !== facet) {
            clusters = facet.restrict(clusters);
        }
    }
    return clusters;
};

ClusteringDialog.prototype._updateAll = function() {
    for (var i = 0; i < this._facets.length; i++) {
        var facet = this._facets[i].facet;
        var clusters = this._getRestrictedClusters(facet);
        facet.update(clusters);
    }
    this._renderTable(this._getRestrictedClusters());
};

ClusteringDialog.prototype._resetFacets = function() {
    for (var i = 0; i < this._facets.length; i++) {
        var r = this._facets[i];
        r.facet.dispose();
        r.elmt.remove();
    }
    this._facets = [];
    
    this._createFacet("# Choices in Cluster", "size");
    this._createFacet("# Rows in Cluster", "rowCount");
    this._createFacet("Average Length of Choices", "avg");
    this._createFacet("Length Variance of Choices", "variance");
};

ClusteringDialog.prototype._createFacet = function(title, property) {
    var elmt = $('<div>').appendTo(this._elmts.facetContainer);
    this._facets.push({
        elmt: elmt,
        facet: new ClusteringDialog.Facet(this, title, property, elmt, this._getBaseClusters())
    });
};

ClusteringDialog.Facet = function(dialog, title, property, elmt, clusters) {
    this._dialog = dialog;
    this._property = property;
    
    var self = this;
    
    var max = Number.NEGATIVE_INFINITY;
    var min = Number.POSITIVE_INFINITY;
    for (var i = 0; i < clusters.length; i++) {
        var cluster = clusters[i];
        var val = cluster[property];
        max = Math.max(max, val);
        min = Math.min(min, val);
    }
    
    this._min = min;
    this._max = max;
    if (min >= max) {
        this._step = 0;
        this._baseBins = [];
    } else {
        var diff = max - min;
        
        this._step = 1;
        if (diff > 10) {
            while (this._step * 100 < diff) {
                this._step *= 10;
            }
        } else {
            while (this._step * 100 > diff) {
                this._step /= 10;
            }
        }
        
        this._min = (Math.floor(this._min / this._step) * this._step);
        this._max = (Math.ceil(this._max / this._step) * this._step);
        this._binCount = 1 + Math.ceil((this._max - this._min) / this._step);
        if (this._binCount > 100) {
            this._step *= 2;
            this._binCount = Math.round((1 + this._binCount) / 2);
        } else if (this._binCount < 3) {
            this._step /= 2;
            this._binCount *= 2;
            this._max = (Math.ceil(this._max / this._step) * this._step);
        }
        this._baseBins = this._computeDistribution(clusters);
        
        this._from = this._min;
        this._to = this._max;
        
        elmt.addClass("clustering-dialog-facet");
        var html = $(
            '<div class="clustering-dialog-facet-header">' + title + '</div>' +
            '<div class="clustering-dialog-facet-slider" bind="sliderWidgetDiv">' +
                '<div class="clustering-dialog-facet-histogram" bind="histogramContainer"></div>' +
            '</div>' +
            '<div class="clustering-dialog-facet-selection" bind="selectionContainer"></div>'
        ).appendTo(elmt);
        
        this._elmts = DOM.bind(html);
        
        this._histogram = new HistogramWidget(this._elmts.histogramContainer, { binColors: [ "#ccccff", "#6666ff" ] });
        this._sliderWidget = new SliderWidget(this._elmts.sliderWidgetDiv);
        
        this._elmts.sliderWidgetDiv.bind("slide", function(evt, data) {
            self._from = data.from;
            self._to = data.to;
            self._setRangeIndicators();
        }).bind("stop", function(evt, data) {
            self._from = data.from;
            self._to = data.to;
            self._setRangeIndicators();
            self._dialog._updateAll();
        });

        this._setRangeIndicators();
    }
};

ClusteringDialog.Facet.prototype.dispose = function() {
};

ClusteringDialog.Facet.prototype.restrict = function(clusters) {
    if (!this._baseBins.length || (this._from == this._min && this._to == this._max)) {
        return clusters;
    }
    
    var clusters2 = [];
    for (var i = 0; i < clusters.length; i++) {
        var cluster = clusters[i];
        var val = cluster[this._property];
        if (val >= this._from && val <= this._to) {
            clusters2.push(cluster);
        }
    }
    return clusters2;
};

ClusteringDialog.Facet.prototype.update = function(clusters) {
    if (!this._baseBins.length) {
        return;
    }
    
    var bins = this._computeDistribution(clusters);

    this._sliderWidget.update(
        this._min, 
        this._max, 
        this._step, 
        this._from,
        this._to
    );
    this._histogram.update(
        this._min, 
        this._max, 
        this._step, 
        [ this._baseBins, bins ]
    );
};

ClusteringDialog.Facet.prototype._setRangeIndicators = function() {
    this._elmts.selectionContainer.html(this._from + " &mdash; " + this._to);
};

ClusteringDialog.Facet.prototype._computeDistribution = function(clusters) {
    var bins = [];
    for (var b = 0; b < this._binCount; b++) {
        bins.push(0);
    }
    
    for (var i = 0; i < clusters.length; i++) {
        var cluster = clusters[i];
        var val = cluster[this._property];
        var bin = Math.round((val - this._min) / this._step);
        bins[bin]++;
    }
    
    return bins;
};
