//Internationalization init
var lang = navigator.language.split("-")[0]
    || navigator.userLanguage.split("-")[0];
var dictionary = "";
$.ajax({
  url : "command/core/load-language?",
  type : "POST",
  async : false,
  data : {
    module : "database",
  },
  success : function(data) {
    dictionary = data['dictionary'];
    lang = data['lang'];
  }
});
$.i18n().load(dictionary, lang);
// End internationalization

Refine.SPARQLImportController = function(createProjectUI) {
  this._createProjectUI = createProjectUI;

  this._parsingPanel = createProjectUI.addCustomPanel();

  createProjectUI.addSourceSelectionUI({
    label: $.i18n('sparql-import/importer-name'),
    id: "sparql",
    ui: new Refine.SPARQLSourceUI(this)
  });

};
Refine.CreateProjectUI.controllers.push(Refine.SPARQLImportController);


