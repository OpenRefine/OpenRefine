function RdfPrefixesManager (dialog,prefixes){
	this._dialog = dialog;
	if(prefixes){
		this._prefixes = prefixes;
		this._showPrefixes();
	}else{
		//prefixes have not been initialized
		var self = this;
		dialog._rdf_schema_prefixes.empty().html('<img src="images/small-spinner.gif" />');
		this._loadPrefixes();
		//get the original schema initialized i.e. give it its copy, prefixes are committed automatically 
		this._dialog._originalSchema.prefixes = [];
		for(var i=0;i<this._prefixes;i++){
			this._dialog._originalSchema.prefixes.push({name:this._prefixes[i].name,uri:this._prefixes[i].uri});
		}
	}
};

RdfPrefixesManager.isPrefixedQname = function(qname){
	return qname.match(/[_a-zA-Z][-_a-zA-Z0-9]*:($|([_a-zA-Z][-_a-zA-Z0-9]*))$/);
};

RdfPrefixesManager.prototype.isKnownPrefix = function(p){
	for(var i=0;i<this._prefixes.length;i++){
		if(this._prefixes[i].prefix===p){
			return true;
		}
	}
	return false;
};

RdfPrefixesManager.deAssemble = function(qname){
	var i = qname.indexOf(':');
	if(i ===-1){
		return {prefix:null,localPart:qname};
	}
	return  {prefix:qname.substring(0,i),localPart:qname.substring(i+1)};
};

RdfPrefixesManager.getPrefix = function(qname){
	var i = qname.indexOf(':');
	if(i ===-1){
		return null;
	}
	return  qname.substring(0,i);
};

RdfPrefixesManager.getFullUri = function(prefixedQname){
	var o = RdfPrefixesManager.deAssemble(prefixedQname);
	if(!o.prefix){
		return null;
	}
	for(var i=0;i<RdfPrefixesManager.prefixes.length;i++){
		if(RdfPrefixesManager.prefixes[i].name===o.prefix){
			return RdfPrefixesManager.prefixes[i].uri + o.localPart;
		}
	}
	return null;
};

RdfPrefixesManager.prototype._addPrefix = function(msg,def_prefix,onDone){
	var self = this;
	var widget = new NewPrefixWidget(self);
	widget.show(msg,def_prefix,function(name,uri){
		self._prefixes.push({name:name,uri:uri});
		//add to the original schema. adding a prefix is automatically committed to the original schema.
		//especially as the index will have been added on the server side 
		if(!self._dialog._originalSchema.prefixes){
			//should never get here as prefixes should have been already initialized
			self._dialog._originalSchema.prefixes = [];
		}
		self._dialog._originalSchema.prefixes.push({name:name,uri:uri});
		
		self._showPrefixes();
		if(onDone){
			onDone();
		}
	});
};

RdfPrefixesManager.prototype.hasPrefix = function(name){
	for(var i=0; i<this._prefixes.length; i++){
		if(this._prefixes[i].name===name){
			return true;
		}
	}
	return false;
};

RdfPrefixesManager.prototype._loadPrefixes = function(onDone){
	var self =this;
	$.get("/command/rdf-exporter-extension/list-prefixes",{project:theProject.id},function(data){
		self._prefixes = data.prefixes;
		self._showPrefixes();
		if(onDone){
			onDone(data);
		}
	},"json");
};

RdfPrefixesManager.prototype._showPrefixes = function(){
	var self = this;
	this._dialog._rdf_schema_prefixes.empty();
	for(var i=0;i<this._prefixes.length;i++){
		self._renderPrefix(this._prefixes[i].name,this._prefixes[i].uri);
	}
	//add button
	$('<a href="#" class="add-prefix-box">add prefix</a>').bind('click',function(e){
		e.preventDefault();
		self._addPrefix();
	}).appendTo(self._dialog._rdf_schema_prefixes);
	
	//configure button
	$('<a href="#" class="manage-vocabularies-box">manage prefixes</a>').bind('click',function(e){
		e.preventDefault();
		self._manageVocabularies();
	}).appendTo(self._dialog._rdf_schema_prefixes);
	
};

RdfPrefixesManager.prototype._renderPrefix = function(prefix,uri){
	this._dialog._rdf_schema_prefixes.append($('<span/>').addClass('rdf-schema-prefix-box').attr('title',uri).text(prefix));
};

RdfPrefixesManager.prototype._manageVocabularies = function(srcElement){
	var widget = new ManageVocabulariesDialog(this);
	widget.show();
};

function NewPrefixWidget(manager){
	this._prefixesManager = manager;
}

NewPrefixWidget.prototype.show = function(msg,def_prefix,onDone){
	var self = this;
    var frame = DialogSystem.createDialog();
    
    frame.width("250px");
    
    var header = $('<div></div>').addClass("dialog-header").text("New Prefix").appendTo(frame);
    var body = $('<div class="grid-layout layout-full"></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    var html = $(
    		'<div class="message" bind="message"></div>' + 
    		'<table>' +
    			'<tr><td>prefix:</td><td style="padding-bottom:4px"> <input type="text" bind="prefix" size="4" /></td></tr>' + 
    			'<tr><td>URI:</td><td style="padding-bottom:4px"> <input type="text" bind="uri" size="25" /></td></tr>' +
    		'</table>' 
    ).appendTo(body);
    
    var elmts = DOM.bind(html);
    
    if(msg){
    	elmts.message.text(msg);
    }
    if(def_prefix){
    	elmts.prefix.val(def_prefix);
    }
    
    var importVocabulary = function(onDone){
    	var name = elmts.prefix.val();
    	var uri = elmts.uri.val();
    	if(self._prefixesManager.hasPrefix(name)){
    		alert('Prefix "' + name + '" is already defined');
    		return;
    	}
		var dismissBusy;
		dismissBusy = DialogSystem.showBusy('Trying to import vocabulary from ' + uri);	
    	$.get("/command/rdf-exporter-extension/add-prefix",{name:name,uri:uri,project: theProject.id},function(data){
    		if (data.code === "error"){
    			alert('Error:' + data.message)
    		}else{
    			DialogSystem.dismissUntil(level - 1);
    			if(onDone){
    				onDone(name,uri);
    			}
    		}
			dismissBusy();
    	});
    };
    
    $('<button></button>').addClass('button').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
    	importVocabulary(onDone);
    }).appendTo(footer);
    
    $('<button></button>').addClass('button').text("Cancel").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    
    
    var level = DialogSystem.showDialog(frame);
    elmts.prefix.focus();
};

function ManageVocabulariesDialog(manager){
	this._prefixesManager = manager;
}

ManageVocabulariesDialog._addImageLink = function(file_name, txt, handler, parent) {
	var imgElem =$('<img/>').attr('src','extension/rdf-exporter-extension/images/'+file_name).attr('alt',txt).addClass('vocab_action_icon'); 
	$('<a/>').attr('href','#').bind('click',function(e){e.preventDefault();handler();}).attr('title',txt).append(imgElem).appendTo(parent);
};

ManageVocabulariesDialog.showSetAsDefaultInfo = function(){
	var frame = DialogSystem.createDialog();
    frame.width("250px");
    var header = $('<div></div>').addClass("dialog-header").text("Set as default prefixes").appendTo(frame);
    var body = $('<div class="grid-layout layout-full"></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    var html = $(
    		'<p>"Set as default" will make the list of the current project\'s prefixes the default one. Any newly created project will have them defined by default. ' +
    		'Existing projects are not affected.</p>'
    ).appendTo(body);
    
    $('<button></button>').addClass('button').text("Close").click(function() {
    	DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    var level = DialogSystem.showDialog(frame);
};

ManageVocabulariesDialog.prototype.show = function(){
	var self = this;
	var frame = DialogSystem.createDialog();
    
    frame.width("600px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Defined Prefixes").appendTo(frame);
    var body = $('<div class="grid-layout layout-full"></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    var html = $(
    		'<div class="vocabulary_container">' +
    		  '<div style="padding-bottom:0.5em;">' +
  		        '<img src="extension/rdf-exporter-extension/images/add.png" alt=""/> <a id="add_prefix_link" href="#" bind="add_prefix_link">Add Prefix</a>' + 
  		      '</div>' + 
    		  '<table class="data-table" style="width:100%" bind="vocabularies_table">' +
    		    '<tr>' +
    		      '<td class="column-header">Prefix</td>' + 
    		      '<td class="column-header">Namespace</td>' + 
    		      '<td class="column-header">Imported</td>' +
    		      '<td class="column-header">Actions</td>' +
    		  '</table>'+
    		  '<div style="width:100%; padding-top:4px">' +
    		    '<div style="float:right;">' +
    		      '<a href="#" bind="set_as_default_button">Set as default</a> ' +
    		      '<a title="click for info" href="#" bind="set_as_default_info">' +
    		      	'<img alt="What\'s this?" src="extension/rdf-exporter-extension/images/questionMarkIcon.jpg" />' +
    		      '</a>' +
    		    '</div>' + 
    		  '</div>'+
    		'</div>'
    ).appendTo(body);
    
    var elmts = DOM.bind(html);
    
    elmts.set_as_default_button.bind('click', function(){
    	var dismissBusy = DialogSystem.showBusy('Updating default prefixes');
		$.post("/command/rdf-exporter-extension/set-default-prefixes",{"project":theProject.id},function(data){
			dismissBusy();
			if (data.code === "error"){
				alert('Error:' + data.message)
			}
		},"json");
    });
    
    elmts.set_as_default_info.bind('click',function(e){
    	e.preventDefault();
    	ManageVocabulariesDialog.showSetAsDefaultInfo();
    });
    
    elmts.add_prefix_link.bind('click',function(e){
    	e.preventDefault();
    	self._prefixesManager._addPrefix('','',function(){
    		self._loadVocabularies();
    		});
    });
    this._vocab_table=elmts.vocabularies_table[0];
    self._loadVocabularies();
    
    $('<button></button>').addClass('button').text("Done").click(function() {
    	DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    
    
    var level = DialogSystem.showDialog(frame);
};

ManageVocabulariesDialog.prototype._loadVocabularies = function(){
	var self = this;
    $.get("/command/rdf-exporter-extension/list-vocabularies",{project:theProject.id},function(o){
	  $("tr:not(:first)",self._vocab_table).remove();
	  for(var i=0;i<o.vocabularies.length;i++){
		var vocabulary = o.vocabularies[i];
		var tr = self._vocab_table.insertRow(self._vocab_table.rows.length);
		$(tr).addClass(i%2==0?'even':'odd');
		td = tr.insertCell(0);
		$(td).text(vocabulary.prefix);
		td = tr.insertCell(1);
		$(td).text(vocabulary.uri);
		td = tr.insertCell(2);
		var actionsTd = tr.insertCell(3);
		
		var deleteVocabFun = function(v){
			return function(){
				if (window.confirm("Are you sure you want to delete prefix \"" + v.prefix + "\"?")){ 
					var dismissBusy = DialogSystem.showBusy('Deleteing vocabulary: ' + v.prefix);
					$.post("/command/rdf-exporter-extension/delete-vocabulary",{"name":v.prefix,"project":theProject.id},function(data){
						dismissBusy();
						if (data.code === "error"){
							alert('Error:' + data.message)
						}else{
							self._loadVocabularies();
							self._prefixesManager._loadPrefixes();
						}
					},"json");
				}
			};
		}(vocabulary);
		
		var refreshVocabFun = function(v){
			return function(){
				var dismissBusy = DialogSystem.showBusy((v.imported? 'Refreshing vocabulary: ':'Importing vocabulary: ') + v.prefix);
				$.post("/command/rdf-exporter-extension/refresh-vocabulary",{"name":v.prefix,"project":theProject.id,"uri":v.uri},function(data){
					dismissBusy();
					if (data.code === "error"){
						alert('Error:' + data.message)
					}else{
						self._loadVocabularies();
					}
				},"json");
			};
		}(vocabulary);
		ManageVocabulariesDialog._addImageLink('delete.png','Delete',deleteVocabFun,actionsTd);	
		
		if(vocabulary.imported){
			$(td).text('Yes');
			ManageVocabulariesDialog._addImageLink('refresh.jpg','Refresh',refreshVocabFun,actionsTd);
			var showMoreInfo = function(v){
				return function(){
					var frame = DialogSystem.createDialog();
				    
				    frame.width("300px");
				    
				    var header = $('<div></div>').addClass("dialog-header").text(v.prefix).appendTo(frame);
				    var body = $('<div class="grid-layout layout-full"></div>').addClass("dialog-body").appendTo(frame);
				    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
				    
				    var html = $(
				    		'<table>' +
				    			'<tr><td>Number of classes:</td><td>' + v.numOfClasses + '</td></tr>' + 
				    			'<tr><td>Number of properties:</td><td>' + v.numOfProperties + '</td></tr>' +
				    			'<tr><td>Import date:</td><td>' + v.importDate + '</td></tr>' +
				    			'<tr><td>Vocabulary extraction form:</td><td>' + v.extractors + '</td></tr>' +
				    		'</table>' 
				    ).appendTo(body);
				    
				    $('<button></button>').addClass('button').text("Ok").click(function() {
				        DialogSystem.dismissUntil(level - 1);
				    }).appendTo(footer);
				    var level = DialogSystem.showDialog(frame);
				};
			}(vocabulary);
			ManageVocabulariesDialog._addImageLink('information.png','More information',showMoreInfo,actionsTd);
		}else{
			$(td).text('No');
			ManageVocabulariesDialog._addImageLink('import.gif','Import',refreshVocabFun,actionsTd);
		}
	  }
    },"json");
};