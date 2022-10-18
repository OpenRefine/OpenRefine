/**
 * Manages schema templates
 */

const WikibaseTemplateManager = {
  /**
   * Keys: Wikibase instance names
   * Values: Array of objects, with two keys:
   *   - 'name': the name of the template
   *   - 'schema': the (potentially incomplete) Wikibase schema
   */
  templates: {}
};

// returns all the templates for a given Wikibase, in the form of an array 
WikibaseTemplateManager.getTemplates = function (wikibaseName) {
  let templates = WikibaseTemplateManager.templates[wikibaseName];
  return templates === undefined ? [] : templates;
}

// returns the template identified by the user-supplied name
WikibaseTemplateManager.getTemplate = function (wikibaseName, templateName) {
  let templates = WikibaseTemplateManager.getTemplates(wikibaseName);
  let matching = templates.filter(t => t.name === templateName);
  if (matching.length > 0) {
    return matching[0];
  } else {
    return undefined;
  }
}

// adds a new template for the given Wikibase instance 
WikibaseTemplateManager.addTemplate = function (wikibaseName, templateName, schema) {
  let template = {
      name: templateName,
      schema: schema
  };
  let templates = WikibaseTemplateManager.templates[wikibaseName];
  if (templates === undefined) {
     templates = [];
  }
  templates = templates.filter(existingTemplate => existingTemplate.name !== templateName);
  templates.push(template);
  WikibaseTemplateManager.templates[wikibaseName] = templates;
}

// deletes a template
WikibaseTemplateManager.deleteTemplate = function (wikibaseName, templateName) {
  let templates = WikibaseTemplateManager.getTemplates(wikibaseName);
  let newTemplates = templates.filter(template => template.name !== templateName);
  WikibaseTemplateManager.templates[wikibaseName] = newTemplates;
}

// renames a template
WikibaseTemplateManager.renameTemplate = function (wikibaseName, oldTemplateName, newTemplateName) {
  let newTemplate = WikibaseTemplateManager.getTemplate(wikibaseName, oldTemplateName);
  if (newTemplate === undefined) {
    return;
  }
  newTemplate.name = newTemplateName;
  let newTemplates = WikibaseTemplateManager.getTemplates(wikibaseName).map(function (template) {
    if (template.name === oldTemplateName) {
      return newTemplate;
    } else {
      return template;
    }
  });
  WikibaseTemplateManager.templates[wikibaseName] = newTemplates;
}

// loads all the templates from the backend
WikibaseTemplateManager.loadTemplates = function(onDone) {
  $.ajax({
    url: "command/core/get-preference?" + $.param({
      name: "wikibase.templates"
    }),
    success: function (data) {
      WikibaseTemplateManager.templates = {};
      if (data.value && data.value !== "null" && data.value !== "[]") {
        // the object is wrapped into an array because the backend does not
        // allow storing arbitrary JSON objects otherwise (sigh...)
        let dummyList = JSON.parse(data.value);
        if (dummyList.length === 1) {
          WikibaseTemplateManager.templates = dummyList[0];
        } 
      }
      if (onDone) {
        onDone();
      }
    },
    dataType: "json"
  });
}

// saves all the templates to the backend
WikibaseTemplateManager.saveTemplates = function() {
  Refine.wrapCSRF(function (token) {
    $.ajax({
      async: false,
      type: "POST",
      url: "command/core/set-preference?" + $.param({
        name: "wikibase.templates"
      }),
      data: {
        "value": JSON.stringify([WikibaseTemplateManager.templates]),
        csrf_token: token
      },
      dataType: "json"
    });
  });

}
