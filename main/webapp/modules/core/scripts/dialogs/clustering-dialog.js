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

    let checkedValue = JSON.parse(Refine.getPreference("ui.clustering.auto-update", false));
    this._createDialog();
    if (checkedValue === true) {
        this._cluster();
    } else {
        this._addClusterMessage();
    }
}

ClusteringDialog.prototype._createDialog = function() {
    var self = this;
    var dialog = $(DOM.loadHTML("core", "scripts/dialogs/clustering-dialog.html"));

    this._elmts = DOM.bind(dialog);
    this._elmts.dialogHeader.text($.i18n('core-dialogs/cluster-edit')+' "' + this._columnName + '"');

    this._elmts.or_dialog_descr.html($.i18n('core-dialogs/cluster-descr'));
    this._elmts.or_dialog_findMore.html($.i18n('core-dialogs/find-more'));
    this._elmts.or_dialog_method.html($.i18n('core-dialogs/method'));
    this._elmts.or_dialog_distance.html($.i18n('core-dialogs/distance-fun'));
    this._elmts.or_dialog_keyCollision.html($.i18n('core-dialogs/key-collision'));
    this._elmts.or_dialog_neighbor.html($.i18n('core-dialogs/nearest-neighbor'));
    this._elmts.or_dialog_keying.html($.i18n('core-dialogs/keying-function'));
    this._elmts.or_dialog_ngramSize.html($.i18n('core-dialogs/ngram-size'));
    this._elmts.or_dialog_radius.html($.i18n('core-dialogs/ngram-radius'));
    this._elmts.or_dialog_blockChars.html($.i18n('core-dialogs/block-chars'));
    this._elmts.or_auto_update.html($.i18n('core-facets/auto-update'));
    this._elmts.selectAllButton.html($.i18n('core-buttons/select-all'));
    this._elmts.deselectAllButton.html($.i18n('core-buttons/deselect-all'));
    this._elmts.exportClusterButton.html($.i18n('core-buttons/export-cluster'));
    this._elmts.applyReClusterButton.html($.i18n('core-buttons/merge-cluster'));
    this._elmts.applyCloseButton.html($.i18n('core-buttons/merge-close'));
    this._elmts.closeButton.html($.i18n('core-buttons/close'));

    this._elmts.methodSelector.on('change',function() {
        var selection = $(this).find("option:selected").text();
        if (selection == $.i18n('core-dialogs/key-collision')) {
            dialog.find(".binning-controls").show();
            dialog.find(".knn-controls").hide();
            self._method = "binning";
            self._elmts.keyingFunctionSelector.trigger('change');
        } else if (selection === $.i18n('core-dialogs/nearest-neighbor')) {
            dialog.find(".binning-controls").hide();
            dialog.find(".knn-controls").show();
            self._method = "knn";
            self._elmts.distanceFunctionSelector.trigger('change');
        }
    });

    ClusteringDialog.prototype._addClusterMessage = function () {
        let self = this;
        let container = self._elmts.tableContainer;
        self._elmts.facetContainer.empty();
        container.empty().html(
            '<div style="display: flex; height: inherit; justify-content: center; align-items: center;">' + '<div>' +
            $.i18n('core-dialogs/click-cluster', self._columnName) + '</div>' + '<div">' +
            '<button class="button" bind="clusterButton" id="clusterButtonId" >' +
            $.i18n('core-facets/cluster') + '</button>' + '</div></div>'
        );
        let elmts = DOM.bind(container);
        elmts.clusterButton.html($.i18n('core-facets/cluster'));

        elmts.clusterButton.on('click', function () {
            self._cluster()
        });
    }

    var changer = function() {
        self._function = $(this).find("option:selected").val();
        $(".function-params").hide();
        $("#" + self._function + "-params").show();
        params_changer();
    };

    this._elmts.keyingFunctionSelector.on('change',changer);
    this._elmts.distanceFunctionSelector.on('change',changer);

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
        self._elmts.resultSummary.empty();
        if (document.getElementById("autoId").checked) {
            self._cluster();
        } else {
            self._addClusterMessage();
        }
    };

    this._elmts.ngramSize.on('change',params_changer);
    this._elmts.radius.on('change',params_changer);
    this._elmts.ngramBlock.on('change',params_changer);
    this._elmts.autoCheckbox.on("change", function() {
        let checkbox = document.getElementById("autoId");
        if (checkbox.checked) {
            Refine.setPreference("ui.clustering.auto-update", true);
            self._cluster();
        } else {
            Refine.setPreference("ui.clustering.auto-update", false);
        }
    });

    this._elmts.selectAllButton.on('click',function() { self._selectAll(); });
    this._elmts.deselectAllButton.on('click',function() { self._deselectAll(); });
    this._elmts.exportClusterButton.on('click',function() { self._onExportCluster(); });
    this._elmts.applyReClusterButton.on('click',function() { self._onApplyReCluster(); });
    this._elmts.applyCloseButton.on('click',function() { self._onApplyClose(); });
    this._elmts.closeButton.on('click',function() { self._dismiss(); });

    self._level = DialogSystem.showDialog(dialog);
    var checkedValue = JSON.parse(Refine.getPreference("ui.clustering.auto-update", false));
    document.getElementById("autoId").checked = checkedValue;

    // Fill in all the keyers and distances
    $.get("command/core/get-clustering-functions-and-distances")
    .done(function(data) {
       var keyers = data.keyers != null ? data.keyers : [];
       var distances = data.distances != null ? data.distances : [];
       var i = 0;
       for(; i < keyers.length; i++) {
          var label = $.i18n('clustering-keyers/'+keyers[i]);
          if (label.startsWith('clustering-keyers')) {
              label = keyers[i];
          }
          var option = $('<option></option>')
             .val(keyers[i])
             .text(label)
             .appendTo(self._elmts.keyingFunctionSelector);
          if (i == 0) {
             option.prop('selected', 'true');
          }
       }
       for(i = 0; i < distances.length; i++) {
          var label = $.i18n('clustering-distances/'+distances[i]);
          if (label.startsWith('clustering-distances')) {
              label = distances[i];
          }
          var option = $('<option></option>')
             .val(distances[i])
             .text(label)
             .appendTo(self._elmts.distanceFunctionSelector);
          if (i == 0) {
             option.prop('selected', 'true');
          }
       }
    })
    .fail(function(error) {
            alert($.i18n('core-dialogs/no-clustering-functions-and-distances'));
    });
};

ClusteringDialog.prototype._renderTable = function(clusters) {
    var self = this;

    var container = this._elmts.tableContainer;

    if (clusters.length > 0) {
        // TODO: This will never get rendered because we're blocking rendering
        container.empty().html('<div>Processing clusters...</div>');

        var table = $('<table></table>').addClass("clustering-dialog-entry-table")[0];

        var trHead = table.insertRow(table.rows.length);
        trHead.className = "header";
        $(trHead.insertCell(0)).text($.i18n('core-dialogs/cluster-size'));
        $(trHead.insertCell(1)).text($.i18n('core-dialogs/row-count'));
        $(trHead.insertCell(2)).text($.i18n('core-dialogs/cluster-values'));
        $(trHead.insertCell(3)).text($.i18n('core-dialogs/merge'));
        $(trHead.insertCell(4)).text($.i18n('core-dialogs/new-cell-val'));

        var entryTemplate = document.createElement('a');
        entryTemplate.href = "javascript:{}";
        entryTemplate.title = $.i18n('core-dialogs/use-this-val');

        var browseLinkTemplate = $('<a target="_new" title="'+$.i18n('core-dialogs/browse-only-these')+'">'+$.i18n('core-dialogs/browse-this-cluster')+'</a>')
                .addClass("clustering-dialog-browse-focus")
                .css("visibility","hidden")


        var renderCluster = function(cluster, index) {
            var tr = table.insertRow();
            tr.className = index % 2 === 0 ? "odd" : "even"; // TODO: Unused?

            var cell = tr.insertCell()
            cell.textContent = cluster.choices.length.toString();

            cell = tr.insertCell();
            cell.textContent = cluster.rowCount.toString();

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

            var ul = document.createElement('ul');
            var choices = cluster.choices;
            var onClick = function() {
              var parent = $(this).closest("tr");
              var value = $(this).attr('data-value');
              cluster.value = value;

              parent.find("input[type='text']").val(value);
              var checkbox = parent.find("input[type='checkbox']");
              checkbox.prop('checked', true).trigger('change');
              return false;
            };
            for (var c = 0; c < choices.length; c++) {
                var choice = choices[c];
                var li = document.createElement('li');
                var entry = entryTemplate.cloneNode();
                entry.textContent = choice.v.toString().replaceAll(' ', '\xa0');
                entry.setAttribute('data-value', choice.v.toString());
                entry.addEventListener('click', onClick);
                li.append(entry);
                if (choice.c > 1) {
                  $('<span></span>').text($.i18n("core-dialogs/cluster-rows", choice.c)).addClass("clustering-dialog-entry-count").appendTo(li);
                }
                facet.s[c] = {
                    "v": {
                        "v":choice.v,
                        "l":choice.v
                    }
                };
                ul.append(li);
            }

            var params = [
                "project=" + encodeURIComponent(theProject.id),
                "ui=" + encodeURIComponent(JSON.stringify({
                    "facets" : [ facet ]
                }))
            ];
            var url = "project?" + params.join("&");

            var div = document.createElement('div');
            div.class = "clustering-dialog-value-focus";

            var browseLink = $(browseLinkTemplate).clone()
                .attr("href",url)
                .appendTo(div);

            $(tr.insertCell(2))
                .on('mouseenter',function() { browseLink.css("visibility", "visible"); })
                .on('mouseleave',function() { browseLink.css("visibility", "hidden"); })
                .append(ul)
                .append(div);

            var editCheck = $('<input type="checkbox" />')
                .on('change',function() {
                    cluster.edit = this.checked;
                }).appendTo(tr.insertCell(3));

            if (cluster.edit) {
                editCheck.prop('checked', true);
            }

            $('<input type="text" size="25" />')
                .val(cluster.value)
                .on("keyup change input",function() {
                    cluster.value = this.value;
                }).appendTo(tr.insertCell(4));

            return choices.length;
        };

        var maxRenderRows = parseInt(
            Refine.getPreference("ui.clustering.choices.limit", 5000)
        );
        maxRenderRows = isNaN(maxRenderRows) || maxRenderRows <= 0 ? 5000 : maxRenderRows;
        var totalRows = 0;
        for (var clusterIndex = 0; clusterIndex < clusters.length && totalRows < maxRenderRows; clusterIndex++) {
            totalRows += renderCluster(clusters[clusterIndex], clusterIndex);
        }

        container.empty().append(table);

        this._elmts.resultSummary.html(
            ((totalRows >= maxRenderRows) ? $.i18n('core-dialogs/cluster-row-limit-exceeded', maxRenderRows) + '<br/> ' : '') +
            ((clusterIndex === this._clusters.length) ?
                $.i18n('core-dialogs/clusters-found', this._clusters.length) :
                $.i18n('core-dialogs/clusters-filtered', clusterIndex, this._clusters.length))
        );

    } else {
        container.html(
            '<div style="margin: 2em;"><div style="font-size: 130%; color: #333;">'+$.i18n('core-dialogs/no-cluster-found')+'</div><div style="padding-top: 1em; font-size: 110%; color: #888;">'+$.i18n('core-dialogs/try-another-method')+'</div></div>'
        );
    }
};

ClusteringDialog.prototype._cluster = function() {
    $('#cluster-and-edit-dialog :input').prop('disabled', true);
    $(".clustering-dialog-facet").css("display","none");
    var self = this;

    var container = this._elmts.tableContainer.html(
        '<div style="margin: 1em; font-size: 130%; color: #888;">'+$.i18n('core-dialogs/clustering')+'<img src="images/small-spinner.gif"></div>'
    );

    this._elmts.resultSummary.empty();

    $.post(
        "command/core/compute-clusters?" + $.param({ project: theProject.id }),
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
            $(".clustering-dialog-facet").css("display","block");
            $('#cluster-and-edit-dialog :input').prop('disabled', false);
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
    $(".clustering-dialog-entry-table input:not(:checked)").prop('checked', true).trigger('change');
};

ClusteringDialog.prototype._deselectAll = function() {
    $(".clustering-dialog-entry-table input:checked").prop('checked', false).trigger('change');
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

ClusteringDialog.prototype._onExportCluster = function() {
    var self = this;
    self._export();
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
        alert($.i18n('core-dialogs/warning-check-boxes'));
    }
};

ClusteringDialog.prototype._export = function() {
    var clusters = this._getRestrictedClusters();
    var projectName = theProject.metadata.name;
    var columnName = this._columnName;
    var timeStamp = (new Date()).toISOString();
    var obj = {
        'projectName': projectName,
        'columnName': columnName,
        'timeStamp': timeStamp,
        'clusterMethod': this._method,
        'keyingFunction': this._function,
        'clusters': clusters
    };
    var data = "text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(obj));
    var link = document.createElement('a');
    link.href = 'data:' + data;
    link.download = "clusters_" + projectName + "_" + columnName + "_" + timeStamp + ".json";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
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

    this._createFacet($.i18n('core-dialogs/choices-in-cluster'), "size");
    this._createFacet($.i18n('core-dialogs/rows-in-cluster'), "rowCount");
    this._createFacet($.i18n('core-dialogs/choice-avg-length'), "avg");
    this._createFacet($.i18n('core-dialogs/choice-var-length'), "variance");
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

        this._elmts.sliderWidgetDiv.on("slide", function(evt, data) {
            self._from = data.from;
            self._to = data.to;
            self._setRangeIndicators();
        }).on("stop", function(evt, data) {
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

    this._histogram.update(
        this._min,
        this._max,
        this._step,
        [ this._baseBins, bins ]
    );
    this._sliderWidget.update(
        this._min,
        this._max,
        this._step,
        this._from,
        this._to
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
