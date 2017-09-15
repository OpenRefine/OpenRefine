ExporterManager.MenuItems.push({});
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
                        id: "wikidata/edit-schema",
                        label: "Edit Wikibase schema...",
                        click: function() { SchemaAlignmentDialog.launch(false); }
                    },
                    {
                        id:"wikidata/manage-account",
                        label: "Manage account",
                        click: function() { ManageAccountDialog.checkAndLaunch(); }
                    },
                    {
                        id:"wikidata/perform-edits",
                        label: "Push to Wikidata...",
                        click: function() { PerformEditsDialog.checkAndLaunch(); }
                    },
                    {               
                        id:"wikidata/export-qs",
                        label: "Export to QuickStatements",
                        click: function() { WikibaseExporterMenuBar.exportTo("quickstatements"); }
                    },

                ]
        }
    );
});

