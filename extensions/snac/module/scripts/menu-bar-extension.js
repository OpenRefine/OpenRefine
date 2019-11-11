// Load the localization file
var dictionary = {};
$.ajax({
	url : "command/core/load-language?",
	type : "POST",
	async : false,
	data : {
	  module : "snac",
//		lang : lang
	},
	success : function(data) {
		dictionary = data['dictionary'];
		lang = data['lang'];
	}
});
$.i18n().load(dictionary, lang);



ExporterManager.MenuItems.push({});
ExporterManager.MenuItems.push(
        {
            id:"performSNACEdits",
            label: $.i18n('snac-extension/perform-edits-on-snac'),
            click: function() { /*PerformEditsDialog.checkAndLaunch();*/ }
        });
ExporterManager.MenuItems.push(
        {
            id:"exportSNACJson",
            label: $.i18n('snac-extension/export-to-json'),
            click: function() { /*WikibaseExporterMenuBar.checkSchemaAndExport("quickstatements");*/ }
        });

/*
WikibaseExporterMenuBar = {};

WikibaseExporterMenuBar.exportTo = function(format) {
    var targetUrl = null;
    if (format ==="quickstatements") {
        targetUrl = "statements.txt";
    } else {
        targetUrl = "schema.json";
    }
    var form = document.createElement("form");
    $(form).css("display", "none")
        .attr("method", "post")
        .attr("action", "command/core/export-rows/"+targetUrl)
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

WikibaseExporterMenuBar.checkSchemaAndExport = function(format) {
  var onSaved = function(callback) {
     WikibaseExporterMenuBar.exportTo(format);
  };
  if (!SchemaAlignmentDialog.isSetUp()) {
     SchemaAlignmentDialog.launch(null);
  } else if (SchemaAlignmentDialog._hasUnsavedChanges) {
     SchemaAlignmentDialog._save(onSaved);
  } else {
     onSaved();
  }
}
*/

//extend the column header menu
$(function(){
    ExtensionBar.MenuItems.push(
        {
            "id":"reconcilesnac",
                "label": $.i18n('snac-extension/menu-label'),
                "submenu" : [
					{
                        id: "snac/edit-schema",
                        label: $.i18n('snac-extension/edit-snac-schema'),
                        click: function() { SNACSchemaAlignmentDialog.launch(false); }
                    },
                    {
                        id:"snac/api-key",
                        label: $.i18n('snac-extension/manage-api-key'),
                        click: function() { 
                            ManageKeysDialog.launch(null, function(success) {}); 
                        }
                    },
                    {},
                    {
                        id:"snac/perform-edits",
                        label: $.i18n('snac-extension/perform-edits-on-snac'),
                        click: function() { /*PerformEditsDialog.checkAndLaunch();*/ }
                    },
                    {
                        id:"snac/export-qs",
                        label: $.i18n('snac-extension/export-to-json'),
                        click: function() { /*WikibaseExporterMenuBar.checkSchemaAndExport("quickstatements");*/ }
                    },

                ]
        }
    );
});
