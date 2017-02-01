var RdfSchemaAlignment = {};

function RdfSchemaAlignmentDialog(schema){
	this._originalSchema = schema || { rootNodes: [] };
    this._schema = cloneDeep(this._originalSchema); // this is what can be munched on
    
    if (!this._schema.rootNodes.length) {
        this._schema.rootNodes.push(RdfSchemaAlignment.createNewRootNode());
    }
    
	//this._schema = { rootNodes: [] };
	//this._schema.rootNodes.push(RdfSchemaAlignment.createNewRootNode());
	this._nodeUIs = [];
    this._createDialog();

	RdfSchemaAlignment._defaultNamespace = this._schema.baseUri;
	
	//initialize vocabularyManager
    this._prefixesManager = new RdfPrefixesManager(this,this._schema.prefixes);
    
    this._replaceBaseUri(RdfSchemaAlignment._defaultNamespace || URL.getHostname()+'/',true);
};

RdfSchemaAlignmentDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    
    frame.width("1000px");
    
    var header = $('<div></div>').addClass("dialog-header").text("RDF Schema Alignment").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    this._constructFooter(footer);
    this._constructBody(body);
  
    this._level = DialogSystem.showDialog(frame);
    
    this._renderBody(body);
};

RdfSchemaAlignmentDialog.prototype._constructFooter = function(footer) {
    var self = this;
    
    $('<button></button>').addClass('button').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
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
    }).appendTo(footer);
    
    $('<button></button>').addClass('button').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
};

RdfSchemaAlignmentDialog.prototype._constructBody = function(body) {
	var self = this;
    $('<p>' +
        'The RDF schema alignment skeleton below specifies how the RDF data that will get generated from your grid-shaped data. ' +
        'The cells in each record of your data will get placed into nodes within the skeleton. Configure the skeleton by specifying which column to substitute into which node.' +
    '</p>').appendTo(body);
    
    var html = $(
    	'<p class="base-uri-space"><span class="emphasized">Base URI:</span> <span bind="baseUriSpan" ></span> <a href="#" bind="editBaseUriLink">edit</a></p>'+
        '<div id="rdf-schema-alignment-tabs" class="refine-tabs">' +
            '<ul>' +
                '<li><a href="#rdf-schema-alignment-tabs-schema">RDF Skeleton</a></li>' +
                '<li><a href="#rdf-schema-alignment-tabs-preview">RDF Preview</a></li>' +
            '</ul>' +
            '<div id="rdf-schema-alignment-tabs-schema">' +
            	'<div class="rdf-scheme-dialog-subheader"><table><tr><td><span style="display:block;width:150px;">Available Prefixes:</span></td><td><div class="rdf-schema-prefixes" bind="rdf_schema_prefixes"></div></td></tr></table></div>' + 
                '<div class="schema-alignment-dialog-canvas rdf-schema-alignment-dialog-canvas"></div>' +
                '<div class="rdf-schema-alignment-body-footer"><a bind="add_another_root_node" href="#">Add another root node</a><a bind="_save_skeleton" href="#" style="float:right">Save</a></div>'  +
            '</div>' +
            '<div id="rdf-schema-alignment-tabs-preview" style="display: none;">' +
                '<div class="rdf-scheme-dialog-subheader">This is a sample <code>Turtle</code> representation of (up-to) the <em>first 10</em> rows</div>' + 
                '<div class="rdf-schema-alignment-dialog-preview" id="rdf-schema-alignment-dialog-preview"></div>' +
            '</div>' +
        '</div>'
    ).appendTo(body);
    var elmts = DOM.bind(html);
    this._baseUriSpan = elmts.baseUriSpan;
    this._rdf_schema_prefixes = elmts.rdf_schema_prefixes;
    elmts.baseUriSpan.text(RdfSchemaAlignment._defaultNamespace);
    
    elmts.editBaseUriLink.click(function(evt){
    	evt.preventDefault();
    	self._editBaseUri($(evt.target));
    });
    elmts._save_skeleton.click(function(e){
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
    
    elmts.add_another_root_node.click(function(e){
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
	var schema = this.getJSON();
	self._previewPane.empty().html('<img src="images/large-spinner.gif" title="loading..."/>');
	$.post(
	    "command/rdf-extension/preview-rdf?" + $.param({ project: theProject.id }),
        { schema: JSON.stringify(schema), engine: JSON.stringify(ui.browsingEngine.getJSON()) },
        function(data) {
        	self._previewPane.empty();
        	self._previewPane.html(linkify('<pre>' + data.v + '</pre>'));
	    },
	    "json"
	);
};

RdfSchemaAlignmentDialog.prototype._renderBody = function(body) {
    var self = this;
    
    $("#rdf-schema-alignment-tabs").tabs({
    	activate:function(evt, tabs){
    		if(tabs.newTab.index()===1){
    			$("#rdf-schema-alignment-tabs-preview").css("display", "");
    			self._previewRdf();
    		}
    	},
    	select:function(evt,ui){
    		if(ui.index===1){
    			$("#rdf-schema-alignment-tabs-preview").css("display", "");
    			self._previewRdf();
    		}
    	}
    });
    //
   // $("#rdf-schema-alignment-tabs-vocabulary-manager").css("display", "");

    this._canvas = $(".schema-alignment-dialog-canvas");
    this._nodeTable = $('<table></table>').addClass("schema-alignment-table-layout").addClass("rdf-schema-alignment-table-layout").appendTo(this._canvas)[0];
    
    for (var i = 0; i < this._schema.rootNodes.length; i++) {
        this._nodeUIs.push(new RdfSchemaAlignmentDialog.UINode(
            this,
            this._schema.rootNodes[i], 
            this._nodeTable, 
            {
                expanded: true,
            }
        ));
    }
    
    this._previewPane = $("#rdf-schema-alignment-dialog-preview");
};



RdfSchemaAlignment.createNewRootNode = function(withDefaultChildren) {
    rootNode = { nodeType: "cell-as-resource", expression:"value", isRowNumberCell:true};
    var links = [];
    if(withDefaultChildren===false){
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

