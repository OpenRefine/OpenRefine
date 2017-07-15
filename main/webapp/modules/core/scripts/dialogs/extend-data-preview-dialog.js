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

function ExtendReconciledDataPreviewDialog(column, columnIndex, rowIndices, onDone) {
  this._column = column;
  this._columnIndex = columnIndex;
  this._rowIndices = rowIndices;
  this._onDone = onDone;
  this._extension = { properties: [] };

  var self = this;
  this._dialog = $(DOM.loadHTML("core", "scripts/views/data-table/extend-data-preview-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.html($.i18n._('core-views')["add-col-recon-col"]+" "+column.name);
  this._elmts.resetButton.click(function() {
    self._extension.properties = [];
    self._update();
  });

  this._elmts.okButton.click(function() {
    if (self._extension.properties.length === 0) {
      alert($.i18n._('core-views')["warning-no-property"]);
    } else {
      DialogSystem.dismissUntil(self._level - 1);
      self._onDone(self._extension,
                  self._service,
                  self._serviceMetadata.identifierSpace,
		  self._serviceMetadata.schemaSpace);
    }
  });
  this._elmts.cancelButton.click(function() {
    DialogSystem.dismissUntil(self._level - 1);
  });

  var dismissBusy = DialogSystem.showBusy();
  var type = (column.reconConfig) && (column.reconConfig.type) ? column.reconConfig.type.id : "";

  this._proposePropertiesUrl = null;
  this._fetchColumnUrl = null;
  this._serviceMetadata = null;
  if ("reconConfig" in column) {
    var service = column.reconConfig.service;
    this._service = service;
    var serviceMetadata = ReconciliationManager.getServiceFromUrl(service);
    this._serviceMetadata = serviceMetadata;
    if ("extend" in serviceMetadata) {
       var extend = serviceMetadata.extend;
       if ("propose_properties" in extend) {
	   var endpoint = extend.propose_properties;
           this._proposePropertiesUrl = endpoint.service_url + endpoint.service_path;
       }
     }
  }

  ExtendReconciledDataPreviewDialog.getAllProperties(this._proposePropertiesUrl, type, function(properties) {
    dismissBusy();
    self._show(properties);
  });
}

ExtendReconciledDataPreviewDialog.getAllProperties = function(url, typeID, onDone) {
  if(url == null) {
    onDone([]);
  } else {
    var done = false;
    $.getJSON(
	url +"?type=" + typeID + "&callback=?",
	null,
	function(data) {
	if (done) return;
	done = true;

	var allProperties = [];
	for (var i = 0; i < data.properties.length; i++) {
	    var property = data.properties[i];
	    var property2 = {
		id: property.id,
		name: property.name
	    };
	    /*if ("id2" in property) {
	    property2.expected = property.schema2;
	    property2.properties = [{
		id: property.id2,
		name: property.name2,
		expected: property.expects
	    }];
	    } else {
	    property2.expected = property.expects;
	    } */
	    allProperties.push(property2);
	}
	allProperties.sort(function(a, b) { return a.name.localeCompare(b.name); });

	onDone(allProperties);
	}
    );

    window.setTimeout(function() {
	if (done) return;

	done = true;
	onDone([]);
    }, 7000); // time to give up?
  }
};

ExtendReconciledDataPreviewDialog.prototype._show = function(properties) {
  this._level = DialogSystem.showDialog(this._dialog);

  var n = this._elmts.suggestedPropertyContainer.offset().top +
  this._elmts.suggestedPropertyContainer.outerHeight(true) -
  this._elmts.addPropertyInput.offset().top;

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
      self._addProperty(property);
    });
  };
  for (var i = 0; i < properties.length; i++) {
    renderSuggestedProperty(properties[i]);
  }

  var suggestConfig = $.extend({}, this._serviceMetadata.suggest.property);
  suggestConfig.key = null;
  suggestConfig.query_param_name = "prefix";

  this._elmts.addPropertyInput.suggestP(suggestConfig).bind("fb-select", function(evt, data) {
    self._addProperty({
      id : data.id,
      name: data.name,
    });
  });
};

ExtendReconciledDataPreviewDialog.prototype._update = function() {
  this._elmts.previewContainer.empty().html(
	'<div bind="progressPanel" class="extend-data-preview-progress"><img src="images/large-spinner.gif" /></div>');

  var self = this;
  var params = {
      project: theProject.id,
      columnName: this._column.name
  };

  console.log(this._extension);
  $.post(
    "command/core/preview-extend-data?" + $.param(params), 
    {
      rowIndices: JSON.stringify(this._rowIndices),
      extension: JSON.stringify(this._extension)
    },
    function(data) {
      self._renderPreview(data);
    },
    "json"
  ).fail(function(data) {
	console.log(data);
  });
};

ExtendReconciledDataPreviewDialog.prototype._addProperty = function(p) {
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

ExtendReconciledDataPreviewDialog.prototype._renderPreview = function(data) {
  var self = this;
  var container = this._elmts.previewContainer.empty();
  if (data.code == "error") {
    container.text("Error.");
    return;
  }

  var table = $('<table>')[0];
  var trHead = table.insertRow(table.rows.length);
  $('<th>').appendTo(trHead).text(this._column.name);

  var renderColumnHeader = function(column) {
    var th = $('<th>').appendTo(trHead);

    $('<span>').html(column.name).appendTo(th);
    $('<br>').appendTo(th);

    $('<a href="javascript:{}"></a>')
    .text($.i18n("core-views")["remove-prop"])
    .addClass("action")
    .attr("title", $.i18n("core-views")["remove-col"])
    .click(function() {
      self._removeProperty(column.id);
    }).appendTo(th);

    $('<a href="javascript:{}"></a>')
    .text($.i18n("core-views")["configure-prop"])
    .addClass("action")
    .attr("title", $.i18n("core-views")["configure-col"])
    .click(function() {
      self._constrainProperty(column.id);
    }).appendTo(th);
  };
  for (var c = 0; c < data.columns.length; c++) {
    renderColumnHeader(data.columns[c]);
  }

  for (var r = 0; r < data.rows.length; r++) {
    var tr = table.insertRow(table.rows.length);
    var row = data.rows[r];

    for (var c = 0; c < row.length; c++) {
      var td = tr.insertCell(tr.cells.length);
      var cell = row[c];
      if (cell !== null) {
        if ($.isPlainObject(cell)) {
          $('<a>').attr("href",
		this._serviceMetadata.identifierSpace + cell.id
		).attr("target", "_blank").text(cell.name).appendTo(td);
        } else {
          $('<span>').text(cell).appendTo(td);
        }
      }
    }
  }

  container.append(table);
};

ExtendReconciledDataPreviewDialog.prototype._removeProperty = function(id) {
  for(var i = this._extension.properties.length - 1; i >= 0; i--) {
    var property = this._extension.properties[i];
    if (property.id == id) {
       this._extension.properties.splice(i, 1);
    }
  }
  this._update();
};

ExtendReconciledDataPreviewDialog.prototype._findProperty = function(id) {
  var properties = this._extension.properties;
  for(var i = properties.length - 1; i >= 0; i--) {
    if (properties[i].id == id) {
       return properties[i];
    }
  }
  return null;
}

ExtendReconciledDataPreviewDialog.prototype._constrainProperty = function(id) {
  var self = this;
  var property = this._findProperty(id);

  var frame = DialogSystem.createDialog();
  frame.width("500px");

  var header = $('<div></div>').addClass("dialog-header").text("Settings for " + id).appendTo(frame);
  var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
  var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

  // by default we display an area where the user can input JSON
  var form = (
    '<tr><td>' +
    'Enter query settings as JSON' +
    '</td></tr>' +
    '<tr><td>' +
    '<textarea style="width: 100%; height: 300px; font-family: monospace;" bind="textarea"></textarea>' +
    '</td></tr>');

  // If the service metadata specifies fields, we build a proper form to make it more user-friendly
  var fields = self._serviceMetadata.extend.property_settings;
  if (fields != null) {
     form = '';
     for(var i = 0; i < fields.length; i++) {
	var field = fields[i];
        var fieldHTML = '';
	var currentValue = field.default;
        if (property.settings != null && property.settings[field.name] != null) {
            currentValue = property.settings[field.name];
        }
	if (field.type == 'select') {
	   fieldHTML += '<span class="property-config-select-label">'+field.label+':</span><br/>';
	   for(var j = 0; j < field.choices.length; j++) {
		var choice = field.choices[j];
		fieldHTML += '<label for="'+field.name+'_'+choice.value+'">';
		fieldHTML += '<input type="radio" name="'+field.name+'" '+
			'value="'+choice.value+'" id="'+field.name+'_'+choice.value+'" ';
		if (choice.value == currentValue) {
		    fieldHTML += 'checked="checked"';
		}
		fieldHTML += ' /> '+choice.name+'</label><br />';
	   }
	} else if (field.type == 'checkbox') {
	   fieldHTML += '<label for="'+field.name+'"><input type="checkbox" name="'+field.name+'" ';
	   if (currentValue == 'on') {
	       fieldHTML += 'checked="checked"'
	   }
	   fieldHTML += ' /> '+field.label+'</label>'
        } else if (field.type == 'number') {
	   fieldHTML += '<label for="'+field.name+'">'+field.label+': <input type="number" name="'+field.name+'" ';
	   fieldHTML += 'value="'+currentValue+'" /></label>';
	}
        if(fieldHTML != '') {
	  form += '<tr><td title="'+field.help_text+'">'+fieldHTML+'</td>';
	  form += '</tr>';
        }
     }    

     if (form == '') {
	form = '<tr><td>'+$.i18n('core-views')['no-settings']+'</td></tr>'
     }
  }

  body.html(
    '<div class="grid-layout layout-normal layout-full"><form class="data-extension-property-config" bind="form"><table>' +
    form +
    '</table></form></div>'
  );
  var bodyElmts = DOM.bind(body);

  if (fields == null) {
    if ("settings" in property) {
	bodyElmts.textarea[0].value = JSON.stringify(property.settings, null, 2);
    } else {
	bodyElmts.textarea[0].value = JSON.stringify({ "limit" : 10 }, null, 2);
    }
  }

  footer.html(
    '<button class="button" bind="okButton">'+$.i18n('core-buttons')['ok']+'</button>' +
    '<button class="button" bind="cancelButton">'+$.i18n('core-buttons')['cancel']+'</button>'
  );
  var footerElmts = DOM.bind(footer);

  var level = DialogSystem.showDialog(frame);
  var dismiss = function() {
    DialogSystem.dismissUntil(level - 1);
  };

  footerElmts.cancelButton.click(dismiss);
  footerElmts.okButton.click(function() {
    try {
      if (fields == null) {
	var o = JSON.parse(bodyElmts.textarea[0].value);
	if (o === undefined) {
	    alert("Please ensure that the JSON you enter is valid.");
	    return;
	}

	if ($.isArray(o) && o.length == 1) {
	    o = o[0];
	}
	if (!$.isPlainObject(o)) {
	    alert("The JSON you enter must be an object, that is, it is of this form { ... }.");
	    return;
	}

	property.settings = o;
      } else {
          var elem = $(bodyElmts.form[0]);
	  var ar = elem.serializeArray();
	  var settings = {};
          for(var i = 0; i < ar.length; i++) {
              settings[ar[i].name] = ar[i].value;
	  }
	  console.log(ar);
	  property.settings = settings;
	  console.log(settings);
      }

      dismiss();

      self._update();
    } catch (e) {
      //console.log(e);
    }
  });

  //bodyElmts.textarea.focus();
};

