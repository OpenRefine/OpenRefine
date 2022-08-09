var WarningsRenderer = {};

// renders a Wikibase entity into a link
WarningsRenderer._renderEntity = function (entity, plainText) {
  if (!entity.id && entity.value) {
    entity.id = entity.value.id;
  }
  var id = entity.id;
  var is_new = entity.siteIri === "http://localhost/entity/";
  if (is_new) {
    id = $.i18n('wikibase-preview/new-id');
  }
  var fullLabel = id;
  if (entity.label) {
    fullLabel = entity.label + ' (' + id + ')';
  }
  if (plainText) {
    return fullLabel;
  }

  var url = entity.iri;
  if (!url && entity.value) {
    url = WikibaseManager.getSelectedWikibaseSiteIri() + entity.value.id;
  }

  if (is_new) {
    return '<span class="wb-preview-new-entity">' + fullLabel + '</span>';
  } else {
    return '<a href="' + url + '" class="wb-preview-entity" target="_blank">' + fullLabel + '</a>';
  }
};

// replaces the issue properties in localization template
WarningsRenderer._replaceIssueProperties = function (template, properties, plainText) {
  template = template.replace(new RegExp('{wikibase_name}', 'g'), WikibaseManager.getSelectedWikibaseName);
  if (!properties) {
    return template;
  }
  var expanded = template;
  for (var key in properties) {
    if (properties.hasOwnProperty(key)) {
      var rendered = properties[key];
      if (key.endsWith('_entity')) {
        rendered = WarningsRenderer._renderEntity(properties[key], plainText);
      }
      expanded = expanded.replace(new RegExp('{' + key + '}', 'g'), rendered);
    }
  }
  return expanded;
};

WarningsRenderer._createFacetForWarning = function (warning) {
  var warningRaised = $.i18n('wikibase-issues/warning-raised');
  var noWarning = $.i18n('wikibase-issues/no-warning');
  var title = WarningsRenderer._replaceIssueProperties($.i18n('warnings-messages/' + warning.type + '/title'), warning.properties, true);
  ui.browsingEngine.addFacet(
      'list',
      {
          columnName: '',
          name: title,
          expression: 'grel:if(wikibaseIssues().inArray('+JSON.stringify(warning.aggregationId)+'), '+JSON.stringify(warningRaised)+', '+JSON.stringify(noWarning)+')',
          selection: [
              {
              v: {
                  v: warningRaised,
                  l: warningRaised
              }
              }
          ],
          selectBlank: false,
          selectError: false,
          omitBlank: false,
          omitError: false,
          invert: false
      },
      {scroll: false}
  );

  // switch to the grid
  SchemaAlignment.switchTab('#view-panel');
};

WarningsRenderer._renderWarning = function (warning, onLocateRows) {
  var title = WarningsRenderer._replaceIssueProperties($.i18n('warnings-messages/' + warning.type + '/title'), warning.properties);
  var body = WarningsRenderer._replaceIssueProperties($.i18n('warnings-messages/' + warning.type + '/body'), warning.properties);
  var tr = $('<tr></tr>').addClass('wb-warning');
  var severityTd = $('<td></td>')
      .addClass('wb-warning-severity')
      .addClass('wb-warning-severity-' + warning.severity)
      .appendTo(tr);
  var bodyTd = $('<td></td>')
      .addClass('wb-warning-body')
      .appendTo(tr);
  var h1 = $('<h1></h1>')
      .html(title)
      .appendTo(bodyTd);
  var p = $('<p></p>')
      .html(body)
      .appendTo(bodyTd);
  if (warning.facetable) {
    var facetingButton = $('<button></button>')
        .addClass('button')
        .text($.i18n('wikibase-issues/locate-offending-rows'))
        .appendTo(bodyTd);
    facetingButton.on('click', function(evt) {
        if (onLocateRows) {
          onLocateRows();
        }

        // the faceting relies on having an up to date schema
        var onSaved = function() {
          WarningsRenderer._createFacetForWarning(warning);
        };
        if (SchemaAlignment._hasUnsavedChanges) {
           SchemaAlignment._save(onSaved);
        } else {
           onSaved();
        }
        evt.preventDefault();
    });
  }
  var countTd = $('<td></td>')
      .addClass('wb-warning-count')
      .appendTo(tr);
  var countSpan = $('<span></span>')
      .text(warning.count)
      .appendTo(countTd);
  return tr;
};


