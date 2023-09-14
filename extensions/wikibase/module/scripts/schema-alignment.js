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

var SchemaAlignment = {
  _isSetUp: false
};

/**
 * Installs the tabs in the UI the first time the Wikidata 
 * extension is called.
 */
SchemaAlignment.setUpTabs = function() {
  this._isSetUp = true;
  this._rightPanel = $('#right-panel');
  this._viewPanel = $('#view-panel').addClass('main-view-panel-tab');
  this._toolPanel = $('#tool-panel');
  this._summaryBar = $('#summary-bar')
        .addClass('main-view-panel-tab-header')
        .addClass('active')
        .attr('href', '#view-panel');

  // append panels
  this._schemaPanel = $('<div id="wikibase-schema-panel"></div>')
        .addClass('main-view-panel-tab')
        .appendTo(this._rightPanel);
  this._issuesPanel = $('<div id="wikibase-issues-panel"></div>')
        .addClass('main-view-panel-tab')
        .appendTo(this._rightPanel);
  this._previewPanel = $('<div id="wikibase-preview-panel"></div>')
        .addClass('main-view-panel-tab')
        .appendTo(this._rightPanel);

  // append tools
  var schemaButton = $('<div></div>')
        .addClass('main-view-panel-tab-header')
        .attr('href', '#wikibase-schema-panel')
        .text($.i18n('wikibase-schema/schema-tab-header'))
        .appendTo(this._toolPanel);
  var issuesButton = $('<div></div>')
        .addClass('main-view-panel-tab-header')
        .attr('href', '#wikibase-issues-panel')
        .text($.i18n('wikibase-schema/warnings-tab-header')+' ')
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
        .attr('href', '#wikibase-preview-panel')
        .text($.i18n('wikibase-schema/edits-preview-tab-header'))
        .appendTo(this._toolPanel);
  this.previewSpinner = $('<img />')
        .attr('src', 'images/large-spinner.gif')
        .attr('width', '16px')
        .appendTo(previewButton);

  this._unsavedIndicator = $('<span></span>')
        .html('&nbsp;*')
        .attr('title', $.i18n('wikibase-schema/unsaved-changes-alt'))
        .hide()
        .appendTo(schemaButton);
 
  $('.main-view-panel-tab-header').on('click',function(e) {
     var targetTab = $(this).attr('href');
     SchemaAlignment.switchTab(targetTab);
     e.preventDefault();
  });

  SchemaAlignment._rerenderTabs();
};

/**
 * Called on tabs setup or Wikibase manifest change.
 */
SchemaAlignment._rerenderTabs = function() {
  if (!SchemaAlignment._isSetUp) {
    SchemaAlignment.setUpTabs();
    return;
  }

  /**
   * Init the schema tab
   */
  this._schemaPanel.empty();
  var schemaTab = $(DOM.loadHTML("wikidata", "scripts/schema-alignment-tab.html")).appendTo(this._schemaPanel);
  var schemaElmts = this._schemaElmts = DOM.bind(schemaTab);
  schemaElmts.targetWikibaseLabel.text($.i18n('wikibase-schema/target-wikibase-instance'));
  let editableEntityTypes = WikibaseManager.getSelectedWikibaseEditableEntityTypes();
  for (let entityType of editableEntityTypes) {
    let addButton = $('<div></div>').addClass("wbs-toolbar");
    let button = $('<button></button>').addClass("button").appendTo(addButton);
    addButton.appendTo(schemaElmts.entityAddButtons);
    button.text($.i18n('wikibase-schema/add-'+entityType+'-button'));
    button.on('click',function(e) {
      SchemaAlignment._addEntity(entityType);
      SchemaAlignment._hasChanged();
      e.preventDefault();
    });
  }
  
  schemaElmts.saveButton
      .text($.i18n('wikibase-schema/save-button'))
      .attr('title', $.i18n('wikibase-schema/save-schema-alt'))
      .prop('disabled', true)
      .addClass('disabled')
      .on('click',function() { SchemaAlignment._save(); });
  schemaElmts.discardButton
      .text($.i18n('wikibase-schema/discard-button'))
      .attr('title', $.i18n('wikibase-schema/discard-schema-changes-alt'))
      .prop('disabled', true)
      .addClass('disabled')
      .on('click',function() { SchemaAlignment._discardChanges(); });

  // Init the wikibase selection UI
  // The choices of the select widget are added when _reset is called
  schemaElmts.wikibaseSelect.on('change', function(e) {
    let wikibaseName = $(this).val();
    let previousWikibase = WikibaseManager.getSelectedWikibaseName();
    if (wikibaseName !== previousWikibase) {
      WikibaseManager.selected = wikibaseName;
      SchemaAlignment.onWikibaseChange();
    }
  });

  // Init template loading UI
  schemaElmts.templateLabel.text($.i18n('wikibase-schema/start-from-an-existing-schema'));
  schemaElmts.saveNewTemplateButton.text($.i18n('wikibase-schema/save-new-schema'));

  WikibaseTemplateManager.loadTemplates(function() {
    SchemaAlignment.updateAvailableTemplates();
  });
  schemaElmts.saveNewTemplateButton.on('click', function(e) {
    SaveSchemaDialog.launch();
    e.preventDefault();
  });
  schemaElmts.templateSelect.on('change', function(e) {
    // check if the schema is empty: if so, ask for confirmation before erasing
    let templateName = $(this).val();
    let wikibaseName = WikibaseManager.getSelectedWikibaseName();
    let template = WikibaseTemplateManager.getTemplate(wikibaseName, templateName);
    let currentJson = SchemaAlignment.getJSON();
    if (currentJson === undefined || currentJson.entityEdits.length === 0 || 
      !SchemaAlignment._hasUnsavedChanges ||
      confirm($.i18n('wikibase-schema/schema-load-erases-schema'))) {
      SchemaAlignment._reset(template.schema);
      SchemaAlignment.preview();
      SchemaAlignment._hasUnsavedChanges = true;
    } else {
      schemaElmts.templateSelect.val('__placeholder__');
    }
  });

  // Init the column area
  this.updateColumns();
  /**
   * Init the issues tab
   */
  this._issuesPanel.empty();
  var issuesTab = $(DOM.loadHTML("wikidata", "scripts/issues-tab.html")).appendTo(this._issuesPanel);
  var issuesElmts = this._issuesElmts = DOM.bind(issuesTab);
  issuesElmts.invalidSchemaWarningIssues.text($.i18n('wikibase-schema/invalid-schema-warning'));
  this.schemaValidationErrorsInIssues = issuesElmts.schemaValidationErrors;

  /**
   * Init the preview tab
   */
  this._previewPanel.empty();
  var previewTab = $(DOM.loadHTML("wikidata", "scripts/preview-tab.html")).appendTo(this._previewPanel);
  var previewElmts = this._previewElmts = DOM.bind(previewTab);
  SchemaAlignment.updateNbEdits(0);
  previewElmts.invalidSchemaWarningPreview.text($.i18n('wikibase-schema/invalid-schema-warning'));
  this.schemaValidationErrorsInPreview = previewElmts.schemaValidationErrors;
  this._previewPanes = $(".schema-alignment-dialog-preview");

  // add all recon services for all the entity types of the Wikibase instance
  var entityTypes = WikibaseManager.getSelectedWikibaseAvailableEntityTypes();
  SchemaAlignment._reconService = {};
  for (let entityType of entityTypes) {
    var reconServiceTemplate = WikibaseManager.getSelectedWikibaseReconEndpoint(entityType);
    if (reconServiceTemplate != null) {
      var reconServiceURL = reconServiceTemplate.replace("${lang}", $.i18n("core-recon/wd-recon-lang"));
      ReconciliationManager.getOrRegisterServiceFromUrl(reconServiceURL, function (service)  {
        SchemaAlignment._reconService[entityType] = service;
      }, false);
    }
  }

  // Load the existing schema
  SchemaAlignment._reset(theProject.overlayModels.wikibaseSchema);

  // Perform initial preview
  SchemaAlignment.preview();
};

SchemaAlignment.onWikibaseChange = function() {
  SchemaAlignment._rerenderTabs();
  SchemaAlignment._save(function () {
    SchemaAlignment._reset(null);
    SchemaAlignment.preview();
  });
};

SchemaAlignment.updateColumns = function() {
  var columns = theProject.columnModel.columns;
  this._columnArea = $(".schema-alignment-dialog-columns-area");
  this._columnArea.empty();
  for (var i = 0; i < columns.length; i++) {
     var column = columns[i];
     var reconConfig = column.reconConfig;
     // make sure the column was reconciled.
     // TODO we could potentially ignore any reconciliation to a siteIRI not
     // mentioned in the manifest…
     var cell = SchemaAlignment._createDraggableColumn(column.name,
        reconConfig && column.reconStats ? reconConfig.identifierSpace : null);
     this._columnArea.append(cell);
  }

  $('.wbs-reconciled-column').draggable({
     helper: "clone",
     cursor: "crosshair",
     snap: ".wbs-entity-input input, .wbs-target-input input",
     zIndex: 100,
  });
  $('.wbs-unreconciled-column').draggable({
     helper: "clone",
     cursor: "crosshair",
     snap: ".wbs-target-input input",
     zIndex: 100,
  });
};

SchemaAlignment.updateAvailableWikibases = function() {
  let wikibaseSelect = $('#wikibase-instance-selector');
  wikibaseSelect.empty();
  let wikibases = WikibaseManager.getAllWikibaseManifests();
  let selectedWikibase = WikibaseManager.getSelectedWikibaseName();
  for (let manifest of wikibases) {
    let wikibaseName = manifest.mediawiki.name;
    let option = $('<option></option>')
      .attr('value', manifest.mediawiki.name)
      .text(manifest.mediawiki.name)
      .appendTo(wikibaseSelect);
    if (selectedWikibase == manifest.mediawiki.name) {
      option.attr('selected', 'selected');
    }
  }
};

SchemaAlignment.updateAvailableTemplates = function() {
  let selectedWikibase = WikibaseManager.getSelectedWikibaseName();
  let templates = WikibaseTemplateManager.getTemplates(selectedWikibase);
  let templateSelect = $('#wikibase-template-select');
  templateSelect.empty();
  $('<option></option>')
    .attr('selected', 'selected')
    .attr('value', '__placeholder__')
    .addClass('placeholder')
    .text($.i18n('wikibase-save-schema-dialog/select-schema'))
    .appendTo(templateSelect);
  for (let template of templates) {
     $('<option></option>')
        .attr('value', template.name)
        .text(template.name)
        .appendTo(templateSelect);
  }   
};

SchemaAlignment.switchTab = function(targetTab) {
  $('.main-view-panel-tab').hide();
  $('.main-view-panel-tab-header').removeClass('active');
  $('.main-view-panel-tab-header[href="'+targetTab+'"]').addClass('active');
  $(targetTab).show();
  resizeAll();

  if (targetTab === "#view-panel") {
    ui.dataTableView.render();
  }
};

SchemaAlignment.isSetUp = function() {
  return SchemaAlignment._isSetUp;
};

SchemaAlignment.launch = function() {
  this._hasUnsavedChanges = false;

  if (!SchemaAlignment.isSetUp()) {
     SchemaAlignment.setUpTabs();
  }
  SchemaAlignment.switchTab('#wikibase-schema-panel');
};


var beforeUnload = function(e) {
  if (SchemaAlignment.isSetUp() && SchemaAlignment._hasUnsavedChanges === true) {
     return $.i18n('wikibase-schema/unsaved-warning');
  }
};

$(window).on('beforeunload', beforeUnload);

SchemaAlignment._reset = function(schema) {
  if (!schema) {
    schema = {};
  }

  // fall back to Wikidata
  if (!schema.siteIri) {
    schema.siteIri = WikidataManifestV1_0.wikibase.site_iri;
  }
  if (!schema.mediaWikiApiEndpoint) {
    schema.mediaWikiApiEndpoint = WikidataManifestV1_0.mediawiki.api;
  }

  if (!schema.entityEdits) {
    schema.entityEdits = [];
  }
  // backwards compatibility for schemas which supported only items (up to OpenRefine 3.5)
  if (schema.itemDocuments) {
	for(let itemEdit in schema.itemDocuments) {
	   schema.entityEdits.push(itemEdit);
	}
  }
  delete schema.itemDocuments;

  this._originalSchema = schema;
  this._schema = cloneDeep(this._originalSchema); // this is what can be munched on
  this._copiedReference = null;

  // update the widget to select the Wikibase instance
  SchemaAlignment.updateAvailableWikibases();

  $('#schema-alignment-entity-edits-container').empty();

  if (this._schema && this._schema.entityEdits) {
    for(let entityEdit of this._schema.entityEdits) {
      let entityType = 'item'; // default, for backwards compatibility
      if (entityEdit.type === 'wbitemeditexpr') {
        entityType = 'item';	
      } else if (entityEdit.type === 'wbmediainfoeditexpr') {
	    entityType = 'mediainfo';
      }
      this._addEntity(entityType, entityEdit);
    }
  }
};

SchemaAlignment._save = function(onDone) {
  var self = this;
  var schema = this.getJSON();

  if (schema === null) {
    alert($.i18n('wikibase-schema/incomplete-schema-could-not-be-saved'));
    return;
  }

  Refine.postProcess(
    "wikidata",
    "save-wikibase-schema",
    {},
    { schema: JSON.stringify(schema) },
    {},
    {   
      onDone: function() {
        theProject.overlayModels.wikibaseSchema = schema;

        $('.invalid-schema-warning').hide();
        self.schemaValidationErrorsInPreview.empty();
        self.schemaValidationErrorsInIssues.empty();
        self._changesCleared();

        if (onDone) onDone();
      },
      onError: function(e) {
        if (e.reason == 'invalid-schema') {
           alert($.i18n('wikibase-schema/incomplete-schema-could-not-be-saved'));
        } else {
           alert(e.message);
        }
      },
    }
  );
};

SchemaAlignment._discardChanges = function() {
  this._reset(theProject.overlayModels.wikibaseSchema);
  this._changesCleared();
};

SchemaAlignment._changesCleared = function() {
  this._hasUnsavedChanges = false;
  this._unsavedIndicator.hide();
  this._schemaElmts.saveButton
        .prop('disabled', true)
        .addClass('disabled');
  this._schemaElmts.discardButton
        .prop('disabled', true)
        .addClass('disabled');
};

SchemaAlignment._createDraggableColumn = function(name, reconciledSiteIRI) {
  var cell = $("<div></div>").addClass('wbs-draggable-column').text(name);
  cell.data({
        'columnName': name,
        'reconciledSiteIRI': reconciledSiteIRI
  });
  if (reconciledSiteIRI !== null) {
    cell.addClass('wbs-reconciled-column');
  } else {
    cell.addClass('wbs-unreconciled-column');
  }
  return cell;
};

SchemaAlignment._plusButton = function(label, element) {
   $('<b></b>').html('+&nbsp;').appendTo(element);
   $('<span></span>').text(label).appendTo(element);
};

SchemaAlignment._makeDeleteButton = function (noText) {
  var button = $('<div></div>').addClass('wbs-remove').append(
     $('<span></span>').addClass('wbs-icon')
  );
  if(noText === undefined) {
     button.append(
     $('<span></span>').text($.i18n('wikibase-schema/remove')));
  }
  return button;
};

/************/
/* ENTITIES */
/************/

SchemaAlignment._addEntity = function(entityType, json) {
  if (entityType === 'item') {
	SchemaAlignment._addItem(json);
  } else if (entityType === 'mediainfo') {
	SchemaAlignment._addMediaInfo(json);
  } else {
	console.error('unsupported entity type: '+entityType);
  }
}

SchemaAlignment._entityToJSON = function (domElem) {
  if (domElem.hasClass('wbs-item')) {
	return SchemaAlignment._itemToJSON(domElem);
  } else if (domElem.hasClass('wbs-mediainfo')) {
	return SchemaAlignment._mediaInfoToJSON(domElem);
  } else {
	console.error('unsupported entity type');
	return null;
  }
}


/**************/
/*** ITEMS ****/
/**************/

SchemaAlignment._addItem = function(json) {
  var subject = null;
  var statementGroups = null;
  var nameDescs = null;
  if (json) {
     subject = json.subject;
     statementGroups = json.statementGroups;
     nameDescs = json.nameDescs;
  }

  var item = $('<div></div>')
	.addClass('wbs-entity')
	.addClass('wbs-item');
  $('#schema-alignment-entity-edits-container').append(item);
  var deleteToolbar = $('<div></div>').addClass('wbs-toolbar')
        .attr('style', 'margin-top: 10px')
        .appendTo(item);
  var deleteButton = SchemaAlignment._makeDeleteButton()
        .appendTo(deleteToolbar)
        .on('click',function(e) {
     item.remove();
     SchemaAlignment._hasChanged();
     e.preventDefault();
  });
  var inputContainer = $('<div></div>').addClass('wbs-entity-input').appendTo(item);
  SchemaAlignment._initField(inputContainer, "wikibase-item", subject);
  var right = $('<div></div>').addClass('wbs-entity-contents').appendTo(item);

  // Terms
  $('<span></span>').addClass('wbs-namedesc-header')
       .text($.i18n('wikibase-schema/terms-header')).appendTo(right);
  $('<div></div>').addClass('wbs-namedesc-container')
        .attr('data-emptyplaceholder', $.i18n('wikibase-schema/empty-terms'))
        .appendTo(right);
  var termToolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
  var addNamedescButton = $('<a></a>').addClass('wbs-add-namedesc')
  .on('click',function(e) {
     SchemaAlignment._addNameDesc(item, null);
     e.preventDefault();
  }).appendTo(termToolbar);
  SchemaAlignment._plusButton(
         $.i18n('wikibase-schema/add-term'), addNamedescButton);

  // Clear the float
  $('<div></div>').attr('style', 'clear: right').appendTo(right);

  // Statements
  $('<div></div>').addClass('wbs-statements-header')
        .text($.i18n('wikibase-schema/statements-header')).appendTo(right);
  $('<div></div>').addClass('wbs-statement-group-container')
        .attr('data-emptyplaceholder', $.i18n('wikibase-schema/empty-statements'))
        .appendTo(right);
  var statementToolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
  var addStatementButton = $('<a></a>').addClass('wbs-add-statement-group')
        .on('click',function(e) {
     SchemaAlignment._addStatementGroup(item, null);
     e.preventDefault();
  }).appendTo(statementToolbar);

  SchemaAlignment._plusButton(
         $.i18n('wikibase-schema/add-statement'), addStatementButton);
   
  if (statementGroups) {
     for(var i = 0; i != statementGroups.length; i++) {
        SchemaAlignment._addStatementGroup(item, statementGroups[i]);
     }
  }
  
  if (nameDescs) {
     for(var i = 0; i != nameDescs.length; i++) {
        SchemaAlignment._addNameDesc(item, nameDescs[i]);
     }
  }
};

SchemaAlignment._itemToJSON = function (item) {
    var statementGroupLst = new Array();
    var statementsDom = item.find('.wbs-statement-group');
    statementsDom.each(function () {
        var statementGroupJSON = SchemaAlignment._statementGroupToJSON($(this));
        statementGroupLst.push(statementGroupJSON);
    });
    var nameDescLst = new Array();
    var nameDescsDom = item.find('.wbs-namedesc');
    nameDescsDom.each(function () {
        var nameDescJSON = SchemaAlignment._nameDescToJSON($(this));
        nameDescLst.push(nameDescJSON);
    });
    var inputContainer = item.find(".wbs-entity-input").first();
    var subjectJSON = SchemaAlignment._inputContainerToJSON(inputContainer);
    return {
      type: 'wbitemeditexpr',
      subject: subjectJSON,
      statementGroups: statementGroupLst,
      nameDescs: nameDescLst
    }; 
};

/***********************
 * MEDIA INFO ENTITIES *
 ***********************/

SchemaAlignment._addMediaInfo = function(json) {
  var subject = null;
  var statementGroups = null;
  var nameDescs = null;
  var filePath = null;
  var fileName = null;
  var wikitext = null;
  var wikitextOverride = false;
  if (json) {
     subject = json.subject;
     statementGroups = json.statementGroups;
     nameDescs = json.nameDescs;
     filePath = json.filePath;
     fileName = json.fileName;
     wikitext = json.wikitext;
     wikitextOverride = json.wikitextOverride;
  }

  var item = $('<div></div>').addClass('wbs-entity')
	.addClass('wbs-mediainfo');
  $('#schema-alignment-entity-edits-container').append(item);
  var deleteToolbar = $('<div></div>').addClass('wbs-toolbar')
        .attr('style', 'margin-top: 10px')
        .appendTo(item);
  var deleteButton = SchemaAlignment._makeDeleteButton()
        .appendTo(deleteToolbar)
        .on('click',function(e) {
     item.remove();
     SchemaAlignment._hasChanged();
     e.preventDefault();
  });
  var inputContainer = $('<div></div>').addClass('wbs-entity-input').appendTo(item);
  SchemaAlignment._initField(inputContainer, "wikibase-mediainfo", subject);
  var right = $('<div></div>').addClass('wbs-entity-contents').appendTo(item);

  // File-specific fields
  var fileFields = $('<div></div>').addClass('wbs-mediainfo-file-fields').appendTo(right);
  
  // File path
  $('<span></span>').text($.i18n('wikibase-schema/mediainfo-file-path'))
	.appendTo(fileFields);
  var pathInputContainer = $('<span></span>')
    .addClass('wbs-file-path-input')
    .appendTo(fileFields);
  SchemaAlignment._initField(pathInputContainer, "filepath", filePath);
  // add dummy "override" field, not supported for now
  $('<span></span>').appendTo(fileFields);

  // File name
  $('<span></span>').text($.i18n('wikibase-schema/mediainfo-file-name'))
	.appendTo(fileFields);
  var nameInputContainer = $('<span></span>')
    .addClass('wbs-file-name-input')
    .appendTo(fileFields);
  SchemaAlignment._initField(nameInputContainer, "filename", fileName);
  // add dummy "override" field, not supported for now
  $('<span></span>').appendTo(fileFields);

  // Wikitext
  $('<span></span>').text($.i18n('wikibase-schema/mediainfo-wikitext'))
	.appendTo(fileFields);
  var wikitextInputContainer = $('<span></span>')
    .addClass('wbs-wikitext-input')
    .appendTo(fileFields);
  SchemaAlignment._initField(wikitextInputContainer, "wikitext", wikitext);
  // add override option
  var overrideSpan = $('<span></span>').appendTo(fileFields);
  var label = $('<label></label>').appendTo(overrideSpan);
  var checkbox = $('<input></input>')
       .attr('type', 'checkbox')
       .addClass('wbs-wikitext-override')
       .prop('checked', wikitextOverride)
       .appendTo(label);
  $('<span></span>').text($.i18n('wikibase-schema/override-wikitext')).appendTo(label);
  checkbox.on('change', function(e) {
    SchemaAlignment._hasChanged();
  });

  if (!WikibaseManager.areStructuredMediaInfoFieldsDisabledForSelectedWikibase()) {
    // Captions
    $('<span></span>').addClass('wbs-namedesc-header')
        .text($.i18n('wikibase-schema/captions-header')).appendTo(right);
    $('<div></div>').addClass('wbs-namedesc-container')
            .attr('data-emptyplaceholder', $.i18n('wikibase-schema/empty-captions'))
            .appendTo(right);
    var termToolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
    var addNamedescButton = $('<a></a>').addClass('wbs-add-namedesc')
    .on('click',function(e) {
        SchemaAlignment._addNameDesc(item, {name_type: 'LABEL_IF_NEW', value: null});
        e.preventDefault();
    }).appendTo(termToolbar);
    SchemaAlignment._plusButton(
            $.i18n('wikibase-schema/add-caption'), addNamedescButton);

    // Clear the float
    $('<div></div>').attr('style', 'clear: right').appendTo(right);

    // Statements
    $('<div></div>').addClass('wbs-statements-header')
            .text($.i18n('wikibase-schema/statements-header')).appendTo(right);
    $('<div></div>').addClass('wbs-statement-group-container')
            .attr('data-emptyplaceholder', $.i18n('wikibase-schema/empty-statements'))
            .appendTo(right);
    var statementToolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
    var addStatementButton = $('<a></a>').addClass('wbs-add-statement-group')
            .on('click',function(e) {
        SchemaAlignment._addStatementGroup(item, null);
        e.preventDefault();
    }).appendTo(statementToolbar);

    SchemaAlignment._plusButton(
            $.i18n('wikibase-schema/add-statement'), addStatementButton);
    
    if (statementGroups) {
        for(var i = 0; i != statementGroups.length; i++) {
            SchemaAlignment._addStatementGroup(item, statementGroups[i]);
        }
    }
    
    if (nameDescs) {
        for(var i = 0; i != nameDescs.length; i++) {
            SchemaAlignment._addNameDesc(item, nameDescs[i]);
        }
    }
  }
};

SchemaAlignment._mediaInfoToJSON = function (mediainfo) {
    var statementGroupLst = new Array();
    var statementsDom = mediainfo.find('.wbs-statement-group');
    statementsDom.each(function () {
        var statementGroupJSON = SchemaAlignment._statementGroupToJSON($(this));
        statementGroupLst.push(statementGroupJSON);
    });
    var nameDescLst = new Array();
    var nameDescsDom = mediainfo.find('.wbs-namedesc');
    nameDescsDom.each(function () {
        var nameDescJSON = SchemaAlignment._nameDescToJSON($(this));
        nameDescLst.push(nameDescJSON);
    });
    var inputContainer = mediainfo.find(".wbs-entity-input").first();
    var subjectJSON = SchemaAlignment._inputContainerToJSON(inputContainer);

    var filePath = SchemaAlignment._inputContainerToJSON(
	      mediainfo.find('.wbs-file-path-input').first());
    var fileName = SchemaAlignment._inputContainerToJSON(
		  mediainfo.find('.wbs-file-name-input').first());
    var wikitext = SchemaAlignment._inputContainerToJSON(
		  mediainfo.find('.wbs-wikitext-input').first());
    var overrideWikitext = mediainfo.find('input[type=checkbox].wbs-wikitext-override').first().prop('checked');
    return {
      type: "wbmediainfoeditexpr",
      subject: subjectJSON,
      filePath: filePath,
      fileName: fileName,
      wikitext: wikitext,
      overrideWikitext: overrideWikitext,
      statementGroups: statementGroupLst,
      nameDescs: nameDescLst
    };
};

/**************************
 * NAMES AND DESCRIPTIONS *
 **************************/

SchemaAlignment._addNameDesc = function(item, json) {
  var term_type = 'ALIAS';
  var value = null;
  var override = false;
  if (json) {
     term_type = json.name_type.replace('_IF_NEW', '');
     value = json.value;
     override = json.name_type.indexOf('_IF_NEW') == -1; 
  } 

  var container = item.find('.wbs-namedesc-container').first();
  var namedesc = $('<div></div>').addClass('wbs-namedesc').appendTo(container);
  var type_container = $('<div></div>').addClass('wbs-namedesc-type').appendTo(namedesc);
  var type_input = $('<select></select>').appendTo(type_container);
  $('<option></option>')
  .val('LABEL')
  .text($.i18n('wikibase-schema/label'))
  .appendTo(type_input);
  $('<option></option>')
  .val('DESCRIPTION')
  .text($.i18n('wikibase-schema/description'))
  .appendTo(type_input);
  $('<option></option>')
  .val('ALIAS')
  .text($.i18n('wikibase-schema/alias'))
  .appendTo(type_input);
  type_input.val(term_type);

  var toolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(namedesc);
  SchemaAlignment._makeDeleteButton().on('click',function(e) {
     namedesc.remove();
     SchemaAlignment._hasChanged();
     e.preventDefault();
  }).appendTo(toolbar);

  $('<div></div>').addClass('wbs-right').appendTo(namedesc);
  var value_container = $('<div></div>').addClass('wbs-namedesc-value').appendTo(namedesc);
  SchemaAlignment._initField(value_container, "monolingualtext", value);

  var override_container = $('<div></div>').addClass('wbs-namedesc-override').appendTo(namedesc);
  var label = $('<label></label>').appendTo(override_container);
  var checkbox = $('<input></input>')
       .attr('type', 'checkbox')
       .prop('checked', override)
       .appendTo(label);
  $('<span></span>').text($.i18n('wikibase-schema/override-term')).appendTo(label);
  checkbox.on('change', function(e) {
    SchemaAlignment._hasChanged();
  });
  type_input.on('change', function(e) {
    var checkbox_visible = type_input.val() !== 'ALIAS';
    if (checkbox_visible) {
       override_container.show();
    } else {
       override_container.hide();
    }
    SchemaAlignment._hasChanged();
  });

};

SchemaAlignment._nameDescToJSON = function (namedesc) {
  var term_type = namedesc.find('select').first().val();
  var type = term_type;
  if (term_type !== 'ALIAS') {
      var override = namedesc.find('input[type=checkbox]').first().prop('checked');
      if (!override) {
         type = term_type + '_IF_NEW';
      }
  }
  var value = namedesc.find('.wbs-namedesc-value').first().data("jsonValue");
  return {
    type: "wbnamedescexpr",    
    name_type: type,
    value: value,
  };
};
  

/********************
 * STATEMENT GROUPS *
 ********************/

SchemaAlignment._addStatementGroup = function(item, json) {
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
  SchemaAlignment._initPropertyField(inputContainer, statementContainer, property);
  var toolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
  var addValueButton = $('<a></a>').addClass('wbs-add-statement').on('click',function(e) {
     var datatype = inputContainer.data("jsonValue").datatype;
     SchemaAlignment._addStatement(statementContainer, datatype, null);
     e.preventDefault();
  }).appendTo(toolbar).hide();
  SchemaAlignment._plusButton($.i18n('wikibase-schema/add-value'), addValueButton);
  var removeButton = SchemaAlignment._makeDeleteButton()
        .addClass('wbs-remove-statement-group')
        .appendTo(toolbar)
        .on('click',function(e) {
     statementGroup.remove();
     e.preventDefault();
  });

  container.append(statementGroup);

  if (statements) {
     for (var i = 0; i != statements.length; i++) {
        SchemaAlignment._addStatement(statementContainer, property.datatype, statements[i]);
        addValueButton.show();
        removeButton.hide();
     }
  } else {
     inputContainer.find('input').trigger('focus');
  }
     
};

SchemaAlignment._statementGroupToJSON = function (statementGroup) {
    var lst = new Array();
    var domStatements = statementGroup.find('.wbs-statement-container').first().children('.wbs-statement');
    domStatements.each(function () {
       var statementJSON = SchemaAlignment._statementToJSON($(this));
       lst.push(statementJSON);
    });
    var inputContainer = statementGroup.find(".wbs-prop-input").first();
    var propertyJSON = SchemaAlignment._inputContainerToJSON(inputContainer);
    return {
      property: propertyJSON,
      statements: lst
    };
};

/**************
 * STATEMENTS *
 **************/

SchemaAlignment._addStatement = function(container, datatype, json) {
  var qualifiers = null;
  var references = null;
  var value = null;
  var editingMode = StatementConfigurationDialog.defaultMode;
  var mergingStrategy = StatementConfigurationDialog.defaultStrategy;
  if (json) {
    qualifiers = json.qualifiers;
    references = json.references;
    value = json.value;
    editingMode = json.mode || 'add_or_merge';
    mergingStrategy = json.mergingStrategy || {'type':'qualifiers','valueMatcher':{'type':'strict'}};
  }
 
  var statement = $('<div></div>').addClass('wbs-statement');
  var inputContainer = $('<div></div>').addClass('wbs-target-input').appendTo(statement);
  SchemaAlignment._initField(inputContainer, datatype, value);
  
  // If we are in a mainsnak...
  // (see https://www.mediawiki.org/wiki/Wikibase/DataModel#Snaks)
  if (container.parents('.wbs-statement').length == 0) {
    inputContainer.children().first().addClass('wbs-mainsnak-input');
    statement
            .data('jsonMode', editingMode)
            .data('jsonMergingStrategy', JSON.stringify(mergingStrategy));
    inputContainer.append(
       $('<span></span>')
        .addClass('wbs-value-placeholder') 
        .text($.i18n('wikibase-preview/delete-all-existing-statements')));
    // add delete button
    var toolbar1 = $('<div></div>').addClass('wbs-toolbar').appendTo(statement);
    SchemaAlignment._makeDeleteButton().on('click',function(e) {
        SchemaAlignment._removeStatement(statement);
        e.preventDefault();
    }).appendTo(toolbar1);
    // add configure button
    var configureButton = $('<div></div>').addClass('wbs-configure');
    $('<span></span>').addClass('wbs-icon').appendTo(configureButton);
    $('<span></span>').text($.i18n('wikibase-schema/configure-statement')).appendTo(configureButton);
    configureButton.appendTo(toolbar1);
    configureButton.on('click',function(e) {
        SchemaAlignment._openStatementConfigurationDialog(statement);
        e.preventDefault();
    });

    // add rank
    var rank = $('<div></div>')
        .addClass('wbs-rank-selector-icon')
        .addClass('wbs-mainsnak-input')
        .prependTo(inputContainer);

    // add qualifiers...
    var qualifiersSection = $('<div></div>').addClass('wbs-qualifiers-section').appendTo(statement);
    var right = $('<div></div>').addClass('wbs-right').appendTo(qualifiersSection);
    var qualifierContainer = $('<div></div>').addClass('wbs-qualifier-container').appendTo(right);
    var toolbar2 = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
    var addQualifierButton = $('<a></a>').addClass('wbs-add-qualifier')
        .on('click',function(e) {
        SchemaAlignment._addQualifier(qualifierContainer, null);
        e.preventDefault();
    }).appendTo(toolbar2);
    SchemaAlignment._plusButton($.i18n('wikibase-schema/add-qualifier'), addQualifierButton);

    if (qualifiers) {
       for (var i = 0; i != qualifiers.length; i++) {
         SchemaAlignment._addQualifier(qualifierContainer, qualifiers[i]);
       }
    }

    // and references
    $('<div></div>').attr('style', 'clear: right').appendTo(statement);
    var referencesSection = $('<div></div>').addClass('wbs-references-section').appendTo(statement);
    var referencesToggleContainer = $('<div></div>').addClass('wbs-references-toggle').appendTo(referencesSection);
    var triangle = $('<div></div>').addClass('triangle-icon').addClass('pointing-right').appendTo(referencesToggleContainer);
    var referencesToggle = $('<a></a>').appendTo(referencesToggleContainer);
    right = $('<div></div>').addClass('wbs-right').appendTo(referencesSection);
    var referenceContainer = $('<div></div>').addClass('wbs-reference-container').appendTo(right);
    referencesToggleContainer.on('click',function(e) {
        triangle.toggleClass('pointing-down');
        triangle.toggleClass('pointing-right');
        referenceContainer.toggle(100);
        e.preventDefault();
    });
    referenceContainer.hide();
    var right2 = $('<div></div>').addClass('wbs-right').appendTo(right);
    var toolbar3 = $('<div></div>').addClass('wbs-toolbar').appendTo(right2);
    var addReferenceButton = $('<a></a>').addClass('wbs-add-reference')
        .on('click',function(e) {
        referenceContainer.show();
        SchemaAlignment._addReference(referenceContainer, null);
        SchemaAlignment._updateReferencesNumber(referenceContainer);
        e.preventDefault();
    }).appendTo(toolbar3);
    SchemaAlignment._plusButton($.i18n('wikibase-schema/add-reference'), addReferenceButton);

    var pasteToolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(referencesToggleContainer);
    var referencePaste = $('<span></span>')
        .addClass('wbs-paste-reference')
        .appendTo(pasteToolbar);
    if (SchemaAlignment._copiedReference === null) {
        referencePaste.hide();
    }
    $('<span></span>').addClass('wbs-icon').appendTo(referencePaste);
    $('<a></a>')
        .addClass('wbs-paste-reference-button')
        .text($.i18n('wikibase-schema/paste-reference'))
        .appendTo(referencePaste)
        .on('click',function(e) {
        if (SchemaAlignment._copiedReference !== null) {
           SchemaAlignment._addReference(referenceContainer, SchemaAlignment._copiedReference);
           SchemaAlignment._updateReferencesNumber(referenceContainer);
           referencePaste.hide();
           SchemaAlignment._hasChanged();
        }
        e.preventDefault();
        e.stopPropagation();
    });

    if (references) {
        for (var i = 0; i != references.length; i++) {
          SchemaAlignment._addReference(referenceContainer, references[i]);
        }
    }
    SchemaAlignment._updateReferencesNumber(referenceContainer);
    SchemaAlignment._updateStatementFromMergingStrategy(statement);
  }

  container.append(statement);
};

// further tweaks to the statement UI if the statement is to be deleted
SchemaAlignment._updateStatementFromMergingStrategy = function (statement) {
  var mode = statement.data('jsonMode');
  var strategyType = JSON.parse(statement.data('jsonMergingStrategy'))['type'];
  var references = statement.find(".wbs-references-section").first();
  var qualifiers = statement.find(".wbs-qualifiers-section").first();
  statement.removeClass();
  statement.addClass('wbs-statement');
  statement.addClass('wbs-statement-mode-'+mode);
  statement.addClass('wbs-statement-strategy-'+strategyType);
};

SchemaAlignment._statementToJSON = function (statement) {
    var inputContainer = statement.find(".wbs-target-input").first();
    var qualifiersList = new Array();
    var referencesList = new Array();
    var editingMode = statement.data('jsonMode');
    var mergingStrategy = JSON.parse(statement.data('jsonMergingStrategy'));
    var mergingStrategyType = mergingStrategy['type'];
    var qualifiersDom = statement.find('.wbs-qualifier-container').first().children();
    qualifiersDom.each(function () {
        var qualifierJSON = SchemaAlignment._qualifierToJSON($(this));
        qualifiersList.push(qualifierJSON);
    });
    var referencesDom = statement.find('.wbs-reference-container').first().children();
    if (editingMode !== 'delete') {
        referencesDom.each(function () {
            var referenceJSON = SchemaAlignment._referenceToJSON($(this));
            referencesList.push(referenceJSON);
        });
    }
    var valueJSON = null;
    if (!(editingMode === 'delete' && mergingStrategyType === 'property')) {
      valueJSON = SchemaAlignment._inputContainerToJSON(inputContainer);
    }
    return {
      value: valueJSON,
      qualifiers: qualifiersList,
      references: referencesList,
      mode: editingMode,
      mergingStrategy: mergingStrategy
    };
};

/**************
 * QUALIFIERS *
 **************/

SchemaAlignment._addQualifier = function(container, json) {
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
  var deleteButton = SchemaAlignment._makeDeleteButton()
            .addClass('wbs-remove-statement-group')
            .appendTo(toolbar1).on('click',function(e) {
    qualifier.remove();
    SchemaAlignment._hasChanged();
    e.preventDefault();
  });
  var statementContainer = $('<div></div>').addClass('wbs-statement-container').appendTo(right);
  SchemaAlignment._initPropertyField(inputContainer, statementContainer, property);
  if (value && property) {
    SchemaAlignment._addStatement(statementContainer, property.datatype, {value:value});
  } else {
    inputContainer.find('input').trigger('focus');
  }
};

SchemaAlignment._qualifierToJSON = function(elem) {
  var prop = elem.find(".wbs-prop-input").first();
  var target = elem.find(".wbs-target-input").first();
  var propJSON = SchemaAlignment._inputContainerToJSON(prop);
  var valueJSON = SchemaAlignment._inputContainerToJSON(target);
  return {
    prop: propJSON,
    value: valueJSON,
  };
};

/**************
 * REFERENCES *
 **************/

SchemaAlignment._addReference = function(container, json) {
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
        .text($.i18n('wikibase-schema/copy-reference'))
        .appendTo(referenceCopy)
        .on('click',function(e) {
     if (SchemaAlignment._copyReference(reference)) {
       $(this).text($.i18n('wikibase-schema/reference-copied'))
              .parent().addClass('wbs-copied-reference');
       container.parent().parent().find('.wbs-paste-reference').hide();
     }
     e.preventDefault();
  });
  var toolbarRef = $('<div></div>').addClass('wbs-toolbar').appendTo(referenceHeader);
  SchemaAlignment._makeDeleteButton().on('click',function(e) {
     reference.remove();
     SchemaAlignment._updateReferencesNumber(container);
     SchemaAlignment._hasChanged();
     e.preventDefault();
  }).appendTo(toolbarRef);
  var right = $('<div></div>').addClass('wbs-right').appendTo(reference);
  var qualifierContainer = $('<div></div>').addClass('wbs-qualifier-container').appendTo(right);
  var toolbar2 = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
  var addSnakButton = $('<a></a>').addClass('wbs-add-qualifier')
        .on('click',function(e) {
      SchemaAlignment._addQualifier(qualifierContainer, null);
      e.preventDefault();
  }).appendTo(toolbar2);
  SchemaAlignment._plusButton($.i18n('wikibase-schema/add-reference-snak'), addSnakButton);

  if (snaks) {
     for (var i = 0; i != snaks.length; i++) {
        SchemaAlignment._addQualifier(qualifierContainer, snaks[i]);
     }
  } else {
     SchemaAlignment._addQualifier(qualifierContainer, null);
  }
};

SchemaAlignment._referenceToJSON = function(reference) {
  var snaks = reference.find('.wbs-qualifier-container').first().children();
  var snaksList = new Array();
  snaks.each(function () {
      var qualifier = SchemaAlignment._qualifierToJSON($(this));
      snaksList.push(qualifier);
  });
  return {
    snaks:snaksList
  };
};

SchemaAlignment._updateReferencesNumber = function(container) {
  var childrenCount = container.children().length;
  var statement = container.parents('.wbs-statement');
  var a = statement.find('.wbs-references-toggle a').first();
  a.html(childrenCount+$.i18n('wikibase-schema/nb-references'));
};

SchemaAlignment._copyReference = function(reference) {
   // mark any other copied reference as not copied
   $('.wbs-copy-reference-button')
        .text($.i18n('wikibase-schema/copy-reference'));
   $('.wbs-copy-reference')
        .removeClass('wbs-copied-reference');
   var copiedReference = SchemaAlignment._referenceToJSON(reference);
   if (copiedReference !== null) {
      SchemaAlignment._copiedReference = copiedReference;
      $('.wbs-paste-reference').show();
      return true;
   } else {
      return false;
   }
};

/************************
 * FIELD INITIALIZATION *
 ************************/

SchemaAlignment._getPropertyType = function(pid, callback) {
  $.ajax({
      url: WikibaseManager.getSelectedWikibaseApi(),
      data: {
        action: "wbgetentities",
        format: "json",
        ids: pid,
        props: "datatype",
        origin: "*",
     },
     dataType: "json",
     success: function(data) {
        callback(data.entities[pid].datatype);
     }});
};

SchemaAlignment._initPropertyField = function(inputContainer, targetContainer, initialValue) {
  var input = $('<input></input>').appendTo(inputContainer);
  input.attr("placeholder", $.i18n('wikibase-schema/property-placeholder'));

  var endpoint = WikibaseManager.getSelectedWikibaseApiForEntityType('property');
  var suggestConfig = {
    mediawiki_endpoint: endpoint,
    entity_type: 'property',
    language: $.i18n("core-recon/wd-recon-lang"),
    view_url: WikibaseManager.getSelectedWikibaseSiteIriForEntityType('property')+'{{id}}'
  };
  
  input.suggestWikibase(suggestConfig).on("fb-select", function(evt, data) {
      SchemaAlignment._getPropertyType(data.id, function(datatype) {
        inputContainer.data("jsonValue", {
          type : "wbpropconstant",
          pid : data.id,
          label: data.name,
          datatype: datatype,
        });
        SchemaAlignment._addStatement(targetContainer, datatype, null);
        var addValueButtons = targetContainer.parent().find('.wbs-add-statement');
        var removeGroupButton = targetContainer.parent().find('.wbs-remove-statement-group');
        removeGroupButton.hide();
        addValueButtons.show();
      });
      SchemaAlignment._hasChanged();
    }).on("fb-textchange", function(evt, data) {
        inputContainer.data("jsonValue", null);
        targetContainer.find('.wbs-statement').remove();
        var addValueButtons = targetContainer.parent().find('.wbs-add-statement');
        var removeGroupButton = targetContainer.parent().find('.wbs-remove-statement-group');
        addValueButtons.hide();
        removeGroupButton.show();
    });
  // adds tweaks to display the validation status more clearly, like in Wikidata
  fixSuggestInput(input);

  // Init with the provided initial value.
  if (initialValue) {
     if (initialValue.type === "wbpropconstant") {
        input.val(initialValue.label);
        input.addClass('wbs-validated-input');
     } 
     inputContainer.data("jsonValue", initialValue);
  }

};

SchemaAlignment._initField = function(inputContainer, mode, initialValue, changedCallback) {
  var input = $('<input></input>').appendTo(inputContainer);
 
  if (! changedCallback) {
    changedCallback = SchemaAlignment._hasChanged;
  }

  // the mode for Wikibase entities has the form "wikibase-{entitytype}", such as "wikibase-item" or "wikibase-property"
  if (this._reconService !== null && (mode.startsWith("wikibase-") || mode === "unit")) {
    var entityType = null;
    if (mode.startsWith("wikibase-")) {
        let placeholderKey = 'wikibase-schema/entity-or-reconciled-column';
        if (mode === 'wikibase-item' || mode === 'wikibase-property' ||
            mode === 'wikibase-mediainfo' || mode === 'wikibase-lexeme' ||
            mode === 'wikibase-form' || mode === 'wikibase-sense') {
            placeholderKey = 'wikibase-schema/input-placeholder-for-'+mode;
        }
        input.attr("placeholder", $.i18n(placeholderKey));
	entityType = mode.slice("wikibase-".length);
    } else {
        input.attr("placeholder", $.i18n('wikibase-schema/unit'));
	entityType = "item";
    }
    var endpoint = WikibaseManager.getSelectedWikibaseApiForEntityType(entityType);
    var suggestConfig = {
      mediawiki_endpoint: endpoint,
      entity_type: entityType,
      language: $.i18n("core-recon/wd-recon-lang"),
      view_url: WikibaseManager.getSelectedWikibaseSiteIriForEntityType(entityType)+'{{id}}'
    };
    
    input.suggestWikibase(suggestConfig).on("fb-select", function(evt, data) {
        inputContainer.data("jsonValue", {
            type : "wbentityidvalueconstant",
            id : data.id,
            label: data.name,
        });
        changedCallback();
    });
    // adds tweaks to display the validation status more clearly, like in Wikidata
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
    input.on('change',function() {
      propagateValue($(this).val());
      changedCallback();
    });

    SchemaAlignment.setupStringInputValidation(input, /^(([\-]?\d{4}(-[0-1]\d(-[0-3]\d)?)?)|TODAY)$/);
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
    input.on('change',function() {
      propagateValue($(this).val());
      changedCallback();
    });

    SchemaAlignment.setupStringInputValidation(input, /^[\-+]?\d+(\.\d*)?[,\/][\-+]?\d+(\.\d*)?([,\/]\d+(\.\d*)?)?$/);
   } else if (mode === "language") {
     input.attr("placeholder", "lang");
     input.addClass("wbs-language-input");
     input.langsuggest().on("fb-select", function(evt, data) {
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
     };

     SchemaAlignment._initField(inputContainerLanguage, "language", langValue, propagateValue);
     SchemaAlignment._initField(inputContainerValue, "string", strValue, propagateValue);

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
     };
     
     SchemaAlignment._initField(inputContainerAmount, "amount", amountValue, propagateValue);
     SchemaAlignment._initField(inputContainerUnit, "unit", unitValue, propagateValue);

   } else {
    var propagateValue = function(val) {
	    var jsonValue = null;
		if (val) {
			jsonValue = {type: "wbstringconstant", value: val};
		}
        inputContainer.data("jsonValue", jsonValue);
    };
    propagateValue(null);
    input.on('change',function() {
      propagateValue($(this).val());
      changedCallback();
    });
    if (mode === "amount") {
        input.attr("placeholder", $.i18n('wikibase-schema/amount'));
        SchemaAlignment.setupStringInputValidation(input, /^[\-+]?\d+(\.\d*)?(E[\-+]\d+)?$/);
    } else if (mode === "url") {
        input.attr("placeholder", $.i18n('wikibase-schema/full-url'));
        SchemaAlignment.setupStringInputValidation(input, /^https?:\/\/.+$/);
    } else if (mode === "tabular-data") {
        input.attr("placeholder", $.i18n('wikibase-schema/tabular-data-with-prefix'));
        SchemaAlignment.setupStringInputValidation(input, /^Data:.+$/);
    } else if (mode === "commonsMedia") {
        input.attr("placeholder", $.i18n('wikibase-schema/commons-media'));
    } else if (mode === "math") {
        input.attr("placeholder", $.i18n('wikibase-schema/math-expression'));
    } else if (mode === "geo-shape") {
        input.attr("placeholder", $.i18n('wikibase-schema/geoshape-with-prefix'));
        SchemaAlignment.setupStringInputValidation(input, /^Data:.+$/);
    } else if (mode === "filepath") {
        input.attr("placeholder", $.i18n('wikibase-schema/file-path-placeholder'));
    } else if (mode === "filename") {
        input.attr("placeholder", $.i18n('wikibase-schema/file-name-placeholder'));
    } else if (mode === "wikitext") {
        input.attr("placeholder", $.i18n('wikibase-schema/wikitext-placeholder'));
    } else {
        // Assume that other datatypes are string-based
        SchemaAlignment.setupStringInputValidation(input, /^.+$/);
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
    var deleteButton = SchemaAlignment._makeDeleteButton(true).appendTo(column);
    deleteButton.attr('alt', $.i18n('wikibase-schema/remove-column'));
    deleteButton.on('click',function (e) {
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
  var entityType = null;
  if (mode.startsWith("wikibase-") || mode === "unit") {
      acceptClass = ".wbs-reconciled-column";
      wbVariableType = "wbentityvariable";
      if (mode === "unit") {
        entityType = "item";
      } else {
        entityType = mode.slice("wikibase-".length);
      }
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
        if (entityType !== null) {
          // check that the siteIRI of the reconciled column matches
          // the expected one.
          var expectedSiteIRI = WikibaseManager.getSelectedWikibaseSiteIriForEntityType(entityType);
          var actualSiteIRI = ui.draggable.data('reconciledSiteIRI');
          if (actualSiteIRI !== expectedSiteIRI) {
            alert($.i18n('wikibase-schema/incompatible-site-iri-reconciled-column', actualSiteIRI, expectedSiteIRI));
            return false;
          }
        }
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
     if (initialValue.type === "wbentityidvalueconstant" ||
        /* item for backwards-compatibility purposes */
        initialValue.type === "wbitemconstant" || initialValue.type === "wbpropconstant") {
        input.val(initialValue.label);
        input.addClass("wbs-validated-input");
     } else if (initialValue.type == "wbentityvariable") {
        var cell = SchemaAlignment._createDraggableColumn(initialValue.columnName, true);
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
        var cell = SchemaAlignment._createDraggableColumn(initialValue.columnName, false);
        acceptDraggableColumn(cell);
     }
     inputContainer.data("jsonValue", initialValue);
  }
};

SchemaAlignment.setupStringInputValidation = function(input, regex) {
  input.on('focus',function() {
    input.removeClass('wbs-unvalidated-input');
  }).on('blur',function() {
    var currentValue = input.val();
    if (regex.test(currentValue)) {
       input.addClass('wbs-validated-input');
    } else {
       input.addClass('wbs-unvalidated-input');
    }
  });
};

SchemaAlignment._inputContainerToJSON = function (inputContainer) {
    var data = inputContainer.data();
    if (data && 'jsonValue' in data) {
       return data.jsonValue;
    } else {
       return null;
    }
};

SchemaAlignment._removeStatement = function(statement) {
  var statementGroup = statement.parents('.wbs-statement-group, .wbs-qualifier').first();
  statement.remove();
  var remainingStatements = statementGroup.find('.wbs-statement').length;
  if (remainingStatements === 0) {
      statementGroup.remove();
  }
  SchemaAlignment._hasChanged();
};

SchemaAlignment._openStatementConfigurationDialog = function(statement) {
  StatementConfigurationDialog.launch(statement);
};

SchemaAlignment.getJSON = function() {
  var list = [];
  var entitiesDom = $('#schema-alignment-entity-edits-container .wbs-entity');
  entitiesDom.each(function () {
     var entityJSON = SchemaAlignment._entityToJSON($(this));
     if (entityJSON !== null) {
        list.push(entityJSON);
     }
  });
  if (list.length === entitiesDom.length) {
    return {
        entityEdits: list,
        siteIri: WikibaseManager.getSelectedWikibaseSiteIri(),
        entityTypeSiteIRI: Object.fromEntries(WikibaseManager.getSelectedWikibaseAvailableEntityTypes().map(et => [et, WikibaseManager.getSelectedWikibaseSiteIriForEntityType(et)])),
        mediaWikiApiEndpoint: WikibaseManager.getSelectedWikibaseApi()
    };
  } else {
    return null;
  }
};

SchemaAlignment._hasChanged = function() {
  SchemaAlignment._hasUnsavedChanges = true;
  SchemaAlignment.preview();
  SchemaAlignment._unsavedIndicator.show();
  SchemaAlignment._schemaElmts.saveButton
        .prop('disabled', false)
        .removeClass('disabled');
  SchemaAlignment._schemaElmts.discardButton
        .prop('disabled', false)
        .removeClass('disabled');
   $('.wbs-copy-reference-button')
        .text($.i18n('wikibase-schema/copy-reference'));
   $('.wbs-copy-reference')
        .removeClass('wbs-copied-reference');
};

SchemaAlignment.updateNbEdits = function(nb_edits) {
  this._previewElmts.previewExplanation.html($.i18n('wikibase-schema/preview-explanation',
      nb_edits,
      WikibaseManager.getSelectedWikibaseMainPage(),
      WikibaseManager.getSelectedWikibaseName()
      ));
};

SchemaAlignment.preview = function() {
  var self = this;
  var countsElem = this.issuesTabCount;

  $('.invalid-schema-warning').hide();
  self.schemaValidationErrorsInPreview.empty();
  self.schemaValidationErrorsInIssues.empty();
  this._previewPanes.empty();
  self._issuesElmts.warningsArea.empty();
  self._issuesElmts.schemaValidationErrors.empty();
  this.updateNbEdits(0);
  var schema = this.getJSON();
  if (schema === null) {
    $('.invalid-schema-warning').show();
    return;
  }
  this.issueSpinner.show();
  this.previewSpinner.show();
  Refine.postCSRF(
    "command/wikidata/preview-wikibase-schema?" + $.param({ project: theProject.id }),
    { schema: JSON.stringify(schema), manifest: JSON.stringify(WikibaseManager.getSelectedWikibase()), engine: JSON.stringify(ui.browsingEngine.getJSON()) },
    function(data) {
      self.issueSpinner.hide();
      self.previewSpinner.hide();
      if ("edits_preview" in data) {
        var previewContainer = self._previewPanes[0];
        EditRenderer.renderEdits(data.edits_preview, previewContainer);
        self.updateNbEdits(data["edit_count"]);
      }

      // update the counts in the issues tab
     var numWarnings = data.warnings ? data.nb_warnings : 0;
     var numErrors = data.errors ? data.errors.length : 0;
     var totalCount = numErrors + numWarnings;
     countsElem.hide();
     if (totalCount) {
       countsElem.text(totalCount);
       countsElem.show();
     }

     if (data.warnings) {
         self._updateWarnings(data.warnings);
     } else {
         self._updateWarnings([]);
     }

      if ("code" in data && data.code === "error" && data.reason == 'invalid-schema') {
         $('.invalid-schema-warning').show();
         SchemaAlignment._renderSchemaValidationErrors(self.schemaValidationErrorsInPreview, data.errors);
         SchemaAlignment._renderSchemaValidationErrors(self.schemaValidationErrorsInIssues, data.errors);
      }
    },
    "json",
    function(error) {
      self.issueSpinner.hide();
      self.previewSpinner.hide();
      console.log("Error while previewing the schema:");
      console.log(error);
    }
  );
};

// Used for injecting tabs in any project where the schema has been defined.
SchemaAlignment.onProjectUpdate = function(options) {
  if(theProject.overlayModels.wikibaseSchema && !SchemaAlignment.isSetUp()) {
    SchemaAlignment.setUpTabs();
  }
  if (SchemaAlignment.isSetUp() && (options.everythingChanged || options.modelsChanged ||
      options.rowsChanged || options.rowMetadataChanged || options.cellsChanged || options.engineChanged)) {
    if (!SchemaAlignment._hasUnsavedChanges) {
      SchemaAlignment._discardChanges();
    }
    SchemaAlignment.updateColumns();
    SchemaAlignment.preview();
  }
};

/*************************
 * WARNINGS RENDERING *
 *************************/

SchemaAlignment._updateWarnings = function(warnings) {
   var self = this;
   
   self._issuesElmts.warningsArea.empty();
   
   var table = $('<table></table>').appendTo(self._issuesElmts.warningsArea);
   for (var i = 0; i != warnings.length; i++) {
      var rendered = WarningsRenderer._renderWarning(warnings[i]);
      rendered.appendTo(table);
   }   

};

/************************************
 * VALIDATION ERRORS FOR THE SCHEMA *
 ************************************/

SchemaAlignment._renderSchemaValidationErrors = function(container, errors) {
  let ul = $('<ul></ul>')
        .addClass('schema-validation-error-list')
        .appendTo(container);
  for (const error of errors) {
    let li = $('<li></li>').appendTo(ul);
    for (const pathElement of error.path) {
      let span = $('<span></span>')
        .addClass('schema-validation-path-element')
        .appendTo(li);
      let localizationKey = 'wikibase-schema/schema-validation/path-element/'+pathElement.type;
      let pathElementText = '';
      if (pathElement.position !== -1 && pathElement.position !== undefined) {
        pathElementText = $.i18n(localizationKey+'/with-position', pathElement.position + 1);
      } else if (pathElement.name) {
        pathElementText = $.i18n(localizationKey+'/with-name', pathElement.name);
      } else {
        pathElementText = $.i18n(localizationKey);
      }
      span.text(pathElementText); 
    }
    var message = $('<span></span>')
        .addClass('schema-validation-error')
        .text(error.message)
        .appendTo(li);
  }
}
