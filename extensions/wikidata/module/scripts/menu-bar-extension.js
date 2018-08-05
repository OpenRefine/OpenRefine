// Load the localization file
var dictionary = {};
$.ajax({
	url : "command/core/load-language?",
	type : "POST",
	async : false,
	data : {
	  module : "wikidata",
//		lang : lang
	},
	success : function(data) {
		dictionary = data;
	}
});
$.i18n.setDictionary(dictionary);



ExporterManager.MenuItems.push({});
ExporterManager.MenuItems.push(
        {
            "id" : "exportQuickStatements",
            "label": $.i18n._('wikidata-extension')["quickstatements-export-name"],
            "click": function() { WikibaseSchemaExporterMenuBar.checkSchemaAndExport(); }
        }
);

WikibaseExporterMenuBar = {};

WikibaseExporterMenuBar.exportTo = function(format) {
    var form = document.createElement("form");
    $(form).css("display", "none")
        .attr("method", "post")
        .attr("action", "command/core/export-rows/statements.txt")
        .attr("target", "gridworks-export");
    $('<input />')
        .attr("name", "engine")
        .attr("value", JSON.stringify(ui.browsingEngine.getJSON()))
        .appendTo(form);
    $('<input />')
        .attr("name", "project")
        .attr("value", theProject.id)
        .appendTo(form);
    $('<input />')
        .attr("name", "format")
        .attr("value", format)
        .appendTo(form);

    document.body.appendChild(form);

    window.open("about:blank", "gridworks-export");
    form.submit();

    document.body.removeChild(form);
};

WikibaseExporterMenuBar.checkSchemaAndExport = function() {
  var onSaved = function(callback) {
     WikibaseExporterMenuBar.exportTo("quickstatements"); 
  };
  if (!SchemaAlignmentDialog.isSetUp()) {
     SchemaAlignmentDialog.launch(null);
  } else if (SchemaAlignmentDialog._hasUnsavedChanges) {
     SchemaAlignmentDialog._save(onSaved);
  } else {
     onSaved();
  }
}

//extend the column header menu
$(function(){
    
    ExtensionBar.MenuItems.push(
        {
            "id":"reconcile",
                "label": $.i18n._('wikidata-extension')["menu-label"],
                "submenu" : [
                    {
                        id: "wikidata/edit-schema",
                        label: $.i18n._('wikidata-extension')["edit-wikidata-schema"],
                        click: function() { SchemaAlignmentDialog.launch(false); }
                    },
                    {
                        id:"wikidata/manage-account",
                        label: $.i18n._('wikidata-extension')["manage-wikidata-account"],
                        click: function() { ManageAccountDialog.checkAndLaunch(); }
                    },
                    {
                        id:"wikidata/perform-edits",
                        label: $.i18n._('wikidata-extension')["perform-edits-on-wikidata"],
                        click: function() { PerformEditsDialog.checkAndLaunch(); }
                    },
                    {               
                        id:"wikidata/export-qs",
                        label: $.i18n._('wikidata-extension')["export-to-qs"],
                        click: function() { WikibaseExporterMenuBar.checkSchemaAndExport(); }
                    },

                ]
        }
    );
});

