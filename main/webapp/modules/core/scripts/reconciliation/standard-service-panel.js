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
  var dismissBusy = self.showBusyReconciling();

  Refine.postCSRF(
    "command/core/guess-types-of-column?" + $.param({
      project: theProject.id, 
      columnName: this._column.name,
      service: this._service.url
    }),
    null, 
    function(data) {
      if (data.code && data.code === 'ok') {
        self._types = data.types;

        if (self._types.length === 0 || "defaultTypes" in self._service) {
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
                name: defaultTypes[id]
              });
            }
          }
        }
      } else {
        alert('Guess Types query failed ' + data.code + ' : ' + data.message);
      }

      dismissBusy();
      f();
    },
    "json"
  );
};

ReconStandardServicePanel.prototype._constructUI = function() {
  var self = this;
  this._panel = $(DOM.loadHTML("core", "scripts/reconciliation/standard-service-panel.html")).appendTo(this._container);
  this._elmts = DOM.bind(this._panel);
  this._elmts.or_proc_accessDocumentation.html($.i18n('core-recon/service-documentation'));
  this._elmts.automatchCheck[0].checked=JSON.parse(Refine.getPreference("ui.reconciliation.automatch", true));
  this._elmts.or_proc_cellType.html($.i18n('core-recon/cell-type'));
  this._elmts.or_proc_colDetail.html($.i18n('core-recon/col-detail'));
  this._elmts.or_proc_againstType.html($.i18n('core-recon/against-type'));
  this._elmts.or_proc_noType.html($.i18n('core-recon/no-type'));
  this._elmts.or_proc_autoMatch.html($.i18n('core-recon/auto-match'));
  this._elmts.or_proc_max_candidates.html($.i18n('core-recon/max-candidates'));
  this._elmts.typeInput.attr('aria-label',$.i18n('core-recon/type'))

  this._elmts.documentationLink.css("display", "none");
  if(this._service.documentation) {
    this._elmts.documentationLink.attr("href", this._service.documentation);
    // Show the documentation link if documentation is available
    this._elmts.documentationLink.css("display", "block");
  } 
  
  this._elmts.againstType.on('change', function() {
    self._elmts.typeInput.trigger('focus').trigger('select');
  });

  this._elmts.noType.on('click', function () {
    self._rewirePropertySuggests(null) // Clear any selected type
  });
  self._populateProperties();
  self._wireEvents();
  self._elmts.editMappedType.on('click', function() {
        $input = self._elmts.typeInput;
        $mappedValue = $(this).parent();
        $input.removeData('data.suggest');
        $label = $mappedValue.parent().find('.mapped-value > a:not(.edit-mapped-value)');
        $input.val($label.text()).prop('disabled',false);
        $mappedValue.toggle();
        $input.trigger('focus');
  });
  this._guessTypes(function () {
    self._populatePanel();
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
      .val(typeID)
      .attr("typeName", typeName)
      .appendTo(td0)
      .on('click',function() {
        self._rewirePropertySuggests(this.value);
      });

      if (check) {
        radio.prop('checked', true);
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
    .text($.i18n('core-recon/warning-type-sugg'))
    .appendTo(this._elmts.typeContainer);

    this._panel
    .find('input[name="type-choice"][value=""]')
    .prop('checked', true);

    this._elmts.typeInput.trigger('focus');
  }
}
  ReconStandardServicePanel.prototype._populateProperties = function () {
  /*
   *  Populate properties
   */
  var detailTableContainer = $('<div>')
  .addClass("grid-layout layout-tightest")
  .appendTo(this._elmts.detailContainer);

  var detailTable = $(
      '<table>' +
      '<tr><th>'+$.i18n('core-recon/column')+'</th><th>'+$.i18n('core-recon/as-property')+'</th></tr>' +
      '</table>'
  ).appendTo(detailTableContainer)[0];

  function renderDetailColumn(column) {
    var tr = detailTable.insertRow(detailTable.rows.length);
    var td0 = tr.insertCell(0);
    var td1 = tr.insertCell(1);
    $(td0).attr("columnName", column.name).html(column.name);
    $(td1).data('id','property').css('position','relative');

    let mappedColumn = $("<span>").addClass("mapped-value");

    mappedColumn.append($("<a>").text(""))
        .append($("<span>").addClass("type-id").text("()"))
        .append($("<a>").addClass("edit-mapped-value").text("edit")
            .on('click', function() {
              $input = $(this).parent().siblings('input[name="property"]');
              $input.removeData('data.suggest');
              $label = $(this).parent().parent().find('.mapped-value > a:not(.edit-mapped-value)');
              $input.val($label.text()).prop('disabled',false);
              mappedColumn.toggle();
              $input.trigger('focus');
            }));

    $(td1).append(mappedColumn)
        .append(
            $("<input>")
                .attr("size", "25")
                .attr("name", "property")
                .attr("spellcheck", "false")
                .data('columnName',column.name)
        );
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
  var input = this._elmts.typeInput.off();

  if ("suggest" in this._service && "type" in this._service.suggest && this._service.suggest.type.service_url) {
    // Old style suggest API
    var suggestOptions = $.extend({}, this._service.suggest.type);
    suggestOptions.key = null;
    suggestOptions.query_param_name = "prefix";
    // CORS/JSONP support
    if (this._service.ui && this._service.ui.access) {
      suggestOptions.access = this._service.ui.access;
    }
    input.suggestT(sanitizeSuggestOptions(suggestOptions)).on("fb-select", function(e, data) {
      let $input = $(e.currentTarget);
      let $td = $input.parent();
      let mapping = $input.data('data.suggest');
      $td.children('.mapped-value').css('display', 'inline-flex');
      $input.val('').prop('disabled', true);
      $td.find('.mapped-value > a:not(.edit-mapped-value)').text(mapping.name);
      $td.find('.mapped-value > .type-id').text("(" + mapping.id + ")");
    });
  }

  input.on("bind fb-select", function(e, data) {
    self._panel
    .find('input[name="type-choice"][value=""]')
    .prop('checked', true);

    self._rewirePropertySuggests(data.id);
  });

  this._rewirePropertySuggests((this._types.length > 0) ? this._types[0] : null);
};

ReconStandardServicePanel.prototype._rewirePropertySuggests = function(type) {
  var inputs = this._panel
  .find('input[name="property"]')
  .off();
  if ("suggest" in this._service && "property" in this._service.suggest && this._service.suggest.property.service_url) {
    // Old style suggest API
    var suggestOptions = $.extend({}, this._service.suggest.property);
    suggestOptions.key = null;
    suggestOptions.query_param_name = "prefix";
    // CORS/JSONP support
    if (this._service.ui && this._service.ui.access) {
      suggestOptions.access = this._service.ui.access;
    }
    if (type) {
      suggestOptions.type = typeof type == "string" ? type : type.id;
    }
    inputs.suggestP(sanitizeSuggestOptions(suggestOptions)).on("fb-select", function(e, data) {
      let $input = $(e.currentTarget);
      let $td = $input.parent();
      let mapping = $input.data('data.suggest');
      $td.children('.mapped-value').css('display', 'inline-flex');
      $td.children('input[name="property"]').val('').prop('disabled', true);
      $td.find('.mapped-value > a:not(.edit-mapped-value)').text(mapping.name);
      $td.find('.mapped-value > .type-id').text("(" + mapping.id + ")");
    });
    }
};

ReconStandardServicePanel.prototype.start = function() {
  let self = this;
  let invalidState = false;

  let type = this._elmts.typeInput.data("data.suggest");
  let hasSuggest = type && type.id && type.name;
  if (!hasSuggest) {
    let value = jQueryTrim(this._elmts.typeInput.val());
    let hasValue = value && value.length > 0;
    if (hasValue) {
      alert('Reconcile against type "'+value+'" is not mapped.');
      invalidState = true;
    }
  }

  let choices = this._panel.find('input[name="type-choice"]:checked');
  if (choices !== null && choices.length > 0) {
    if (choices[0].value == '-') { // TODO: This is the signal value for "no type", but don't think it's used anymore
      type = null;
    } else if (choices[0].value != "") {
      type = {
          id: choices[0].value,
          name: choices.attr("typeName")
      };
    }
  }

  let columnDetails = [];

  $.each(
    this._panel.find('input[name="property"]'),
    function(index) {
      let property = $(this).data("data.suggest");
      let hasSuggest = property && property.id && property.name;
      if (hasSuggest) {
        columnDetails.push({
          column: $(this).data("columnName"),
          property: {
            id: property.id,
            name: property.name
          }
        });
      } else {
        let value = jQueryTrim(this.value);
        let hasValue = value && value.length > 0;
        if (hasValue) {
          alert('Column '+$(this).data("columnName")+' is not mapped.');
          invalidState = true;
        }
      }
    }
  )

  if (invalidState) {
    return false;
  }

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
        batchSize: (Number.isInteger(this._service.batchSize) && this._service.batchSize > 0) ? this._service.batchSize : 10,
        columnDetails: columnDetails,
        limit: parseInt(this._elmts.maxCandidates[0].value) || 0
      })
    },
    { cellsChanged: true, columnStatsChanged: true }
  );

  return true;
};

ReconStandardServicePanel.prototype.showBusyReconciling = function(message) {
  var frame = document.getElementsByClassName("type-container")[0];

  var body = $('<div>').attr('id', 'loading-message').appendTo(frame);
  $('<img>').attr("src", "images/large-spinner.gif").appendTo(body);

  return function() {
    $(body).remove()
  };
};
