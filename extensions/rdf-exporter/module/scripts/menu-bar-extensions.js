MenuBar.insertBefore(
    [ "core/project", "core/export", "core/export-templating" ],
    [
        {
            "label":"RDF as RDF/XML",
            "click": function() { RdfExporterMenuBar.exportRDF("rdf", "rdf"); }
        },
        {
            "label":"RDF as Turtle",
            "click": function() { RdfExporterMenuBar.exportRDF("n3", "ttl"); }
        },
        {}
    ]
);

MenuBar.insertBefore(
    [ "core/schemas", "core/load-info-freebase" ],
    [
        {
            label: "Edit RDF Schema...",
            click: function() { RdfExporterMenuBar.editRdfSchema(false); }
        },
        {
            label: "Reset RDF Schema...",
            click: function() { RdfExporterMenuBar.editRdfSchema(true); }
        },
        {}
    ]
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
        .attr("action", "/command/core/export-rows/" + name + "." + ext)
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

