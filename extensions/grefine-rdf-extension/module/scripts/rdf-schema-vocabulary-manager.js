function RdfPrefixesManager (dialog,prefixes){
	this._dialog = dialog;
	//prefixes have not been initialized
	var self = this;
	dialog._rdf_schema_prefixes.empty().html('<img src="images/small-spinner.gif" />');
	if(!prefixes){
		this._getDefaultPrefixes(function(data){
			self._prefixes = data.prefixes;
			self._showPrefixes(self._prefixes);
		});
	}else{
		self._prefixes = prefixes;
		this._savePrefixes();
		this._showPrefixes();
	}
	RdfPrefixesManager.prefixes = this._prefixes;
};

RdfPrefixesManager.prototype._getDefaultPrefixes = function(onDone){
	var self =this;
	$.get("command/rdf-extension/get-default-prefixes",{project:theProject.id},function(data){
		if(onDone){
			onDone(data);
		}
	},"json");
};

RdfPrefixesManager.prototype._savePrefixes = function(onDone){
	var self =this;
	$.post("command/rdf-extension/save-prefixes",{project:theProject.id,prefixes:JSON.stringify(self._prefixes)},function(data){
		if(onDone){
			onDone(data);
		}
	},"json");
};
RdfPrefixesManager.prototype._showManagePrefixesWidget = function(){
	var self = this;
	var vocabManager = new ManageVocabsWidget(self);
	vocabManager.show();
};

RdfPrefixesManager.prototype._showPrefixes = function(){
	var self = this;
	this._dialog._rdf_schema_prefixes.empty();
	for(var i=0;i<self._prefixes.length;i++){
		self._renderPrefix(self._prefixes[i].name,self._prefixes[i].uri);
	}
	//add button
	$('<a href="#" class="add-prefix-box">add prefix</a>').bind('click',function(e){
		e.preventDefault();
		self._addPrefix();
	}).appendTo(self._dialog._rdf_schema_prefixes);
	
	//configure button
	$('<a href="#" class="manage-vocabularies-box">manage prefixes</a>').bind('click',function(e){
		e.preventDefault();
		self._showManagePrefixesWidget();
	}).appendTo(self._dialog._rdf_schema_prefixes);
	
};

RdfPrefixesManager.prototype._renderPrefix = function(prefix,uri){
	this._dialog._rdf_schema_prefixes.append($('<span/>').addClass('rdf-schema-prefix-box').attr('title',uri).text(prefix));
};

RdfPrefixesManager.prototype._removePrefix = function(name){
	var self = this;
	for(var i=0;i<self._prefixes.length;i++){
		if(name===self._prefixes[i].name){
			self._prefixes.splice(i,1);
		}
	}
};

RdfPrefixesManager.prototype._addPrefix = function(msg,def_prefix,onDone){
	var self = this;
	var widget = new NewPrefixWidget(self);
	widget.show(msg,def_prefix,function(name,uri){
		self._prefixes.push({name:name,uri:uri});
		self._savePrefixes(function(){
			self._showPrefixes();
		});
		if(onDone){
			onDone();
		}
	});
};

RdfPrefixesManager.prototype._hasPrefix = function(name){
	for(var i=0; i<this._prefixes.length; i++){
		if(this._prefixes[i].name===name){
			return true;
		}
	}
	return false;
};

/*
* Some utility functions
*/
RdfPrefixesManager.isPrefixedQname = function(qname){
	return qname.match(/[_a-zA-Z][-_a-zA-Z0-9]*:($|([_a-zA-Z][-_a-zA-Z0-9]*))$/);
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

