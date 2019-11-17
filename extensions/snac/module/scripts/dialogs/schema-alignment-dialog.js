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

SchemaAlignment._cleanName = function(s) {
  return s.replace(/\W/g, " ").replace(/\s+/g, " ").toLowerCase();
};

var SNACSchemaAlignmentDialog = {};

/**
 * Installs the tabs in the UI the first time the snac
 * extension is called.
 */
SNACSchemaAlignmentDialog.setUpTabs = function() {
  var self = this;
  this._rightPanel = $('#right-panel');
  this._viewPanel = $('#view-panel').addClass('main-view-panel-tab');
  this._toolPanel = $('#tool-panel');
  this._summaryBar = $('#summary-bar')
        .addClass('main-view-panel-tab-header')
        .addClass('active')
        .attr('href', '#view-panel');

  this._schemaPanel = $('<div id="snac-schema-panel"></div>')
        .addClass('main-view-panel-tab')
        .appendTo(this._rightPanel);
  this._issuesPanel = $('<div id="snac-issues-panel"></div>')
        .addClass('main-view-panel-tab')
        .appendTo(this._rightPanel);
  this._previewPanel = $('<div id="snac-preview-panel"></div>')
        .addClass('main-view-panel-tab')
        .appendTo(this._rightPanel);

  var schemaButton = $('<div></div>')
        .addClass('main-view-panel-tab-header')
        .attr('href', '#snac-schema-panel')
        .text($.i18n('snac-schema/schema-tab-header'))
        .appendTo(this._toolPanel);
  var issuesButton = $('<div></div>')
        .addClass('main-view-panel-tab-header')
        .attr('href', '#snac-issues-panel')
        .text($.i18n('snac-schema/warnings-tab-header')+' ')
        .appendTo(this._toolPanel);
  this.issuesTabCount = $('<span></span>')
        .addClass('schema-alignment-total-warning-count')
        .appendTo(issuesButton)
        .hide();
  this.issueSpinner = $('<img />')
        .attr('src', 'images/large-spinner.gif')
        .attr('width', '16px')
        .appendTo(issuesButton);
  var previewButton = $('<div></div>')
        .addClass('main-view-panel-tab-header')
        .attr('href', '#snac-preview-panel')
        .text($.i18n('snac-schema/edits-preview-tab-header'))
        .appendTo(this._toolPanel);
  this.previewSpinner = $('<img />')
        .attr('src', 'images/large-spinner.gif')
        .attr('width', '16px')
        .appendTo(previewButton);

  this._unsavedIndicator = $('<span></span>')
        .html('&nbsp;*')
        .attr('title', $.i18n('snac-schema/unsaved-changes-alt'))
        .hide()
        .appendTo(schemaButton);
  $('.main-view-panel-tab-header').click(function(e) {
     var targetTab = $(this).attr('href');
     SNACSchemaAlignmentDialog.switchTab(targetTab);
     e.preventDefault();
  });

  /**
   * Init the schema tab
   */
  var schemaTab = $(DOM.loadHTML("snac", "scripts/schema-alignment-tab.html")).appendTo(this._schemaPanel);
  var schemaElmts = this._schemaElmts = DOM.bind(schemaTab);
  schemaElmts.dialogExplanation.text($.i18n('snac-schema/dialog-explanation'));
  this._plusButton($.i18n('snac-schema/add-item-button'), schemaElmts.addItemButton);
  schemaElmts.addItemButton.click(function(e) {
    self._addItem();
    SNACSchemaAlignmentDialog._hasChanged();
    e.preventDefault();
  });
  schemaElmts.saveButton
        .text($.i18n('snac-schema/save-button'))
        .attr('title', $.i18n('snac-schema/save-schema-alt'))
        .prop('disabled', true)
        .addClass('disabled')
        .click(function() { SNACSchemaAlignmentDialog._save(); });
  schemaElmts.discardButton
        .text($.i18n('snac-schema/discard-button'))
        .attr('title', $.i18n('snac-schema/discard-schema-changes-alt'))
        .prop('disabled', true)
        .addClass('disabled')
        .click(function() { SNACSchemaAlignmentDialog._discardChanges(); });

  this._wikibasePrefix = "http://www.snac.org/entity/"; // hardcoded for now

  // Init the column area
  this.updateColumns();

  var url = ReconciliationManager.ensureDefaultServicePresent();
  SNACSchemaAlignmentDialog._reconService = ReconciliationManager.getServiceFromUrl(url);

   /**
   * Init the dropdowns area
   */
  // this.addDropdowns();

  /**
   * Init the issues tab
   */
  var issuesTab = $(DOM.loadHTML("snac", "scripts/issues-tab.html")).appendTo(this._issuesPanel);
  var issuesElmts = this._issuesElmts = DOM.bind(issuesTab);
  issuesElmts.invalidSchemaWarningIssues.text($.i18n('snac-schema/invalid-schema-warning-issues'));

  
  /**
   * Init the preview tab
   */
  var previewTab = $(DOM.loadHTML("snac", "scripts/preview-tab.html")).appendTo(this._previewPanel);
  var previewElmts = this._previewElmts = DOM.bind(previewTab);
  SNACSchemaAlignmentDialog.updateNbEdits(0);
  previewElmts.invalidSchemaWarningPreview.text($.i18n('snac-schema/invalid-schema-warning-preview'));

  this._previewPanes = $(".schema-alignment-dialog-preview");

  // Load the existing schema
  this._reset(theProject.overlayModels.wikibaseSchema);
  // Perform initial preview
  this.preview();
}

SNACSchemaAlignmentDialog.updateColumns = function() {
  var columns = theProject.columnModel.columns;
  this._columnArea = $(".schema-alignment-dialog-columns-area");
  this._columnArea.addClass("snac-tab");
  this._columnArea.empty();

  var SNACcolumns = ["ID", "Type", "Title", "Display Entry", "Link", "Abstract", "Extent", "Date", "Language", "Holding Repository SNAC ID", "Note"];
  this._dropdownArea = $(".schema-alignment-dialog-dropdown-area");
  this._dropdownArea.addClass("snac-tab");
  this._refcolumnArea = $(".schema-alignment-dialog-columns-area--ref");
  this._refcolumnArea.addClass("snac-tab");
  //this._refcolumnArea.empty();
  /*var cell = SNACSchemaAlignmentDialog._createDraggableColumn("title",
    reconConfig && reconConfig.identifierSpace === this._wikibasePrefix && column.reconStats, "csv");
  var cell = $("<div></div>").addClass('wbs-draggable-column').addClass('wbs-title').text("CSV Columns");*/

  for (var i = 0; i < columns.length; i++) {
     var column = columns[i];
     var reconConfig = column.reconConfig;
     var cell = SNACSchemaAlignmentDialog._createDraggableColumn(column.name,
        reconConfig && reconConfig.identifierSpace === this._wikibasePrefix && column.reconStats);
     this._columnArea.append(cell);

     var selectList = $("<select></select>").addClass('selectColumn');
     this._dropdownArea.append(selectList);

     var defaultoption = document.createElement("option");
      defaultoption.setAttribute("value", "");
      defaultoption.text = "Select an Option";
      defaultoption.classList.add("dropdown-default");
      selectList.append(defaultoption);

     //Create and append the options
     for (var j = 0; j < SNACcolumns.length; j++) {
        var option = document.createElement("option");
        option.setAttribute("value", SNACcolumns[j]);
        option.text = SNACcolumns[j];
        option.classList.add("dropdown-option");

        selectList.append(option);
     }
  }

  /*for (var i = 0; i < columns.length; i++) {
     var column = columns[i];
     var reconConfig = column.reconConfig;
     var cell = SNACSchemaAlignmentDialog._createDraggableColumn("RE",
        reconConfig && reconConfig.identifierSpace === this._wikibasePrefix && column.reconStats);
     this._refcolumnArea.append(cell);
  }*/

  $('.wbs-reconciled-column').draggable({
     helper: "clone",
     cursor: "crosshair",
     snap: ".wbs-item-input input, .wbs-target-input input",
     zIndex: 100,
  });
  $('.wbs-unreconciled-column').draggable({
     helper: "clone",
     cursor: "crosshair",
     snap: ".wbs-target-input input",
     zIndex: 100,
  });
}

// SNACSchemaAlignmentDialog.addDropdowns = function() {
//   var columns = theProject.columnModel.columns;
//   var SNACcolumns = ["ID", "Type", "Title", "Display Entry", "Link", "Abstract", "Extent", "Date", "Language", "Holding Repository SNAC ID", "Note"];
//   this._dropdownArea = $(".schema-alignment-dialog-dropdown-area");
//   this._dropdownArea.addClass("snac-tab");

//   for (var i = 0; i < columns.length; i++) {
//     //Create and append select list
//     var selectList = document.createElement("select");
//     selectList.setAttribute("id", "mySelect");
//     this._dropdownArea.appendChild(selectList);

//     //Create and append the options
//     for (var j = 0; j < SNACcolumns.length; j++) {
//         var option = document.createElement("option");
//         option.setAttribute("value", SNACcolumns[j]);
//         option.text = SNACcolumns[j];
//         selectList.append(option);
//   }
// }

SNACSchemaAlignmentDialog.switchTab = function(targetTab) {
  $('.main-view-panel-tab').hide();
  $('.main-view-panel-tab-header').removeClass('active');
  $('.main-view-panel-tab-header[href="'+targetTab+'"]').addClass('active');
  $(targetTab).show();
  resizeAll();
  var panelHeight = this._viewPanel.height();
  this._schemaPanel.height(panelHeight);
  this._issuesPanel.height(panelHeight);
  this._previewPanel.height(panelHeight);
  // Resize the inside of the schema panel
  var headerHeight = this._schemaElmts.schemaHeader.outerHeight();
  this._schemaElmts.canvas.height(panelHeight - headerHeight - 10);

  if (targetTab === "#view-panel") {
     ui.dataTableView.render();
  }
}

SNACSchemaAlignmentDialog.isSetUp = function() {
  return $('#snac-schema-panel').length !== 0;
}

SNACSchemaAlignmentDialog.launch = function(onDone) {
  this._onDone = onDone;
  this._hasUnsavedChanges = false;

  if (!SNACSchemaAlignmentDialog.isSetUp()) {
     SNACSchemaAlignmentDialog.setUpTabs();
  }
  SNACSchemaAlignmentDialog.switchTab('#snac-schema-panel');

  // this._createDialog();
}


var beforeUnload = function(e) {
  if (SNACSchemaAlignmentDialog.isSetUp() && SNACSchemaAlignmentDialog._hasUnsavedChanges === true) {
     return (e = $.i18n('snac-schema/unsaved-warning'));
  }
};

$(window).bind('beforeunload', beforeUnload);

SNACSchemaAlignmentDialog._reset = function(schema) {
  this._originalSchema = schema || { itemDocuments: [] };
  this._schema = cloneDeep(this._originalSchema); // this is what can be munched on
  this._copiedReference = null;

  $('#schema-alignment-statements-container').empty();

  if (this._schema && this._schema.itemDocuments) {
    for(var i = 0; i != this._schema.itemDocuments.length; i++) {
      this._addItem(this._schema.itemDocuments[i]);
    }
  }
};

SNACSchemaAlignmentDialog._save = function(onDone) {
  var self = this;
  var schema = this.getJSON();

  if (schema === null) {
    alert($.i18n('snac-schema/incomplete-schema-could-not-be-saved'));
  }

  Refine.postProcess(
    "snac",
    "save-wikibase-schema",
    {},
    { schema: JSON.stringify(schema) },
    {},
    {
      onDone: function() {
        theProject.overlayModels.wikibaseSchema = schema;

        $('.invalid-schema-warning').hide();
        self._changesCleared();

        if (onDone) onDone();
      },
      onError: function(e) {
        alert($.i18n('snac-schema/incomplete-schema-could-not-be-saved'));
      },
    }
  );
};

SNACSchemaAlignmentDialog._discardChanges = function() {
  this._reset(theProject.overlayModels.wikibaseSchema);
  this._changesCleared();
}

SNACSchemaAlignmentDialog._changesCleared = function() {
  this._hasUnsavedChanges = false;
  this._unsavedIndicator.hide();
  this._schemaElmts.saveButton
        .prop('disabled', true)
        .addClass('disabled');
  this._schemaElmts.discardButton
        .prop('disabled', true)
        .addClass('disabled');
}

//format cells for columns
SNACSchemaAlignmentDialog._createDraggableColumn = function(name, reconciled, org) {
  var cell = $("<div></div>").addClass('wbs-draggable-column').text(name);
  if (reconciled) {
    cell.addClass('wbs-reconciled-column');
  } else {
    cell.addClass('wbs-unreconciled-column');
  }
  // cell.addClass(org);

 // cell.addClass(columnType);
  return cell;
}

SNACSchemaAlignmentDialog._plusButton = function(label, element) {
   var plus = $('<b></b>').html('+&nbsp;').appendTo(element);
   var span = $('<span></span>').text(label)
        .appendTo(element);
}

SNACSchemaAlignmentDialog._makeDeleteButton = function (noText) {
  var button = $('<div></div>').addClass('wbs-remove').append(
     $('<span></span>').addClass('wbs-icon')
  );
  if(noText === undefined) {
     button.append(
     $('<span></span>').text($.i18n('snac-schema/remove')));
  }
  return button;
}

/**************/
/*** ITEMS ****/
/**************/

SNACSchemaAlignmentDialog._addItem = function(json) {
  var subject = null;
  var statementGroups = null;
  var nameDescs = null;
  if (json) {
     subject = json.subject;
     statementGroups = json.statementGroups;
     nameDescs = json.nameDescs;
  }

  var item = $('<div></div>').addClass('wbs-item');
  $('#schema-alignment-statements-container').append(item);
  var deleteToolbar = $('<div></div>').addClass('wbs-toolbar')
        .attr('style', 'margin-top: 10px')
        .appendTo(item);
  var deleteButton = SNACSchemaAlignmentDialog._makeDeleteButton()
        .appendTo(deleteToolbar)
        .click(function(e) {
     item.remove();
     SNACSchemaAlignmentDialog._hasChanged();
     e.preventDefault();
  });
  var inputContainer = $('<div></div>').addClass('wbs-item-input').appendTo(item);
  SNACSchemaAlignmentDialog._initField(inputContainer, "wikibase-item", subject);
  var right = $('<div></div>').addClass('wbs-item-contents').appendTo(item);

  // Terms
  $('<span></span>').addClass('wbs-namedesc-header')
       .text($.i18n('snac-schema/terms-header')).appendTo(right);
  $('<div></div>').addClass('wbs-namedesc-container')
        .attr('data-emptyplaceholder', $.i18n('snac-schema/empty-terms'))
        .appendTo(right);
  var termToolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
  var addNamedescButton = $('<a></a>').addClass('wbs-add-namedesc')
  .click(function(e) {
     SNACSchemaAlignmentDialog._addNameDesc(item, null);
     e.preventDefault();
  }).appendTo(termToolbar);
  SNACSchemaAlignmentDialog._plusButton(
         $.i18n('snac-schema/add-term'), addNamedescButton);

  // Clear the float
  $('<div></div>').attr('style', 'clear: right').appendTo(right);

  // Statements
  $('<div></div>').addClass('wbs-statements-header')
        .text($.i18n('snac-schema/statements-header')).appendTo(right);
  $('<div></div>').addClass('wbs-statement-group-container')
        .attr('data-emptyplaceholder', $.i18n('snac-schema/empty-statements'))
        .appendTo(right);
  var statementToolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
  var addStatementButton = $('<a></a>').addClass('wbs-add-statement-group')
        .click(function(e) {
     SNACSchemaAlignmentDialog._addStatementGroup(item, null);
     e.preventDefault();
  }).appendTo(statementToolbar);

  SNACSchemaAlignmentDialog._plusButton(
         $.i18n('snac-schema/add-statement'), addStatementButton);

  if (statementGroups) {
     for(var i = 0; i != statementGroups.length; i++) {
        SNACSchemaAlignmentDialog._addStatementGroup(item, statementGroups[i]);
     }
  }

  if (nameDescs) {
     for(var i = 0; i != nameDescs.length; i++) {
        SNACSchemaAlignmentDialog._addNameDesc(item, nameDescs[i]);
     }
  }
}

SNACSchemaAlignmentDialog._itemToJSON = function (item) {
    var statementGroupLst = new Array();
    var statementsDom = item.find('.wbs-statement-group');
    statementsDom.each(function () {
        var statementGroupJSON = SNACSchemaAlignmentDialog._statementGroupToJSON($(this));
        if (statementGroupJSON !== null) {
          statementGroupLst.push(statementGroupJSON);
        }
    });
    var nameDescLst = new Array();
    var nameDescsDom = item.find('.wbs-namedesc');
    nameDescsDom.each(function () {
        var nameDescJSON = SNACSchemaAlignmentDialog._nameDescToJSON($(this));
        if (nameDescJSON !== null) {
           nameDescLst.push(nameDescJSON);
        }
    });
    var inputContainer = item.find(".wbs-item-input").first();
    var subjectJSON = SNACSchemaAlignmentDialog._inputContainerToJSON(inputContainer);
    if (subjectJSON !== null &&
        statementGroupLst.length === statementsDom.length &&
        nameDescLst.length === nameDescsDom.length) {
      return {subject: subjectJSON,
            statementGroups: statementGroupLst,
            nameDescs: nameDescLst};
    } else {
      return null;
    }
};

/**************************
 * NAMES AND DESCRIPTIONS *
 **************************/

SNACSchemaAlignmentDialog._addNameDesc = function(item, json) {
  var type = 'ALIAS';
  var value = null;
  if (json) {
     type = json.name_type;
     value = json.value;
  }

  var container = item.find('.wbs-namedesc-container').first();
  var namedesc = $('<div></div>').addClass('wbs-namedesc').appendTo(container);
  var type_container = $('<div></div>').addClass('wbs-namedesc-type').appendTo(namedesc);
  var type_input = $('<select></select>').appendTo(type_container);
  $('<option></option>')
  .attr('value', 'LABEL')
  .text($.i18n('snac-schema/label'))
  .appendTo(type_input);
  $('<option></option>')
  .attr('value', 'DESCRIPTION')
  .text($.i18n('snac-schema/description'))
  .appendTo(type_input);
  $('<option></option>')
  .attr('value', 'ALIAS')
  .text($.i18n('snac-schema/alias'))
  .appendTo(type_input);
  type_input.val(type);
  type_input.on('change', function(e) {
    SNACSchemaAlignmentDialog._hasChanged();
  });

  var toolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(namedesc);
  SNACSchemaAlignmentDialog._makeDeleteButton().click(function(e) {
     namedesc.remove();
     SNACSchemaAlignmentDialog._hasChanged();
     e.preventDefault();
  }).appendTo(toolbar);

  var right = $('<div></div>').addClass('wbs-right').appendTo(namedesc);
  var value_container = $('<div></div>').addClass('wbs-namedesc-value').appendTo(namedesc);
  SNACSchemaAlignmentDialog._initField(value_container, "monolingualtext", value);

}

SNACSchemaAlignmentDialog._nameDescToJSON = function (namedesc) {
  var type = namedesc.find('select').first().val();
  var value = namedesc.find('.wbs-namedesc-value').first().data("jsonValue");
  return {
    type: "wbnamedescexpr",
    name_type: type,
    value: value,
  };
}


/********************
 * STATEMENT GROUPS *
 ********************/

SNACSchemaAlignmentDialog._addStatementGroup = function(item, json) {
  var property = null;
  var statements = null;
  if (json) {
     property = json.property;
     statements = json.statements;
  }

  var container = item.find('.wbs-statement-group-container').first();
  var statementGroup = $('<div></div>').addClass('wbs-statement-group');
  var inputContainer = $('<div></div>').addClass('wbs-prop-input').appendTo(statementGroup);
  var right = $('<div></div>').addClass('wbs-right').appendTo(statementGroup);
  var statementContainer = $('<div></div>').addClass('wbs-statement-container').appendTo(right);
  SNACSchemaAlignmentDialog._initPropertyField(inputContainer, statementContainer, property);
  var toolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
  var addValueButton = $('<a></a>').addClass('wbs-add-statement').click(function(e) {
     var datatype = inputContainer.data("jsonValue").datatype;
     SNACSchemaAlignmentDialog._addStatement(statementContainer, datatype, null);
     e.preventDefault();
  }).appendTo(toolbar).hide();
  SNACSchemaAlignmentDialog._plusButton($.i18n('snac-schema/add-value'), addValueButton);
  var removeButton = SNACSchemaAlignmentDialog._makeDeleteButton()
        .addClass('wbs-remove-statement-group')
        .appendTo(toolbar)
        .click(function(e) {
     statementGroup.remove();
     e.preventDefault();
  });

  container.append(statementGroup);

  if (statements) {
     for (var i = 0; i != statements.length; i++) {
        SNACSchemaAlignmentDialog._addStatement(statementContainer, property.datatype, statements[i]);
        addValueButton.show();
        removeButton.hide();
     }
  } else {
     inputContainer.find('input').focus();
  }

}

SNACSchemaAlignmentDialog._statementGroupToJSON = function (statementGroup) {
    var lst = new Array();
    var domStatements = statementGroup.find('.wbs-statement-container').first().children('.wbs-statement');
    domStatements.each(function () {
       var statementJSON = SNACSchemaAlignmentDialog._statementToJSON($(this));
       if (statementJSON !== null) {
          lst.push(statementJSON);
       }
    });
    var inputContainer = statementGroup.find(".wbs-prop-input").first();
    var propertyJSON = SNACSchemaAlignmentDialog._inputContainerToJSON(inputContainer);
    if (propertyJSON !== null && domStatements.length === lst.length && lst.length > 0) {
       return {property: propertyJSON,
              statements: lst};
    } else {
       return null;
    }
};

/**************
 * STATEMENTS *
 **************/

SNACSchemaAlignmentDialog._addStatement = function(container, datatype, json) {
  var qualifiers = null;
  var references = null;
  var value = null;
  if (json) {
    qualifiers = json.qualifiers;
    references = json.references;
    value = json.value;
  }

  var statement = $('<div></div>').addClass('wbs-statement');
  var inputContainer = $('<div></div>').addClass('wbs-target-input').appendTo(statement);
  SNACSchemaAlignmentDialog._initField(inputContainer, datatype, value);

  // If we are in a mainsnak...
  // (see https://www.mediawiki.org/wiki/Wikibase/DataModel#Snaks)
  if (container.parents('.wbs-statement').length == 0) {
    // add delete button
    var toolbar1 = $('<div></div>').addClass('wbs-toolbar').appendTo(statement);
    SNACSchemaAlignmentDialog._makeDeleteButton().click(function(e) {
        SNACSchemaAlignmentDialog._removeStatement(statement);
        e.preventDefault();
    }).appendTo(toolbar1);

    // add rank
    var rank = $('<div></div>').addClass('wbs-rank-selector-icon').prependTo(inputContainer);

    // add qualifiers...
    var right = $('<div></div>').addClass('wbs-right').appendTo(statement);
    var qualifierContainer = $('<div></div>').addClass('wbs-qualifier-container').appendTo(right);
    var toolbar2 = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
    var addQualifierButton = $('<a></a>').addClass('wbs-add-qualifier')
        .click(function(e) {
        SNACSchemaAlignmentDialog._addQualifier(qualifierContainer, null);
        e.preventDefault();
    }).appendTo(toolbar2);
    SNACSchemaAlignmentDialog._plusButton($.i18n('snac-schema/add-qualifier'), addQualifierButton);

    if (qualifiers) {
       for (var i = 0; i != qualifiers.length; i++) {
         SNACSchemaAlignmentDialog._addQualifier(qualifierContainer, qualifiers[i]);
       }
    }

    // and references
    $('<div></div>').attr('style', 'clear: right').appendTo(statement);
    var referencesToggleContainer = $('<div></div>').addClass('wbs-references-toggle').appendTo(statement);
    var triangle = $('<div></div>').addClass('triangle-icon').addClass('pointing-right').appendTo(referencesToggleContainer);
    var referencesToggle = $('<a></a>').appendTo(referencesToggleContainer);
    right = $('<div></div>').addClass('wbs-right').appendTo(statement);
    var referenceContainer = $('<div></div>').addClass('wbs-reference-container').appendTo(right);
    referencesToggleContainer.click(function(e) {
        triangle.toggleClass('pointing-down');
        triangle.toggleClass('pointing-right');
        referenceContainer.toggle(100);
        e.preventDefault();
    });
    referenceContainer.hide();
    var right2 = $('<div></div>').addClass('wbs-right').appendTo(right);
    var toolbar3 = $('<div></div>').addClass('wbs-toolbar').appendTo(right2);
    var addReferenceButton = $('<a></a>').addClass('wbs-add-reference')
        .click(function(e) {
        referenceContainer.show();
        SNACSchemaAlignmentDialog._addReference(referenceContainer, null);
        SNACSchemaAlignmentDialog._updateReferencesNumber(referenceContainer);
        e.preventDefault();
    }).appendTo(toolbar3);
    SNACSchemaAlignmentDialog._plusButton($.i18n('snac-schema/add-reference'), addReferenceButton);

    var pasteToolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(referencesToggleContainer);
    var referencePaste = $('<span></span>')
        .addClass('wbs-paste-reference')
        .appendTo(pasteToolbar);
    if (SNACSchemaAlignmentDialog._copiedReference === null) {
        referencePaste.hide();
    }
    var pasteIcon = $('<span></span>').addClass('wbs-icon').appendTo(referencePaste);
    var referencePasteButton = $('<a></a>')
        .addClass('wbs-paste-reference-button')
        .text($.i18n('snac-schema/paste-reference'))
        .appendTo(referencePaste)
        .click(function(e) {
        if (SNACSchemaAlignmentDialog._copiedReference !== null) {
           SNACSchemaAlignmentDialog._addReference(referenceContainer, SNACSchemaAlignmentDialog._copiedReference);
           SNACSchemaAlignmentDialog._updateReferencesNumber(referenceContainer);
           referencePaste.hide();
           SNACSchemaAlignmentDialog._hasChanged();
        }
        e.preventDefault();
        e.stopPropagation();
    });

    if (references) {
        for (var i = 0; i != references.length; i++) {
          SNACSchemaAlignmentDialog._addReference(referenceContainer, references[i]);
        }
    }
    SNACSchemaAlignmentDialog._updateReferencesNumber(referenceContainer);
  }
  container.append(statement);
}

SNACSchemaAlignmentDialog._statementToJSON = function (statement) {
    var inputContainer = statement.find(".wbs-target-input").first();
    var qualifiersList = new Array();
    var referencesList = new Array();
    var qualifiersDom = statement.find('.wbs-qualifier-container').first().children();
    qualifiersDom.each(function () {
        var qualifierJSON = SNACSchemaAlignmentDialog._qualifierToJSON($(this));
        if (qualifierJSON !== null) {
           qualifiersList.push(qualifierJSON);
        }
    });
    var referencesDom = statement.find('.wbs-reference-container').first().children();
    referencesDom.each(function () {
        var referenceJSON = SNACSchemaAlignmentDialog._referenceToJSON($(this));
        if (referenceJSON !== null) {
          referencesList.push(referenceJSON);
        }
    });
    var valueJSON = SNACSchemaAlignmentDialog._inputContainerToJSON(inputContainer);
    if (referencesList.length === referencesDom.length &&
        qualifiersList.length === qualifiersDom.length &&
        valueJSON !== null) {
      return {
        value: valueJSON,
        qualifiers: qualifiersList,
        references: referencesList,
      };
    } else {
      return null;
    }
};

/**************
 * QUALIFIERS *
 **************/

SNACSchemaAlignmentDialog._addQualifier = function(container, json) {
  var property = null;
  var value = null;
  if (json) {
    property = json.prop;
    value = json.value;
  }

  var qualifier = $('<div></div>').addClass('wbs-qualifier').appendTo(container);
  var toolbar1 = $('<div></div>').addClass('wbs-toolbar').appendTo(qualifier);
  var inputContainer = $('<div></div>').addClass('wbs-prop-input').appendTo(qualifier);
  var right = $('<div></div>').addClass('wbs-right').appendTo(qualifier);
  var deleteButton = SNACSchemaAlignmentDialog._makeDeleteButton()
            .addClass('wbs-remove-statement-group')
            .appendTo(toolbar1).click(function(e) {
    qualifier.remove();
    SNACSchemaAlignmentDialog._hasChanged();
    e.preventDefault();
  });
  var statementContainer = $('<div></div>').addClass('wbs-statement-container').appendTo(right);
  SNACSchemaAlignmentDialog._initPropertyField(inputContainer, statementContainer, property);
  if (value && property) {
    SNACSchemaAlignmentDialog._addStatement(statementContainer, property.datatype, {value:value});
  } else {
    inputContainer.find('input').focus();
  }
}

SNACSchemaAlignmentDialog._qualifierToJSON = function(elem) {
  var prop = elem.find(".wbs-prop-input").first();
  var target = elem.find(".wbs-target-input").first();
  var propJSON = SNACSchemaAlignmentDialog._inputContainerToJSON(prop);
  var valueJSON = SNACSchemaAlignmentDialog._inputContainerToJSON(target);
  if (propJSON !== null && valueJSON !== null) {
    return {
        prop: propJSON,
        value: valueJSON,
    };
  } else {
    return null;
  }
}

/**************
 * REFERENCES *
 **************/

SNACSchemaAlignmentDialog._addReference = function(container, json) {
  var snaks = null;
  if (json) {
     snaks = json.snaks;
  }

  var reference = $('<div></div>').addClass('wbs-reference').appendTo(container);
  var referenceHeader = $('<div></div>').addClass('wbs-reference-header').appendTo(reference);
  var referenceCopy = $('<span></span>').addClass('wbs-copy-reference').appendTo(referenceHeader);
  var referenceCopyIcon = $('<span></span>').addClass('wbs-icon').appendTo(referenceCopy);
  var copyButton = $('<span></span>')
        .addClass('wbs-copy-reference-button')
        .text($.i18n('snac-schema/copy-reference'))
        .appendTo(referenceCopy)
        .click(function(e) {
     if (SNACSchemaAlignmentDialog._copyReference(reference)) {
       $(this).text($.i18n('snac-schema/reference-copied'))
              .parent().addClass('wbs-copied-reference');
       container.parent().parent().find('.wbs-paste-reference').hide();
     }
     e.preventDefault();
  });
  var toolbarRef = $('<div></div>').addClass('wbs-toolbar').appendTo(referenceHeader);
  SNACSchemaAlignmentDialog._makeDeleteButton().click(function(e) {
     reference.remove();
     SNACSchemaAlignmentDialog._updateReferencesNumber(container);
     SNACSchemaAlignmentDialog._hasChanged();
     e.preventDefault();
  }).appendTo(toolbarRef);
  var right = $('<div></div>').addClass('wbs-right').appendTo(reference);
  var qualifierContainer = $('<div></div>').addClass('wbs-qualifier-container').appendTo(right);
  var toolbar2 = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
  var addSnakButton = $('<a></a>').addClass('wbs-add-qualifier')
        .click(function(e) {
      SNACSchemaAlignmentDialog._addQualifier(qualifierContainer, null);
      e.preventDefault();
  }).appendTo(toolbar2);
  SNACSchemaAlignmentDialog._plusButton($.i18n('snac-schema/add-reference-snak'), addSnakButton);

  if (snaks) {
     for (var i = 0; i != snaks.length; i++) {
        SNACSchemaAlignmentDialog._addQualifier(qualifierContainer, snaks[i]);
     }
  } else {
     SNACSchemaAlignmentDialog._addQualifier(qualifierContainer, null);
  }
}

SNACSchemaAlignmentDialog._referenceToJSON = function(reference) {
  var snaks = reference.find('.wbs-qualifier-container').first().children();
  var snaksList = new Array();
  snaks.each(function () {
      var qualifier = SNACSchemaAlignmentDialog._qualifierToJSON($(this));
      if (qualifier !== null) {
         snaksList.push(qualifier);
      }
  });
  if (snaksList.length === snaks.length) {
      return {snaks:snaksList};
  } else {
      return null;
  }
}

SNACSchemaAlignmentDialog._updateReferencesNumber = function(container) {
  var childrenCount = container.children().length;
  var statement = container.parents('.wbs-statement');
  var a = statement.find('.wbs-references-toggle a').first();
  a.html(childrenCount+$.i18n('snac-schema/nb-references'));
}

SNACSchemaAlignmentDialog._copyReference = function(reference) {
   // mark any other copied reference as not copied
   $('.wbs-copy-reference-button')
        .text($.i18n('snac-schema/copy-reference'));
   $('.wbs-copy-reference')
        .removeClass('wbs-copied-reference');
   var copiedReference = SNACSchemaAlignmentDialog._referenceToJSON(reference);
   if (copiedReference !== null) {
      SNACSchemaAlignmentDialog._copiedReference = copiedReference;
      $('.wbs-paste-reference').show();
      return true;
   } else {
      return false;
   }
}

/************************
 * FIELD INITIALIZATION *
 ************************/

SNACSchemaAlignmentDialog._getPropertyType = function(pid, callback) {
  $.ajax({
      url:'https://www.snac.org/w/api.php',
      data: {
        action: "wbgetentities",
        format: "json",
        ids: pid,
        props: "datatype",
     },
     dataType: "jsonp",
     success: function(data) {
        callback(data.entities[pid].datatype);
     }});
}

SNACSchemaAlignmentDialog._initPropertyField = function(inputContainer, targetContainer, initialValue) {
  var input = $('<input></input>').appendTo(inputContainer);
  input.attr("placeholder", $.i18n('snac-schema/property-placeholder'));

  if (this._reconService !== null) {
    endpoint = this._reconService.suggest.property;
    var suggestConfig = $.extend({}, endpoint);
    suggestConfig.key = null;
    suggestConfig.query_param_name = "prefix";

    input.suggestP(suggestConfig).bind("fb-select", function(evt, data) {
        // Fetch the type of this property and add the appropriate target value type
        var statementGroup = inputContainer.parents(".wbs-statement-group, .wbs-qualifier").first();
        SNACSchemaAlignmentDialog._getPropertyType(data.id, function(datatype) {
          inputContainer.data("jsonValue", {
            type : "wbpropconstant",
            pid : data.id,
            label: data.name,
            datatype: datatype,
          });
          SNACSchemaAlignmentDialog._addStatement(targetContainer, datatype, null);
          var addValueButtons = targetContainer.parent().find('.wbs-add-statement');
          var removeGroupButton = targetContainer.parent().find('.wbs-remove-statement-group');
          removeGroupButton.hide();
          addValueButtons.show();
        });
        SNACSchemaAlignmentDialog._hasChanged();
    }).bind("fb-textchange", function(evt, data) {
        inputContainer.data("jsonValue", null);
        targetContainer.find('.wbs-statement').remove();
        var addValueButtons = targetContainer.parent().find('.wbs-add-statement');
        var removeGroupButton = targetContainer.parent().find('.wbs-remove-statement-group');
        addValueButtons.hide();
        removeGroupButton.show();
    });
   // adds tweaks to display the validation status more clearly, like in snac
   fixSuggestInput(input);
  }

  // Init with the provided initial value.
  if (initialValue) {
     if (initialValue.type === "wbpropconstant") {
        input.val(initialValue.label);
        input.addClass('wbs-validated-input');
     }
     inputContainer.data("jsonValue", initialValue);
  }

}

SNACSchemaAlignmentDialog._initField = function(inputContainer, mode, initialValue, changedCallback) {
  var input = $('<input></input>').appendTo(inputContainer);

  if (! changedCallback) {
    changedCallback = SNACSchemaAlignmentDialog._hasChanged;
  }

  if (this._reconService !== null && (mode === "wikibase-item" || mode === "unit")) {
    if (mode === "wikibase-item") {
        input.attr("placeholder", $.i18n('snac-schema/item-or-reconciled-column'));
    } else {
        input.attr("placeholder", $.i18n('snac-schema/unit'));
    }
    var endpoint = null;
    endpoint = this._reconService.suggest.entity;
    var suggestConfig = $.extend({}, endpoint);
    suggestConfig.key = null;
    suggestConfig.query_param_name = "prefix";
    if ('view' in this._reconService && 'url' in this._reconService.view && !('view_url' in endpoint)) {
       suggestConfig.view_url = this._reconService.view.url;
    }


    input.suggest(suggestConfig).bind("fb-select", function(evt, data) {
        inputContainer.data("jsonValue", {
            type : "wbitemconstant",
            qid : data.id,
            label: data.name,
        });
        changedCallback();
    });
    // adds tweaks to display the validation status more clearly, like in snac
    fixSuggestInput(input);

  } else if (this._reconService !== null && mode === "wikibase-property") {
    var endpoint = null;
    endpoint = this._reconService.suggest.property;
    var suggestConfig = $.extend({}, endpoint);
    suggestConfig.key = null;
    suggestConfig.query_param_name = "prefix";

    input.suggestP(suggestConfig).bind("fb-select", function(evt, data) {
        inputContainer.data("jsonValue", {
            type : "wbpropconstant",
            pid : data.id,
            label: data.name,
            datatype: "not-important",
        });
        changedCallback();
    });
    // adds tweaks to display the validation status more clearly, like in snac
    fixSuggestInput(input);

  } else if (mode === "time") {
     input.attr("placeholder", "YYYY(-MM(-DD))");
     var propagateValue = function(val) {
        // TODO add validation here
        inputContainer.data("jsonValue", {
           type: "wbdateconstant",
           value: val,
        });
    };
    propagateValue("");
    input.change(function() {
      propagateValue($(this).val());
      changedCallback();
    });

    SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^\d{4}(-[0-1]\d(-[0-3]\d)?)?$/);
   } else if (mode === "globe-coordinate") {
     input.attr("placeholder", "lat,lon");
     var propagateValue = function(val) {
        // TODO add validation here
        inputContainer.data("jsonValue", {
           type: "wblocationconstant",
           value: val,
        });
    };
    propagateValue("");
    input.change(function() {
      propagateValue($(this).val());
      changedCallback();
    });

    SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^[\-+]?\d+(\.\d*)?[,\/][\-+]?\d+(\.\d*)?([,\/]\d+(\.\d*)?)?$/);
   } else if (mode === "language") {
     input.attr("placeholder", "lang");
     input.addClass("wbs-language-input");
     input.langsuggest().bind("fb-select", function(evt, data) {
        inputContainer.data("jsonValue", {
            type: "wblanguageconstant",
            id: data.id,
            label: data.name,
        });
        changedCallback();
     });
     fixSuggestInput(input);

   } else if (mode === "monolingualtext") {
     input.remove();
     var inputContainerLanguage = $('<div></div>')
     .addClass('wbs-monolingual-container')
     .width('30%')
     .appendTo(inputContainer);
     var inputContainerValue = $('<div></div>')
     .addClass('wbs-monolingual-container')
     .width('70%')
     .appendTo(inputContainer);

     var langValue = null;
     var strValue = null;
     if (initialValue) {
         langValue = initialValue.language;
         strValue = initialValue.value;
     }

     var propagateValue = function() {
        inputContainer.data("jsonValue", {
           type: "wbmonolingualexpr",
           language: inputContainerLanguage.data("jsonValue"),
           value: inputContainerValue.data("jsonValue"),
        });
        changedCallback();
     }

     SNACSchemaAlignmentDialog._initField(inputContainerLanguage, "language", langValue, propagateValue);
     SNACSchemaAlignmentDialog._initField(inputContainerValue, "string", strValue, propagateValue);

   } else if (mode === "quantity") {
     input.remove();
     var inputContainerAmount = $('<div></div>')
     .addClass('wbs-quantity-container')
     .width('60%')
     .appendTo(inputContainer);
     var inputContainerUnit = $('<div></div>')
     .addClass('wbs-quantity-container')
     .width('40%')
     .appendTo(inputContainer);

     var amountValue = null;
     var unitValue = null;
     if (initialValue) {
        amountValue = initialValue.amount;
        unitValue = initialValue.unit;
     }

     var propagateValue = function() {
        inputContainer.data("jsonValue", {
           type: "wbquantityexpr",
           amount: inputContainerAmount.data("jsonValue"),
           unit: inputContainerUnit.data("jsonValue"),
        });
        changedCallback();
     }

     SNACSchemaAlignmentDialog._initField(inputContainerAmount, "amount", amountValue, propagateValue);
     SNACSchemaAlignmentDialog._initField(inputContainerUnit, "unit", unitValue, propagateValue);

   } else {
    var propagateValue = function(val) {
        inputContainer.data("jsonValue", {
           type: "wbstringconstant",
           value: val,
        });
    };
    propagateValue("");
    input.change(function() {
      propagateValue($(this).val());
      changedCallback();
    });
    if (mode === "amount") {
        input.attr("placeholder", $.i18n('snac-schema/amount'));
        SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^[\-+]?\d+(\.\d*)?(E[\-+]\d+)?$/);
    } else if (mode === "url") {
        input.attr("placeholder", $.i18n('snac-schema/full-url'));
        SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^https?:\/\/.+$/);
    } else if (mode === "tabular-data") {
        input.attr("placeholder", $.i18n('snac-schema/tabular-data-with-prefix'));
        SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^Data:.+$/);
    } else if (mode === "commonsMedia") {
        input.attr("placeholder", $.i18n('snac-schema/commons-media'));
    } else if (mode === "math") {
        input.attr("placeholder", $.i18n('snac-schema/math-expression'));
    } else if (mode === "geo-shape") {
        input.attr("placeholder", $.i18n('snac-schema/geoshape-with-prefix'));
        SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^Data:.+$/);
    } else {
        SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^.+$/);
    }
    if (mode !== "external-id" &&
        mode !== "url" &&
        mode !== "string" &&
        mode !== "amount" &&
        mode !== "tabular-data" &&
        mode !== "commonsMedia" &&
        mode !== "geo-shape" &&
        mode !== "math") {
       alert($.i18n('snac-schema/datatype-not-supported-yet'));
    }
  }

  var acceptDraggableColumn = function(column) {
    input.hide();
    input.val("");
    var columnDiv = $('<div></div>').appendTo(inputContainer);
    column.appendTo(columnDiv);
    var origText = column.text();
    column.text("");
    column.append($('<div></div>').addClass('wbs-restricted-column-name').text(origText));
    var deleteButton = SNACSchemaAlignmentDialog._makeDeleteButton(true).appendTo(column);
    deleteButton.attr('alt', $.i18n('snac-schema/remove-column'));
    deleteButton.click(function (e) {
        columnDiv.remove();
        input.show();
        inputContainer.data("jsonValue", null);
        changedCallback();
        e.preventDefault();
    });
  };

  // Make it droppable
  var acceptClass = ".wbs-draggable-column";
  var wbVariableType = "wbstringvariable";
  if (mode === "wikibase-item" || mode === "unit") {
      acceptClass = ".wbs-reconciled-column";
      wbVariableType = "wbitemvariable";
  } else if (mode === "time") {
      wbVariableType = "wbdatevariable";
  } else if (mode === "globe-coordinate") {
      wbVariableType = "wblocationvariable";
  } else if (mode === "monolingualtext" || mode === "quantity") {
      wbVariableType = null; // not droppable directly
  } else if (mode === "language") {
      wbVariableType = "wblanguagevariable";
  }

  if (wbVariableType) {
    inputContainer.droppable({
        accept: acceptClass,
    }).on("drop", function (evt, ui) {
        var column = ui.draggable.clone();
        acceptDraggableColumn(column);
        inputContainer.data("jsonValue", {
            type : wbVariableType,
            columnName: ui.draggable.text(),
        });
        changedCallback();
        return true;
    }).on("dropactivate", function(evt, ui) {
        input.addClass("wbs-accepting-input");
    }).on("dropdeactivate", function(evt, ui) {
        input.removeClass("wbs-accepting-input");
    });
  }

  // Init with the provided initial value.
  if (initialValue) {
     if (initialValue.type === "wbitemconstant" || initialValue.type === "wbpropconstant") {
        input.val(initialValue.label);
        input.addClass("wbs-validated-input");
     } else if (initialValue.type == "wbitemvariable") {
        var cell = SNACSchemaAlignmentDialog._createDraggableColumn(initialValue.columnName, true);
        acceptDraggableColumn(cell);
     } else if (initialValue.type === "wbstringconstant" ||
                initialValue.type === "wbdateconstant" ||
                initialValue.type === "wblocationconstant") {
        input.val(initialValue.value);
     } else if (initialValue.type === "wblanguageconstant") {
        input.val(initialValue.id);
        input.addClass("wbs-validated-input");
     } else if (initialValue.type === "wbstringvariable" ||
                initialValue.type === "wbdatevariable" ||
                initialValue.type === "wblocationvariable" ||
                initialValue.type === "wblanguagevariable") {
        var cell = SNACSchemaAlignmentDialog._createDraggableColumn(initialValue.columnName, false);
        acceptDraggableColumn(cell);
     }
     inputContainer.data("jsonValue", initialValue);
  }
}

SNACSchemaAlignmentDialog.setupStringInputValidation = function(input, regex) {
  input.focus(function() {
    input.removeClass('wbs-unvalidated-input');
  }).blur(function() {
    var currentValue = input.val();
    if (regex.test(currentValue)) {
       input.addClass('wbs-validated-input');
    } else {
       input.addClass('wbs-unvalidated-input');
    }
  });
}

SNACSchemaAlignmentDialog._inputContainerToJSON = function (inputContainer) {
    var data = inputContainer.data();
    if (data && 'jsonValue' in data) {
       return data.jsonValue;
    } else {
       return null;
    }
};

SNACSchemaAlignmentDialog._removeStatement = function(statement) {
  var statementGroup = statement.parents('.wbs-statement-group, .wbs-qualifier').first();
  statement.remove();
  var remainingStatements = statementGroup.find('.wbs-statement').length;
  if (remainingStatements === 0) {
      statementGroup.remove();
  }
  SNACSchemaAlignmentDialog._hasChanged();
}

SNACSchemaAlignmentDialog.getJSON = function() {
  var list = new Array();
  var itemsDom = $('#schema-alignment-statements-container .wbs-item');
  itemsDom.each(function () {
     var itemJSON = SNACSchemaAlignmentDialog._itemToJSON($(this));
     if (itemJSON !== null) {
        list.push(itemJSON);
     }
  });
  if (list.length === itemsDom.length) {
    return {
        'itemDocuments': list,
        'wikibasePrefix': this._wikibasePrefix,
    };
  } else {
    return null;
  }
};

SNACSchemaAlignmentDialog._hasChanged = function() {
  SNACSchemaAlignmentDialog._hasUnsavedChanges = true;
  SNACSchemaAlignmentDialog.preview();
  SNACSchemaAlignmentDialog._unsavedIndicator.show();
  SNACSchemaAlignmentDialog._schemaElmts.saveButton
        .prop('disabled', false)
        .removeClass('disabled');
  SNACSchemaAlignmentDialog._schemaElmts.discardButton
        .prop('disabled', false)
        .removeClass('disabled');
   $('.wbs-copy-reference-button')
        .text($.i18n('snac-schema/copy-reference'));
   $('.wbs-copy-reference')
        .removeClass('wbs-copied-reference');
}

SNACSchemaAlignmentDialog.updateNbEdits = function(nb_edits) {
   this._previewElmts.previewExplanation.text(
      $.i18n('snac-schema/preview-explanation').replace('{nb_edits}',nb_edits));
}

SNACSchemaAlignmentDialog.preview = function() {
  var self = this;

  $('.invalid-schema-warning').hide();
  this._previewPanes.empty();
  this.updateNbEdits(0);
  this.issueSpinner.show();
  this.previewSpinner.show();
  var schema = this.getJSON();
  if (schema === null) {
    $('.invalid-schema-warning').show();
    return;
  }
  $.post(
    "command/snac/preview-wikibase-schema?" + $.param({ project: theProject.id }),
    { schema: JSON.stringify(schema), engine: JSON.stringify(ui.browsingEngine.getJSON()) },
    function(data) {
      self.issueSpinner.hide();
      self.previewSpinner.hide();
      if ("edits_preview" in data) {
        var previewContainer = self._previewPanes[0];
        EditRenderer.renderEdits(data.edits_preview, previewContainer);
        self.updateNbEdits(data["edit_count"]);
      }

      if (data.warnings) {
          self._updateWarnings(data.warnings, data.nb_warnings);
      } else {
          self._updateWarnings([], 0);
      }

      if ("code" in data && data.code === "error") {
         $('.invalid-schema-warning').show();
      }
    },
    "json"
  );
};

Refine.registerUpdateFunction(function(options) {
   // Inject tabs in any project where the schema has been defined
   if(theProject.overlayModels.wikibaseSchema && !SNACSchemaAlignmentDialog.isSetUp()) {
       SNACSchemaAlignmentDialog.setUpTabs();
   }
   if (SNACSchemaAlignmentDialog.isSetUp() && (options.everythingChanged || options.modelsChanged ||
       options.rowsChanged || options.rowMetadataChanged || options.cellsChanged || options.engineChanged)) {
       if (!SNACSchemaAlignmentDialog._hasUnsavedChanges) {
          SNACSchemaAlignmentDialog._discardChanges();
       }
       SNACSchemaAlignmentDialog.updateColumns();
       SNACSchemaAlignmentDialog.preview();
   }
});

/*************************
 * WARNINGS RENDERING *
 *************************/

SNACSchemaAlignmentDialog._updateWarnings = function(warnings, totalCount) {
   var mainDiv = $('#snac-issues-panel');
   var countsElem = this.issuesTabCount;

   // clear everything
   mainDiv.empty();
   countsElem.hide();

   var table = $('<table></table>').appendTo(mainDiv);
   for (var i = 0; i != warnings.length; i++) {
      var rendered = WarningsRenderer._renderWarning(warnings[i]);
      rendered.appendTo(table);
   }

   // update the counts
   if (totalCount) {
        countsElem.text(totalCount);
        countsElem.show();
   }
}
