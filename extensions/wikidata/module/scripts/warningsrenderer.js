var WarningsRenderer = {};

// renders a Wikibase entity into a link
WarningsRenderer._renderEntity = function(entity) {
  if (!entity.id && entity.value) {
      entity.id = entity.value.id;
  }
  var id = entity.id;
  var is_new = entity.siteIri == "http://localhost/entity/";
  if (is_new) {
     id = $.i18n('wikidata-preview/new-id');
  }
  var fullLabel = id;
  if (entity.label) {
      fullLabel = entity.label + ' (' + id + ')';
  }

  var url = entity.iri;
  if (!url && entity.value) {
     url = 'http://www.wikidata.org/entity/'+entity.value.id;
  }

  if (is_new) {
     return '<span class="wb-preview-new-entity">'+fullLabel+'</span>';
  } else {
     return '<a href="'+url+'" class="wb-preview-entity" target="_blank">'+fullLabel+'</a>';
  }
}

// replaces the issue properties in localization template
WarningsRenderer._replaceIssueProperties = function(template, properties) {
  if (!properties) {
    return template;
  }
  var expanded = template;
  for (var key in properties) {
    if (properties.hasOwnProperty(key)) {
       var rendered = properties[key];
       if (key.endsWith('_entity')) {
          rendered = WarningsRenderer._renderEntity(properties[key]);
       }
       expanded = expanded.replace(new RegExp('{'+key+'}', 'g'), rendered);
    }
  }
  return expanded;
}     

WarningsRenderer._renderWarning = function(warning) {
  var title = WarningsRenderer._replaceIssueProperties($.i18n('warnings-messages/'+warning.type+'/title'), warning.properties);
  var body = WarningsRenderer._replaceIssueProperties($.i18n('warnings-messages/'+warning.type+'/body'), warning.properties);
  var tr = $('<tr></tr>').addClass('wb-warning');
  var severityTd = $('<td></td>')
       .addClass('wb-warning-severity')
       .addClass('wb-warning-severity-'+warning.severity)
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
  var countTd = $('<td></td>')
       .addClass('wb-warning-count')
       .appendTo(tr);
  var countSpan = $('<span></span>')
       .text(warning.count)
       .appendTo(countTd);
  return tr;
}


