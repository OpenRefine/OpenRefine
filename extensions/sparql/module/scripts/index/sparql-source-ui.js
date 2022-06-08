Refine.SPARQLSourceUI = function(controller) {
  this._controller = controller;
};

Refine.SPARQLSourceUI.prototype.attachUI = function(body) {
  this._body = body;
  
  this._body.html(DOM.loadHTML("sparql", "scripts/index/import-from-sparql-form.html"));
  this._elmts = DOM.bind(this._body);
  
  $('#or-import-sparql').text($.i18n('sparql-import/importer-label'));
  this._elmts.queryButton.html($.i18n('sparql-buttons/query'));
  
   //To Do: Get complete url info
  this._elmts.queryButton.on('click',function(evt){
	var queryInfo = {};
	queryInfo.query = jQueryTrim($( "#sparql-import-textarea" ).val());
	if(!query.length === 0){
		self._controller.startImportingData(queryInfo);
	}
});
  
};

Refine.SPARQLSourceUI.prototype.focus = function() {
};
