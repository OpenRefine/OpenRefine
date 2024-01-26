/**
 * renders an item update (an edit on an item) in HTML.
 */

var EditRenderer = {};

// settings
EditRenderer.maxTerms = 15; // max number of terms displayed
EditRenderer.maxStatements = 25; // max number of statements per statement group

// main method: takes a DOM element and a list
// of edits to render there.
EditRenderer.renderEdits = function(edits, container) {
   for(var i = 0; i < edits.length; i++) {
	  if (edits[i].type === 'item') {
		EditRenderer._renderItem(edits[i], container);
	  } else if (edits[i].type === 'mediainfo') {
		EditRenderer._renderMediaInfo(edits[i], container);
	  } else {
		console.error("unsupported entity type to be rendered in preview tab: "+edits[i].type);
	  }
   }
};

/**************/
/*** ITEMS ****/
/**************/

EditRenderer._renderItem = function(json, container) {
  var item = $('<div></div>').addClass('wbs-item').appendTo(container);
  var inputContainer = $('<div></div>').addClass('wbs-item-input').appendTo(item);
  EditRenderer._renderEntity(json.subject, inputContainer);
  var right = $('<div></div>').addClass('wbs-item-contents').appendTo(item);

  // Terms
  if ((json.labels && json.labels.length) ||
      (json.labelsIfNew && json.labelsIfNew.length) ||
      (json.descriptions && json.descriptions.length) ||
      (json.descriptionsIfNew && json.descriptionsIfNew.length) ||
      (json.addedAliases && json.addedAliases.length)) {
    var termsContainer = $('<div></div>').addClass('wbs-namedesc-container')
        .appendTo(right);
    
    this._renderTermsList(json.labels, "label-override", termsContainer);
    this._renderTermsList(json.labelsIfNew, "label-if-new", termsContainer);
    this._renderTermsList(json.descriptions, "description-override", termsContainer);
    this._renderTermsList(json.descriptionsIfNew, "description-if-new", termsContainer);
    this._renderTermsList(json.addedAliases, "alias", termsContainer);

    // Clear the float
    $('<div></div>').attr('style', 'clear: right').appendTo(right);
  }

  // Statements
  if (json.statementGroups && json.statementGroups.length) {
    // $('<div></div>').addClass('wbs-statements-header')
    //        .text($.i18n('wikibase-schema/statements-header')).appendTo(right);
    var statementsGroupContainer = $('<div></div>').addClass('wbs-statement-group-container')
            .appendTo(right);
    for(var i = 0; i != json.statementGroups.length; i++) {
       EditRenderer._renderStatementGroup(json.statementGroups[i], statementsGroupContainer);
    }
  }
};

/***************************/
/*** MEDIAINFO ENTITIES ****/
/***************************/

EditRenderer._renderMediaInfo = function(json, container) {
  var mediainfo = $('<div></div>').addClass('wbs-entity').addClass('wbs-mediainfo').appendTo(container);
  var inputContainer = $('<div></div>').addClass('wbs-mediainfo-input').appendTo(mediainfo);
  EditRenderer._renderEntity(json.subject, inputContainer);
  var right = $('<div></div>').addClass('wbs-entity-contents').appendTo(mediainfo);

  // File-related fields
  if (json.filePath || json.fileName || json.wikitext) {
	  var fileFields = $('<div></div>').addClass('wbs-mediainfo-file-fields').appendTo(right);
	  
      // File path
      if (json.filePath) {
		  $('<span></span>').text($.i18n('wikibase-schema/mediainfo-file-path'))
			.appendTo(fileFields);
		  $('<span></span>')
		    .addClass('wbs-file-path-input')
			.text(json.filePath)
		    .appendTo(fileFields);
      }
	
      // File name
      if (json.fileName) {
		  $('<span></span>').text($.i18n('wikibase-schema/mediainfo-file-name'))
			.appendTo(fileFields);
		  $('<span></span>')
		    .addClass('wbs-file-name-input')
			.text(json.fileName)
		    .appendTo(fileFields);
      }

      // Wikitext
      if (json.wikitext) {
		  $('<span></span>').text($.i18n(
                                json.overrideWikitext ?
                                'wikibase-schema/mediainfo-wikitext-override' :
                                'wikibase-schema/mediainfo-wikitext-no-override'))
			.appendTo(fileFields);
		  $('<span></span>')
		    .addClass('wbs-wikitext-input')
			.text(json.wikitext)
		    .appendTo(fileFields);
      }
   }

  // Terms
  if ((json.labels && json.labels.length) ||
      (json.labelsIfNew && json.labelsIfNew.length)) {
    var termsContainer = $('<div></div>').addClass('wbs-namedesc-container')
        .appendTo(right);
    
    this._renderTermsList(json.labels, "caption-override", termsContainer);
    this._renderTermsList(json.labelsIfNew, "caption-if-new", termsContainer);

    // Clear the float
    $('<div></div>').attr('style', 'clear: right').appendTo(right);
  }

  // Statements
  if (json.statementGroups && json.statementGroups.length) {
    // $('<div></div>').addClass('wbs-statements-header')
    //        .text($.i18n('wikibase-schema/statements-header')).appendTo(right);
    var statementsGroupContainer = $('<div></div>').addClass('wbs-statement-group-container')
            .appendTo(right);
    for(var i = 0; i != json.statementGroups.length; i++) {
       EditRenderer._renderStatementGroup(json.statementGroups[i], statementsGroupContainer);
    }
  }
};


/**************************
 * NAMES AND DESCRIPTIONS *
 **************************/

EditRenderer._renderTermsList = function(termList, termType, termsContainer) {
    if(!termList) {
        return;
    }
    for(var i = 0; i != Math.min(termList.length, this.maxTerms); i++) {
        EditRenderer._renderTerm(termType, termList[i], termsContainer);
    }
    if(termList.length > this.maxTerms) {
        $('<div></div>').addClass('wbs-namedesc').text('...').appendTo(termsContainer);
    }
};

EditRenderer._renderTerm = function(termType, json, container) {
  var namedesc = $('<div></div>').addClass('wbs-namedesc').appendTo(container);
  var type_container = $('<div></div>').addClass('wbs-namedesc-type').appendTo(namedesc);
  var type_span = $('<span></span>').appendTo(type_container)
        .text($.i18n('wikibase-schema/'+termType));

  var right = $('<div></div>').addClass('wbs-right').appendTo(namedesc);
  var value_container = $('<div></div>').addClass('wbs-namedesc-value').appendTo(namedesc);
  EditRenderer._renderValue({datavalue:json,datatype:'monolingualtext'}, value_container); 
};

/********************
 * STATEMENT GROUPS *
 ********************/

EditRenderer._renderStatementGroup = function(json, container) {

  var statementGroup = $('<div></div>').addClass('wbs-statement-group').appendTo(container);
  var inputContainer = $('<div></div>').addClass('wbs-prop-input').appendTo(statementGroup);
  var right = $('<div></div>').addClass('wbs-right').appendTo(statementGroup);
  EditRenderer._renderEntity(json.property, inputContainer);

  var statementContainer = $('<div></div>').addClass('wbs-statement-container').appendTo(right);
  for (var i = 0; i != json.statementEdits.length; i++) {
     EditRenderer._renderStatement(json.statementEdits[i], statementContainer);
  }
  if(json.statementEdits.length > EditRenderer.maxStatements) {
     $('<div></div>')
         .text('...')
         .addClass('wbs-statement')
         .appendTo(statementContainer);
  }
};

/**************
 * STATEMENTS *
 **************/

EditRenderer._renderStatement = function(json, container) {
 
  var statement = $('<div></div>').addClass('wbs-statement').appendTo(container);
  var inputContainer = $('<div></div>').addClass('wbs-target-input').appendTo(statement);
  var mode = json.mode;
  var strategyType = json.mergingStrategy.type;
  if (!(mode === 'delete' && strategyType === 'property')) {
    EditRenderer._renderValue(json.statement.mainsnak, inputContainer);
  } else {
    inputContainer.append(
      $('<span></span>').addClass('wbs-value-placeholder')
        .text($.i18n('wikibase-preview/delete-all-existing-statements'))
    );
  }

  // mode and strategy
  statement.addClass('wbs-statement-mode-' + json.mode);
  statement.addClass('wbs-statement-strategy-' + json.mergingStrategy.type);

  // add rank
  var rank = $('<div></div>').addClass('wbs-rank-selector-icon').prependTo(inputContainer);

  // add qualifiers...
  var qualifiersSection = $('<div></div>').addClass('wbs-qualifiers-section').appendTo(statement);
  var right = $('<div></div>').addClass('wbs-right').appendTo(qualifiersSection);
  var qualifierContainer = $('<div></div>').addClass('wbs-qualifier-container').appendTo(right);

  if (json.statement.qualifiers) {
    for (var pid_id in json.statement['qualifiers-order']) {
      var pid = json.statement['qualifiers-order'][pid_id];
      if (json.statement.qualifiers.hasOwnProperty(pid)) {
        var qualifiers = json.statement.qualifiers[pid];
        for (var i = 0; i != qualifiers.length; i++) {
          EditRenderer._renderSnak(qualifiers[i], qualifierContainer);
        }
      }
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
  referencesToggleContainer.on('click',function () {
      triangle.toggleClass('pointing-down');
      triangle.toggleClass('pointing-right');
      referenceContainer.toggle(100);
  });
  referenceContainer.hide();

  if (json.statement.references) {
      for (var i = 0; i != json.statement.references.length; i++) {
          EditRenderer._renderReference(json.statement.references[i], referenceContainer);
      }
  }
  EditRenderer._updateReferencesNumber(referenceContainer);
};

/*********************************
 * QUALIFIER AND REFERENCE SNAKS *
 *********************************/

EditRenderer._renderSnak = function(json, container) {

  var qualifier = $('<div></div>').addClass('wbs-qualifier').appendTo(container);
  var toolbar1 = $('<div></div>').addClass('wbs-toolbar').appendTo(qualifier);
  var inputContainer = $('<div></div>').addClass('wbs-prop-input').appendTo(qualifier);
  var right = $('<div></div>').addClass('wbs-right').appendTo(qualifier);
  var statementContainer = $('<div></div>').addClass('wbs-statement-container').appendTo(right);
  EditRenderer._renderEntity(json.full_property, inputContainer);
  EditRenderer._renderValue(json, statementContainer);
};

/**************
 * REFERENCES *
 **************/

EditRenderer._renderReference = function(json, container) {
  var reference = $('<div></div>').addClass('wbs-reference').appendTo(container);
  var referenceHeader = $('<div></div>').addClass('wbs-reference-header').appendTo(reference);
  var right = $('<div></div>').addClass('wbs-right').appendTo(reference);
  var qualifierContainer = $('<div></div>').addClass('wbs-qualifier-container').appendTo(right);

  for (var pid in json.snaks) {
    if (json.snaks.hasOwnProperty(pid)) {
      var snaks = json.snaks[pid];
      for(var i = 0; i != snaks.length; i++) {
          EditRenderer._renderSnak(snaks[i], qualifierContainer);
      }
    }
  }
}

EditRenderer._updateReferencesNumber = function(container) {
  var childrenCount = container.children().length;
  var statement = container.parents('.wbs-statement');
  var a = statement.find('.wbs-references-toggle a').first();
  a.html(childrenCount+$.i18n('wikibase-schema/nb-references'));
};

/*******************
 * VALUE RENDERING *
 *******************/

EditRenderer.renderedValueCache = {};

EditRenderer._renderEntity = function(json, container) {
  var html = WarningsRenderer._renderEntity(json);
  $(html).appendTo(container);
};

EditRenderer._renderValue = function(json, container) {
  var input = $('<span></span>').appendTo(container);
  var mode = json.datatype;
  
  if (mode === "wikibase-item" || mode === "wikibase-property") {
    EditRenderer._renderEntity(json.datavalue, container);
  } else if (json.snaktype === "somevalue") {
    $('<span></span>')
      .text($.i18n('wikibase-schema/some-value'))
      .appendTo(container);
  } else if (json.snaktype === "novalue") {
    $('<span></span>')
      .text($.i18n('wikibase-schema/no-value'))
      .appendTo(container);
  } else {
    var jsonValue = JSON.stringify(json.datavalue);
    var fullJsonValue = JSON.stringify(json);
    if (fullJsonValue in EditRenderer.renderedValueCache) {
        $('<span>'+EditRenderer.renderedValueCache[fullJsonValue]+'</span>').appendTo(container);
    } else {
        var params = {
            action: 'wbformatvalue',
            generate: 'text/html',
            datavalue: jsonValue,
            options: '{"lang":"'+$.i18n('core-recon/wd-recon-lang')+'"}',
            format: 'json'
        };
        if ('property' in json) {
            params.property = json.property;
        } else {
            params.datatype = json.datatype;
        }
        $.get(
            WikibaseManager.getSelectedWikibaseApi(),
            params,
            function (data) {
                if('result' in data) {
                    EditRenderer.renderedValueCache[fullJsonValue] = data.result;
                    $('<span>'+data.result+'</span>').appendTo(container);
                }
            },
            'jsonp'
        );
    }
  }
};


