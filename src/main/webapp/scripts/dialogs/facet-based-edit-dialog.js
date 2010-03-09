function FacetBasedEditDialog(columnName, expression) {
    this._columnName = columnName;
    this._expression = expression;
    this._method = "binning";
    this._function = "fingerprint";
    this._params = {};
    
    this._facets = [];
    
    this._createDialog();
    this._cluster();
}

FacetBasedEditDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("900px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Cluster & Edit column " + this._columnName).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    var html = $(
        '<div>' +
            '<div class="facet-based-edit-dialog-controls"><table width="100%">' +
                '<tr>' +
                    '<td>' +
                        'Method: <select bind="methodSelector">' +
                            '<option selected="true">key collision</option>' +
                            '<option>nearest neightbor</option>' +
                        '</select>' +
                    '</td>' +
                    '<td>' +
                        '<div class="binning-controls">Keying Function: <select bind="keyingFunctionSelector">' +
                            '<option selected="true">fingerprint</option>' +
                            '<option>ngram-fingerprint</option>' +
                            '<option>double-metaphone</option>' +
                            '<option>metaphone</option>' +
                            '<option>soundex</option>' +
                        '</select></div>' +
                        '<div class="knn-controls hidden">Distance Function: <select bind="distanceFunctionSelector">' +
                            '<option selected="true">levenshtein</option>' +
                            '<option>jaro</option>' +
                            '<option>jaccard</option>' +
                            '<option>gzip</option>' +
                            '<option>bzip2</option>' +
                            '<option>PPM</option>' +
                        '</select></div>' +
                    '</td>' +
                    '<td>' +
                        '<div id="ngram-fingerprint-params" class="function-params hidden">' +
                          'Ngram Size: <input type="text" value="1" bind="ngramSize" size="3">' +
                        '</div>' + 
                        '<div class="knn-controls hidden">' +
                            'Radius: <input type="text" value="0.1" bind="radius" size="3">' +
                        '</div>' + 
                    '</td>' +
                    '<td bind="resultSummary" align="right">' +
                    '</td>' +
                '</tr>' +
                '<tr>' +
                    '<td colspan="3">' +
                        '<div bind="tableContainer" class="facet-based-edit-dialog-table-container"></div>' +
                    '</td>' +
                    '<td bind="facetContainer" width="200"></td>' +
                '</tr>' +
            '</table></div>' +
        '</div>'
    ).appendTo(body);
    
    this._elmts = DOM.bind(html);

    this._elmts.methodSelector.change(function() {
        var selection = $(this).find("option:selected").text();
        if (selection == 'key collision') {
            body.find(".binning-controls").show();
            body.find(".knn-controls").hide();
            self._method = "binning";
            self._elmts.keyingFunctionSelector.change();
        } else if (selection = 'nearest neightbor') {
            body.find(".binning-controls").hide();
            body.find(".knn-controls").show();
            self._method = "knn";
            self._elmts.distanceFunctionSelector.change();
        }
    });

    var changer = function() {
        self._function = $(this).find("option:selected").text();
        $(".function-params").hide();
        $("#" + self._function + "-params").show();
        self._cluster();
    };
    
    this._elmts.keyingFunctionSelector.change(changer);
    this._elmts.distanceFunctionSelector.change(changer);
    
    this._elmts.ngramSize.change(function() {
        try {
            self._params = { "ngram-size" : parseInt($(this).val()) };
            self._cluster();
        } catch (e) {
            alert("ngram size must be a number");
        }
    });

    this._elmts.radius.change(function() {
        try {
            self._params = { "radius" : parseFloat($(this).val()) };
            self._cluster();
        } catch (e) {
            alert("radius must be a number");
        }
    });
    
    //this._elmts.clusterButton.click(function() { self._cluster(); });
    //this._elmts.unclusterButton.click(function() { self._uncluster(); });
    
    $('<button></button>').text("Apply & Re-Cluster").click(function() { self._onApplyReCluster(); }).appendTo(footer);
    $('<button></button>').text("Apply & Close").click(function() { self._onApplyClose(); }).appendTo(footer);
    $('<button></button>').text("Close").click(function() { self._dismiss(); }).appendTo(footer);
    
    this._level = DialogSystem.showDialog(frame);
    
    $("#recon-dialog-tabs").tabs();
    $("#recon-dialog-tabs-strict").css("display", "");
};

FacetBasedEditDialog.prototype._renderTable = function(clusters) {
    var self = this;
    
    var container = this._elmts.tableContainer;
    var table = $('<table></table>').addClass("facet-based-edit-dialog-entry-table")[0];
    
    var trHead = table.insertRow(table.rows.length);
    trHead.className = "header";
    $(trHead.insertCell(0)).text("Cluster Size");
    $(trHead.insertCell(1)).text("Row Count");
    $(trHead.insertCell(2)).text("Values in Cluster");
    $(trHead.insertCell(3)).text("Edit?");
    $(trHead.insertCell(4)).text("New Cell Value");
    
    var renderCluster = function(cluster) {
        var tr = table.insertRow(table.rows.length);
        tr.className = table.rows.length % 2 == 0 ? "odd" : "even";
        
        $(tr.insertCell(0)).text(cluster.choices.length);
        
        $(tr.insertCell(1)).text(cluster.rowCount);
        
        var ul = $('<ul>');
        var choices = cluster.choices;
        var rowCount = 0;
        for (var c = 0; c < choices.length; c++) {
            var choice = choices[c];
            var li = $('<li>').appendTo(ul);
            $('<span>').text(choice.v).appendTo(li);
            $('<span>').text("(" + choice.c + " rows)").addClass("facet-based-edit-dialog-entry-count").appendTo(li);
            
            rowCount += choice.c;
        }
        $(tr.insertCell(2)).append(ul);
        
        var editCheck = $('<input type="checkbox" />')
            .appendTo(tr.insertCell(3))
            .click(function() {
                cluster.edit = !cluster.edit;
            });
        if (cluster.edit) {
            editCheck.attr("checked", "true");
        }
        
        var input = $('<input size="25" />')
            .attr("value", cluster.value)
            .appendTo(tr.insertCell(4))
            .keyup(function() {
                cluster.value = this.value;
            });
    };
    for (var i = 0; i < clusters.length; i++) {
        renderCluster(clusters[i]);
    }
    
    container.empty().append(table);
    
    this._elmts.resultSummary.text(
        (clusters.length === this._clusters.length) ?
            (this._clusters.length + " clusters found.") :
            (clusters.length + " clusters filtered from " + this._clusters.length + " total.")
    );
};

FacetBasedEditDialog.prototype._cluster = function() {
    var self = this;
    
    var container = this._elmts.tableContainer.html(
        '<div style="margin: 1em; font-size: 130%; color: #888;">Loading... <img src="/images/small-spinner.gif"></div>'
    );
    
    this._elmts.resultSummary.empty();

    $.post(
        "/command/compute-clusters?" + $.param({ project: theProject.id }),
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
}

FacetBasedEditDialog.prototype._updateData = function(data) {
    var clusters = [];
    $.each(data, function() {
        var cluster = {
            edit: true,
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

FacetBasedEditDialog.prototype._onApplyClose = function() {
    var self = this;        
    this._apply(function() {
        self._dismiss();
    });
};

FacetBasedEditDialog.prototype._onApplyReCluster = function() {
    var self = this;        
    this._apply(function() {
        self._cluster();
    });
};

FacetBasedEditDialog.prototype._apply = function(onDone) {
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
        Gridworks.postProcess(
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

FacetBasedEditDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

FacetBasedEditDialog.prototype._getBaseClusters = function() {
    return [].concat(this._clusters);
};

FacetBasedEditDialog.prototype._getRestrictedClusters = function(except) {
    var clusters = this._getBaseClusters();
    for (var i = 0; i < this._facets.length; i++) {
        var facet = this._facets[i].facet;
        if (except !== facet) {
            clusters = facet.restrict(clusters);
        }
    }
    return clusters;
};

FacetBasedEditDialog.prototype._updateAll = function() {
    for (var i = 0; i < this._facets.length; i++) {
        var facet = this._facets[i].facet;
        var clusters = this._getRestrictedClusters(facet);
        facet.update(clusters);
    }
    this._renderTable(this._getRestrictedClusters());
};

FacetBasedEditDialog.prototype._resetFacets = function() {
    for (var i = 0; i < this._facets.length; i++) {
        var r = this._facets[i];
        r.facet.dispose();
        r.elmt.remove();
    }
    this._facets = [];
    
    this._createFacet("Cluster Size", "size");
    this._createFacet("Row Count", "rowCount");
    this._createFacet("Value Length Average", "avg");
    this._createFacet("Value Length Variance", "variance");
};

FacetBasedEditDialog.prototype._createFacet = function(title, property) {
    var elmt = $('<div>').appendTo(this._elmts.facetContainer);
    this._facets.push({
        elmt: elmt,
        facet: new FacetBasedEditDialog.Facet(this, title, property, elmt, this._getBaseClusters())
    });
};

FacetBasedEditDialog.Facet = function(dialog, title, property, elmt, clusters) {
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
        this._bins = [];
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
        }
        this._baseBins = this._computeDistribution(clusters);
        
        this._from = this._min;
        this._to = this._max;
        
        elmt.addClass("facet-based-edit-dialog-facet");
        var html = $(
            '<div class="facet-based-edit-dialog-facet-header">' + title + '</div>' +
            '<div class="facet-based-edit-dialog-facet-histogram" bind="histogramContainer"></div>' +
            '<div class="facet-based-edit-dialog-facet-slider" bind="slider"></div>' +
            '<div class="facet-based-edit-dialog-facet-selection" bind="selectionContainer"></div>'
        ).appendTo(elmt);
        
        this._elmts = DOM.bind(html);
        this._elmts.slider.slider({
            min: this._min,
            max: this._max,
            values: [ this._from, this._to ],
            stop: function(evt, ui) {
                self._from = ui.values[0];
                self._to = ui.values[1];
                self._setRangeIndicators();
                self._dialog._updateAll();
            }
        });
        this._setRangeIndicators();
    }
};

FacetBasedEditDialog.Facet.prototype.dispose = function() {
};

FacetBasedEditDialog.Facet.prototype.restrict = function(clusters) {
    if (this._baseBins.length == 0 || (this._from == this._min && this._to == this._max)) {
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

FacetBasedEditDialog.Facet.prototype.update = function(clusters) {
    if (this._baseBins.length == 0) {
        return;
    }
    
    var bins = this._computeDistribution(clusters);
    
    var max = 0;
    for (var i = 0; i < this._baseBins.length; i++) {
        max = Math.max(max, this._baseBins[i]);
    }
    
    var values = [];
    var diffs = [];
    for (var i = 0; i < this._baseBins.length; i++) {
        var v = Math.ceil(100 * bins[i] / max);
        var diff = Math.ceil(100 * this._baseBins[i] / max) - v;
        
        values.push(v == 0 ? 0 : Math.max(2, v)); // use min 2 to make sure something shows up
        diffs.push(diff == 0 ? 0 : Math.max(2, diff));
    }
        
    this._elmts.histogramContainer.empty();
    $('<img />').attr("src", 
        "http://chart.apis.google.com/chart?" + [
            "chs=" + this._elmts.histogramContainer[0].offsetWidth + "x50",
            "cht=bvs&chbh=r,0&chco=000088,aaaaff",
            "chd=t:" + values.join(",") + "|" + diffs.join(",")
        ].join("&")
    ).appendTo(this._elmts.histogramContainer);
};

FacetBasedEditDialog.Facet.prototype._setRangeIndicators = function() {
    this._elmts.selectionContainer.text(this._from + " to " + this._to);
};

FacetBasedEditDialog.Facet.prototype._computeDistribution = function(clusters) {
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
