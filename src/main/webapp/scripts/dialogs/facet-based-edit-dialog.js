function FacetBasedEditDialog(columnName, entries) {
    this._columnName = columnName;
    this._entries = entries;
    
    this._createDialog();
    this._cluster();
}

FacetBasedEditDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("800px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Facet-based edit of column " + this._columnName).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    var html = $(
        '<div>' +
            '<div bind="tableContainer" class="facet-based-edit-dialog-table-container"></div>' +
            '<div class="facet-based-edit-dialog-controls">' +
                '<button bind="clusterButton">Cluster</button> ' +
                '<button bind="unclusterButton">Un-cluster</button> ' +
            '</div>' +
        '</div>'
    ).appendTo(body);
    
    this._elmts = DOM.bind(html);
    this._elmts.clusterButton.click(function() { self._cluster(); });
    this._elmts.unclusterButton.click(function() { self._uncluster(); });
    
    $('<button></button>').text("OK").click(function() { self._onOK(); }).appendTo(footer);
    $('<button></button>').text("Cancel").click(function() { self._dismiss(); }).appendTo(footer);
    
    this._level = DialogSystem.showDialog(frame);
    
    $("#recon-dialog-tabs").tabs();
    $("#recon-dialog-tabs-strict").css("display", "");
};

FacetBasedEditDialog.prototype._renderTable = function() {
    var self = this;
    var container = this._elmts.tableContainer.empty();
    
    var table = $('<table></table>').addClass("facet-based-edit-dialog-entry-table").appendTo(container)[0];
    
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
            $('<span>').text(choice.v.l).appendTo(li);
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
        
        var input = $('<input size="35" />')
            .attr("value", cluster.value)
            .appendTo(tr.insertCell(2))
            .keyup(function() {
                cluster.value = this.value;
            });
    };
    for (var i = 0; i < this._clusters.length; i++) {
        renderCluster(this._clusters[i]);
    }
};

FacetBasedEditDialog.prototype._onOK = function() {
};

FacetBasedEditDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

FacetBasedEditDialog.prototype._cluster = function() {
    var clusters = [];
    var map = {};
    $.each(this._entries, function() {
        var choice = {
            v: this.v,
            c: this.c
        };
        
        var s = this.v.l.toLowerCase().replace(/\W/g, ' ').replace(/\s+/g, ' ').split(" ").sort().join(" ");
        if (s in map) {
            map[s].choices.push(choice);
        } else {
            map[s] = {
                edit: false,
                choices: [ choice ]
            };
            clusters.push(map[s]);
        }
    });
    
    $.each(clusters, function() {
        this.choices.sort(function(a, b) {
            var c = b.c - a.c;
            return c != 0 ? c : a.v.l.localeCompare(b.v.l);
        });
        this.value = this.choices[0].v.l;
    });
    clusters.sort(function(a, b) {
        var c = b.choices.length - a.choices.length;
        return c != 0 ? c : a.value.localeCompare(b.value);
    });
    
    this._clusters = clusters;
    this._renderTable();
};

FacetBasedEditDialog.prototype._uncluster = function() {
    var clusters = [];
    $.each(this._entries, function() {
        var cluster = {
            edit: false,
            choices: [{
                v: this.v,
                c: this.c
            }],
            value: this.v.l
        };
        clusters.push(cluster);
    });
    
    this._clusters = clusters;
    this._renderTable();
};
