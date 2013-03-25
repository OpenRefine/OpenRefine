function ManageVocabsWidget(manager){
	this._prefixesManager = manager;
}

ManageVocabsWidget.prototype.show = function(){
	var self = this;
	
    var dialog = $(DOM.loadHTML("rdf-extension", "scripts/dialogs/rdf-schema-manage-vocabs-widget.html"));
    self._level = DialogSystem.showDialog(dialog);
    self._elmts = DOM.bind(dialog);
    
    self._elmts.cancelButton.click(function() { self._dismiss(); });
    
	self._elmts.addPrefixBtn.click(function(e){
		e.preventDefault();
		self._prefixesManager._addPrefix(
				false,
				false,
				function(){
					self.renderBody();
					}
				);
	});

    
    self.renderBody();
	
	self._elmts.okButton.click(function() {
    	self._prefixesManager._showPrefixes();
		self._dismiss();
	});
	
};

ManageVocabsWidget.prototype.renderBody = function(){
	var self = this;
	
	var table = self._elmts.prefixesTable;
	table.empty();
    table.append($('<tr>').addClass('rdf-table-even')
    		.append($('<th/>').text('Prefix'))
    		.append($('<th/>').text('URI'))
    		.append($('<th/>').text('Delete'))
    		.append($('<th/>').text('Refresh'))
    		);
    
	var getDeleteHandler = function(name){
		return function(e){
			e.preventDefault();
			dismissBusy = DialogSystem.showBusy('Deleting prefix ' + name);
			$.post(
					'command/rdf-extension/remove-prefix',
					{
						'name':name,
						'project':theProject.id
					},
					function(data)
					{
						dismissBusy();
						if(data.code === 'error'){
							//TODO
							console.log("Error removing prefix");
						}else{
							self._prefixesManager._removePrefix(name);
							self.renderBody();
						}
					}
				);
    	}; 
    };
    
    var getRefreshHandler = function(name,uri){
    	return function(e){
    		e.preventDefault();
    		if (window.confirm("This will re-fetch the vocabulary from \"" + uri + 
    				"\" If you added it by uploading a file you need to delete it" + 
    				"and add it again to refresh the index. Continue?")) 
    		{
    			
    			dismissBusy = DialogSystem.showBusy('Refreshing prefix ' + name);
    			$.post('command/rdf-extension/refresh-prefix',
    					{
    						'name':name,
    						'uri':uri,
    						'project':theProject.id
    						
    					},
						function(data) {
							dismissBusy();
		    				if(data.code==='error'){
		    					alert('something went wrong: ' + data.messge);
		    				}
						});
    		}
    	}; 
    };
    
	
	
	for(var i = 0; i< self._prefixesManager._prefixes.length; i++){
		var name = self._prefixesManager._prefixes[i].name;
		var uri = self._prefixesManager._prefixes[i].uri;
		var delete_handle = $('<a/>').text('delete').attr('href','#').click(getDeleteHandler(name));
		var refresh_handle = $('<a/>').text('refresh').attr('href','#').click(getRefreshHandler(name,uri));
		var tr = $('<tr/>').addClass(i%2==1?'rdf-table-even':'rdf-table-odd')
		.append($('<td>').text(self._prefixesManager._prefixes[i].name))
		.append($('<td>').text(self._prefixesManager._prefixes[i].uri))
		.append($('<td>').html(delete_handle))
		.append($('<td>').html(refresh_handle));
		table.append(tr);
	}

};


ManageVocabsWidget.prototype._dismiss = function() {
	DialogSystem.dismissUntil(this._level - 1);
};
