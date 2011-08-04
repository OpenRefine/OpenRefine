/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

var FreebaseExtension = { handlers: {} };

FreebaseExtension.handlers.editSchemaAlignment = function() {
  new SchemaAlignmentDialog(theProject.overlayModels.freebaseProtograph, function(newProtograph) {});
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
      label: "Align to Freebase's schemas...",
      click: function() { FreebaseExtension.handlers.editSchemaAlignment(false); }
    },
    {
      "id" : "freebase/load-info-freebase",
      label: "Load into Freebase...",
      click: function() { FreebaseExtension.handlers.loadIntoFreebase(); }
    },
    {},
    {
      "id" : "freebase/browse-load",
      label: "Browse data load details...",
      click: function() { FreebaseExtension.handlers.browseToDataLoad(); }
    },
    {
      "id" : "freebase/import-qa-data",
      label: "Import QA data",
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
            "freebase",
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
      label: "Add columns from Freebase ...",
      click: doAddColumnFromFreebase
    }
  );
});
