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

var SchemaAlignment = {};

SchemaAlignment.autoAlign = function() {
  var protograph = {};

  var columns = theProject.columnModel.columns;

  var typedCandidates = [];
  var candidates = [];

  for (var c = 0; c < columns.length; c++) {
    var column = columns[c];
    var typed = (column.reconConfig) && 
    ReconciliationManager.isFreebaseIdOrMid(column.reconConfig.identifierSpace) &&
    ReconciliationManager.isFreebaseId(column.reconConfig.schemaSpace);

    var candidate = {
      status: "unbound",
      typed: typed,
      index: c,
      column: column
    };

    candidates.push(candidate);
    if (typed) {
      typedCandidates.push(candidate);
    }
  }

  if (typedCandidates.length > 0) {

  } else {
    var queries = {};
    for (var i = 0; i < candidates.length; i++) {
      var candidate = candidates[i];
      var name = SchemaAlignment._cleanName(candidate.column.name);
      var key = "t" + i + ":search";
      queries[key] = {
        "query" : name,
        "limit" : 10,
        "type" : "/type/type,/type/property",
        "type_strict" : "any"
      };
    }

    SchemaAlignment._batchSearch(queries, function(result) {
      console.log(result);
    });
  }
};

SchemaAlignment._batchSearch = function(queries, onDone) {
  var keys = [];
  for (var n in queries) {
    if (queries.hasOwnProperty(n)) {
      keys.push(n);
    }
  }

  var result = {};
  var args = [];
  var makeBatch = function(keyBatch) {
    var batch = {};
    for (var k = 0; k < keyBatch.length; k++) {
      var key = keyBatch[k];
      batch[key] = queries[key];
    }

    args.push("http://api.freebase.com/api/service/search?" + 
        $.param({ "queries" : JSON.stringify(batch) }) + "&callback=?");

    args.push(null); // no data
    args.push(function(data) {
      for (var k = 0; k < keyBatch.length; k++) {
        var key = keyBatch[k];
        result[key] = data[key];
      }
    });
  };

  for (var i = 0; i < keys.length; i += 10) {
    makeBatch(keys.slice(i, i + 10));
  }

  args.push(function() {
    onDone(result);
  });

  Ajax.chainGetJSON.apply(null, args);
};

SchemaAlignment._cleanName = function(s) {
  return s.replace(/\W/g, " ").replace(/\s+/g, " ").toLowerCase();
};

SchemaAlignment.createNewRootNode = function() {
  var rootNode = null;
  var links = [];
  var columns = theProject.columnModel.columns;
  for (var i = 0; i < columns.length; i++) {
    var column = columns[i];
    var target = {
        nodeType: "cell-as-topic",
        columnName: column.name,
        createForNoReconMatch: true
    };
    if ((column.reconConfig) && 
        ReconciliationManager.isFreebaseIdOrMid(column.reconConfig.identifierSpace) &&
        ReconciliationManager.isFreebaseId(column.reconConfig.schemaSpace) &&
        (column.reconConfig.type)) {

      target.type = {
        id: column.reconConfig.type.id,
        name: column.reconConfig.type.name
      };
    }

    if (column.name == theProject.columnModel.keyColumnName) {
      rootNode = target;
    } else {
      links.push({
        property: null,
        target: target
      });
    }
  }

  rootNode = rootNode || { nodeType: "cell-as-topic" };
  rootNode.links = links;

  return rootNode;
};

var SchemaAlignmentDialog = {};

SchemaAlignmentDialog.launch = function(onDone) {
  this._onDone = onDone;
  this._hasUnsavedChanges = false;

  this._createDialog();
  this._reset(theProject.overlayModels.wikibaseSchema, true);
}

SchemaAlignment

SchemaAlignmentDialog._reset = function(schema, initial) {
  this._originalSchema = schema || { changes: [] };
  this._schema = cloneDeep(this._originalSchema); // this is what can be munched on

  $('#schema-alignment-statements-container').empty();

  /*
  this._nodeUIs = [];
  for (var i = 0; i < this._protograph.rootNodes.length; i++) {
    this._nodeUIs.push(new SchemaAlignmentDialog.UINode(
      this,
      this._protograph.rootNodes[i], 
      this._nodeTable, 
      {
        expanded: true,
        mustBeCellTopic: true
      }
    ));
  }*/
  // TODO

  if (!this._schema.changes.length) {
    // this._addItem();
  }
  // this.preview(initial);
};

SchemaAlignmentDialog._save = function(onDone) {
  var self = this;
  var schema = this.getJSON();
  console.log(schema);

  Refine.postProcess(
    "wikidata",
    "save-wikibase-schema",
    {},
    { schema: JSON.stringify(schema) },
    {},
    {   
      onDone: function() {
        theProject.overlayModels.wikibaseSchema = schema;

        self._elmts.statusIndicator.hide();
        self._hasUnsavedChanges = false;

        if (onDone) onDone();
      }
    }
  );
};

SchemaAlignmentDialog._createDialog = function() {
  var self = this;
  var frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/schema-alignment-dialog.html"));
  var elmts = this._elmts = DOM.bind(frame);

  this._level = DialogSystem.showDialog(frame);
  this._wikibasePrefix = "http://www.wikidata.org/entity/"; // hardcoded for now

  // Init the column area
  var columns = theProject.columnModel.columns;
  this._columnArea = $(".schema-alignment-dialog-columns-area");
  for (var i = 0; i < columns.length; i++) {
     var column = columns[i];
     var reconConfig = column.reconConfig;
     var cell = $("<div></div>").addClass('wbs-draggable-column').text(columns[i].name);
     console.log(column.reconStats);
     if (reconConfig && reconConfig.identifierSpace === this._wikibasePrefix && column.reconStats) {
         cell.addClass('wbs-reconciled-column');
     } else {
         cell.addClass('wbs-unreconciled-column');
     }
     this._columnArea.append(cell);
  }

  $('.wbs-reconciled-column').draggable({
     helper: "clone",
     cursor: "crosshair",
     snap: ".wbs-item-input input, .wbs-target-input input",
  });
  $('.wbs-unreconciled-column').draggable({
     helper: "clone",
     cursor: "crosshair",
     snap: ".wbs-target-input input",
  });


  var dismiss = function() {
    DialogSystem.dismissUntil(self._level - 1);
  };

  elmts.saveButton.click(function() {
    self._save();
  });
  elmts.resetButton.click(function() {
    self._reset(null);
  });
  elmts.closeButton.click(function() {
    if (!self._hasUnsavedChanges || window.confirm("There are unsaved changes. Close anyway?")) {
      dismiss();
    }
  });

  elmts.addItemButton.click(function() {
    self._addItem();
  });

  $("#schema-alignment-tabs").tabs();

  this._previewPanes = $(".schema-alignment-dialog-preview");

  this._canvas = $(".schema-alignment-dialog-canvas");
  this._nodeTable = $('<table></table>').addClass("schema-alignment-table-layout").appendTo(this._canvas)[0];

  var url = ReconciliationManager.ensureDefaultServicePresent();
  SchemaAlignmentDialog._reconService = ReconciliationManager.getServiceFromUrl(url);
};

SchemaAlignmentDialog._addItem = function() {
  var item = $('<div></div>').addClass('wbs-item');
  var inputContainer = $('<div></div>').addClass('wbs-item-input').appendTo(item);
  SchemaAlignmentDialog._initField(inputContainer, "item");
  var right = $('<div></div>').addClass('wbs-right').appendTo(item);
  $('<div></div>').addClass('wbs-statement-group-container').appendTo(right);
  var toolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
  $('<a></a>').addClass('wbs-add-statement-group').text('add statement').click(function() {
     SchemaAlignmentDialog._addStatementGroup(item);
  }).appendTo(toolbar);

  SchemaAlignmentDialog._addStatementGroup(item);
  $('#schema-alignment-statements-container').append(item);
}

SchemaAlignmentDialog._itemToJSON = function (item) {
    var lst = new Array();
    item.find('.wbs-statement-group').each(function () {
        lst.push(SchemaAlignmentDialog._statementGroupToJSON($(this)));
    });
    var inputContainer = item.find(".wbs-item-input").first();
    return {subject: SchemaAlignmentDialog._inputContainerToJSON(inputContainer),
            statementGroups: lst}; 
};

SchemaAlignmentDialog._addStatementGroup = function(item) {
  var container = item.find('.wbs-statement-group-container').first();
  var statementGroup = $('<div></div>').addClass('wbs-statement-group');
  var inputContainer = $('<div></div>').addClass('wbs-prop-input').appendTo(statementGroup);
  SchemaAlignmentDialog._initField(inputContainer, "property");
  var right = $('<div></div>').addClass('wbs-right').appendTo(statementGroup);
  $('<div></div>').addClass('wbs-statement-container').appendTo(right);
  var toolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
  $('<a></a>').addClass('wbs-add-statement').text('add value').click(function() {
     SchemaAlignmentDialog._addStatement(statementGroup);
  }).appendTo(toolbar);
  container.append(statementGroup);
  SchemaAlignmentDialog._addStatement(statementGroup);
}

SchemaAlignmentDialog._statementGroupToJSON = function (statementGroup) {
    var lst = new Array();
    statementGroup.find('.wbs-statement').each(function () {
    lst.push(SchemaAlignmentDialog._statementToJSON($(this)));
    });
    var inputContainer = statementGroup.find(".wbs-prop-input").first();
    return {property: SchemaAlignmentDialog._inputContainerToJSON(inputContainer),
            statements: lst};
};


SchemaAlignmentDialog._addStatement = function(statementGroup) {
  var container = statementGroup.find('.wbs-statement-container').first();
  var statement = $('<div></div>').addClass('wbs-statement');
  var toolbar1 = $('<div></div>').addClass('wbs-toolbar').appendTo(statement);
  $('<img src="images/close.png" />').attr('alt', 'remove statement').click(function() {
     SchemaAlignmentDialog._removeStatement(statement);
  }).appendTo(toolbar1);
  var inputContainer = $('<div></div>').addClass('wbs-target-input').appendTo(statement);
  SchemaAlignmentDialog._initField(inputContainer, "target");
  var right = $('<div></div>').addClass('wbs-right').appendTo(statement);
  $('<div></div>').addClass('wbs-qualifier-container').appendTo(right);
  var toolbar2 = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
  $('<a></a>').addClass('wbs-add-qualifier').text('add qualifier').appendTo(toolbar2);
  container.append(statement);
}

SchemaAlignmentDialog._statementToJSON = function (statement) {
    var inputContainer = statement.find(".wbs-target-input").first();
    return {
        value:SchemaAlignmentDialog._inputContainerToJSON(inputContainer),
        qualifiers:[],
    };
};

SchemaAlignmentDialog._initField = function(inputContainer, mode) {
  var input = $('<input></input>').appendTo(inputContainer);

  if (this._reconService !== null) {
    var endpoint = null;
    if (mode === "item" || mode === "target") {
      endpoint = this._reconService.suggest.entity;
    } else if (mode === "property") {
      endpoint = this._reconService.suggest.property;
    }
    var suggestConfig = $.extend({}, endpoint);
    suggestConfig.key = null;
    suggestConfig.query_param_name = "prefix";
    

    input.suggestP(suggestConfig).bind("fb-select", function(evt, data) {
        if (mode ===  "item") {
            inputContainer.data("jsonValue", {
                type : "wbitemconstant",
                qid : data.id,
                label: data.name,
            });
        } else if (mode === "property") {
            inputContainer.data("jsonValue", {
                type : "wbpropconstant",
                pid : data.id,
                label: data.name,
            });
        }
    });
  }

  // If it isn't a property, make it droppable
  if (mode !== "property") {
    var acceptClass = ".wbs-draggable-column";
    if (mode === "item") {
        acceptClass = ".wbs-reconciled-column";
    }
        
    inputContainer.droppable({
        accept: acceptClass,
    }).on("drop", function (evt, ui) {
        input.hide();
        var columnDiv = $('<div></div>').appendTo(inputContainer);
        var column = ui.draggable.clone().appendTo(columnDiv);
        var deleteButton = $('&nbsp;<img src="images/close.png" />').addClass('wbs-delete-column-button').appendTo(column);
        deleteButton.attr('alt', 'remove column');
        deleteButton.click(function () {
            columnDiv.remove();
            input.show();
        });
        inputContainer.data("jsonValue", {
            type : "wbitemvariable",
            columnName: ui.draggable.text(),
        });
        return true; 
    }).on("dropactivate", function(evt, ui) {
        input.addClass("wbs-accepting-input");
    }).on("dropdeactivate", function(evt, ui) {
        input.removeClass("wbs-accepting-input");
    });
  }
}

SchemaAlignmentDialog._inputContainerToJSON = function (inputContainer) {
    return inputContainer.data().jsonValue;
};

SchemaAlignmentDialog._removeStatement = function(statement) {
  var statementGroup = statement.parents('.wbs-statement-group').first();
  statement.remove();
  var remainingStatements = statementGroup.find('.wbs-statement').length;
  if (remainingStatements === 0) {
      statementGroup.remove();
  }
}
/*
SchemaAlignmentDialog._addStatement = function() {
  var newStatement = $('<div></div>').addClass('schema-alignment-statement');
  var subject = $('<div></div>').addClass('schema-alignment-subject').appendTo(newStatement);
  var prop = $('<div></div>').addClass('schema-alignment-prop').appendTo(newStatement);
  var target = $('<div></div>').addClass('schema-alignment-target').appendTo(newStatement);
  var qualifiersArea = $('<div></div>').addClass('schema-alignment-qualifiers').appendTo(newStatement);
  var addQualifier = $('<p></p>').addClass('schema-alignment-add-qualifier').text('Add qualifier').appendTo(newStatement);
  $('#schema-alignment-statements-container').append(newStatement);
}
*/

SchemaAlignmentDialog.getJSON = function() {
  var list = new Array();
  $('.wbs-item').each(function () {
     list.push(SchemaAlignmentDialog._itemToJSON($(this)));
  });
  return {
        'changes': list,
        'wikibasePrefix': this._wikibasePrefix,
  };
};

SchemaAlignmentDialog.preview = function(initial) {
  var self = this;

  this._previewPanes.empty();
  if (!(initial)) {
    this._elmts.statusIndicator.show().text("There are unsaved changes.");
    this._hasUnsavedChanges = true;
  }

  var protograph = this.getJSON();
  $.post(
    "command/freebase/preview-protograph?" + $.param({ project: theProject.id }),
    { protograph: JSON.stringify(protograph), engine: JSON.stringify(ui.browsingEngine.getJSON()) },
    function(data) {
      if ("mqllike" in data) {
        $(self._previewPanes[0]).text(JSON.stringify(data.mqllike, null, 2));
      }
      if ("tripleloader" in data) {
        $(self._previewPanes[1]).text(data.tripleloader);
      }
    },
    "json"
  );
};

SchemaAlignmentDialog._findColumn = function(cellIndex) {
  var columns = theProject.columnModel.columns;
  for (var i = 0; i < columns.length; i++) {
    var column = columns[i];
    if (column.cellIndex == cellIndex) {
      return column;
    }
  }
  return null;
};
