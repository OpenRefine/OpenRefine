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

function SchemaAlignmentDialog(protograph) {
    var protograph = {
        rootNodes: [
            {
                nodeType: "existing",
                column: "name",
                linkages: [
                ]
            }
        ]
    };
    
    this._createDialog();
};

SchemaAlignmentDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    
    frame.width("1000px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Schema Alignment").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    this._renderFooter(footer);
    this._renderBody(body);
    
    this._level = DialogSystem.showDialog(frame);
};

SchemaAlignmentDialog.prototype._renderFooter = function(footer) {
    var self = this;
    
    $('<button></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
        self._onDone(self._getNewProtograph());
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
};

SchemaAlignmentDialog.prototype._renderBody = function(body) {
    var self = this;
    
    this._canvas = $('<div></div>').addClass("schema-alignment-dialog-canvas").appendTo(body);
};
