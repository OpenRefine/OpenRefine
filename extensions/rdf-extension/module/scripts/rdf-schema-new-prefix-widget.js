function NewPrefixWidget(manager){
	this._prefixesManager = manager;
}

NewPrefixWidget.prototype.show = function(msg,def_prefix, onDone){
	var self = this;
    
    var dialog = $(DOM.loadHTML("rdf-extension","scripts/dialogs/new-prefix-widget.html"));
    self._elmts = DOM.bind(dialog);
    self._level = DialogSystem.showDialog(dialog);
    
    
    if(msg){
    	self._elmts.message.addClass('message').html(msg);
    }
    
    if(def_prefix){
    	self._elmts.prefix.val(def_prefix);
    	self.suggestUri(def_prefix);
    }

   self._elmts.file_upload_form.submit(function(e){
	   e.preventDefault();
	   
	   	var fetchOption = self._elmts.fetching_options_table.find('input[name="vocab_fetch_method"]:checked').val();		
	   	
    	var name = self._elmts.prefix.val();
    	var uri = self._elmts.uri.val();
    	if(self._prefixesManager._hasPrefix(name)){
    		alert('Prefix "' + name + '" is already defined');
    		return;
    	}
    	
    	var force_import = (self._elmts.forceImport.attr('checked') !== undefined);
    	
    	var dismissBusy;

    	if(fetchOption === 'file'){
    		//prepare values
    		$('#vocab-hidden-prefix').val(name);
    		$('#vocab-hidden-uri').val(uri);
    		$('#vocab-hidden-project').val(theProject.id);
    			
    		dismissBusy = DialogSystem.showBusy('Uploading vocabulary ');

    		$(this).ajaxSubmit({
    				
    				url: "command/rdf-extension/upload-file-add-prefix",
    				type: "POST",
    				dataType: "json",
    				success:function(data) {
    					dismissBusy();
    					if (data.code === 'error')
    					{
    						alert("Error: " + data.message);
    						
    					} else {
	    			    	if(onDone){
	    						onDone(name,uri);
	    						self._dismiss();
	    			    	}	
    					}
    			    }
    		});
    		return false;
    		
    		
    	} 
    	
		dismissBusy = DialogSystem.showBusy('Trying to import vocabulary from ' + uri);
    	
		$.post("command/rdf-extension/add-prefix",
    			{
    				name:name,
    				uri:uri,
    				"fetch-url":uri,
    				project: theProject.id,
    				"force-import": force_import,
    				fetch:fetchOption
				},
				function(data)
				{
					dismissBusy();
		    		if (data.code === "error"){
		    			alert('Error:' + data.message);
						//todo: add force input option
						console.log("Force import...");
						console.log(force_import);

		    		}else{
		    			if(onDone){
		    				onDone(name,uri);
		    				self._dismiss();
		    			}
		    		}

				}
			);
	    	
    });
    
    
    self._elmts.okButton.click(function() {
    	self._elmts.file_upload_form.submit();
    	   	
    });
    
    self._elmts.cancelButton.click(function() {
        self._dismiss();
    });
    
    self._elmts.advancedButton.click(function() {
        self._elmts.fetching_options_table.show();
        $('#advanced_options_button').hide();
        $('#advanced_options_button').attr("disabled", "true");
    });
    
    self._elmts.fetching_options_table
	.hide()
	.find('input[name="vocab_fetch_method"]')
	.click(
			function(){
				var upload = $(this).val()!=='file';
				self._elmts.fetching_options_table.find('.upload_file_inputs').attr('disabled',upload);
			}
		);
    
    self._elmts.prefix.bind('change',function(){
    	self.suggestUri($(this).val());
    	}).focus();
    
    self._elmts.prefix.change(function(){
    	self.suggestUri($(this).val());
    	}).focus();
};

NewPrefixWidget.prototype.suggestUri = function(prefix){
	var self = this;
	$.get(
			'command/rdf-extension/get-prefix-cc-uri',
			{prefix:prefix},
			function(data){
				if(!self._elmts.uri.val() && data.uri){
					self._elmts.uri.val(data.uri);
					if(self._elmts.message.text()){
						self._elmts.uri_note.html('(a suggestion from <em><a target="_blank" href="http://prefix.cc">prefix.cc</a></em> is provided)');
					}else{
						self._elmts.uri_note.html('(suggested by <a target="_blank" href="http://prefix.cc">prefix.cc</a>)');
					}
				}
			},
			"json"
		);
};

NewPrefixWidget.prototype._dismiss =  function() {
	var self = this;
	DialogSystem.dismissUntil(self._level - 1);
};
