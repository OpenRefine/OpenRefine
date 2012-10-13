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

function ReconStandardServicePanel(column, service, container) {
  this._column = column;
  this._service = service;
  this._container = container;
  this._types = [];

  this._constructUI();
}

ReconStandardServicePanel.prototype._guessTypes = function(f) {
  var self = this;
  var dismissBusy = DialogSystem.showBusy();

  $.post(
    "command/core/guess-types-of-column?" + $.param({
      project: theProject.id, 
      columnName: this._column.name,
      service: this._service.url
    }),
    null, 
    function(data) {
      self._types = data.types;

      if (self._types.length === 0 && "defaultTypes" in self._service) {
        var defaultTypes = {};
        $.each(self._service.defaultTypes, function() {
          defaultTypes[this.id] = this.name;
        });
        $.each(self._types, function() {
          delete defaultTypes[typeof this == "string" ? this : this.id];
        });
        for (var id in defaultTypes) {
          if (defaultTypes.hasOwnProperty(id)) {
            self._types.push({
              id: id,
              name: defaultTypes[id].name
            });
          }
        }
      }

      dismissBusy();
      f();
    }
  );
};

ReconStandardServicePanel.prototype._constructUI = function() {
  var self = this;
  this._panel = $(DOM.loadHTML("core", "scripts/reconciliation/standard-service-panel.html")).appendTo(this._container);
  this._elmts = DOM.bind(this._panel);

  this._elmts.rawServiceLink.attr("href", this._service.url);

  this._guessTypes(function() {
    self._populatePanel();
    self._wireEvents();
  });
};

ReconStandardServicePanel.prototype.activate = function() {
  this._panel.show();
};

ReconStandardServicePanel.prototype.deactivate = function() {
  this._panel.hide();
};

ReconStandardServicePanel.prototype.dispose = function() {
  this._panel.remove();
  this._panel = null;

  this._column = null;
  this._service = null;
  this._container = null;
};

ReconStandardServicePanel.prototype._populatePanel = function() {
  var self = this;

  /*
   *  Populate types
   */
  if (this._types.length > 0) {
    var typeTableContainer = $('<div>')
    .addClass("grid-layout layout-tightest")
    .appendTo(this._elmts.typeContainer);

    var typeTable = $('<table></table>').appendTo(typeTableContainer)[0];

    var createTypeChoice = function(type, check) {
      var typeID = typeof type == "string" ? type : type.id;
      var typeName = typeof type == "string" ? type : (type.name || type.id);

      var tr = typeTable.insertRow(typeTable.rows.length);
      var td0 = tr.insertCell(0);
      var td1 = tr.insertCell(1);

      td0.width = "1%";
      var radio = $('<input type="radio" name="type-choice">')
      .attr("value", typeID)
      .attr("typeName", typeName)
      .appendTo(td0)
      .click(function() {
        self._rewirePropertySuggests(this.value);
      });

      if (check) {
        radio.attr("checked", "true");
      }

      if (typeName == typeID) {
        $(td1).html(typeName);
      } else {
        $(td1).html(
            typeName + 
            '<br/>' +
            '<span class="type-id">' + typeID + '</span>');
      }
    };
    for (var i = 0; i < this._types.length; i++) {
      createTypeChoice(this._types[i], i === 0);
    }
  } else {
    $('<div>')
    .addClass("recon-dialog-standard-service-panel-message")
    .text("Sorry, we can't suggest any type for your data. Please specify a type yourself below.")
    .appendTo(this._elmts.typeContainer);

    this._panel
    .find('input[name="type-choice"][value=""]')
    .attr("checked", "true");

    this._elmts.typeInput.focus();
  }

  /*
   *  Populate properties
   */
  var detailTableContainer = $('<div>')
  .addClass("grid-layout layout-tightest")
  .appendTo(this._elmts.detailContainer);

  var detailTable = $(
      '<table>' +
      '<tr><th>Column</th><th>Include?</th><th>As Property</th></tr>' +
      '</table>'
  ).appendTo(detailTableContainer)[0];

  function renderDetailColumn(column) {
    var tr = detailTable.insertRow(detailTable.rows.length);
    var td0 = tr.insertCell(0);
    var td1 = tr.insertCell(1);
    var td2 = tr.insertCell(2);

    $(td0).html(column.name);
    $('<input type="checkbox" />')
    .attr("columnName", column.name)
    .appendTo(td1);
    $('<input size="25" name="property" />')
    .attr("columnName", column.name)
    .appendTo(td2);
  }
  var columns = theProject.columnModel.columns;
  for (var i = 0; i < columns.length; i++) {
    var column = columns[i];
    if (column !== this._column) {
      renderDetailColumn(column);
    }
  }
};

ReconStandardServicePanel.prototype._wireEvents = function() {
  var self = this;
  var input = this._elmts.typeInput.unbind();

  if ("suggest" in this._service && "type" in this._service.suggest && this._service.suggest.type.service_url) {
    // Old style suggest API
    var suggestOptions = $.extend({}, this._service.suggest.type);
    suggestOptions.key = null;
    suggestOptions.query_param_name = "prefix";
    input.suggestT(suggestOptions);
  } else if (this._isInFreebaseSchemaSpace()) {
    input.suggestT({ filter : '(all type:/type/type)' });
  }

  input.bind("fb-select", function(e, data) {
    self._panel
    .find('input[name="type-choice"][value=""]')
    .attr("checked", "true");

    self._rewirePropertySuggests(data.id);
  });

  this._rewirePropertySuggests((this._types.length > 0) ? this._types[0] : null);
};

ReconStandardServicePanel.prototype._rewirePropertySuggests = function(type) {
  var inputs = this._panel
  .find('input[name="property"]')
  .unbind();

  if ("suggest" in this._service && "property" in this._service.suggest && this._service.suggest.property.service_url) {
    // Old style suggest API
    var suggestOptions = $.extend({}, this._service.suggest.property);
    suggestOptions.key = null;
    suggestOptions.query_param_name = "prefix";
    if (type) {
      suggestOptions.ac_param = { schema: typeof type == "string" ? type : type.id };
    }
    inputs.suggestP(suggestOptions);
  } else if (this._isInFreebaseSchemaSpace()) {
    var namespace = (type) ? (typeof type == "string" ? type : type.id) : "/common/topic"
    inputs.suggestP({
      filter : '(all type:/type/property (any namespace:/type/object namespace:' + namespace + '))'
    });
  }
};

ReconStandardServicePanel.prototype._isInFreebaseIdentifierSpace = function() {
  return ReconciliationManager.isFreebaseIdOrMid(this._service.identifierSpace);
};

ReconStandardServicePanel.prototype._isInFreebaseSchemaSpace = function() {
  return ReconciliationManager.isFreebaseId(this._service.schemaSpace);
};

ReconStandardServicePanel.prototype.start = function() {
  var self = this;

  var type = this._elmts.typeInput.data("data.suggest");
  if (!(type)) {
    type = {
        id: this._elmts.typeInput[0].value,
        name: this._elmts.typeInput[0].value
    };
  }

  var choices = this._panel.find('input[name="type-choice"]:checked');
  if (choices !== null && choices.length > 0) {
    if (choices[0].value == '-') {
      type = null;
    } else if (choices[0].value != "") {
      type = {
          id: choices[0].value,
          name: choices.attr("typeName")
      };
    }
  }

  var columnDetails = [];
  $.each(
    this._panel.find('input[name="property"]'),
    function() {
      var property = $(this).data("data.suggest");
      if (property && property.id) {
        columnDetails.push({
          column: this.getAttribute("columnName"),
          property: {
            id: property.id,
            name: property.name
          }
        });
      } else {
        var property = $.trim(this.value);
        if (property) {
          columnDetails.push({
            column: this.getAttribute("columnName"),
            property: {
              id: property,
              name: property
            }
          });
        }
      }
    }
  );

  Refine.postCoreProcess(
    "reconcile",
    {},
    {
      columnName: this._column.name,
      config: JSON.stringify({
        mode: "standard-service",
        service: this._service.url,
        identifierSpace: this._service.identifierSpace,
        schemaSpace: this._service.schemaSpace,
        type: (type) ? { id: type.id, name: type.name } : null,
            autoMatch: this._elmts.automatchCheck[0].checked,
            columnDetails: columnDetails
      })
    },
    { cellsChanged: true, columnStatsChanged: true }
  );
};

