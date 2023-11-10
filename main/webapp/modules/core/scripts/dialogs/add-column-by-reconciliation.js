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
  this._dialog = $(DOM.loadHTML("core", "scripts/views/data-table/add-column-by-reconciliation.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.html($.i18n('core-views/add-col-recon-col')+" "+column.name);
  this._elmts.okButton.html($.i18n('core-buttons/ok'));
  this._elmts.cancelButton.html($.i18n('core-buttons/cancel'));
  this._elmts.resetButton.html($.i18n('core-buttons/reset'));
  this._elmts.addPropertyHeader.html($.i18n('core-dialogs/add-property'));
  this._elmts.suggestedPropertyHeader.html($.i18n('core-dialogs/suggested-properties'));
  this._elmts.previewHeader.html($.i18n('core-dialogs/preview'));
  this._elmts.resetButton.on('click',function() {
    self._extension.properties = [];
    self._update();
  });

  this._elmts.okButton.on('click',function() {
    if (self._extension.properties.length === 0) {
      alert($.i18n('core-views/warning-no-property'));
    } else {
      DialogSystem.dismissUntil(self._level - 1);
      self._onDone(self._extension,
                  self._service,
                  self._serviceMetadata.identifierSpace,
                  self._serviceMetadata.schemaSpace);
    }
  });
  this._elmts.cancelButton.on('click',function() {
    DialogSystem.dismissUntil(self._level - 1);
  });

  var type = (column.reconConfig) && (column.reconConfig.type) ? column.reconConfig.type.id : "";

  this._proposePropertiesUrl = null;
  this._fetchColumnUrl = null;
  this._serviceMetadata = null;
  var extend = null;
  if ("reconConfig" in column) {
    var service = column.reconConfig.service;
    this._service = service;
    var serviceMetadata = ReconciliationManager.getServiceFromUrl(service);
    this._serviceMetadata = serviceMetadata;
    if (serviceMetadata != null && "extend" in serviceMetadata) {
       var extend = serviceMetadata.extend;
       if ("propose_properties" in extend) {
           var endpoint = extend.propose_properties;
           this._proposePropertiesUrl = endpoint.service_url + endpoint.service_path;
       }
     }
  }

  if (this._serviceMetadata === null) {
     alert($.i18n('core-views/extend-not-reconciled'));
  } else if(extend === null) {
     alert($.i18n('core-views/extend-not-supported'));
  } else {
    var dismissBusy = DialogSystem.showBusy();
    ExtendReconciledDataPreviewDialog.getAllProperties(this._proposePropertiesUrl, type, function(properties) {
      dismissBusy();
      self._show(properties);
    });
  }
}

ExtendReconciledDataPreviewDialog.getAllProperties = function(url, typeID, onDone) {
  if(url == null) {
    onDone([]);
  } else {
    var done = false;
    var onSuccess = function (data) {
      if (done) return;
      done = true;
      var allProperties = [];
      for (var i = 0; i < data.properties.length; i++) {
        var property = data.properties[i];
        var property2 = {
          id: property.id,
          name: property.name,
        };
        allProperties.push(property2);
      }
      allProperties.sort(function (a, b) {
        return a.name.localeCompare(b.name);
      });

      onDone(allProperties);
    };

    $.ajax(url + "?type=" + typeID, {
      dataType: "json",
      success: onSuccess,
      timeout: 7000,
      error: function () {
        $.ajax(url + "?type=" + typeID, {
          dataType: "jsonp",
          success: onSuccess,
          timeout: 7000,
          error: function () {
            if (done) return;

            done = true;
            onDone([]);
          }
        });
      },
    });
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
    .on('click',function() {
      self._addProperty(property);
    });
  };
  for (var i = 0; i < properties.length; i++) {
    renderSuggestedProperty(properties[i]);
  }

  var suggestConfig = $.extend({}, this._serviceMetadata.suggest && this._serviceMetadata.suggest.property);
  suggestConfig.key = null;
  suggestConfig.query_param_name = "prefix";

  // CORS/JSONP support
  if (this._serviceMetadata.ui && this._serviceMetadata.ui.access) {
    suggestConfig.access = this._serviceMetadata.ui.access;
  }
  this._elmts.addPropertyInput.suggestP(sanitizeSuggestOptions(suggestConfig)).on("fb-select", function(evt, data) {
    self._addProperty({
      id : data.id,
      name: data.name,
    });
  });
};

ExtendReconciledDataPreviewDialog.prototype._update = function() {
  this._elmts.previewContainer.empty().html(
        '<div bind="progressPanel" class="add-column-by-reconciliation-progress"><img src="images/large-spinner.gif" /></div>');

  var self = this;
  var params = {
      project: theProject.id,
      columnName: this._column.name
  };

  if(this._extension.properties.length === 0) {
    // if the column selection is empty, reset the view
    this._elmts.previewContainer.empty();
  } else {
    // otherwise, refresh the preview
    Refine.postCSRF(
        "command/core/preview-extend-data?" + $.param(params),
        {
        rowIndices: JSON.stringify(this._rowIndices),
        extension: JSON.stringify(this._extension)
        },
        function(data) {
        self._renderPreview(data);
        },
        "json",
        function(data) {
           alert($.i18n('core-views/internal-err'));
        });
  }
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
    .text($.i18n('core-views/remove-prop'))
    .addClass("action")
    .css("margin-right", "5px")
    .attr("title", $.i18n('core-views/remove-col'))
    .on('click',function() {
      self._removeProperty(column.id);
    }).appendTo(th);

    $('<a href="javascript:{}"></a>')
    .text($.i18n('core-views/configure-prop'))
    .addClass("action")
    .attr("title", $.i18n('core-views/configure-col'))
    .on('click',function() {
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
          var service = self._serviceMetadata;
          var href = (service.view && service.view.url) ?
            encodeURI(service.view.url.replace("{{id}}", cell.id)) :
            service.identifierSpace + cell.id;
          $('<a>').attr("href", href).attr("target", "_blank").text(cell.name).appendTo(td);
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
    if (property.id === id) {
       this._extension.properties.splice(i, 1);
    }
  }
  this._update();
};

ExtendReconciledDataPreviewDialog.prototype._findProperty = function(id) {
  var properties = this._extension.properties;
  for(var i = properties.length - 1; i >= 0; i--) {
    if (properties[i].id === id) {
       return properties[i];
    }
  }
  return null;
}

ExtendReconciledDataPreviewDialog.prototype._constrainProperty = function(id) {
  var self = this;
  var property = this._findProperty(id);

  var frame = DialogSystem.createDialog();

  var header = $('<div></div>').addClass("dialog-header").text("Settings for " + id).appendTo(frame);
  var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
  var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

  var fields = self._serviceMetadata.extend.property_settings;
  var table = $('<table></table>');
  if (fields != null) {
     for(var i = 0; i < fields.length; i++) {
        var field = fields[i];
        var fieldHTML = '';
        var currentValue = field['default'];
        if (property.settings != null && property.settings[field.name] != null) {
            currentValue = property.settings[field.name];
        }
        var tr = $('<tr></tr>');
        var td = $('<td></td>').attr('title', field.help_text).appendTo(tr);
        if (field.type === 'select') {
           var fieldLabel = $('<span></span>').text(field.label+':').appendTo(td);
           td.append($('<br/>'));
           for(var j = 0; j < field.choices.length; j++) {
                var choice = field.choices[j];
                var labelElem = $('<label></label>').attr('for', field.name+'_'+choice.value).appendTo(td);
                var inputElem = $('<input type="radio" />').attr(
                                'id', field.name+'_'+choice.value).val(choice.value).attr(
                                'name', field.name).appendTo(labelElem);

                if (choice.value === currentValue) {
                    inputElem.prop('checked', true);
                }
                labelElem.append(' '+choice.name);
                td.append('<br/>');
           }
           td.append(fieldHTML);
        } else if (field.type === 'checkbox') {
           var label = $('<label></label>').attr('for', field.name).appendTo(td);
           var input = $('<input type="checkbox" />').attr('name', field.name).appendTo(label);
           if (currentValue === 'on') {
               input.prop('checked', true);
           }
           label.append(' '+field.label);
        } else if (field.type === 'number' || field.type == 'text') {
           var label = $('<label></label>').attr('for', field.name).appendTo(td);
           label.append(field.label+': ');
           var input = $('<input />').attr(
                'name', field.name).attr(
                'type', field.type).val(currentValue).appendTo(label);
        } 
        if (tr.children().length > 0) {
            table.append(tr);
        }
     }    
  }

  if (table.children().length === 0)  {
     var tr = $('<tr></tr>').appendTo(table);
     $('<td></td>').text($.i18n('core-views/no-settings')).appendTo(tr);
   }

  var form = $('<form class="data-extension-property-config" bind="form"></form>').append(table);
  var gridLayout = $('<div class="grid-layout layout-normal layout-full"></div>').append(form);
  body.append(gridLayout);
  var bodyElmts = DOM.bind(body);

  footer.html(
    '<button class="button" bind="okButton">'+$.i18n('core-buttons/ok')+'</button>' +
    '<button class="button" bind="cancelButton">'+$.i18n('core-buttons/cancel')+'</button>'
  );
  var footerElmts = DOM.bind(footer);

  var level = DialogSystem.showDialog(frame);
  var dismiss = function() {
    DialogSystem.dismissUntil(level - 1);
  };

  footerElmts.cancelButton.on('click',dismiss);
  footerElmts.okButton.on('click',function() {
    try {
      if (fields != null) {
          var elem = $(bodyElmts.form[0]);
          var ar = elem.serializeArray();
          var settings = {};
          for(var i = 0; i < ar.length; i++) {
              settings[ar[i].name] = ar[i].value;
          }
          property.settings = settings;
      }

      dismiss();

      self._update();
    } catch (e) {
        alert($.i18n('core-views/internal-err'));
    }
  });

  //bodyElmts.textarea.focus();
};

