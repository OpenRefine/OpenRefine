var RdfSchemaAlignment = {};

function RdfSchemaAlignmentDialog(schema){
	this._init(schema);
    this._buildBody();
    
    //initialize vocabularyManager
    this._prefixesManager = new RdfPrefixesManager(this, this._schema.prefixes);   
    this._replaceBaseUri(RdfSchemaAlignment._defaultNamespace || URL.getHostname()+'/',true);

};

RdfSchemaAlignmentDialog.prototype._init = function(schema) {
	var self = this;
	self._originalSchema = schema || { rootNodes: [] };	
	self._schema = cloneDeep(self._originalSchema); // this is what can be munched on
    
    if (!self._schema.rootNodes.length) {
        self._schema.rootNodes.push(RdfSchemaAlignment.createNewRootNode());
    }
    
	//this._schema = { rootNodes: [] };
	//this._schema.rootNodes.push(RdfSchemaAlignment.createNewRootNode());
	self._nodeUIs = [];
	
	RdfSchemaAlignment._defaultNamespace = self._schema.baseUri;		
};

RdfSchemaAlignmentDialog.prototype._buildBody = function() {
    var self = this;
    
    var dialog = $(DOM.loadHTML("rdf-extension", "scripts/dialogs/rdf-schema-alignment.html"));
    self._elmts = DOM.bind(dialog);
    
    self._elmts.cancelButton.click(function() { DialogSystem.dismissUntil(self._level - 1);});
    
    self._elmts.okButton.click(function () {
    	var schema = self.getJSON();
    	Refine.postProcess(
    	        "rdf-extension",
                "save-rdf-schema",
                {},
                { schema: JSON.stringify(schema) },
                {},
                {   
                    onDone: function() {
                        DialogSystem.dismissUntil(self._level - 1);
                        theProject.overlayModels.rdfSchema = schema;
                    }
                }
            );
    });
    
    var body = self._elmts.dialogBody;  
    self._constructBody(body);
    self._level = DialogSystem.showDialog(dialog);    
    this._renderBody(body);
};


RdfSchemaAlignmentDialog.prototype._constructBody = function(body) {
	var self = this;    
    self._baseUriSpan = self._elmts.baseUriSpan;
    self._rdf_schema_prefixes = self._elmts.rdf_schema_prefixes;
    self._elmts.baseUriSpan.text(RdfSchemaAlignment._defaultNamespace);
    
    self._elmts.editBaseUriLink.click(function(evt){
    	evt.preventDefault();
    	self._editBaseUri($(evt.target));
    });
    self._elmts._save_skeleton.click(function(e){
    	e.preventDefault();
    	var schema = self.getJSON();
    	Refine.postProcess(
    	        "rdf-extension",
                "save-rdf-schema",
                {},
                { schema: JSON.stringify(schema) },
                {},
                {   
                    onDone: function() {
                       theProject.overlayModels.rdfSchema = schema;
                    }
                }
            );
    });
    
    self._elmts.add_another_root_node.click(function(e){
    	e.preventDefault();
    	var newRootNode = RdfSchemaAlignment.createNewRootNode(false)
    	self._schema.rootNodes.push(newRootNode);
    	self._nodeUIs.push(new RdfSchemaAlignmentDialog.UINode(
            self,
            newRootNode, 
            self._nodeTable, 
            {
                expanded: true,
            }
        ));
    });
    
};

RdfSchemaAlignmentDialog.prototype._previewRdf = function(){
	var self = this;
	var schema = self.getJSON();
	self._previewPane.empty().html('<img src="images/large-spinner.gif" title="loading..."/>');
	$.post(
	    "command/rdf-extension/preview-rdf?" + $.param({ project: theProject.id }),
        { schema: JSON.stringify(schema), engine: JSON.stringify(ui.browsingEngine.getJSON()) },
        function(data) {
        	self._previewPane.empty();
        	self._previewPane.html(linkify(data.v));
	    },
	    "json"
	);
};

RdfSchemaAlignmentDialog.prototype._renderBody = function(body) {
    var self = this;
    
    $("#rdf-schema-alignment-tabs").tabs({
    	select:function(evt,tabs){
    		if(tabs.index===1){
    			self._previewRdf();
    		}
    	}
    });
    //$("#rdf-schema-alignment-tabs-preview").css("display", "");
    //$("#rdf-schema-alignment-tabs-vocabulary-manager").css("display", "");

    self._canvas = $(".schema-alignment-dialog-canvas");
    self._nodeTable = $('<table></table>')
     .addClass("schema-alignment-table-layout")
     .addClass("rdf-schema-alignment-table-layout")
     .appendTo(self._canvas)[0];
    
    for (var i = 0; i < self._schema.rootNodes.length; i++) {
        self._nodeUIs.push(new RdfSchemaAlignmentDialog.UINode(
            self,
            self._schema.rootNodes[i], 
            self._nodeTable, 
            {
                expanded: true,
            }
        ));
    }
    
    self._previewPane = $("#rdf-schema-alignment-dialog-preview");
};



RdfSchemaAlignment.createNewRootNode = function(withDefaultChildren) {
    rootNode = { nodeType: "cell-as-resource", expression:"value", isRowNumberCell:true};
    var links = [];
    if(withDefaultChildren === false){
    	rootNode.links = links;
    	return rootNode;
    }
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        var target = {
            nodeType: "cell-as-literal",
            columnName: column.name,
        };        
        links.push({
                uri: null,
                curie:null,
                target: target
            });
    }
    rootNode.links = links;
    
    return rootNode;
};

RdfSchemaAlignmentDialog.prototype._editBaseUri = function(src){
	var self = this;
	var menu = MenuSystem.createMenu().width('400px');
	menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newBaseUri" size="50"><br/>'+
			'<button class="button" bind="applyButton">Apply</button>' + 
			'<button class="button" bind="cancelButton">Cancel</button></div>'
			);
	MenuSystem.showMenu(menu,function(){});
	MenuSystem.positionMenuLeftRight(menu, src);
	var elmts = DOM.bind(menu);
	elmts.newBaseUri.val(RdfSchemaAlignment._defaultNamespace).focus().select();
	elmts.applyButton.click(function() {
		var newBaseUri = elmts.newBaseUri.val();
		/*if(!newBaseUri || !newBaseUri.substring(7)=='http://'){
			alert('Base URI should start with http://');
			return;
		}*/
        MenuSystem.dismissAll();
        self._replaceBaseUri(newBaseUri);
    });
	
	elmts.cancelButton.click(function() {
	   MenuSystem.dismissAll();
	});
};
RdfSchemaAlignmentDialog.prototype._replaceBaseUri = function(newBaseUri,doNotSave){
	var self = this;
	RdfSchemaAlignment._defaultNamespace = newBaseUri;
	if(!doNotSave){
		$.post("command/rdf-extension/save-baseURI?" + $.param({project: theProject.id }),{baseURI:newBaseUri},function(data){
			if (data.code === "error"){
				alert('Error:' + data.message)
			}else{
				self._baseUriSpan.empty().text(newBaseUri);
			}
		},"json");
	}else{
		self._baseUriSpan.empty().text(newBaseUri);
	}
};

RdfSchemaAlignmentDialog.prototype.getJSON = function() {
    var rootNodes = [];
    for (var i = 0; i < this._nodeUIs.length; i++) {
        var node = this._nodeUIs[i].getJSON();
        if (node !== null) {
            rootNodes.push(node);
        }
    }
    
    var prefixes = [];
    for(var i=0; i<this._prefixesManager._prefixes.length; i++) {
    	prefixes.push({"name":this._prefixesManager._prefixes[i].name,"uri":this._prefixesManager._prefixes[i].uri});
    }
    return {
    	prefixes:prefixes,
    	baseUri:RdfSchemaAlignment._defaultNamespace,
        rootNodes: rootNodes
    };
};

RdfSchemaAlignmentDialog._findColumn = function(columnName) {
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        if (column.name == columnName) {
            return column;
        }
    }
    return null;
};

