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

var SchemaAlignment = {};

SchemaAlignment.autoAlign = function() {
    var protograph = {};
    
    var columns = theProject.columnModel.columns;
    
    var typedCandidates = [];
    var candidates = [];
    
    for (var c = 0; c < columns.length; c++) {
        var column = columns[c];
        var typed = (column.reconConfig) && 
            ReconciliationManager.isFreebaseIdOrMid(column.reconConfig.identifierSpace) &&
            ReconciliationManager.isFreebaseId(column.reconConfig.schemaSpace);
        
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
            var name = SchemaAlignment._cleanName(candidate.column.name);
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
    });
    
    Ajax.chainGetJSON.apply(null, args);
};

SchemaAlignment._cleanName = function(s) {
    return s.replace(/\W/g, " ").replace(/\s+/g, " ").toLowerCase();
};

SchemaAlignment.createNewRootNode = function() {
    var rootNode = null;
    var links = [];
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        var target = {
            nodeType: "cell-as-topic",
            columnName: column.name,
            createForNoReconMatch: true
        };
        if ((column.reconConfig) && 
            ReconciliationManager.isFreebaseIdOrMid(column.reconConfig.identifierSpace) &&
            ReconciliationManager.isFreebaseId(column.reconConfig.schemaSpace) &&
            (column.reconConfig.type)) {

            target.type = {
                id: column.reconConfig.type.id,
                name: column.reconConfig.type.name
            };
        }
        
        if (column.name == theProject.columnModel.keyColumnName) {
            rootNode = target;
        } else {
            links.push({
                property: null,
                target: target
            });
        }
    }
    
    rootNode = rootNode || { nodeType: "cell-as-topic" };
    rootNode.links = links;
    
    return rootNode;
};

function SchemaAlignmentDialog(protograph, onDone) {
    this._onDone = onDone;
    this._originalProtograph = protograph || { rootNodes: [] };
    this._protograph = cloneDeep(this._originalProtograph); // this is what can be munched on
    
    if (!this._protograph.rootNodes.length) {
        this._protograph.rootNodes.push(SchemaAlignment.createNewRootNode());
    }
    
    this._nodeUIs = [];
    this._createDialog();
    this.preview();
}

SchemaAlignmentDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    
    frame.width("800px");
    
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
    
    $('<button class="button"></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        var protograph = self.getJSON();
        
        Refine.postProcess(
            "freebase",
            "save-protograph",
            {},
            { protograph: JSON.stringify(protograph) },
            {},
            {   
                onDone: function() {
                    DialogSystem.dismissUntil(self._level - 1);
                    theProject.overlayModels.freebaseProtograph = protograph;
                }
            }
        );
    }).appendTo(footer);
    
    $('<button class="button"></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
};

SchemaAlignmentDialog.prototype._constructBody = function(body) {
    $('<p>' +
        'The schema alignment skeleton below specifies how the graph-shaped data that will get generated ' +
        'from your grid-shaped data and written into Freebase. The cells in each record of your data will ' +
        'get placed into nodes within the skeleton. Configure the skeleton by specifying which ' +
        'column to substitute into which node. A node can also be an automatically generated ' +
        'anonymous node, or it can be an explicit value or topic that is the same for all records.' +
    '</p>').appendTo(body);
    
    $(
        '<div id="schema-alignment-tabs" class="refine-tabs">' +
            '<ul>' +
                '<li><a href="#schema-alignment-tabs-protograph">Skeleton</a></li>' +
                '<li><a href="#schema-alignment-tabs-preview-mqllike">MQL-like Preview</a></li>' +
                '<li><a href="#schema-alignment-tabs-preview-tripleloader">TripleLoader Preview</a></li>' +
            '</ul>' +
            '<div id="schema-alignment-tabs-protograph">' +
                '<div class="schema-alignment-dialog-canvas"></div>' +
            '</div>' +
            '<div id="schema-alignment-tabs-preview-mqllike" style="display: none;">' +
                '<div class="schema-alignment-dialog-preview"></div>' +
            '</div>' +
            '<div id="schema-alignment-tabs-preview-tripleloader" style="display: none;">' +
                '<div class="schema-alignment-dialog-preview"></div>' +
            '</div>' +
        '</div>'
    ).appendTo(body);
};

SchemaAlignmentDialog.prototype._renderBody = function(body) {
    var self = this;
    
    $("#schema-alignment-tabs").tabs();
    $("#schema-alignment-tabs-preview-mqllike").css("display", "");
    $("#schema-alignment-tabs-preview-tripleloader").css("display", "");

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
    
    this._previewPanes = $(".schema-alignment-dialog-preview");
};

SchemaAlignmentDialog.prototype.getJSON = function() {
    var rootNodes = [];
    for (var i = 0; i < this._nodeUIs.length; i++) {
        var node = this._nodeUIs[i].getJSON();
        if (node !== null) {
            rootNodes.push(node);
        }
    }
    
    return {
        rootNodes: rootNodes
    };
};

SchemaAlignmentDialog.prototype.preview = function() {
    var self = this;
    
    this._previewPanes.empty();
    
    var protograph = this.getJSON();
    $.post(
        "/command/freebase/preview-protograph?" + $.param({ project: theProject.id }),
        { protograph: JSON.stringify(protograph), engine: JSON.stringify(ui.browsingEngine.getJSON()) },
        function(data) {
            if ("mqllike" in data) {
                $(self._previewPanes[0]).text(JSON.stringify(data.mqllike, null, 2));
            }
            if ("tripleloader" in data) {
                $(self._previewPanes[1]).text(data.tripleloader);
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
