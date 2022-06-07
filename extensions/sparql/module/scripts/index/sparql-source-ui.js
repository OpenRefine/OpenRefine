Refine.SPARQLSourceUI = function(controller) {
  this._controller = controller;
};

Refine.SPARQLSourceUI.prototype.attachUI = function(body) {
  this._body = body;
  
  this._body.html(DOM.loadHTML("sparql", "scripts/index/import-from-sparql-form.html"));
  this._elmts = DOM.bind(this._body);
  
  $('#or-import-sparql').text($.i18n('core-index-import/sparql-label'));
  this._elmts.queryButton.html($.i18n('core-buttons/query'));
};

Refine.SPARQLSourceUI.prototype.focus = function() {
}
