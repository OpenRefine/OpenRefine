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

SchemaAlignmentDialog.UILink = function(dialog, link, table, options, parentUINode) {
  this._dialog = dialog;
  this._link = link;
  this._options = options;
  this._parentUINode = parentUINode;

  // Make sure target node is there
  this._link.target = this._link.target || { nodeType: "cell-as-value" }

  this._tr = table.insertRow(table.rows.length);
  this._tdMain = this._tr.insertCell(0);
  this._tdToggle = this._tr.insertCell(1);
  this._tdDetails = this._tr.insertCell(2);

  $(this._tdMain).addClass("schema-alignment-link-main").attr("width", "250").addClass("padded");
  $(this._tdToggle).addClass("schema-alignment-link-toggle").attr("width", "1%").addClass("padded");
  $(this._tdDetails).addClass("schema-alignment-link-details").attr("width", "90%");

  this._collapsedDetailDiv = $('<div></div>').appendTo(this._tdDetails).addClass("padded").html("...");
  this._expandedDetailDiv = $('<div></div>').appendTo(this._tdDetails).addClass("schema-alignment-detail-container");
  var self = this;
  var show = function() {
    if (self._options.expanded) {
      self._collapsedDetailDiv.hide();
      self._expandedDetailDiv.show();
    } else {
      self._collapsedDetailDiv.show();
      self._expandedDetailDiv.hide();
    }
  };
  show();

  $(this._tdToggle).html("&nbsp;");
  $('<img />')
  .attr("src", this._options.expanded ? "images/expanded.png" : "images/collapsed.png")
  .appendTo(this._tdToggle)
  .click(function() {
    self._options.expanded = !self._options.expanded;

    $(this).attr("src", self._options.expanded ? "images/expanded.png" : "images/collapsed.png");

    show();
  });

  this._renderMain();
  this._renderDetails();
};

SchemaAlignmentDialog.UILink.prototype._renderMain = function() {
  $(this._tdMain).empty();

  var label = this._link.property !== null ? 
      (this._link.property.id + ((this._link.condition) ? " [?]" : "")) : 
      "property?";

  var self = this;

  $('<img />')
  .attr("title", "remove property")
  .attr("src", "images/close.png")
  .css("cursor", "pointer")
  .prependTo(this._tdMain)
  .click(function() {
    window.setTimeout(function() {
      self._parentUINode.removeLink(self);
      self._tr.parentNode.removeChild(self._tr);
      self._dialog.preview();
    }, 100);
  });

  var a = $('<a href="javascript:{}"></a>')
  .addClass("schema-alignment-link-tag")
  .html(label)
  .appendTo(this._tdMain)
  .click(function(evt) {
    self._startEditProperty(this);
  });

  $('<img />').attr("src", "images/arrow-start.png").prependTo(a);
  $('<img />').attr("src", "images/arrow-end.png").appendTo(a);
};

SchemaAlignmentDialog.UILink.prototype._renderDetails = function() {
  if (this._targetUI) {
    this._targetUI.dispose();
  }
  if (this._tableDetails) {
    this._tableDetails.remove();
  }

  this._tableDetails = $('<table></table>').addClass("schema-alignment-table-layout").appendTo(this._expandedDetailDiv);
  this._targetUI = new SchemaAlignmentDialog.UINode(
      this._dialog,
      this._link.target, 
      this._tableDetails[0], 
      { expanded: "links" in this._link.target && this._link.target.links.length > 0 });
};

SchemaAlignmentDialog.UILink.prototype._startEditProperty = function(elmt) {
  var sourceTypeID = this._parentUINode.getExpectedType();
  var targetNode = this._targetUI._node;
  var targetTypeID = "type" in targetNode && targetNode.type !== null ? targetNode.type.id : null;
  var targetTypeName = "columnNames" in targetNode ? targetNode.columnNames[0] : null;

  if (sourceTypeID !== null) {
    var self = this;
    var dismissBusy = DialogSystem.showBusy();

    var instanceCount = 0;
    var outgoing = [];
    var incoming = [];

    var onDone = function(properties) {
      dismissBusy();

      self._showPropertySuggestPopup(
          elmt, 
          properties
      );
    };

    SchemaAlignmentDialog.UILink._getPropertiesOfType(
        sourceTypeID,
        targetTypeID,
        targetTypeName,
        onDone
    );
  } else {
    this._showPropertySuggestPopup(elmt, []);
  }
};

SchemaAlignmentDialog.UILink._getPropertiesOfType = function(typeID, targetTypeID, targetTypeName, onDone) {
  var done = false;

  var params = {
      "type" : typeID
  };
  if (targetTypeID !== null) {
    params.expects = targetTypeID;
  } else if (targetTypeName !== null) {
    params.expects = targetTypeName;
  }

  $.getJSON(
    Refine.refineHelperService + "/get_properties_of_type?" + $.param(params) + "&callback=?",
    null,
    function(data) {
      if (done) return;

      done = true;
      onDone(data.properties || []);
    }
  );

  window.setTimeout(function() {
    if (done) return;

    done = true;
    onDone([]);
  }, 7000); // time to give up?
};

SchemaAlignmentDialog.UILink.prototype._showPropertySuggestPopup = function(elmt, suggestions) {
  self = this;

  var menu = MenuSystem.createMenu().width("350px");

  var commitProperty = function(p) {
    window.setTimeout(function() { MenuSystem.dismissAll(); }, 100);

    if ("id2" in p) {
      // self._targetUI.dispose();
      self._link.property = {
        id: p.id,
        name: p.name
      };
      self._link.target = {
        nodeType: "anonymous",
        links: [{
          property: {
            id: p.id2,
            name: p.name2
          },
          target: self._link.target
        }]
      };

      self._renderDetails();
    } else {
      self._link.property = {
          id: p.id,
          name: p.name
      };
    }

    var conditionColumnName = conditionalSelect[0].value;
    if (conditionColumnName != "") {
      self._link.condition = { columnName: conditionColumnName };
    } else {
      delete self._link.condition;
    }

    self._configureTarget();
  };

  var divConditional = $('<div>')
  .addClass("schema-alignment-link-menu-section")
  .html("Assert link when 'true' is found in column<br/>").appendTo(menu);

  var conditionalSelect = $('<select>').appendTo(divConditional);
  $('<option>')
  .text("(always assert)")
  .attr("value", "")
  .attr("name", "schema-alignment-link-menu-condition")
  .appendTo(conditionalSelect);

  for (var c = 0; c < theProject.columnModel.columns.length; c++) {
    var column = theProject.columnModel.columns[c];
    var option = $('<option>')
    .text(column.name)
    .attr("value", column.name)
    .attr("name", "schema-alignment-link-menu-condition")
    .appendTo(conditionalSelect);

    if ((self._link.condition) && column.name == self._link.condition.columnName) {
      option.attr("selected", "true");
    }
  }

  var divSearch;
  if (suggestions.length > 0) {
    divSearch = $('<div>')
    .addClass("schema-alignment-link-menu-section")
    .css("margin-bottom", "2em")
    .html('<div>Search for a property or pick one below</div>').appendTo(menu);

    var createSuggestion = function(suggestion) {
      var menuItem = MenuSystem.createMenuItem().appendTo(menu);

      $('<span>')
      .text(suggestion.name)
      .attr("title", suggestion.id)
      .appendTo(menuItem);

      if ("name2" in suggestion) {
        $('<span>').html(" &raquo; ").appendTo(menuItem);

        $('<span>')
        .text(suggestion.name2)
        .attr("title", suggestion.id2)
        .appendTo(menuItem);
      }

      menuItem.click(function() {
        commitProperty(suggestion);
      });
    };

    for (var i = 0; i < suggestions.length && i < 10; i++) {
      createSuggestion(suggestions[i]);
    }
  } else {
    divSearch = $('<div>')
    .addClass("schema-alignment-link-menu-section-last")
    .html('<div>Search for a property</div>').appendTo(menu);
  }
  var input = $('<input />').appendTo($('<div>').appendTo(divSearch));

  MenuSystem.showMenu(menu, function(){});
  MenuSystem.positionMenuAboveBelow(menu, $(elmt));

  var suggestOptions = {
      type : '/type/property'
  };
  var sourceTypeID = this._parentUINode.getExpectedType();
  if (sourceTypeID !== null) {
    suggestOptions.ac_param = { schema: sourceTypeID };
  }
  input.suggestP(suggestOptions).bind("fb-select", function(e, data) { commitProperty(data); });

  input[0].focus();
};

SchemaAlignmentDialog.UILink.prototype.getJSON = function() {
  if ("property" in this._link && this._link.property !== null &&
      "target" in this._link && this._link.target !== null) {

    var targetJSON = this._targetUI.getJSON();
    if (targetJSON !== null) {
      var json = {
          property: cloneDeep(this._link.property),
          target: targetJSON
      };
      if (this._link.condition) {
        json.condition = cloneDeep(this._link.condition);
      }
      return json;
    }
  }
  return null;
};

SchemaAlignmentDialog.UILink.prototype._configureTarget = function() {
  var self = this;
  var dismissBusy = DialogSystem.showBusy();

  $.getJSON(
    "http://api.freebase.com/api/service/mqlread?query=" + JSON.stringify({
      query: {
        "id" : this._link.property.id,
        "type" : "/type/property",
        "expected_type" : {
          "id" : null,
          "name" : null,
          "/freebase/type_hints/mediator" : null
        }
      }
    }) + "&callback=?",
    null,
    function(o) {
      dismissBusy();

      if ("result" in o) {
        var expected_type = o.result.expected_type;
        self._link.target.type = {
            id: expected_type.id,
            name: expected_type.name
        };
        if (expected_type["/freebase/type_hints/mediator"] === true) {
          self._link.target.nodeType = "anonymous";
        } else if (expected_type.id == "/type/key") {
          self._link.target.nodeType = "cell-as-key";
        } else if (expected_type.id.match(/^\/type\//)) {
          self._link.target.nodeType = "cell-as-value";
        } else if (!("topic" in self._link.target)) {
          self._link.target.nodeType = "cell-as-topic";
          self._link.target.createForNoReconMatch = true;
        }

        self._targetUI.render();
      }

      self._renderMain();
      self._dialog.preview();
    },
    "jsonp"
  );
};
