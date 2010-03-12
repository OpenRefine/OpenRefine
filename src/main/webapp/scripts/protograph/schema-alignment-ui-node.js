SchemaAlignmentDialog.UINode = function(dialog, node, table, options) {
    this._dialog = dialog;
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

SchemaAlignmentDialog.UINode.prototype.dispose = function() {
    // nothing for now
};

SchemaAlignmentDialog.UINode.prototype.removeLink = function(linkUI) {
    for (var i = this._linkUIs.length - 1; i >= 0; i--) {
        if (this._linkUIs[i] === linkUI) {
            this._linkUIs.splice(i, 1);
            this._node.links.splice(i, 1);
            break;
        }
    }
};

SchemaAlignmentDialog.UINode.prototype._isExpandable = function() {
    return this._node.nodeType == "cell-as-topic" ||
        this._node.nodeType == "anonymous" ||
        this._node.nodeType == "topic";
};

SchemaAlignmentDialog.UINode.prototype.render = function() {
    this._renderMain();
    if (this._isExpandable()) {
        this._showExpandable();
    } else {
        this._hideExpandable();
    }
};

SchemaAlignmentDialog.UINode.prototype.getExpectedType = function() {
    if ("type" in this._node) {
        return this._node.type.id;
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
            self._showNodeConfigDialog();
        });
        
    if (this._node.nodeType == "cell-as-topic" || 
        this._node.nodeType == "cell-as-value" || 
        this._node.nodeType == "cell-as-key") {
        
        if ("columnName" in this._node) {
            a.html(" cell");
            
            $('<span></span>')
                .text(this._node.columnName)
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
            this._linkUIs.push(new SchemaAlignmentDialog.UILink(
                this._dialog, 
                this._node.links[i], 
                this._tableLinks, 
                { expanded: true }, 
                this
            ));
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
                self._dialog,
                newLink, 
                self._tableLinks,
                {
                    expanded: true,
                    mustBeCellTopic: false
                },
                self
            ));
        });
};

SchemaAlignmentDialog.UINode.prototype._showColumnPopupMenu = function(elmt) {
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
            label: columns[index].name,
            click: function() {
                self._node.nodeType = "cell-as-topic";
                self._node.columnName = columns[index].name;
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

SchemaAlignmentDialog.UINode.prototype._showNodeConfigDialog = function() {
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
    var literalTypeSelectHtml =
        '<option value="/type/text" checked>text</option>' +
        '<option value="/type/int">int</option>' +
        '<option value="/type/float">float</option>' +
        '<option value="/type/double">double</option>' +
        '<option value="/type/boolean">boolean</option>' +
        '<option value="/type/datetime">date/time</option>';
    
    var html = $(
        '<table class="grid-layout layout-normal layout-full">' +
            '<tr>' +
                '<td>' +
                    '<table class="grid-layout layout-tight">' +
                        '<tr>' +
                            '<td>' +
                                '<div class="schema-align-node-dialog-node-type">' +
                                    '<input type="radio" name="schema-align-node-dialog-node-type" value="cell-as" bind="radioNodeTypeCellAs" /> Set to Cell in Column' +
                                '</div>' +
                            '</td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td>' +
                                '<table class="grid-layout layout-tight">' +
                                    '<tr>' +
                                        '<td><div class="schema-alignment-node-dialog-column-list" bind="divColumns"></div></td>' +
                                        '<td>' +
                                            '<table class="grid-layout layout-tight" cols="4">' +
                                                '<tr>' +
                                                    '<td colspan="4">The cell\'s content is used ...</td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td><input type="radio" name="schema-align-node-dialog-node-subtype" value="cell-as-topic" bind="radioNodeTypeCellAsTopic" /></td>' +
                                                    '<td colspan="3">to specify a Freebase topic, as reconciled</td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td></td>' +
                                                    '<td colspan="1" width="1%"><input type="checkbox" bind="radioNodeTypeCellAsTopicCreate" /></td>' +
                                                    '<td colspan="2">If not reconciled, create new topic named by the cell\'s content, and assign it a type</td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td></td>' +
                                                    '<td></td>' +
                                                    '<td colspan="1">Type:</td>' +
                                                    '<td colspan="1"><input bind="cellAsTopicNodeTypeInput" /></td>' +
                                                '</tr>' +
                                                
                                                '<tr>' +
                                                    '<td><input type="radio" name="schema-align-node-dialog-node-subtype" value="cell-as-value" bind="radioNodeTypeCellAsValue" /></td>' +
                                                    '<td colspan="3">as a literal value</td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td></td>' +
                                                    '<td colspan="2">Literal type</td>' +
                                                    '<td colspan="1"><select bind="cellAsValueTypeSelect">' + literalTypeSelectHtml + '</select></td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td></td>' +
                                                    '<td colspan="2">Language (for text)</td>' +
                                                    '<td colspan="1"><input bind="cellAsValueLanguageInput" /></td>' +
                                                '</tr>' +
                                                
                                                '<tr>' +
                                                    '<td><input type="radio" name="schema-align-node-dialog-node-subtype" value="cell-as-key" bind="radioNodeTypeCellAsKey" /></td>' +
                                                    '<td colspan="3">as a key in a namespace</td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td></td>' +
                                                    '<td colspan="2">Namespace</td>' +
                                                    '<td colspan="1"><input bind="cellAsKeyInput" /></td>' +
                                                '</tr>' +
                                            '</table>' +
                                        '</td>' +
                                    '</tr>' +
                                '</table>' +
                            '</td>' +
                        '</tr>' +
                    '</table>' +
                '</td>' +
                
                '<td>' +
                    '<table class="grid-layout layout-tight">' +
                        '<tr>' +
                            '<td colspan="3">' +
                                '<div class="schema-align-node-dialog-node-type">' +
                                    '<input type="radio" name="schema-align-node-dialog-node-type" value="anonymous" bind="radioNodeTypeAnonymous" /> Generate an anonymous graph node' +
                                '</div>' +
                            '</td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>Assign a type to the node</td>' +
                            '<td>&nbsp;<input bind="anonymousNodeTypeInput" /></td>' +
                        '</tr>' +
                    
                        '<tr>' +
                            '<td colspan="3">' +
                                '<div class="schema-align-node-dialog-node-type">' +
                                    '<input type="radio" name="schema-align-node-dialog-node-type" value="topic" bind="radioNodeTypeTopic" /> Use one existing Freebase topic' +
                                '</div>' +
                            '</td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>Topic</td>' +
                            '<td><input bind="topicNodeTypeInput" /></td>' +
                        '</tr>' +
                    
                        '<tr>' +
                            '<td colspan="3">' +
                                '<div class="schema-align-node-dialog-node-type">' +
                                    '<input type="radio" name="schema-align-node-dialog-node-type" value="value" bind="radioNodeTypeValue" /> Use a literal value' +
                                '</div>' +
                            '</td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>Value</td>' +
                            '<td><input bind="valueNodeTypeValueInput" /></td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>Value type</td>' +
                            '<td><select bind="valueNodeTypeValueTypeSelect">' + literalTypeSelectHtml + '</select></td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>Language</td>' +
                            '<td><input bind="valueNodeTypeLanguageInput" /></td>' +
                        '</tr>' +
                    '</table>' +
                '</td>' +
            '</tr>' +
        '</table>'
    ).appendTo(body);
    
    var elmts = DOM.bind(html);
    
    var tableColumns = $('<table></table>')
        .attr("cellspacing", "5")
        .attr("cellpadding", "0")
        .appendTo(elmts.divColumns)[0];
        
    var makeColumnChoice = function(column, columnIndex) {
        var tr = tableColumns.insertRow(tableColumns.rows.length);
        
        var radio = $('<input />')
            .attr("type", "radio")
            .attr("value", column.name)
            .attr("name", "schema-align-node-dialog-column")
            .appendTo(tr.insertCell(0))
            .click(function() {
                elmts.radioNodeTypeCellAs[0].checked = true;
                
                if ("reconConfig" in column) {
                    var typeID = column.reconConfig.type.id;
                    var typeName = column.reconConfig.type.name;
                    
                    elmts.cellAsTopicNodeTypeInput[0].value = typeName;
                    elmts.cellAsTopicNodeTypeInput.data("data.suggest", { "id" : typeID, "name" : typeName });
                    elmts.radioNodeTypeCellAsTopicCreate[0].checked = true;
                    elmts.radioNodeTypeCellAsTopic[0].checked = true;
                }
            });
            
        if ((!("columnName" in self._node) || self._node.columnName == null) && columnIndex == 0) {
            radio.attr("checked", "true");
        } else if (column.name == self._node.columnName) {
            radio.attr("checked", "true");
        }
            
        $('<span></span>').text(column.name).appendTo(tr.insertCell(1));
    };
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        makeColumnChoice(columns[i], i);
    }
        
    elmts.anonymousNodeTypeInput
        .bind("focus", function() { elmts.radioNodeTypeAnonymous[0].checked = true; })
        .suggestT({ type: "/type/type" });
        
    elmts.topicNodeTypeInput
        .bind("focus", function() { elmts.radioNodeTypeTopic[0].checked = true; })
        .suggest({});
        
    elmts.valueNodeTypeValueInput
        .bind("focus", function() { elmts.radioNodeTypeValue[0].checked = true; });
    elmts.valueNodeTypeValueTypeSelect
        .bind("focus", function() { elmts.radioNodeTypeValue[0].checked = true; });
    elmts.valueNodeTypeLanguageInput
        .bind("focus", function() { elmts.radioNodeTypeValue[0].checked = true; })
        .suggest({ type: "/type/lang" });
        
    elmts.radioNodeTypeCellAsTopicCreate
        .click(function() {
            elmts.radioNodeTypeCellAs[0].checked = true;
            elmts.radioNodeTypeCellAsTopic[0].checked = true;
        });
    elmts.cellAsTopicNodeTypeInput
        .bind("focus", function() {
            elmts.radioNodeTypeCellAs[0].checked = true;
            elmts.radioNodeTypeCellAsTopic[0].checked = true; 
        })
        .suggestT({ type: "/type/type" });
        
    elmts.cellAsValueTypeSelect
        .bind("focus", function() {
            elmts.radioNodeTypeCellAs[0].checked = true;
            elmts.radioNodeTypeCellAsValue[0].checked = true; 
        });
    elmts.cellAsValueLanguageInput
        .bind("focus", function() {
            elmts.radioNodeTypeCellAs[0].checked = true;
            elmts.radioNodeTypeCellAsValue[0].checked = true; 
        })
        .suggest({ type: "/type/lang" });
        
    elmts.cellAsKeyInput
        .bind("focus", function() {
            elmts.radioNodeTypeCellAs[0].checked = true;
            elmts.radioNodeTypeCellAsKey[0].checked = true;
        })
        .suggest({ type: "/type/namespace" });
        
    elmts.radioNodeTypeCellAsTopic[0].checked = true; // just make sure some subtype is selected
    if (this._node.nodeType.match(/^cell-as-/)) {
        elmts.radioNodeTypeCellAs[0].checked = true;
        if (this._node.nodeType == "cell-as-topic") {
            elmts.radioNodeTypeCellAsTopic[0].checked = true;
        } else if (this._node.nodeType == "cell-as-value") {
            elmts.radioNodeTypeCellAsValue[0].checked = true;
        } else if (this._node.nodeType == "cell-as-key") {
            elmts.radioNodeTypeCellAsKey[0].checked = true;
        }
    } else if (this._node.nodeType == "anonymous") {
        elmts.radioNodeTypeAnonymous[0].checked = true;
    } else if (this._node.nodeType == "topic") {
        elmts.radioNodeTypeTopic[0].checked = true;
    } else if (this._node.nodeType == "value") {
        elmts.radioNodeTypeValue[0].checked = true;
    }
    
    if ("type" in this._node) {
        elmts.anonymousNodeTypeInput[0].value = this._node.type.name;
        elmts.anonymousNodeTypeInput.data("data.suggest", this._node.type);
        
        elmts.cellAsTopicNodeTypeInput[0].value = this._node.type.name;
        elmts.cellAsTopicNodeTypeInput.data("data.suggest", this._node.type);
    }
    if ("topic" in this._node) {
        elmts.topicNodeTypeInput[0].value = this._node.topic.name;
        elmts.topicNodeTypeInput.data("data.suggest", this._node.topic);
    }
    if ("namespace" in this._node) {
        elmts.cellAsKeyInput[0].value = this._node.namespace.name;
        elmts.cellAsKeyInput.data("data.suggest", this._node.namespace);
    }
    if ("createForNoReconMatch" in this._node) {
        elmts.radioNodeTypeCellAsTopicCreate[0].checked = this._node.createForNoReconMatch;
    }
    if ("lang" in this._node) {
        elmts.valueNodeTypeLanguageInput[0].value = this._node.lang;
        elmts.valueNodeTypeLanguageInput.data("data.suggest", { id: this._node.lang });
        
        elmts.cellAsValueLanguageInput[0].value = this._node.lang;
        elmts.cellAsValueLanguageInput.data("data.suggest", { id: this._node.lang });
    }
    if ("valueType" in this._node) {
        elmts.valueNodeTypeValueTypeSelect[0].value = this._node.valueType;
        elmts.cellAsValueTypeSelect[0].value = this._node.valueType;
    }
    
    /*--------------------------------------------------
     * Footer
     *--------------------------------------------------
     */
    
    var getResultJSON = function() {
        var node = {
            nodeType: $("input[name='schema-align-node-dialog-node-type']:checked")[0].value
        };
        if (node.nodeType == "cell-as") {
            node.nodeType = $("input[name='schema-align-node-dialog-node-subtype']:checked")[0].value;
            node.columnName = $("input[name='schema-align-node-dialog-column']:checked")[0].value;
            
            if (node.nodeType == "cell-as-topic") {
                if (elmts.radioNodeTypeCellAsTopicCreate[0].checked) {
                    node.createForNoReconMatch = true;
                    
                    var t = elmts.cellAsTopicNodeTypeInput.data("data.suggest");
                    if (!(t)) {
                        alert("For creating a new graph node, you need to specify a type for it.");
                        return null;
                    }
                    node.type = {
                        id: t.id,
                        name: t.name
                    };
                } else {
                    node.createForNoReconMatch = false;
                }
            } else if (node.nodeType == "cell-as-value") {
                node.valueType = elmts.cellAsValueTypeSelect[0].value;
                
                if (node.valueType == "/type/text") {
                    var l = elmts.cellAsValueLanguageInput.data("data.suggest");
                    node.lang = (l) ? l.id : "/lang/en";
                }
            } else if (node.nodeType == "cell-as-key") {
                var t = elmts.cellAsKeyInput.data("data.suggest");
                if (!(t)) {
                    alert("Please specify the namespace.");
                    return null;
                }
                node.namespace = {
                    id: t.id,
                    name: t.name
                };
            }
        } else if (node.nodeType == "anonymous") {
            var t = elmts.anonymousNodeTypeInput.data("data.suggest");
            if (!(t)) {
                alert("For generating an anonymous graph node, you need to specify a type for it.");
                return null;
            }
            node.type = {
                id: t.id,
                name: t.name
            };
        } else if (node.nodeType == "topic") {
            var t = elmts.topicNodeTypeInput.data("data.suggest");
            if (!(t)) {
                alert("Please specify which existing Freebase topic to use.");
                return null;
            }
            node.topic = {
                id: t.id,
                name: t.name
            };
        } else if (node.nodeType == "value") {
            node.value = $.trim(elmts.valueNodeTypeValueInput[0].value);
            if (node.value.length == 0) {
                alert("Please specify the value to use.");
                return null;
            }
            node.valueType = elmts.valueNodeTypeValueTypeSelect[0].value;
            
            if (node.valueType == "/type/text") {
                var l = elmts.valueNodeTypeLanguageInput.data("data.suggest");
                node.lang = (l) ? l.id : "/lang/en";
            }
        }
        
        return node;
    };
    
    $('<button></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        var node = getResultJSON();
        if (node != null) {
            DialogSystem.dismissUntil(level - 1);
            
            self._node = node;
            self.render();
            self._dialog.preview();
        }
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
                type: "type" in this._node ? cloneDeep(this._node.type) : { "id" : "/common/topic", "name" : "Topic", "cvt" : false },
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
                type: cloneDeep(this._node.namespace)
            };
        }
    } else if (this._node.nodeType == "topic") {
        if (!("topic" in this._node) || this._node.topic == null) {
            return null;
        }
        result = {
            nodeType: this._node.nodeType,
            topic: cloneDeep(this._node.topic)
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
            type: cloneDeep(this._node.type)
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

