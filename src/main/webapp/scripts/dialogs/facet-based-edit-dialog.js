function FacetBasedEditDialog(columnName, expression, entries) {
    this._columnName = columnName;
    this._expression = expression;
    this._entries = entries;
    this._method = "binning";
    this._function = "fingerprint";
    this._params = {};
    
    this._createDialog();
    this._cluster();
}

FacetBasedEditDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("900px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Facet-based edit of column " + this._columnName).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    var html = $(
        '<div>' +
            '<div class="facet-based-edit-dialog-controls"><table><tr>' +
                '<td>' +
                    'Method: <select bind="methodSelector">' +
                        '<option selected="true">key collision</option>' +
                        '<option>nearest neightbor</option>' +
                    '</select>' +
                '</td>' +
                '<td>' +
                    '<div id="binning-controls">Keying Function: <select bind="keyingFunctionSelector">' +
                        '<option selected="true">fingerprint</option>' +
                        '<option>ngram-fingerprint</option>' +
                        '<option>double-metaphone</option>' +
                        '<option>metaphone</option>' +
                        '<option>soundex</option>' +
                    '</select></div>' +
                    '<div id="knn-controls" class="hidden">Distance Function: <select bind="distanceFunctionSelector">' +
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
                      'Ngram Size: <input type="text" value="1" bind="ngramSize">' +
                    '</div>' + 
                '</td>' +
            '</tr></table></div>' +
            '<div bind="tableContainer" class="facet-based-edit-dialog-table-container"></div>' +
        '</div>'
    ).appendTo(body);
    
    this._elmts = DOM.bind(html);

    this._elmts.methodSelector.change(function() {
        var selection = $(this).find("option:selected").text();
        if (selection == 'key collision') {
            body.find("#binning-controls").show();
            body.find("#knn-controls").hide();
            self._method = "binning";
            self._elmts.keyingFunctionSelector.change();
        } else if (selection = 'nearest neightbor') {
            body.find("#binning-controls").hide();
            body.find("#knn-controls").show();
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

    //this._elmts.clusterButton.click(function() { self._cluster(); });
    //this._elmts.unclusterButton.click(function() { self._uncluster(); });
    
    $('<button></button>').text("OK").click(function() { self._onOK(); }).appendTo(footer);
    $('<button></button>').text("Cancel").click(function() { self._dismiss(); }).appendTo(footer);
    
    this._level = DialogSystem.showDialog(frame);
    
    $("#recon-dialog-tabs").tabs();
    $("#recon-dialog-tabs-strict").css("display", "");
};

FacetBasedEditDialog.prototype._renderTable = function() {
    var self = this;
    var container = this._elmts.tableContainer;
    
    var table = $('<table></table>').addClass("facet-based-edit-dialog-entry-table")[0];
    
    var trHead = table.insertRow(table.rows.length);
    trHead.className = "header";
    $(trHead.insertCell(0)).text("Current facet choices");
    $(trHead.insertCell(1)).text("Edit?");
    $(trHead.insertCell(2)).text("New cell value");
    
    var renderCluster = function(cluster) {
        var tr = table.insertRow(table.rows.length);
        tr.className = table.rows.length % 2 == 0 ? "odd" : "even";
        
        var ul = $(tr.insertCell(0));
        var choices = cluster.choices;
        for (var c = 0; c < choices.length; c++) {
            var choice = choices[c];
            var li = $('<li>').appendTo(ul);
            $('<span>').text(choice.v).appendTo(li);
            $('<span>').text(" (" + choice.c + ")").appendTo(li);
        }
        
        var editCheck = $('<input type="checkbox" />')
            .appendTo(tr.insertCell(1))
            .click(function() {
                cluster.edit = !cluster.edit;
            });
        if (cluster.edit) {
            editCheck.attr("checked", "true");
        }
        
        var input = $('<input size="55" />')
            .attr("value", cluster.value)
            .appendTo(tr.insertCell(2))
            .keyup(function() {
                cluster.value = this.value;
            });
    };
    for (var i = 0; i < this._clusters.length; i++) {
        renderCluster(this._clusters[i]);
    }
    
    container.empty().append(table);
};

FacetBasedEditDialog.prototype._cluster = function() {
    var self = this;
    
    var container = this._elmts.tableContainer.html(
        '<div style="margin: 1em; font-size: 130%; color: #888;">Loading... <img src="/images/small-spinner.gif"></div>'
    );

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
            var clusters = [];
            $.each(data, function() {
                clusters.push({
                    edit: true,
                    choices: this,
                    value: this[0].v
                });
            });
            self._clusters = clusters;
            self._renderTable();
        },
        "json"
    );
}

FacetBasedEditDialog.prototype._onOK = function() {
    var edits = [];
    for (var i = 0; i < this._clusters.length; i++) {
        var cluster = this._clusters[i];
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
            "facet-based-edit",
            {},
            {
                columnName: this._columnName,
                expression: this._expression,
                edits: JSON.stringify(edits)
            },
            { cellsChanged: true }
        );
        
        this._dismiss();
    } else {
        alert("You must check some Edit? checkboxes for your edits to be applied.");
    }
};

FacetBasedEditDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

