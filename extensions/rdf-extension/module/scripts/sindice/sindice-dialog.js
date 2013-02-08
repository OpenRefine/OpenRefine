function SindiceDialog(){
	
}

SindiceDialog.prototype.show = function(column){
	var self = this;
	self._column = column;
    var frame = DialogSystem.createDialog();
    frame.width("400px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Related RDF datasets").appendTo(frame);
    var body = $('<div class="grid-layout layout-full"></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    var html = $(
    		'<div class="" ><span>List of domains:</span><div class="rdf-extension-sindice-domain-container" bind="domains_container"></div></div>'    		
    ).appendTo(body);
    
    self._elmts = DOM.bind(html);
    
    self._level = DialogSystem.showDialog(frame);
    self.guessDomain();
    self._footer(footer);
};

SindiceDialog.prototype.guessDomain = function(column){
	var self = this;
	var dismissBusy = DialogSystem.showBusy('Finding related RDF datasets...(This could take up to 5 minutes)');
	$.post("command/rdf-extension/sindiceGuessType",{"project":theProject.id,"columnName":self._column.name},function(data){
		dismissBusy();
		if(data.code==='error'){
			alert(data.message);
		}else{
			
			if(data.domains.length==0){
				self._elmts.domains_container.text('No domains were found!');
			}else{
				for(var i=0;i<data.domains.length;i++){
					var domain = data.domains[i];
					var option = $('<input />').attr('type','radio').attr('value',domain).attr('name','domain_radio').attr('checked',i==0);
					self._elmts.domains_container.append($('<div>').append(option).append($('<span/>').text(domain)));
				}
			}
		}
	});
};

SindiceDialog.prototype._footer = function(footer){
	var self = this;
	$('<button></button>').addClass('button').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
	
	$('<button></button>').addClass('button').text("Add domain-specific Sindice service").click(function() {
		var domain = self._elmts.domains_container.find('input[name="domain_radio"]:checked').val();
		if(!domain){
			alert("a domain needs to be selected");
			return;
		}
		$.post("command/rdf-extension/addSindiceService",{"domain":domain},function(data){
			RdfReconciliationManager.registerService(data,self._level);
		},"json");
    }).appendTo(footer);
};
