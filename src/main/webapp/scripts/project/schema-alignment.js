var SchemaAlignment = {};

SchemaAlignment.autoAlign = function() {
    var protograph = {};
    
    var columns = theProject.columnModel.columns;
    
    var typedCandidates = [];
    var candidates = [];
    
    for (var c = 0; c < columns.length; c++) {
        var column = columns[c];
        var typed = "reconConfig" in column && column.reconConfig != null;
        var candidate = {
            status: "unbound",
            typed: typed,
            index: c,
            column: column
        };
        
        candidates.push(candidate);
        if (typed) {
            typedCandidates.push(candidate);
        }
    }
    
    if (typedCandidates.length > 0) {
        
    } else {
        var queries = {};
        for (var i = 0; i < candidates.length; i++) {
            var candidate = candidates[i];
            var name = SchemaAlignment._cleanName(candidate.column.headerLabel);
            var key = "t" + i + ":search";
            queries[key] = {
                "query" : name,
                "limit" : 10,
                "type" : "/type/type,/type/property",
                "type_strict" : "any"
            };
        }
        
        SchemaAlignment._batchSearch(queries, function(result) {
            console.log(result);
        });
    }
};

SchemaAlignment._batchSearch = function(queries, onDone) {
    var keys = [];
    for (var n in queries) {
        if (queries.hasOwnProperty(n)) {
            keys.push(n);
        }
    }
    
    var result = {};
    var args = [];
    var makeBatch = function(keyBatch) {
        var batch = {};
        for (var k = 0; k < keyBatch.length; k++) {
            var key = keyBatch[k];
            batch[key] = queries[key];
        }
        
        args.push("http://api.freebase.com/api/service/search?" + 
            $.param({ "queries" : JSON.stringify(batch) }) + "&callback=?");
            
        args.push(null); // no data
        args.push(function(data) {
            for (var k = 0; k < keyBatch.length; k++) {
                var key = keyBatch[k];
                result[key] = data[key];
            }
        });
    };
    
    for (var i = 0; i < keys.length; i += 10) {
        makeBatch(keys.slice(i, i + 10));
    }
    
    args.push(function() {
        onDone(result);
    })
    
    Ajax.chainGetJSON.apply(null, args);
};

SchemaAlignment._cleanName = function(s) {
    return s.replace(/\W/g, " ").replace(/\s+/g, " ").toLowerCase();
}

function SchemaAlignmentDialog(protograph, onDone) {
    this._onDone = onDone;
    this._originalProtograph = protograph || { rootNodes: [] };
    this._protograph = cloneDeep(this._originalProtograph); // this is what can be munched on
    
    if (this._protograph.rootNodes.length == 0) {
        this._protograph.rootNodes.push({
            nodeType: "cell-as-topic",
            links: [
            ]
        });
    }
    
    this._nodeUIs = [];
    this._createDialog();
    this.preview();
};

SchemaAlignmentDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    
    frame.width("1000px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Schema Alignment").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    this._constructFooter(footer);
    this._constructBody(body);
    
    this._level = DialogSystem.showDialog(frame);
    
    this._renderBody(body);
};

SchemaAlignmentDialog.prototype._constructFooter = function(footer) {
    var self = this;
    
    $('<button></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        var protograph = self.getJSON();
        
        $.post(
            "/command/save-protograph?" + $.param({ project: theProject.id }),
            { protograph: JSON.stringify(protograph) },
            function(data) {
                if (data.code == "error") {
                    alert("Failed to save protograph");
                    return;
                } else if (data.code == "ok") {
                    ui.historyWidget.update();
                } else {
                    ui.processWidget.update();
                }
                
                DialogSystem.dismissUntil(self._level - 1);
                
                theProject.protograph = protograph;
                
                self._onDone(protograph);
            },
            "json"
        );
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
};

SchemaAlignmentDialog.prototype._constructBody = function(body) {
    $('<p>' +
        'The protograph serves as a skeleton for the graph-shaped data that will get generated ' +
        'from your grid-shaped data and written into Freebase. The cells in each record of your data will ' +
        'get placed into nodes within the protograph. Configure the protograph by specifying which ' +
        'column to substitute into which node. A node can also be an automatically generated ' +
        'anonymous node, or it can be an explicit value or topic that is the same for all records.' +
    '</p>').appendTo(body);
    
    $(
        '<div id="schema-alignment-tabs">' +
            '<ul>' +
                '<li><a href="#schema-alignment-tabs-protograph">Protograph</a></li>' +
                '<li><a href="#schema-alignment-tabs-preview">Preview</a></li>' +
            '</ul>' +
            '<div id="schema-alignment-tabs-protograph">' +
                '<div class="schema-alignment-dialog-canvas"></div>' +
            '</div>' +
            '<div id="schema-alignment-tabs-preview" style="display: none;">' +
                '<div class="schema-alignment-dialog-preview"></div>' +
            '</div>' +
        '</div>'
    ).appendTo(body);
};

SchemaAlignmentDialog.prototype._renderBody = function(body) {
    var self = this;
    
    $("#schema-alignment-tabs").tabs();
    $("#schema-alignment-tabs-preview").css("display", "");

    this._canvas = $(".schema-alignment-dialog-canvas");
    this._nodeTable = $('<table></table>').addClass("schema-alignment-table-layout").appendTo(this._canvas)[0];
    
    for (var i = 0; i < this._protograph.rootNodes.length; i++) {
        this._nodeUIs.push(new SchemaAlignmentDialog.UINode(
            this,
            this._protograph.rootNodes[i], 
            this._nodeTable, 
            {
                expanded: true,
                mustBeCellTopic: true
            }
        ));
    }
    
    this._previewPane = $(".schema-alignment-dialog-preview");
};

SchemaAlignmentDialog.prototype.getJSON = function() {
    var rootNodes = [];
    for (var i = 0; i < this._nodeUIs.length; i++) {
        var node = this._nodeUIs[i].getJSON();
        if (node != null) {
            rootNodes.push(node);
        }
    }
    
    return {
        rootNodes: rootNodes
    };
};

SchemaAlignmentDialog.prototype.preview = function() {
    var self = this;
    
    this._previewPane.empty();
    
    var protograph = this.getJSON();
    $.post(
        "/command/preview-protograph?" + $.param({ project: theProject.id }),
        { protograph: JSON.stringify(protograph) },
        function(data) {
            if ("result" in data) {
                self._previewPane.text(JSON.stringify(data.result, null, 2));
            }
        },
        "json"
    );
};

SchemaAlignmentDialog._findColumn = function(cellIndex) {
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        if (column.cellIndex == cellIndex) {
            return column;
        }
    }
    return null;
};
