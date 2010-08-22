RdfSchemaAlignmentDialog.UINode = function(dialog, node, table, options) {
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
    //this._renderTypes();
    
    this._expanded = options.expanded;
    if (this._isExpandable()) {
        this._showExpandable();
    }
};

RdfSchemaAlignmentDialog.UINode.prototype._renderMain = function() {
    $(this._tdMain).empty();
    var self = this;
    
    var type_html = !this._isExpandable()? '' : '<tr>' +
    	   '<td>' +
  	      	'<table bind="rdfTypesTable">' +
  	      	    '<tr bind="rdfTypesTr"><td bind="rdfTypesTd">&nbsp;</td></tr>' +
  	      		'<tr bind="addRdfTyprTr">' + 
  	      			'<td>' +
  	      				'<div class="padded">' +
  	      					'<a bind="addRdfTypeLink" href="#" class="action">add rdf:type</a>' +
  	      				'</div>' + 
  	      			'</td>' +
  	      		'</tr>' +
  	      	'</table>' +
	       '</td>' +
  	  '</tr>';
    var html = $(
    	'<table>' +
    	  '<tr>' +
    	    '<td bind="nodeLabel">' +
    	    '</td>' +
    	  '</tr>' +
    	  type_html + 
    	'</table>'
    	  ).appendTo(this._tdMain)
    	;
    
    var elmts = DOM.bind(html);
    this._tdNodeLabel = elmts.nodeLabel;
    if(elmts.addRdfTypeLink){
    	var typesTable = $('<table></table>')[0];
    	if(self._node.rdfTypes && self._node.rdfTypes.length>0){
    		var func = function(i){
    			return function(){
    				self._removeRdfType(i);
					self._renderMain();
    			};
    		};
    		for(var i=0;i<self._node.rdfTypes.length;i++){
    			//var f = func(i);
    			var tr = typesTable.insertRow(typesTable.rows.length);
    			var img = $('<img />').attr("title", "remove type").attr("src", "images/close.png").css("cursor", "pointer").click(
    				
    						func(i)
    	           
    			);
    			$(tr).append($('<td>').append(img));
    			$(tr).append($('<td>').text(self._getTypeName(self._node.rdfTypes[i])));
    		}
    		elmts.rdfTypesTd.html(typesTable);
    	}else{
    		elmts.rdfTypesTr.remove();
    	}
    	elmts.addRdfTypeLink.click(function(evt){
    		evt.preventDefault();
    		self._addRdfType(evt.target);
    	});
    }
    var a = $('<a href="javascript:{}"></a>')
        .addClass("schema-alignment-node-tag")
        .appendTo(this._tdNodeLabel)
        .click(function(evt) {
            self._showNodeConfigDialog();
        });
        
    if (this._node.nodeType == "cell-as-resource" || 
        this._node.nodeType == "cell-as-literal") {
        
    	if(this._node.columnIndex===-1){
    		$('<span></span>')
        	.text('Row Number')
        	.addClass("schema-alignment-node-column")
        	.prependTo(a);
    	}else{
    		if ("columnName" in this._node) {
    			a.html(" cell");
            
    			$('<span></span>')
                	.text(this._node.columnName)
                	.addClass("schema-alignment-node-column")
                	.prependTo(a);
    		} else {
    			a.html(this._options.mustBeCellTopic ? "Which column?" : "Configure...");
    		}
    	}
    } else if (this._node.nodeType == "resource") {
        if ("uri" in this._node) {
            a.html(this._node.uri);
        } else {
            a.html("Which Resource?");
        }
    } else if (this._node.nodeType == "literal") {
        if ("value" in this._node) {
            a.html(this._node.value);
        } else {
            a.html("What value?");
        }
    } else if (this._node.nodeType == "blank") {
        a.html("(blank) ");
    }else if (this._node.nodeType == "cell-as-blank") {
    	a.html("(blank) cell");
    }
    
    //Types
    /*var aux_table = $('<table>').appendTo($(this._tdMain));
    aux_table.append($('<tr>').append(td));
    this._typesTd = $('<td>').attr("width", "250").appendTo($('<tr>').appendTo(aux_table));
    this._renderTypes();*/
};


RdfSchemaAlignmentDialog.UINode.prototype._isExpandable = function() {
    return this._node.nodeType == "cell-as-resource" ||
        this._node.nodeType == "blank" ||
        this._node.nodeType == "resource" ||
        this._node.nodeType == "cell-as-blank" ;
};

RdfSchemaAlignmentDialog.UINode.prototype._showExpandable = function() {
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

RdfSchemaAlignmentDialog.UINode.prototype._renderDetails = function() {
    var self = this;

    this._tableLinks = $('<table></table>').addClass("schema-alignment-table-layout").appendTo(this._expandedDetailDiv)[0];
    
    if ("links" in this._node && this._node.links !== null) {
        for (var i = 0; i < this._node.links.length; i++) {
            this._linkUIs.push(new RdfSchemaAlignmentDialog.UILink(
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
                    nodeType: "cell-as-literal"
                }
            };
            self._linkUIs.push(new RdfSchemaAlignmentDialog.UILink(
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

RdfSchemaAlignmentDialog.UINode.prototype._showNodeConfigDialog = function(){
	var self = this;
    var frame = DialogSystem.createDialog();
    
    frame.width("800px");
    
    var header = $('<div></div>').addClass("dialog-header").text("RDF Node").appendTo(frame);
    var body = $('<div class="grid-layout layout-full"></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    /*--------------------------------------------------
     * Body
     *--------------------------------------------------
     */
    var literalTypeSelectHtml =
        '<option value="untyped" checked>untyped</option>' +
        '<option value="http://www.w3.org/2001/XMLSchema#int">xsd:int</option>' +
        '<option value="http://www.w3.org/2001/XMLSchema#double">xsd:double</option>' + 
        //'<option value="/type/float">float</option>' +
        //'<option value="/type/double">double</option>' +
        //'<option value="/type/boolean">boolean</option>' +
        '<option value="http://www.w3.org/2001/XMLSchema#date">xsd:date</option>';
    
    var html = $(
        '<table class="layout-normal">' +
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
                                                    '<td><input type="radio" name="schema-align-node-dialog-node-subtype" value="cell-as-resource" bind="radioNodeTypeCellAsResource" /></td>' +
                                                    '<td colspan="3">as a resource</td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td></td>' +
                                                    '<td></td>' +
                                                    '<td colspan="1">URI:</td>' +
                                                    '<td colspan="1"><input bind="cellAsResourceUriInput" name="cellAsResourceUri" /></td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td></td>' +
                                                    '<td></td>' +
                                                    '<td></td>' +
                                                    '<td colspan="1"><a href="#" bind="previewCellUri">preview/edit</a></td>' +
                                                '</tr>' +
                                                
                                                '<tr>' +
                                                    '<td><input type="radio" name="schema-align-node-dialog-node-subtype" value="cell-as-literal" bind="radioNodeTypeCellAsLiteral" /></td>' +
                                                    '<td colspan="3">as a literal value</td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td></td>' +
                                                    '<td colspan="2">Literal type</td>' +
                                                    '<td colspan="1"><select bind="cellAsLiteralTypeSelect">' + literalTypeSelectHtml + '</select></td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td></td>' +
                                                    '<td colspan="2">Language (for text)</td>' +
                                                    '<td colspan="1"><input bind="cellAsLiteralLanguageInput" /></td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                	'<td><input type="radio" name="schema-align-node-dialog-node-subtype" value="cell-as-blank" bind="radioNodeTypeCellAsBlank" /></td>' +
                                                	'<td colspan="3">as a blank node</td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td colspan="4"><div class="note">* relative URIs will be resolved against base URI</div></td>' + 
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
                                    '<input type="radio" name="schema-align-node-dialog-node-type" value="resource" bind="radioNodeTypeResource" /> Use a constant resource' +
                                '</div>' +
                            '</td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>URI:</td>' +
                            '<td><input bind="resourceNodeTypeInput" /></td>' +
                        '</tr>' +
                    
                        '<tr>' +
                            '<td colspan="3">' +
                                '<div class="schema-align-node-dialog-node-type">' +
                                    '<input type="radio" name="schema-align-node-dialog-node-type" value="literal" bind="radioNodeTypeLiteral" /> Use a constant literal value' +
                                '</div>' +
                            '</td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>Value</td>' +
                            '<td><input bind="literalNodeTypeValueInput" /></td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>Value type</td>' +
                            '<td><select bind="literalNodeTypeValueTypeSelect">' + literalTypeSelectHtml + '</select></td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td></td>' +
                            '<td>Language</td>' +
                            '<td><input bind="literalNodeTypeLanguageInput" /></td>' +
                        '</tr>' +
                        '<tr>' +
                            '<td colspan="3">' +
                                '<div class="schema-align-node-dialog-node-type">' +
                                    '<input type="radio" name="schema-align-node-dialog-node-type" value="blank" bind="radioNodeTypeBlank" /> Generate a blank node (shared between all rows)' +
                                '</div>' +
                            '</td>' +
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
            //.attr("value", column.name)
            .attr("value",columnIndex)
            .attr("name", "schema-align-node-dialog-column")
            .appendTo(tr.insertCell(0))
            .click(function() {
                elmts.radioNodeTypeCellAs[0].checked = true;
            });
            
        if ((!("columnName" in self._node) || !self._node.columnName) && columnIndex === -1) {
            radio.attr("checked", "true");
        } else if (column.name == self._node.columnName) {
            radio.attr("checked", "true");
        }
        
        var td = tr.insertCell(1);
        if(columnIndex===-1){
        	$(td).addClass('highlighted');
        }
        $('<span></span>').text(column.name).appendTo(td);
    };
    
    //Add Row Number
    makeColumnChoice({name:'Row Number'},-1);
    
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        makeColumnChoice(columns[i], i);
    }
    
    
    
    
    elmts.cellAsResourceUriInput.attr("disabled","disabled");
   /* elmts.previewCellUri.add(elmts.radioNodeTypeCellAsResource[0])
    .bind("click", function() { 
    	alert("shifit");
    	elmts.radioNodeTypeCellAs[0].checked=true;
    	elmts.radioNodeTypeCellAsResource[0].checked = true; 
    });*/
    
    
    
    elmts.cellAsLiteralTypeSelect.add(elmts.radioNodeTypeCellAsLiteral[0])
    .bind("focus", function() { 
    	elmts.radioNodeTypeCellAs[0].checked=true;
    	elmts.radioNodeTypeCellAsLiteral[0].checked = true; 
    });
    
     elmts.cellAsLiteralLanguageInput
    .bind("focus", function() { 
    	elmts.radioNodeTypeCellAs[0].checked=true;
    	elmts.radioNodeTypeCellAsLiteral[0].checked = true; 
    });
    
     elmts.radioNodeTypeCellAsBlank.add(elmts.radioNodeTypeCellAsResource).add(elmts.radioNodeTypeCellAsLiteral).bind("focus", function() { 
     	elmts.radioNodeTypeCellAs[0].checked=true;
     });
     
     elmts.resourceNodeTypeInput
    .bind("focus", function() { 
    	elmts.radioNodeTypeResource[0].checked = true; 
    });
   
    elmts.literalNodeTypeValueInput
    .bind("focus", function() { elmts.radioNodeTypeLiteral[0].checked = true; });
    
    elmts.literalNodeTypeValueTypeSelect
    .bind("focus", function() { elmts.radioNodeTypeLiteral[0].checked = true; });
    
    elmts.literalNodeTypeLanguageInput
    .bind("focus", function() { elmts.radioNodeTypeLiteral[0].checked = true; });
    
    
    elmts.resourceNodeTypeInput.add(elmts.literalNodeTypeValueInput[0]).add(elmts.literalNodeTypeValueTypeSelect[0])
    .add(elmts.literalNodeTypeLanguageInput[0]).add(elmts.radioNodeTypeBlank[0]).add(elmts.radioNodeTypeResource[0])
    .add(elmts.radioNodeTypeLiteral[0])
    .bind("focus",function(){
    	elmts.radioNodeTypeCellAsBlank[0].checked = false;
    	elmts.radioNodeTypeCellAsLiteral[0].checked = false;
    	elmts.radioNodeTypeCellAsResource[0].checked = false;
    });
    
   // elmts.radioNodeTypeCellAsResource[0].checked = true; // just make sure some subtype is selected
    var uri_expr = "gel:value.urlify(baseURI)";
    if (this._node.nodeType.match(/^cell-as-/)) {
        elmts.radioNodeTypeCellAs[0].checked = true;
        if (this._node.nodeType == "cell-as-resource") {
            elmts.radioNodeTypeCellAsResource[0].checked = true;
            uri_expr = this._node.uriExpression || uri_expr;
        } else if (this._node.nodeType == "cell-as-literal") {
            elmts.radioNodeTypeCellAsLiteral[0].checked = true;
            $('>option[value=' +this._node.valueType +']',elmts.cellAsLiteralTypeSelect).attr('selected','selected');
        } else if (this._node.nodeType == "cell-as-blank") {
            elmts.radioNodeTypeCellAsBlank[0].checked = true;
        }
    } else if (this._node.nodeType == "blank") {
        elmts.radioNodeTypeBlank[0].checked = true;
    } else if (this._node.nodeType == "resource") {
        elmts.radioNodeTypeResource[0].checked = true;
        if(this._node.uri){
        	elmts.resourceNodeTypeInput.val(this._node.uri);
        }
    } else if (this._node.nodeType == "literal") {
        elmts.radioNodeTypeLiteral[0].checked = true;
        if(this._node.value){
        	elmts.literalNodeTypeValueInput.val(this._node.value);
        }
        $('>option[value=' +this._node.valueType +']',elmts.literalNodeTypeValueTypeSelect).attr('selected','selected');
    }
    
    elmts.cellAsResourceUriInput.val(uri_expr);

    //preview/edit URI
    //elmts.cellAsResourceUriInput.attr("disabled","disabled").val('"' + RdfSchemaAlignment._defaultNamespace  + '" + value.urlify()');
    elmts.previewCellUri.click(function(evt){
    	evt.preventDefault();
    	var cellIndex = $("input[name='schema-align-node-dialog-column']:checked")[0].value;
    	var expr = $("input[name='cellAsResourceUri']").val();
    	self._previewURI(expr,cellIndex,elmts);
    });
    
    
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
            
            node.columnIndex = parseInt($("input[name='schema-align-node-dialog-column']:checked")[0].value);
            if(node.columnIndex!==-1){
            	node.columnName = theProject.columnModel.columns[node.columnIndex].name;
        	}else{
        		node.isRowNumberCell = true;
        	}
            
            if (node.nodeType == "cell-as-resource") {
                    node.uriExpression = elmts.cellAsResourceUriInput[0].value;
            } else if (node.nodeType == "cell-as-literal") {
                node.valueType = elmts.cellAsLiteralTypeSelect[0].value;
                
                if (node.valueType == "untyped") {
                    var l = elmts.cellAsLiteralLanguageInput[0].value;
                    node.lang = l;
                }
            } else if (node.nodeType == "cell-as-blank") {
                //DO NOTHING
            }
        } else if (node.nodeType == "blank") {
        	//DO NOTHING
        } else if (node.nodeType == "resource") {
           node.uri = elmts.resourceNodeTypeInput[0].value;
           if (!node.uri.length) {
               alert("Please specify the URI to use.");
               return null;
           }
        } else if (node.nodeType == "literal") {
            node.value = $.trim(elmts.literalNodeTypeValueInput[0].value);
            if (!node.value.length) {
                alert("Please specify the value to use.");
                return null;
            }
            node.valueType = elmts.literalNodeTypeValueTypeSelect[0].value;
            
            if (node.valueType == "untyped") {
            	node.lang = elmts.literalNodeTypeLanguageInput[0].value;
            }
        }
        
        return node;
    };
    
    $('<button></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
    	var node = getResultJSON();
    	if(self._node.rdfTypes){
    		node.rdfTypes = cloneDeep(self._node.rdfTypes);
    	}
        if (node !== null) {
            DialogSystem.dismissUntil(level - 1);
            
            self._node = node;
            /*if('columnIndex' in node){
            	if(node.columnIndex!==-1){
            		self._node.columnName = theProject.columnModel.columns[node.columnIndex].name;
            	}else{
            		self._node.isRowNumberCell = true;
            	}
            }*/
            self.render();
            //self._dialog.preview();
        }
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    
    var level = DialogSystem.showDialog(frame);
};

RdfSchemaAlignmentDialog.UINode.prototype._previewURI = function(expr,cellIndex,elmts){
	var self = this;
	//FIXME
	cellIndex = cellIndex==='-1'?0:cellIndex;
	DataTableView.promptExpressionOnVisibleRows(
	        {
	        	"cellIndex":cellIndex
	        },
	        "Custom Facet on column ", 
	        expr,
	        function(expression){
	        	expression = expression.substring(4);
	        	$("input[name='cellAsResourceUri']").val(expression);
	        	elmts.radioNodeTypeCellAs[0].checked=true;
	        	elmts.radioNodeTypeCellAsResource[0].checked = true;
	        }
	);
	        
};

RdfSchemaAlignmentDialog.UINode.prototype.render = function() {
    this._renderMain();
    if (this._isExpandable()) {
        this._showExpandable();
    } else {
        this._hideExpandable();
    }
};

RdfSchemaAlignmentDialog.UINode.prototype.removeLink = function(linkUI) {
    for (var i = this._linkUIs.length - 1; i >= 0; i--) {
        if (this._linkUIs[i] === linkUI) {
            this._linkUIs.splice(i, 1);
            this._node.links.splice(i, 1);
            break;
        }
    }
};

RdfSchemaAlignmentDialog.UINode.prototype._hideExpandable = function() {
    $(this._tdToggle).hide();
    $(this._tdDetails).hide();
};

RdfSchemaAlignmentDialog.UINode.prototype._addRdfType = function(src){
	var self = this;
	new RdfSchemaAlignmentDialog.RdfResourceDialog(src,'class', function (obj) {
			self._addNodeRdfType(obj.id,obj.name);
	});
};

RdfSchemaAlignmentDialog.UINode.prototype._removeRdfType = function(index){
	var self = this;
	self._node.rdfTypes.splice(index,1);
};

RdfSchemaAlignmentDialog.UINode.prototype._addNodeRdfType = function(uri,curie){
	if(!this._node.rdfTypes){
		this._node.rdfTypes = [];
	}
    this._node.rdfTypes.push({uri:uri,curie:curie});
    this._renderMain();
};

RdfSchemaAlignmentDialog.UINode.prototype._getTypeName = function(t){
	if(!t){return '';}
	if(t.curie !== undefined && t.curie!==''){
		return t.curie;
	}else{
		return t.uri;
	}
};

RdfSchemaAlignmentDialog.UINode.prototype.getJSON = function() {
    var result = null;
    var getLinks = false;
    
    if (this._node.nodeType.match(/^cell-as-/)) {
        if (!("columnIndex" in this._node || this._node.isRowNumberCell) || this._node.columnIndex===undefined) {
            return null;
        }
            
        if (this._node.nodeType == "cell-as-resource") {
        	if (!this._node.isRowNumberCell){
        		result = {
        				nodeType: this._node.nodeType,
        				columnIndex: this._node.columnIndex,
        				uriExpression: this._node.uriExpression,
        				isRowNumberCell: this._node.isRowNumberCell || false,
        				columnName: this._node.columnName
        				//	type: "type" in this._node ? cloneDeep(this._node.type) : { "id" : "/common/topic", "name" : "Topic", "cvt" : false }
        		};
        	}else{
        		result = {
        				nodeType: this._node.nodeType,
        				columnIndex: this._node.columnIndex,
        				uriExpression: this._node.uriExpression,
        				isRowNumberCell: this._node.isRowNumberCell || false
        				//	type: "type" in this._node ? cloneDeep(this._node.type) : { "id" : "/common/topic", "name" : "Topic", "cvt" : false }
        		};
        	}
            getLinks = true;
        } else if (this._node.nodeType == "cell-as-literal") {
            result = {
                nodeType: this._node.nodeType,
                columnIndex: this._node.columnIndex,
                valueType: "valueType" in this._node ? this._node.valueType : "untyped",
                lang: "lang" in this._node ? this._node.lang : "en",
                columnName: this._node.columnName
            };
        } else if (this._node.nodeType == "cell-as-blank") {
            result = {
                nodeType: this._node.nodeType,
                columnIndex: this._node.columnIndex,
                columnName: this._node.columnName
            };
            getLinks = true;
        }
    } else if (this._node.nodeType == "resource") {
        result = {
            nodeType: this._node.nodeType,
            uri: this._node.uri
        };
        getLinks = true;
    } else if (this._node.nodeType == "literal") {
        if (!("value" in this._node) || !this._node.value) {
            return null;
        }
        result = {
            nodeType: this._node.nodeType,
            value: this._node.value,
            valueType: "valueType" in this._node ? this._node.valueType : "untyped",
            lang: "lang" in this._node ? this._node.lang : "en"
        };
    } else if (this._node.nodeType == "blank") {
        result = {
            nodeType: this._node.nodeType,
            columnIndex: this._node.columnIndex
        };
        getLinks = true;
    }
    
    if (!result) {
        return null;
    }
    if (getLinks) {
    	var rdfTypes = [];
    	if(this._node.rdfTypes){
    		for(var i=0;i<this._node.rdfTypes.length; i++){
    			rdfTypes.push({uri:this._node.rdfTypes[i].uri,
    							curie:this._node.rdfTypes[i].curie
    				});
    		}
    		result.rdfTypes = rdfTypes;
    	}
        var links = [];
        for (var i = 0; i < this._linkUIs.length; i++) {
            var link = this._linkUIs[i].getJSON();
            if (link !== null) {
                links.push(link);
            }
        }
        result.links = links;
    }
    
    return result;
};

RdfSchemaAlignmentDialog.NewRdfResourceDialog = function(elmt,defaultVal,onDone){
	var menu = MenuSystem.createMenu().width('400px');
	menu.html('<div class="schema-alignment-link-menu-type-search">' + 
			'<span class="schema-alignment-node-column">URI: <small>(relative URIs will be resolved against base URI)</small></span>' + 
			'<input type="text" bind="newResourceUri" size="50"><br/>' +
			'<button bind="applyBtn">Apply</button>' + 
			'<button bind="cancelBtn">Cancel</button>'
			//'<input type="text" bind="newResourceUri2" size="50" style="display:none">'
			);
	MenuSystem.showMenu(menu,function(){});
	MenuSystem.positionMenuLeftRight(menu, $(elmt));
	var elmts = DOM.bind(menu);
	elmts.newResourceUri.val(defaultVal).focus().select();
	elmts.cancelBtn.click(function(){
		MenuSystem.dismissAll();
	});
	
	elmts.applyBtn.click(function(){
		var val = elmts.newResourceUri.val();
		if(!val){
			alert('Enter URI');
			return;
		}
		MenuSystem.dismissAll();
		if(val.substring(0,1)===':'){
			val = val.substring(1);
		}
		var obj = {
				id:val,
				name:val.match('^http://')?val:':'+val
		};
		onDone(obj);
	});
};
RdfSchemaAlignmentDialog.RdfResourceDialog = function(elmt,lookFor,onDone,defaultVal){
	var menu = MenuSystem.createMenu().width('400px');
	menu.html('<div class="schema-alignment-link-menu-type-search">' + 
			'<span>Search for ' + lookFor + ':</span>' + 
			'<input type="text" bind="newResourceUri" >' 
			//'<input type="text" bind="newResourceUri2" size="50" style="display:none">'
			);
	MenuSystem.showMenu(menu,function(){});
	MenuSystem.positionMenuLeftRight(menu, $(elmt));
	var elmts = DOM.bind(menu);
	
	/*elmts.createOne.click(function(evt){
		evt.preventDefault();
		MenuSystem.dismissAll();
		new RdfSchemaAlignmentDialog.NewRdfResourceDialog(elmt,defaultVal||'',onDone);
	});*/
	//Autocomplete
	/*elmts.newResourceUri.autoComplete({
		script:'/command/rdf-exporter-extension/search-vocabularies?type=' + lookFor + '&',
		varname:'input',
		json:true,
		shownoresults:true,
		maxresults:16,
		cache:false,
		timeout:null,
		minchars:2,
		callback: function (obj) { 
			MenuSystem.dismissAll();
			if(onDone){
				onDone(obj);
			}
		}
	});
	elmts.newResourceUri.focus();*/
	
	elmts.newResourceUri.suggestterm({type_strict:lookFor}).bind('fb-select',function(e,data){
		MenuSystem.dismissAll();
		if(onDone){
			onDone(data);
		}
	}).bind('fb-select-new',function(e,val){
		MenuSystem.dismissAll();
		new RdfSchemaAlignmentDialog.NewRdfResourceDialog(elmt,defaultVal||'',onDone);
	});
	elmts.newResourceUri.focus();
};