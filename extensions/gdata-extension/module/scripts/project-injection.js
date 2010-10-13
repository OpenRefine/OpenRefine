// Client side resources to be injected for gData extension

// Add items to the exporter menu
MenuBar.insertBefore(
        [ "core/project", "core/export", "core/export-templating" ],
        [
            {
                "label":"Google Spreadsheet",
                "click": function() {}
            },
            {
                "label":"Google Data",
                "click": function() {}
            },
            {} // separator
        ]
);

var gdataExtension = {};
