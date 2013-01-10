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

//STATIC: reading JSON from files

function ZemantaExtendDataPreviewDialog(column, columnIndex, cellReconId, rowIndices, onDone) {
  this._column = column;
  this._columnIndex = columnIndex;
  this._cellReconId = cellReconId;
  this._rowIndices = rowIndices;
  this._onDone = onDone;
  this._extension = { properties: [] };

  var self = this;
  this._dialog = $(DOM.loadHTML("zemanta", "scripts/dialogs/extend-data-preview-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.text("Add Columns from DBpedia based on Column (static) " + column.name);
  this._elmts.resetButton.click(function() {
    self._extension.properties = [];
    self._update();
  });

  this._elmts.okButton.click(function() {
    if (self._extension.properties.length === 0) {
      alert("Please add some (DBpedia) properties first.");
    } else {
      DialogSystem.dismissUntil(self._level - 1);
      self._onDone(self._extension);
    }
  });
  this._elmts.cancelButton.click(function() {
    DialogSystem.dismissUntil(self._level - 1);
  });

  var dismissBusy = DialogSystem.showBusy();
  var type = cellReconId;
      
  console.log("Recon type: " + type);
  

  ZemantaExtendDataPreviewDialog.getAllProperties(type, function(properties) {
    dismissBusy();
    self._show(properties);
  });
}

ZemantaExtendDataPreviewDialog.getAllProperties = function(type, onDone) {
	console.log("Static getAllProperties...");
  var done = true;
  var data = { "head": { "link": [], "vars": ["property", "label"] },
		  "results": { "distinct": false, "ordered": true, "bindings": [
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/ontology/runtime" }	, "label": { "type": "literal", "xml:lang": "en", "value": "runtime (s)" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/ontology/abstract" }	, "label": { "type": "literal", "xml:lang": "en", "value": "has abstract" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/ontology/starring" }	, "label": { "type": "literal", "xml:lang": "en", "value": "starring" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/ontology/wikiPageExternalLink" }	, "label": { "type": "literal", "xml:lang": "en", "value": "Link from a Wikipage to an external page" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/ontology/musicComposer" }	, "label": { "type": "literal", "xml:lang": "en", "value": "music composer" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/ontology/thumbnail" }	, "label": { "type": "literal", "xml:lang": "en", "value": "thumbnail" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/ontology/Work/runtime" }	, "label": { "type": "literal", "xml:lang": "en", "value": "runtime (m)" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/ontology/gross" }	, "label": { "type": "literal", "xml:lang": "en", "value": "gross ($)" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/ontology/distributor" }	, "label": { "type": "literal", "xml:lang": "en", "value": "distributor" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/casting" }	, "label": { "type": "literal", "xml:lang": "en", "value": "casting" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/costumes" }	, "label": { "type": "literal", "xml:lang": "en", "value": "costumes" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/stunts" }	, "label": { "type": "literal", "xml:lang": "en", "value": "stunts" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/producer" }	, "label": { "type": "literal", "xml:lang": "en", "value": "producer" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/starring" }	, "label": { "type": "literal", "xml:lang": "en", "value": "starring" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/writer" }	, "label": { "type": "literal", "xml:lang": "en", "value": "writer" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/director" }	, "label": { "type": "literal", "xml:lang": "en", "value": "director" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/studio" }	, "label": { "type": "literal", "xml:lang": "en", "value": "studio" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/language" }	, "label": { "type": "literal", "xml:lang": "en", "value": "language" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/cinematography" }	, "label": { "type": "literal", "xml:lang": "en", "value": "cinematography" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/gross" }	, "label": { "type": "literal", "xml:lang": "en", "value": "gross" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/music" }	, "label": { "type": "literal", "xml:lang": "en", "value": "music" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/runtime" }	, "label": { "type": "literal", "xml:lang": "en", "value": "runtime" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/distributor" }	, "label": { "type": "literal", "xml:lang": "en", "value": "distributor" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/editing" }	, "label": { "type": "literal", "xml:lang": "en", "value": "editing" }},
		                                                                { "property": { "type": "uri", "value": "http://dbpedia.org/property/caption" }	, "label": { "type": "literal", "xml:lang": "en", "value": "Caption" }} ] } } ;

	  
	  
	  var allProperties = [];
	  var numOfProperties = data.results.bindings.length;
	  for (var i = 0; i < numOfProperties; i++) {
	    var property = data.results.bindings[i];
	    var property2 = {
	      id: property.property.value,
	      name: property.label.value
	    };
	    allProperties.push(property2);
	  }
	  allProperties.sort(function(a, b) {
	    return a.name.localeCompare(b.name);
	  });
	  
	  onDone(allProperties);
      
    
  window.setTimeout(function() {
    if (done) return;

    done = true;
    console.log("DBpedia timed out...");
    alert("DBpedia (static) request timed out... please try agan later.");
    onDone([]);
  }, 7000); // time to give up?
};

ZemantaExtendDataPreviewDialog.prototype._show = function(properties) {
  this._level = DialogSystem.showDialog(this._dialog);

  console.log("_show ...");

  var n = this._elmts.suggestedPropertyContainer.outerHeight(true);

  this._elmts.previewContainer.height(Math.floor(n));

  var self = this;
  var container = this._elmts.suggestedPropertyContainer;
  var renderSuggestedProperty = function(property) {
    var label = ("properties" in property) ? (property.name + " &raquo; " + property.properties[0].name) : property.name;
    var div = $('<div>').addClass("suggested-property").appendTo(container);

    $('<a>')
    .attr("href", "javascript:{}")
    .html(label)
    .appendTo(div)
    .click(function() {
      console.log("Add property: " + property.id);
      self._addProperty(property);
    });
  };
  for (var i = 0; i < properties.length; i++) {
    renderSuggestedProperty(properties[i]);
  }

};

ZemantaExtendDataPreviewDialog.prototype._update = function() {
  this._elmts.previewContainer.empty().text("Querying DBpedia ...");

  var self = this;
  var params = {
      project: theProject.id,
      columnName: this._column.name
  };
  
  console.log("Updating...");

  $.post(
    "/command/zemanta/preview-extend-data?" + $.param(params), 
    {
      rowIndices: JSON.stringify(this._rowIndices),
      extension: JSON.stringify(this._extension)
    },
    function(data) {
      console.log("... entering _renderPreview..");
      self._renderPreview(data);
    },
    "json"
  );
};

ZemantaExtendDataPreviewDialog.prototype._addProperty = function(p) {

 console.log(".. adding property to preview");

  var addSeveralToList = function(properties, oldProperties) {
    for (var i = 0; i < properties.length; i++) {
      addToList(properties[i], oldProperties);
    }
  };
  var addToList = function(property, oldProperties) {
    for (var i = 0; i < oldProperties.length; i++) {
      var oldProperty = oldProperties[i];
      if (oldProperty.id == property.id) {
        if ("included" in property) {
          oldProperty.included = "included" in oldProperty ? 
              (oldProperty.included || property.included) : 
                property.included;
        }

        if ("properties" in property) {
          if ("properties" in oldProperty) {
            addSeveralToList(property.properties, oldProperty.properties);
          } else {
            oldProperty.properties = property.properties;
          }
        }
        return;
      }
    }

    oldProperties.push(property);
  };

  addToList(p, this._extension.properties);

  this._update();
};

ZemantaExtendDataPreviewDialog.prototype._renderPreview = function(data) {

  console.log("...rendering preview...");
  
  var self = this;
  var container = this._elmts.previewContainer.empty();
  if (data.code == "error") {
    container.text("Error.");
    return;
  }

  var table = $('<table>')[0];
  console.log("table.rows.length: " + table.rows.length);
  var trHead = table.insertRow(table.rows.length);
  $('<th>').appendTo(trHead).text(this._column.name);

  var renderColumnHeader = function(column) {
    var th = $('<th>').appendTo(trHead);

    $('<span>').html(column.names.join(" &raquo; ")).appendTo(th);
    $('<br>').appendTo(th);

    $('<a href="javascript:{}"></a>')
    .text("remove")
    .addClass("action")
    .attr("title", "Remove this column")
    .click(function() {
      self._removeProperty(column.path);
    }).appendTo(th);
  };
  for (var c = 0; c < data.columns.length; c++) {
    renderColumnHeader(data.columns[c]);
  }

  console.log("... adding values to table in render preview ...");
  for (var r = 0; r < data.rows.length; r++) {
    var tr = table.insertRow(table.rows.length);
    var row = data.rows[r];

	console.log("row.length: " + row.length);

    for (var c = 0; c < row.length; c++) {
      var td = tr.insertCell(tr.cells.length);
      var cell = row[c];
      if (cell !== null) {
          console.log("Cell not null: " + cell.id);
        if ($.isPlainObject(cell)) {
          $('<a>').attr("href", cell.id).text(cell.name).appendTo(td);
        } else {
          $('<span>').text(cell).appendTo(td);
        }
      }
    }
  }

  container.append(table);
};

ZemantaExtendDataPreviewDialog.prototype._removeProperty = function(path) {
  var removeFromList = function(path, index, properties) {
    var id = path[index];

    for (var i = properties.length - 1; i >= 0; i--) {
      var property = properties[i];
      if (property.id == id) {
        if (index === path.length - 1) {
          if ("included" in property) {
            delete property.included;
          }
        } else if ("properties" in property && property.properties.length > 0) {
          removeFromList(path, index + 1, property.properties);
        }

        if (!("properties" in property) || property.properties.length === 0) {
          properties.splice(i, 1);
        }

        return;
      }
    }
  };

  removeFromList(path, 0, this._extension.properties);

  this._update();
};

ZemantaExtendDataPreviewDialog.prototype._findProperty = function(path) {
  var find = function(path, index, properties) {
    var id = path[index];

    for (var i = properties.length - 1; i >= 0; i--) {
      var property = properties[i];
      if (property.id == id) {
        if (index === path.length - 1) {
          return property;
        } else if ("properties" in property && property.properties.length > 0) {
          return find(path, index + 1, property.properties);
        }
        break;
      }
    }

    return null;
  };

  return find(path, 0, this._extension.properties);
};


