function ExporterManager(button) {
    this._button = button;
    this._initializeUI();
}

ExporterManager.handlers = {};

ExporterManager.MenuItems = [
    {
        "id" : "core/export-project",
        "label": "Export Project",
        "click": function() { ExporterManager.handlers.exportProject(); }
    },
    {},
    {
        "id" : "core/export-tsv",
        "label": "Tab-Separated Value",
        "click": function() { ExporterManager.handlers.exportRows("tsv", "tsv"); }
    },
    {
        "id" : "core/export-csv",
        "label": "Comma-Separated Value",
        "click": function() { ExporterManager.handlers.exportRows("csv", "csv"); }
    },
    {
        "id" : "core/export-html-table",
        "label": "HTML Table",
        "click": function() { ExporterManager.handlers.exportRows("html", "html"); }
    },
    {
        "id" : "core/export-excel",
        "label": "Excel",
        "click": function() { ExporterManager.handlers.exportRows("xls", "xls"); }
    },
    {},
    {
        "id" : "core/export-tripleloader",
        "label": "Tripleloader",
        "click": function() { ExporterManager.handlers.exportTripleloader("tripleloader"); }
    },
    {
        "id" : "core/export-mqlwrite",
        "label": "MQLWrite",
        "click": function() { ExporterManager.handlers.exportTripleloader("mqlwrite"); }
    },
    {},
    {
        "id" : "core/export-templating",
        "label": "Templating...",
        "click": function() { new TemplatingExporterDialog(); }
    }
];

ExporterManager.prototype._initializeUI = function() {
    this._button.click(function(evt) {
        MenuSystem.createAndShowStandardMenu(
            ExporterManager.MenuItems,
            this,
            { horizontal: false }
        );
        
        evt.preventDefault();
        return false;
    });
};

ExporterManager.handlers.exportTripleloader = function(format) {
    if (!theProject.overlayModels.freebaseProtograph) {
        alert(
            "You haven't done any schema alignment yet,\nso there is no triple to export.\n\n" +
            "Use the Schemas > Edit Schema Alignment Skeleton...\ncommand to align your data with Freebase schemas first."
        );
    } else {
        ExporterManager.handlers.exportRows(format, "txt");
    }
};

ExporterManager.handlers.exportRows = function(format, ext) {
    var name = $.trim(theProject.metadata.name.replace(/\W/g, ' ')).replace(/\s+/g, '-');
    var form = document.createElement("form");
    $(form)
        .css("display", "none")
        .attr("method", "post")
        .attr("action", "/command/core/export-rows/" + name + "." + ext)
        .attr("target", "refine-export");

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

    window.open("about:blank", "refine-export");
    form.submit();
    
    document.body.removeChild(form);
};

ExporterManager.handlers.exportProject = function() {
    var name = $.trim(theProject.metadata.name.replace(/\W/g, ' ')).replace(/\s+/g, '-');
    var form = document.createElement("form");
    $(form)
        .css("display", "none")
        .attr("method", "post")
        .attr("action", "/command/core/export-project/" + name + ".google-refine.tar.gz")
        .attr("target", "refine-export");
    $('<input />')
        .attr("name", "project")
        .attr("value", theProject.id)
        .appendTo(form);

    document.body.appendChild(form);

    window.open("about:blank", "refine-export");
    form.submit();

    document.body.removeChild(form);
};

