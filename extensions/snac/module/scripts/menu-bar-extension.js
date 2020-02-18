// Load the localization file
//menu-bar-extension.js

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

SNACExporterMenuBar = {};

SNACExporterMenuBar.exportTo = function(format) {
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

SNACExporterMenuBar.checkSchemaAndExport = function(format) {
  var onSaved = function(callback) {
     SNACExporterMenuBar.exportTo(format);
  };
  if (!SNACSchemaAlignmentDialog.isSetUp()) {
     SNACSchemaAlignmentDialog.launch(null);
  } else if (SNACSchemaAlignmentDialog._hasUnsavedChanges) {
     SNACSchemaAlignmentDialog._save(onSaved);
  } else {
     onSaved();
  }
}

SNACExporterMenuBar.stringToJSONDownload = function(){
	$.get(
			"command/snac/resource",
			function(data) {
				let dataStr = JSON.stringify(JSON.parse(data.resource), 0 , 4);
		    let dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr);

				var date = new Date();
		    let exportFileDefaultName = date.toISOString().substr(0,10) + '-SNAC-resources.json';

		    let linkElement = document.createElement('a');
		    linkElement.setAttribute('href', dataUri);
		    linkElement.setAttribute('download', exportFileDefaultName);
		    linkElement.click();
			});
}

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
                    {
                        id:"snac/perform-edits",
                        label: $.i18n('snac-extension/perform-edits-on-snac'),
                        click: function() { /*PerformEditsDialog.checkAndLaunch();*/
                            if(validationCount == 0){
                                ManageUploadDialog.launch(null, function(success) {});
                            }
                            else{
                                window.alert("Error: unable to upload edits to SNAC. Please fix the " + validationCount + " issue(s) first.");
                            }
                            
                        }
                    },
                    {
                        id:"snac/export-schema",
                        label: $.i18n('snac-extension/export-to-json'),
                        click: function() { 
                            if(validationCount == 0){
                                SNACExporterMenuBar.stringToJSONDownload();;
                            }
                            else{
                                window.alert("Error: unable to upload edits to SNAC. Please fix the " + validationCount + " issue(s) first.");
                            }
                        }
                    },

                ]
        }
    );
});
