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

Refine.SPARQLImportingController.prototype.startImportingData = function(queryInfo) {
  
  var self = this;
  Refine.postCSRF(
    "command/core/create-importing-job",
    null,
    function(data) {
      Refine.wrapCSRF(function(token) {
        $.post(
            "command/core/importing-controller?" + $.param({
            "controller": "sparql/sparql-importing-controller",
            "subCommand": "initialize-parser-ui",
            "dataUrl": queryInfo,
            "csrf_token": token
            }),
            null,
            function(data2) {
            
            if (data2.status == 'ok') {
                console.log(data2);
            } else {
                alert(data2.message);
            }
            },
            "json"
        );
      });
    },
    "json"
  );
};
