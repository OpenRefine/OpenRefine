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
    this._originalProtograph = protograph;
    this._protograph = cloneDeep(protograph); // this is what can be munched on
    this._nodeUIs = [];
    
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
                self._onDone(prototgraph);
            },
            "json"
        );
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
};

SchemaAlignmentDialog.prototype._renderBody = function(body) {
    var self = this;
    
    this._canvas = $('<div></div>').addClass("schema-alignment-dialog-canvas").appendTo(body);
    this._nodeTable = $('<table></table>').addClass("schema-alignment-table-layout").appendTo(this._canvas)[0];
    
    for (var i = 0; i < this._originalProtograph.rootNodes.length; i++) {
        this._nodeUIs.push(new SchemaAlignmentDialog.UINode(
            this._protograph.rootNodes[i], 
            this._nodeTable, 
            {
                expanded: true,
                mustBeCellTopic: true
            }
        ));
    }
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

/*----------------------------------------------------------------------
 *  UINode
 *----------------------------------------------------------------------
 */
SchemaAlignmentDialog.UINode = function(node, table, options) {
    this._node = node;
    this._options = options;
    
    this._linkUIs = [];
    this._detailsRendered = false;
    
    this._tr = table.insertRow(table.rows.length);
    this._tdMain = this._tr.insertCell(0);
    this._tdToggle = this._tr.insertCell(1);
    this._tdDetails = this._tr.insertCell(2);
    
    $(this._tdMain).addClass("schema-alignment-node-main").attr("width", "250").addClass("padded");
    $(this._tdToggle).addClass("schema-alignment-node-toggle").attr("width", "1%").addClass("padded").hide();
    $(this._tdDetails).addClass("schema-alignment-node-details").attr("width", "90%").hide();
    
    this._renderMain();
    
    this._expanded = options.expanded;
    if (this._isExpandable()) {
        this._showExpandable();
    }
};

SchemaAlignmentDialog.UINode.prototype._isExpandable = function() {
    return this._node.nodeType == "cell-as-topic";
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

SchemaAlignmentDialog.UINode.prototype._renderMain = function() {
    $(this._tdMain).empty();
    
    var self = this;
    var a = $('<a href="javascript:{}"></a>')
        .addClass("schema-alignment-node-tag")
        .appendTo(this._tdMain)
        .click(function(evt) {
            if (self._options.mustBeCellTopic) {
                self._showTagMenu(this);
            } else {
                self._showTagDialog();
            }
        });
        
    if (this._node.nodeType == "cell-as-topic" || 
        this._node.nodeType == "cell-as-value" || 
        this._node.nodeType == "cell-as-key") {
        
        if ("cellIndex" in this._node) {
            a.html(" cell");
            
            $('<span></span>')
                .text(SchemaAlignmentDialog._findColumn(this._node.cellIndex).headerLabel)
                .addClass("schema-alignment-node-column")
                .prependTo(a);
        } else {
            a.html(this._options.mustBeCellTopic ? "Which column?" : "Configure...");
        }
    } else if (this._node.nodeType == "topic") {
        if ("topic" in this._node) {
            a.html(this._node.topic.name);
        } else if ("id" in this._node) {
            a.html(this._node.topic.id);
        } else {
            a.html("Which topic?");
        }
    } else if (this._node.nodeType == "value") {
        if ("value" in this._node) {
            a.html(this._node.value);
        } else {
            a.html("What value?");
        }
    } else if (this._node.nodeType == "anonymous") {
        a.html("(anonymous)");
    }
    
    $('<img />').attr("src", "images/down-arrow.png").appendTo(a);
};

SchemaAlignmentDialog.UINode.prototype._showExpandable = function() {
    $(this._tdToggle).show();
    $(this._tdDetails).show();
    
    if (this._detailsRendered) {
        return;
    }
    this._detailsRendered = true;
    
    this._collapsedDetailDiv = $('<div></div>').appendTo(this._tdDetails).addClass("padded").html("...");
    this._expandedDetailDiv = $('<div></div>').appendTo(this._tdDetails).addClass("schema-alignment-detail-container");
    
    this._renderDetails();
    
    var self = this;
    var show = function() {
        if (self._expanded) {
            self._collapsedDetailDiv.hide();
            self._expandedDetailDiv.show();
        } else {
            self._collapsedDetailDiv.show();
            self._expandedDetailDiv.hide();
        }
    };
    show();
    
    $(this._tdToggle).html("&nbsp;");
    $('<img />')
        .attr("src", this._expanded ? "images/expanded.png" : "images/collapsed.png")
        .appendTo(this._tdToggle)
        .click(function() {
            self._expanded = !self._expanded;
            
            $(this).attr("src", self._expanded ? "images/expanded.png" : "images/collapsed.png");
            
            show();
        });
};

SchemaAlignmentDialog.UINode.prototype._hideExpandable = function() {
    $(this._tdToggle).hide();
    $(this._tdDetails).hide();
};

SchemaAlignmentDialog.UINode.prototype._renderDetails = function() {
    var self = this;

    this._tableLinks = $('<table></table>').addClass("schema-alignment-table-layout").appendTo(this._expandedDetailDiv)[0];
    
    if ("links" in this._node && this._node.links != null) {
        for (var i = 0; i < this._node.links.length; i++) {
            this._linkUIs.push(new SchemaAlignmentDialog.UILink(this._node.links[i], this._tableLinks, true));
        }
    }
    
    var divFooter = $('<div></div>').addClass("padded").appendTo(this._expandedDetailDiv);
    
    $('<a href="javascript:{}"></a>')
        .addClass("action")
        .text("add property")
        .appendTo(divFooter)
        .click(function() {
            var newLink = {
                property: null,
                target: {
                    nodeType: "cell-as-value"
                }
            };
            self._linkUIs.push(new SchemaAlignmentDialog.UILink(
                newLink, 
                self._tableLinks,
                {
                    expanded: true,
                    mustBeCellTopic: false
                }
            ));
        });
};

SchemaAlignmentDialog.UINode.prototype._showTagMenu = function(elmt) {
    self = this;
    
    var menu = [];
    
    if (!this._options.mustBeCellTopic) {
        menu.push({
            label: "Anonymous Node",
            click: function() {
                self._node.nodeType = "anonymous";
                self._showExpandable();
                self._renderMain();
            }
        });
        menu.push({
            label: "Freebase Topic",
            click: function() {
                self._node.nodeType = "topic";
                self._hideExpandable();
                self._renderMain();
            }
        });
        menu.push({
            label: "Value",
            click: function() {
                self._node.nodeType = "value";
                self._hideExpandable();
                self._renderMain();
            }
        });
        menu.push({}); // separator
    }
    
    var columns = theProject.columnModel.columns;
    var createColumnMenuItem = function(index) {
        menu.push({
            label: columns[index].headerLabel,
            click: function() {
                self._node.nodeType = "cell-as-topic";
                self._node.cellIndex = columns[index].cellIndex;
                self._showExpandable();
                self._renderMain();
            }
        });
    };
    for (var i = 0; i < columns.length; i++) {
        createColumnMenuItem(i);
    }
    
    MenuSystem.createAndShowStandardMenu(menu, elmt);
};

SchemaAlignmentDialog.UINode.prototype._showTagDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    
    frame.width("800px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Protograph Node").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    /*--------------------------------------------------
     * Body
     *--------------------------------------------------
     */
    
    var html = $(
        '<table>' +
            '<tr>' +
                '<td>' +
                    '<p><input type="radio" name="schema-align-node-dialog-node-type" value="cell-as" id="radioCellAs" /> Set to Cell in Column</p>' +
                    '<table>' +
                        '<tr>' +
                            '<td><div class="schema-alignment-node-dialog-column-list" id="divColumns"></div></td>' +
                            '<td>' +
                                '<p>The cell\'s content will be ...</p>' +
                                '<table>' +
                                    '<tr>' +
                                        '<td><input type="radio" name="schema-align-node-dialog-node-subtype" value="cell-as-topic" id="radioCellAsTopic" /></td>' +
                                        '<td colspan="2">name of a topic (as reconciled)</td>' +
                                    '</tr>' +
                                    '<tr>' +
                                        '<td></td>' +
                                        '<td>of Freebase type</td>' +
                                        '<td><input /></td>' +
                                    '</tr>' +
                                    '<tr>' +
                                        '<td></td>' +
                                        '<td>create new topic if not reconciled</td>' +
                                        '<td><input type="checkbox" /></td>' +
                                    '</tr>' +
                                    
                                    '<tr>' +
                                        '<td><input type="radio" name="schema-align-node-dialog-node-subtype" value="cell-as-value" id="radioCellAsValue" /></td>' +
                                        '<td colspan="2">a literal value</td>' +
                                    '</tr>' +
                                    '<tr>' +
                                        '<td></td>' +
                                        '<td>of literal type</td>' +
                                        '<td><input /></td>' +
                                    '</tr>' +
                                    '<tr>' +
                                        '<td></td>' +
                                        '<td>in language</td>' +
                                        '<td><input /></td>' +
                                    '</tr>' +
                                    
                                    '<tr>' +
                                        '<td><input type="radio" name="schema-align-node-dialog-node-subtype" value="cell-as-key" id="radioCellAsKey" /></td>' +
                                        '<td colspan="2">a key in a namespace</td>' +
                                    '</tr>' +
                                    '<tr>' +
                                        '<td></td>' +
                                        '<td>in namespace</td>' +
                                        '<td><input /></td>' +
                                    '</tr>' +
                                '</table>' +
                            '</td>' +
                        '</tr>' +
                    '</table>' +
                '</td>' +
                
                '<td>' +
                    '<p><input type="radio" name="schema-align-node-dialog-node-type" value="anonymous" id="radioAnonymous" /> Set to Anonymous Node</p>' +
                    '<table>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>of Freebase type</td>' +
                            '<td><input /></td>' +
                        '</tr>' +
                    '</table>' +
                    
                    '<p><input type="radio" name="schema-align-node-dialog-node-type" value="topic" id="radioTopic" /> Set to Freebase Topic</p>' +
                    '<table>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>topic</td>' +
                            '<td><input /></td>' +
                        '</tr>' +
                    '</table>' +
                    
                    '<p><input type="radio" name="schema-align-node-dialog-node-type" value="value" id="radioValue" /> Set to Literal Value</p>' +
                    '<table>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>value</td>' +
                            '<td><input /></td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>value type</td>' +
                            '<td><input /></td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>lang</td>' +
                            '<td><input /></td>' +
                        '</tr>' +
                    '</table>' +
                '</td>' +
            '</tr>' +
        '</table>'
    ).appendTo(body);
    
    var elmtMap = DOM.bind(html, [ "divColumns" ]);
    
    var makeColumnChoice = function(column) {
        var div = $('<div></div>')
            .addClass("schema-alignment-node-dialog-column-choice")
            .appendTo(elmtMap.divColumns);
            
        var radio = $('<input />')
            .attr("type", "radio")
            .attr("value", column.cellIndex)
            .attr("name", "schema-align-node-dialog-column")
            .appendTo(div);
            
        if (column.cellIndex == self._node.cellIndex) {
            radio.attr("checked", "true");
        }
            
        $('<span></span>').text(column.headerLabel).appendTo(div);
    };
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        makeColumnChoice(columns[i]);
    }
        
    
    /*--------------------------------------------------
     * Footer
     *--------------------------------------------------
     */
    
    $('<button></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        DialogSystem.dismissUntil(level - 1);
        self._onDone(self._getNewProtograph());
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    var level = DialogSystem.showDialog(frame);
};

SchemaAlignmentDialog.UINode.prototype.getJSON = function() {
    var result = null;
    var getLinks = false;
    
    if (this._node.nodeType.match(/^cell-as-/)) {
        if (!("columnName" in this._node) || this._node.columnName == null) {
            return null;
        }
            
        if (this._node.nodeType == "cell-as-topic") {
            result = {
                nodeType: this._node.nodeType,
                columnName: this._node.columnName,
                type: "type" in this._node ? cloneNode(this._node.type) : { "id" : "/common/topic", "name" : "Topic", "cvt" : false },
                createForNoReconMatch: "createForNoReconMatch" in this._node ? this._node.createForNoReconMatch : true
            };
            getLinks = true;
        } else if (this._node.nodeType == "cell-as-value") {
            result = {
                nodeType: this._node.nodeType,
                columnName: this._node.columnName,
                valueType: "valueType" in this._node ? this._node.valueType : "/type/text",
                lang: "lang" in this._node ? this._node.lang : "/lang/en"
            };
        } else if (this._node.nodeType == "cell-as-key") {
            if (!("namespace" in this._node) || this._node.namespace == null) {
                return null;
            }
            result = {
                nodeType: this._node.nodeType,
                columnName: this._node.columnName,
                type: cloneNode(this._node.namespace)
            };
        }
    } else if (this._node.nodeType == "topic") {
        if (!("topic" in this._node) || this._node.topic == null) {
            return null;
        }
        result = {
            nodeType: this._node.nodeType,
            topic: cloneNode(this._node.topic)
        };
        getLinks = true;
    } else if (this._node.nodeType == "value") {
        if (!("value" in this._node) || this._node.value == null) {
            return null;
        }
        result = {
            nodeType: this._node.nodeType,
            value: this._node.value,
            valueType: "valueType" in this._node ? this._node.valueType : "/type/text",
            lang: "lang" in this._node ? this._node.lang : "/lang/en"
        };
    } else if (this._node.nodeType == "anonymous") {
        if (!("type" in this._node) || this._node.type == null) {
            return null;
        }
        result = {
            nodeType: this._node.nodeType,
            type: cloneNode(this._node.type)
        };
        getLinks = true;
    }
    
    if (result == null) {
        return null;
    }
    if (getLinks) {
        var links = [];
        for (var i = 0; i < this._linkUIs.length; i++) {
            var link = this._linkUIs[i].getJSON();
            if (link != null) {
                links.push(link);
            }
        }
        result.links = links;
    }
    
    return result;
};

/*----------------------------------------------------------------------
 *  UILink
 *----------------------------------------------------------------------
 */
SchemaAlignmentDialog.UILink = function(link, table, expanded) {
    this._link = link;
    this._expanded = expanded;
    
    this._tr = table.insertRow(table.rows.length);
    this._tdMain = this._tr.insertCell(0);
    this._tdToggle = this._tr.insertCell(1);
    this._tdDetails = this._tr.insertCell(2);
    
    $(this._tdMain).addClass("schema-alignment-link-main").attr("width", "250").addClass("padded");
    $(this._tdToggle).addClass("schema-alignment-link-toggle").attr("width", "1%").addClass("padded");
    $(this._tdDetails).addClass("schema-alignment-link-details").attr("width", "90%");
    
    this._collapsedDetailDiv = $('<div></div>').appendTo(this._tdDetails).addClass("padded").html("...");
    this._expandedDetailDiv = $('<div></div>').appendTo(this._tdDetails).addClass("schema-alignment-detail-container");
    var self = this;
    var show = function() {
        if (self._expanded) {
            self._collapsedDetailDiv.hide();
            self._expandedDetailDiv.show();
        } else {
            self._collapsedDetailDiv.show();
            self._expandedDetailDiv.hide();
        }
    };
    show();
    
    $(this._tdToggle).html("&nbsp;");
    $('<img />')
        .attr("src", this._expanded ? "images/expanded.png" : "images/collapsed.png")
        .appendTo(this._tdToggle)
        .click(function() {
            self._expanded = !self._expanded;
            
            $(this).attr("src", self._expanded ? "images/expanded.png" : "images/collapsed.png");
            
            show();
        });
    
    this._renderMain();
    this._renderDetails();
};

SchemaAlignmentDialog.UILink.prototype._renderMain = function() {
    $(this._tdMain).empty()
    
    var label = this._link.property != null ? this._link.property.id : "property?";
    
    var self = this;
    
    var a = $('<a href="javascript:{}"></a>')
        .addClass("schema-alignment-link-tag")
        .html(label)
        .appendTo(this._tdMain)
        .click(function(evt) {
            self._showTagSuggest(this);
        });
        
    $('<img />').attr("src", "images/arrow-start.png").prependTo(a);
    $('<img />').attr("src", "images/arrow-end.png").appendTo(a);
};

SchemaAlignmentDialog.UILink.prototype._renderDetails = function() {
    var tableDetails = $('<table></table>').addClass("schema-alignment-table-layout").appendTo(this._expandedDetailDiv)[0];
    this._targetUI = new SchemaAlignmentDialog.UINode(this._link.target, tableDetails, true);
};

SchemaAlignmentDialog.UILink.prototype._showTagSuggest = function(elmt) {
    self = this;
    
    var fakeMenu = MenuSystem.createMenu()
        .width(300)
        .height(100)
        .css("background", "none")
        .css("border", "none");
    
    var input = $('<input />').appendTo(fakeMenu);
    
    var level = MenuSystem.showMenu(fakeMenu, function(){});
    MenuSystem.positionMenuAboveBelow(fakeMenu, $(elmt));

    input.suggest({ type : '/type/property' }).bind("fb-select", function(e, data) {
        self._link.property = {
            id: data.id,
            name: data.name
        };
        
        window.setTimeout(function() {
            MenuSystem.dismissAll();
            self._renderMain();
        }, 100);
    });
    input[0].focus();
};

SchemaAlignmentDialog.UILink.prototype.getJSON = function() {
    if ("property" in this._link && this._link.property != null &&
        "target" in this._link && this._link.target != null) {
        
        var targetJSON = this._targetUI.getJSON();
        if (targetJSON != null) {
            return {
                property: cloneNode(this._link.property),
                target: targetJSON
            };
        }
    }
    return null;
};
