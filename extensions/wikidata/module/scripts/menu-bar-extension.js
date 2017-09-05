/*
ExporterManager.MenuItems.push({});//add separator
ExporterManager.MenuItems.push(
		{
			"id" : "exportRdfXml",
          	"label":"RDF as RDF/XML",
          	"click": function() { RdfExporterMenuBar.exportRDF("rdf", "rdf");}
		}
);
ExporterManager.MenuItems.push(
		{
			"id" : "exportRdfTurtle",
        	"label":"RDF as Turtle",
        	"click": function() { RdfExporterMenuBar.exportRDF("Turtle", "ttl"); }
		}
);

RdfExporterMenuBar = {};

RdfExporterMenuBar.exportRDF = function(format, ext) {
    if (!theProject.overlayModels.rdfSchema) {
        alert(
            "You haven't done any RDF schema alignment yet!"
        );
    } else {
        RdfExporterMenuBar.rdfExportRows(format, ext);
    }
};

RdfExporterMenuBar.rdfExportRows = function(format, ext) {
    var name = $.trim(theProject.metadata.name.replace(/\W/g, ' ')).replace(/\s+/g, '-');
    var form = document.createElement("form");
    $(form)
        .css("display", "none")
        .attr("method", "post")
        .attr("action", "command/core/export-rows/" + name + "." + ext)
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

RdfExporterMenuBar.editRdfSchema = function(reset) {
    new RdfSchemaAlignmentDialog(reset ? null : theProject.overlayModels.rdfSchema);
};
*/

ExporterManager.MenuItems.push(
		{
			"id" : "exportQuickStatements",
        	"label":"QuickStatements",
        	"click": function() { WikibaseExporterMenuBar.exportTo("quickstatements"); }
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

//extend the column header menu
$(function(){
    
	ExtensionBar.MenuItems.push(
			{
				"id":"reconcile",
				"label": "Wikidata",
				"submenu" : [
					{
						"id": "wikidata/edit-schema",
						label: "Edit Wikibase schema...",
						click: function() { SchemaAlignmentDialog.launch(false); }
					},
			    ]
		        }
        );
});

