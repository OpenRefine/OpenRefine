var FreebaseExtension = { handlers: {} };

FreebaseExtension.handlers.editSchemaAlignment = function(reset) {
    new SchemaAlignmentDialog(
        reset ? null : theProject.overlayModels.freebaseProtograph, function(newProtograph) {});
};

FreebaseExtension.handlers.loadIntoFreebase = function() {
    new FreebaseLoadingDialog();
};

FreebaseExtension.handlers.browseToDataLoad = function() {
    // The form has to be created as part of the click handler. If you create it
    // inside the getJSON success handler, it won't work.
    
    var form = document.createElement("form");
    $(form)
        .css("display", "none")
        .attr("method", "GET")
        .attr("target", "dataload");

    document.body.appendChild(form);
    var w = window.open("about:blank", "dataload");
    
    $.getJSON(
        "/command/core/get-preference?" + $.param({ project: theProject.id, name: "freebase.load.jobID" }),
        null,
        function(data) {
            if (data.value == null) {
                alert("You have not tried to load the data in this project into Freebase yet.");
            } else {
                $(form).attr("action", "http://refinery.freebaseapps.com/load/" + data.value);
                form.submit();
                w.focus();
            }
            document.body.removeChild(form);
        }
    );
};

FreebaseExtension.handlers.importQAData = function() {
    Refine.postProcess(
        "freebase-extension",
        "import-qa-data",
        {},
        {},
        { cellsChanged: true }
    );
};

ExtensionBar.addExtensionMenu({
    "id" : "freebase",
    "label" : "Freebase",
    "submenu" : [
        {
            "id" : "freebase/schema-alignment",
            label: "Edit Schema Aligment Skeleton ...",
            click: function() { FreebaseExtension.handlers.editSchemaAlignment(false); }
        },
        {
            "id" : "freebase/reset-schema-alignment",
            label: "Reset Schema Alignment Skeleton ...",
            click: function() { FreebaseExtension.handlers.editSchemaAlignment(true); }
        },
        {},
        {
            "id" : "freebase/load-info-freebase",
            label: "Load into Freebase ...",
            click: function() { FreebaseExtension.handlers.loadIntoFreebase(); }
        },
        {
            "id" : "freebase/browse-load",
            label: "Browse to Data Load ...",
            click: function() { FreebaseExtension.handlers.browseToDataLoad(); }
        },
        {
            "id" : "freebase/import-qa-data",
            label: "Import QA Data",
            click: function() { FreebaseExtension.handlers.importQAData(); }
        }
    ]
});

DataTableColumnHeaderUI.extendMenu(function(column, columnHeaderUI, menu) {
    var columnIndex = Refine.columnNameToColumnIndex(column.name);
    var doAddColumnFromFreebase = function() {
        var o = DataTableView.sampleVisibleRows(column);
        new ExtendDataPreviewDialog(
            column, 
            columnIndex, 
            o.rowIndices, 
            function(extension) {
                Refine.postProcess(
                    "freebase-extension",
                    "extend-data", 
                    {
                        baseColumnName: column.name,
                        columnInsertIndex: columnIndex + 1
                    },
                    {
                        extension: JSON.stringify(extension)
                    },
                    { rowsChanged: true, modelsChanged: true }
                );
            }
        );
    };

    MenuSystem.insertAfter(
        menu,
        [ "core/edit-column", "core/add-column-by-fetching-urls" ],
        {
            id: "freebase/add-columns-from-freebase",
            label: "Add Columns From Freebase ...",
            click: doAddColumnFromFreebase
        }
    );
});
