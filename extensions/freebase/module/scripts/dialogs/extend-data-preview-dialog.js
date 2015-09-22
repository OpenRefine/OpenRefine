/*

Copyright 2010,2012 Google Inc. and other contributors
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

function ExtendDataPreviewDialog(column, columnIndex, rowIndices, onDone) {
  this._serviceRecords = [];
  this._selectedServiceRecordIndex = -1;

  this._column = column;
  this._columnIndex = columnIndex;
  this._rowIndices = rowIndices;
  this._onDone = onDone;
  this._extension = { properties: [], service: null };

  this._createDialog();
}

ExtendDataPreviewDialog.prototype._createDialog = function() {
  var self = this;
  this._dialog = $(DOM.loadHTML("freebase", "scripts/dialogs/extend-data-preview-dialog.html"));
  this._elmts = DOM.bind(this._dialog);

  this._elmts.dialogHeader.text($.i18n._('fb-extend')["add-column"]+' "' + this._column.name + '"');
  this._elmts.addProperty.html($.i18n._('fb-extend')["add-property"]);
  this._elmts.suggestedProperties.text($.i18n._('fb-extend')["suggested-properties"]);
  this._elmts.preview.text($.i18n._('fb-extend')["preview"]);

  this._elmts.servicePanelMessage.html($.i18n._('fb-extend')["pick-service"]);
  this._elmts.serviceListTitle.html($.i18n._('fb-extend')["service-title"]);
  this._elmts.addServiceButton.html($.i18n._('core-buttons')["add-service"]+"...");
  
  this._elmts.resetButton.text($.i18n._('fb-buttons')["reset"]);
  this._elmts.okButton.html('&nbsp;&nbsp;'+$.i18n._('fb-buttons')["ok"]+'&nbsp;&nbsp;');
  this._elmts.cancelButton.text($.i18n._('fb-buttons')["cancel"]);
  
  this._elmts.servicePanel.hide();

  this._elmts.addServiceButton.click(function() { self._onAddService(); });

  this._elmts.resetButton.click(function() {
    self._extension.properties = [];
    self._update();
  });

  this._elmts.okButton.click(function() { self._onOK(); });
  this._elmts.cancelButton.click(function() { self._dismiss(); });

  this._level = DialogSystem.showDialog(this._dialog);

  this._populateDialog();
};

ExtendDataPreviewDialog.prototype._onOK = function() {
  if (this._extension.properties.length === 0) {
    alert($.i18n._('fb-extend')["warning-add-properties"]);
  }
  else {
    DialogSystem.dismissUntil(this._level - 1);
    this._onDone(this._extension);
  }
};

ExtendDataPreviewDialog.prototype._dismiss = function() {
  this._cleanDialog();
  DialogSystem.dismissUntil(this._level - 1);
};

ExtendDataPreviewDialog.prototype._cleanDialog = function() {
  for (var i = 0; i < this._serviceRecords.length; i++) {
    var record = this._serviceRecords[i];
    record.selector.remove();
  }
  this._serviceRecords = [];
  this._selectedServiceRecordIndex = -1;
  this._elmts.servicePanel.hide();
  this._elmts.servicePanelMessage.show();
  this._elmts.suggestedPropertyContainer.empty();
  this._elmts.previewContainer.empty();
};

ExtendDataPreviewDialog.prototype._populateDialog = function() {
  var self = this;

  var services = ExtendDataManager.getAllServices();
  if (services.length > 0) {
    var renderService = function(service) {
      var record = {
          service: service,
      };

      record.selector = $('<a>')
      .attr("href", "javascript:{}")
      .addClass("recon-dialog-service-selector")
      .text(service.name)
      .appendTo(self._elmts.serviceList)
      .click(function() {
        self._toggleServices();
        self._selectService(record);
      });

      $('<a>')
      .html("&nbsp;")
      .addClass("recon-dialog-service-selector-remove")
      .prependTo(record.selector)
      .click(function() {
        ExtendDataManager.unregisterService(service, function() {
          self._refresh(-1);
        });
      });

      self._serviceRecords.push(record);
    };

    for (var i = 0; i < services.length; i++) {
      renderService(services[i]);
    }

    $('.recon-dialog-service-opener').click(function() {
      self._toggleServices();
    });
  }
};

ExtendDataPreviewDialog.prototype._toggleServices = function() {
  var self = this;
  self._toggleServiceTitle(500);
  self._toggleServiceList(500);
};

ExtendDataPreviewDialog.prototype._toggleServiceTitle = function(duration) {
  var title = $('.recon-dialog-service-opener-title');
  title.animate({
    width : 'toggle'
  }, duration, 'swing', function() {
  });
};

ExtendDataPreviewDialog.prototype._toggleServiceList = function(duration) {
  $(".recon-dialog-service-list").toggle("slide", duration);
};

ExtendDataPreviewDialog.prototype._refresh = function(newSelectIndex) {
  this._cleanDialog();
  this._populateDialog();
  if (newSelectIndex >= 0) {
    this._selectService(this._serviceRecords[newSelectIndex]);
  }
};

ExtendDataPreviewDialog.prototype._onAddService = function() {
  var self = this;
  var dialog = $(DOM.loadHTML("freebase", "scripts/dialogs/add-service-dialog.html"));
  var elmts = DOM.bind(dialog);

  elmts.dialogHeader.html($.i18n._('fb-extend')["add-mql-srv"]);
  elmts.or_recon_enterName.html($.i18n._('fb-extend')["enter-name"]+":");
  elmts.or_recon_enterUrl.html($.i18n._('core-recon')["enter-url"]+":");
  elmts.addButton.html($.i18n._('core-buttons')["add-service"]);
  elmts.cancelButton.html($.i18n._('core-buttons')["cancel"]);

  var level = DialogSystem.showDialog(dialog);
  var dismiss = function() {
    DialogSystem.dismissUntil(level - 1);
  };

  elmts.cancelButton.click(dismiss);
  elmts.addButton.click(function() {
    var name = $.trim(elmts.inputName.val());
    var url = $.trim(elmts.inputUrl.val());
    if (url.length > 0) {
      if (name.length == 0) {
        name = url;
      }
      ExtendDataManager.registerStandardService(url, name, function(index) {
        self._refresh(index);
      });
    }
    dismiss();
  });
  elmts.inputName.focus().select();
};

ExtendDataPreviewDialog.prototype._selectService = function(record) {
  var self = this;

  for (var i = 0; i < this._serviceRecords.length; i++) {
    if (record === this._serviceRecords[i]) {
      if (i !== this._selectedServiceRecordIndex) {
        if (this._selectedServiceRecordIndex >= 0) {
          var oldRecord = this._serviceRecords[this._selectedServiceRecordIndex];
          oldRecord.selector.removeClass("selected");
        }

        this._elmts.servicePanelMessage.hide();
        this._elmts.servicePanel.show();

        record.selector.addClass("selected");

        var dismissBusy = DialogSystem.showBusy();
        var type = (this._column.reconConfig) && (this._column.reconConfig.type) ? this._column.reconConfig.type.id : "/common/topic";

        this._extension.service = record.service;

        // This should happen only after a service is chosen
        ExtendDataPreviewDialog.getAllProperties(record.service, type, function(properties) {
          dismissBusy();
          self._show(properties);
        });

        this._selectedServiceRecordIndex = i;
        return;
      }
    }
  }
};

ExtendDataPreviewDialog.getAllProperties = function(service, typeID, onDone) {
  var done = false;

  // First try using /get_properties_of_type
  $.getJSON(
    service.url + "/get_properties_of_type?type=" + typeID + "&callback=?",
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
        if ("id2" in property) {
          property2.expected = property.schema2;
          property2.properties = [{
            id: property.id2,
            name: property.name2,
            expected: property.expects
          }];
        } else {
          property2.expected = property.expects;
        }
        allProperties.push(property2);
      }
      allProperties.sort(function(a, b) { return a.name.localeCompare(b.name); });

      onDone(allProperties);
    }
  );

  window.setTimeout(function() {
    if (done) return;
    done = true;

    var done2 = false;

    // That failed, so try an MQL read for finding properties, and fake the "expected" object keys.

    var query = [{
      "type": "/type/property",
      "schema": { "id": typeID },
      "id": null,
      "name": null
    }];

    $.getJSON(
      service.url + "/mqlread/?lang=/lang/en&query=" + JSON.stringify(query) + "&callback=?",
      null,
      function(data) {
        if (done2) return;
        done2 = true;

        var allProperties = [];
        for (var i = 0; i < data.result.length; i++) {
          var property = data.result[i];
          var property2 = {
            id: property.id,
            name: property.name
          };
          if ("id2" in property) {
            property2.expected = property.schema2;
            property2.properties = [{
              id: property.id2,
              name: property.name2,
              expected: property.expects
            }];
          } else {
            if ("expects" in property) {
              property2.expected = property.expects;
            }
            else {
              property2.expected = { "id": "/type/text", "name": "Text" };
            }
          }
          allProperties.push(property2);
        }
        allProperties.sort(function(a, b) { return a.name.localeCompare(b.name); });

        onDone(allProperties);
      }
    );

    window.setTimeout(function() {
      if (done2) return;
      done2 = true;
      onDone([]);
    }, 7000);

  }, 7000); // time to give up?
};

ExtendDataPreviewDialog.prototype._show = function(properties) {
  var n = this._elmts.suggestedPropertyContainer.offset().top +
  this._elmts.suggestedPropertyContainer.outerHeight(true) -
  this._elmts.addPropertyInput.offset().top;

  this._elmts.previewContainer.height(Math.floor(n));

  var self = this;
  var container = this._elmts.suggestedPropertyContainer.empty();
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

  var suggestConfig = {
      type: '/type/property', // NOTE: requires patched Suggest to pass this through
      // Default returns id, lang, mid, name, notable {id,name}, score
      mql_output : JSON.stringify({'name':null,'id':null,'mid':null, '/type/property/expected_type':{'name':null,'id':null}}),
  };
  if ((this._column.reconConfig) && (this._column.reconConfig.type)) {
    suggestConfig.filter = '(should (any namespace:/type/object namespace:/common/topic namespace:' + this._column.reconConfig.type.id + '))';
  }

  // Disable Add Property box for non-Freebase services
  // TODO: Find out with an API call (which doesn't exist?) whether there's a suggest service.
  if (this._extension.service.url.indexOf('freebase') > -1) {
    this._elmts.addPropertyInput.prop('disabled', false);
    this._elmts.addPropertyInput.suggestP(suggestConfig).bind("fb-select", function(evt, data) {
      var expected = data['/type/property/expected_type'];
      self._addProperty({
        id : data.id,
        name: data.name,
        expected: {
          id: expected.id,
          name: expected.name
        }
      });
    });
  }
  else {
    this._elmts.addPropertyInput.prop('disabled', true);
  }
};

ExtendDataPreviewDialog.prototype._update = function() {
  this._elmts.previewContainer.empty().text($.i18n._('fb-extend')["querying-freebase"]);

  var self = this;
  var params = {
      project: theProject.id,
      columnName: this._column.name
  };

  $.post(
    "command/freebase/preview-extend-data?" + $.param(params), 
    {
      rowIndices: JSON.stringify(this._rowIndices),
      extension: JSON.stringify(this._extension)
    },
    function(data) {
      self._renderPreview(data);
    },
    "json"
  );
};

ExtendDataPreviewDialog.prototype._addProperty = function(p) {
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

ExtendDataPreviewDialog.prototype._renderPreview = function(data) {
  var self = this;
  var container = this._elmts.previewContainer.empty();
  if (data.code == "error") {
    container.text($.i18n._('core-dialogs')["error"]);
    return;
  }

  var table = $('<table>')[0];
  var trHead = table.insertRow(table.rows.length);
  $('<th>').appendTo(trHead).text(this._column.name);

  var renderColumnHeader = function(column) {
    var th = $('<th>').appendTo(trHead);

    $('<span>').html(column.names.join(" &raquo; ")).appendTo(th);
    $('<br>').appendTo(th);

    $('<a href="javascript:{}"></a>')
    .text("remove")
    .addClass("action")
    .attr("title", $.i18n._('fb-extend')["remove-column"])
    .click(function() {
      self._removeProperty(column.path);
    }).appendTo(th);

    $('<a href="javascript:{}"></a>')
    .text("constrain")
    .addClass("action")
    .attr("title", $.i18n._('fb-extend')["add-constraints"])
    .click(function() {
      self._constrainProperty(column.path);
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
          $('<a>').attr("href", (self._extension.service.viewUrl != null) ? self._extension.service.viewUrl.replace("{{id}}", cell.id) : cell.id).text(cell.name).appendTo(td);
        } else {
          $('<span>').text(cell).appendTo(td);
        }
      }
    }
  }

  container.append(table);
};

ExtendDataPreviewDialog.prototype._removeProperty = function(path) {
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

ExtendDataPreviewDialog.prototype._findProperty = function(path) {
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

ExtendDataPreviewDialog.prototype._constrainProperty = function(path) {
  var self = this;
  var property = this._findProperty(path);

  var frame = DialogSystem.createDialog();
  frame.width("500px");

  var header = $('<div></div>').addClass("dialog-header").text(" " + path.join(" > ")).appendTo(frame);
  var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
  var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

  body.html(
    '<div class="grid-layout layout-normal layout-full"><table>' +
    '<tr><td>' +
    $.i18n._('fb-extend')["mql-constraints"] +
    '</td></tr>' +
    '<tr><td>' +
    '<textarea style="width: 100%; height: 300px; font-family: monospace;" bind="textarea"></textarea>' +
    '</td></tr>' +
    '</table></div>'
  );
  var bodyElmts = DOM.bind(body);

  if ("constraints" in property) {
    bodyElmts.textarea[0].value = JSON.stringify(property.constraints, null, 2);
  } else {
    bodyElmts.textarea[0].value = JSON.stringify({ "limit" : 10 }, null, 2);
  }

  footer.html(
    '<button class="button" bind="okButton">&nbsp;&nbsp;'+$.i18n._('fb-buttons')["ok"]+'&nbsp;&nbsp;</button>' +
    '<button class="button" bind="cancelButton">'+$.i18n._('fb-buttons')["cancel"]+'</button>'
  );
  var footerElmts = DOM.bind(footer);

  var level = DialogSystem.showDialog(frame);
  var dismiss = function() {
    DialogSystem.dismissUntil(level - 1);
  };

  footerElmts.cancelButton.click(dismiss);
  footerElmts.okButton.click(function() {
    try {
      var o = JSON.parse(bodyElmts.textarea[0].value);
      if (o === undefined) {
        alert($.i18n._('fb-extend')["warning-valid-json"]);
        return;
      }

      if ($.isArray(o) && o.length == 1) {
        o = o[0];
      }
      if (!$.isPlainObject(o)) {
        alert($.i18n._('fb-extend')["warning-json-obj"]);
        return;
      }

      property.constraints = o;

      dismiss();

      self._update();
    } catch (e) {
      //console.log(e);
    }
  });

  bodyElmts.textarea.focus();
};
