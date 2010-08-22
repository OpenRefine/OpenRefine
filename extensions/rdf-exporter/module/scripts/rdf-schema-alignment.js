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
    this._replaceBaseUri(RdfSchemaAlignment._defaultNamespace || URL.getHostname()+'/');
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
    
    $('<button></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
    	var schema = self.getJSON();
    	Gridworks.postProcess(
    	        "rdf-exporter-extension",
                "save-rdf-schema",
                {},
                { schema: JSON.stringify(schema) },
                {},
                {   
                    onDone: function() {
                        DialogSystem.dismissUntil(self._level - 1);
                        theProject.schema = schema;
                    }
                }
            );
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
};

RdfSchemaAlignmentDialog.prototype._constructBody = function(body) {
	var self = this;
    $('<p>' +
        'RDF Schema Alignment... node types are: cell-as-resource, cell-as-literal, cell-as-blank, literal, resource, and blank' +
    '</p>').appendTo(body);
    
    var html = $(
    	'<p>Base URI: <span bind="baseUriSpan" class="emphasized"></span> <a href="#" bind="editBaseUriLink">edit</a></p>'+
        '<div id="rdf-schema-alignment-tabs" class="gridworks-tabs">' +
            '<ul>' +
                '<li><a href="#rdf-schema-alignment-tabs-schema">RDF Skeleton</a></li>' +
                '<li><a href="#rdf-schema-alignment-tabs-preview">RDF Preview</a></li>' +
                '<li><a href="#rdf-schema-alignment-tabs-vocabulary-manager">Vocabulary Manager</a></li>' +
            '</ul>' +
            '<div id="rdf-schema-alignment-tabs-schema">' +
                '<div class="schema-alignment-dialog-canvas rdf-schema-alignment-dialog-canvas"></div>' +
            '</div>' +
            '<div id="rdf-schema-alignment-tabs-preview" style="display: none;">' +
                '<div class="schema-alignment-dialog-preview" id="rdf-schema-alignment-dialog-preview"></div>' +
            '</div>' +
            '<div id="rdf-schema-alignment-tabs-vocabulary-manager" style="display: none;">' +
                '<div id="rdf-vocabulary-manager" class="schema-alignment-dialog-preview"></div>' +
            '</div>' +
        '</div>'
    ).appendTo(body);
    var elmts = DOM.bind(html);
    this._baseUriSpan = elmts.baseUriSpan;
    elmts.baseUriSpan.text(RdfSchemaAlignment._defaultNamespace);
    
    elmts.editBaseUriLink.click(function(evt){
    	evt.preventDefault();
    	self._editBaseUri($(evt.target));
    });
    
};

RdfSchemaAlignmentDialog.prototype._previewRdf = function(){
	var self = this;
	var schema = this.getJSON();
	self._previewPane.empty().html('<img src="images/large-spinner.gif" title="loading..."/>');
	$.post(
	    "/command/rdf-exporter-extension/preview-rdf?" + $.param({ project: theProject.id }),
        { schema: JSON.stringify(schema), engine: JSON.stringify(ui.browsingEngine.getJSON()) },
        function(data) {
        	self._previewPane.empty();
        	self._previewPane.text(data.v);
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
    $("#rdf-schema-alignment-tabs-preview").css("display", "");
    $("#rdf-schema-alignment-tabs-vocabulary-manager").css("display", "");

    this._canvas = $(".schema-alignment-dialog-canvas");
    this._nodeTable = $('<table></table>').addClass("schema-alignment-table-layout").appendTo(this._canvas)[0];
    
    for (var i = 0; i < this._schema.rootNodes.length; i++) {
        this._nodeUIs.push(new RdfSchemaAlignmentDialog.UINode(
            this,
            this._schema.rootNodes[i], 
            this._nodeTable, 
            {
                expanded: true,
                mustBeCellTopic: true
            }
        ));
    }
    
    this._previewPane = $("#rdf-schema-alignment-dialog-preview");
    
    this._vocabularyManager = $("#rdf-vocabulary-manager");
    var vocab_html = $(
    		'<div id="vocabulary-list" bind="vocabularyList" class="vocabulary-manager-list">' + 
    		'</div>' + 
    		'<div id="new-vocabulary" bind="newVocabulary" class="vocabulary-manager-footer">' +
    		'</div>'
    		);
    var elmts = DOM.bind(vocab_html);
    this._vocabularyManager.html(vocab_html);
    this._vocabularyListContainer = elmts.vocabularyList;
    this._newVocabularyContainer = elmts.newVocabulary;
    this._listVocabularies();
};
//TODO refactor vocabulary manager into its stand-alone Javascript object (~Class)
RdfSchemaAlignmentDialog.prototype._listVocabularies = function(){
	var self = this;
	self._vocabularyListContainer.empty();
	self._renderVocabularyFooter();
	
	
	$.get("/command/rdf-exporter-extension/list-vocabularies",{},function(o){
		var vocab_table = $('<table></table>').width('100%').addClass('data-table')[0];
		var tr = vocab_table.insertRow(vocab_table.rows.length);
		$(tr).addClass('');
		$(tr).html('<td></td><td>Prefix</td><td>Namespace</td>');
		$('td',tr).addClass('column-header');
		for(var i=0;i<o.vocabularies.length;i++){
			var vocabulary = o.vocabularies[i];
			var tr = vocab_table.insertRow(vocab_table.rows.length);
			$(tr).addClass(i%2==0?'even':'odd');
			var td = tr.insertCell(0);
			var getDelFunc = function(){
				var vocab_uri = vocabulary.uri; 
				return function(){self._deleteVocabulary(vocab_uri);}
			}();
			var del_link = $('<a>').html('<img title="remove vocabulary" src="images/close.png" style="cursor: pointer;" />').click(getDelFunc).appendTo($(td));
			
			td = tr.insertCell(1);
			$(td).text(vocabulary.name );
			td = tr.insertCell(2);
			$(td).text(vocabulary.uri);
		}
		$(vocab_table).appendTo(self._vocabularyListContainer);
	},"json");
};

RdfSchemaAlignmentDialog.prototype._renderVocabularyFooter = function(){
	var self = this;
	self._newVocabularyContainer.empty();
	var html = $(
			'<h2>Add New:</h2>' +
			'<form>'+
			'<table width="900px">' +
				'<tr><td>prefix:</td><td> <input type="text" bind="prefix" size="12"></td><td> e.g. rdf, rdfs, foaf, skos, dcat...</td></tr>' +
				'<tr><td>url:</td><td> <input type="text" bind="url" size="40"></td><td> The URL where vocabulary definition can be obtained as RDF/XML</td></tr>' +
				'<tr><td>namespace:</td><td> <input type="text" bind="namespace" size="40"></td><td> <span>The namespace under which vocabulary classes and properties are defined (only those will be imported)</span><br/><span> it is usually the same as the URL.</span> </td></tr>' +
				'<tr><td colspan="3"><input type="submit" bind="addvocab" id="addvocab-submit" value="Import Vocabulary"></td></tr>' +
			'</table>' +
			'</form>'
			);
	
	var elmts = DOM.bind(html);
    elmts.addvocab.click(function(e){
    	e.preventDefault();
    	var prefix = elmts.prefix.val();
    	var namespace = elmts.namespace.val();
    	var url = elmts.url.val();
    	if(!(prefix && namespace && url)){
    		alert('Please enter all required parameters');
    		return ;
    	}
		var dismissBusy = DialogSystem.showBusy('Importing vocabulary from: ' + url);
    	$.get("/command/rdf-exporter-extension/import-vocabulary",{prefix:prefix,namespace:namespace,url:url},function(data){
    		dismissBusy();
    		if (data.code === "error"){
    			alert('Error:' + data.message)
    		}else{
    			self._listVocabularies();
    		}
    	},"json");
    });
    
    html.appendTo(self._newVocabularyContainer);
};

RdfSchemaAlignmentDialog.prototype._deleteVocabulary = function(uri){
	var self = this;
	var dismissBusy = DialogSystem.showBusy('Deleteing vocabulary: ' + uri);
	$.post("/command/rdf-exporter-extension/delete-vocabulary",{uri:uri},function(data){
		dismissBusy();
		if (data.code === "error"){
			alert('Error:' + data.message)
		}else{
			self._listVocabularies();
		}
	},"json");
};

RdfSchemaAlignment.createNewRootNode = function() {
    var links = [];
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        var target = {
            nodeType: "cell-as-literal",
            columnName: column.name,
            columnIndex:i
        };        
        links.push({
                uri: null,
                curie:null,
                target: target
            });
    }
    
    rootNode = { nodeType: "cell-as-resource", uriExpression:"gel:row.index.urlify(baseURI)", columnIndex:-1, isRowNumberCell:true};
    rootNode.links = links;
    
    return rootNode;
};

RdfSchemaAlignmentDialog.prototype._editBaseUri = function(src){
	var self = this;
	var menu = MenuSystem.createMenu().width('400px');
	menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newBaseUri" size="50"><br/>'+
			'<button bind="applyButton">Apply</button>' + 
			'<button bind="cancelButton">Cancel</button></div>'
			);
	MenuSystem.showMenu(menu,function(){});
	MenuSystem.positionMenuLeftRight(menu, src);
	var elmts = DOM.bind(menu);
	elmts.newBaseUri.val(RdfSchemaAlignment._defaultNamespace).focus().select();
	elmts.applyButton.click(function() {
		var newBaseUri = elmts.newBaseUri.val();
		if(!newBaseUri || !newBaseUri.substring(7)=='http://'){
			alert('Base URI should start with http://');
			return;
		}
        MenuSystem.dismissAll();
        self._replaceBaseUri(newBaseUri);
    });
	
	elmts.cancelButton.click(function() {
	   MenuSystem.dismissAll();
	});
};
RdfSchemaAlignmentDialog.prototype._replaceBaseUri = function(newBaseUri){
	var self = this;
	RdfSchemaAlignment._defaultNamespace = newBaseUri;
	$.post("/command/rdf-exporter-extension/save-baseURI?" + $.param({project: theProject.id }),{baseURI:newBaseUri},function(data){
		if (data.code === "error"){
			alert('Error:' + data.message)
		}else{
			self._baseUriSpan.empty().text(newBaseUri);
		}
	},"json");
};

RdfSchemaAlignmentDialog.prototype.getJSON = function() {
    var rootNodes = [];
    for (var i = 0; i < this._nodeUIs.length; i++) {
        var node = this._nodeUIs[i].getJSON();
        if (node !== null) {
            rootNodes.push(node);
        }
    }
    return {
    	baseUri:RdfSchemaAlignment._defaultNamespace,
        rootNodes: rootNodes
    };
};