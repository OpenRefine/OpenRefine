// Load the localization file
var dictionary = {};
$.ajax({
  url: "command/core/load-language?",
  type: "POST",
  async: false,
  data: {
    module: "wikidata",
//		lang : lang
  },
  success: function (data) {
    dictionary = data['dictionary'];
    lang = data['lang'];
  }
});
$.i18n().load(dictionary, lang);


ExporterManager.MenuItems.push({});
ExporterManager.MenuItems.push({
  id: "performWikibaseEdits",
  label: $.i18n('wikibase-extension/wikibase-edits'),
  click: function () {
    PerformEditsDialog.checkAndLaunch();
  }
});
ExporterManager.MenuItems.push({
  id: "exportQuickStatements",
  label: $.i18n('wikibase-extension/qs-file'),
  click: function () {
    WikibaseExporterMenuBar.checkSchemaAndExport("quickstatements");
  }
});
ExporterManager.MenuItems.push({
  id: "exportWikibaseSchema",
  label: $.i18n('wikibase-extension/wikibase-schema'),
  click: function () {
    WikibaseExporterMenuBar.checkSchemaAndExport("wikibase-schema");
  }
});

WikibaseExporterMenuBar = {};

WikibaseExporterMenuBar.exportTo = function (format) {
  var targetUrl = null;
  if (format === "quickstatements") {
    targetUrl = "statements.txt";
  } else {
    targetUrl = "schema.json";
  }
  var form = document.createElement("form");
  $(form).css("display", "none")
      .attr("method", "post")
      .attr("action", "command/core/export-rows/" + targetUrl)
      .attr("target", "openrefine-export-" + format);
  $('<input />')
      .attr("name", "engine")
      .val(JSON.stringify(ui.browsingEngine.getJSON()))
      .appendTo(form);
  $('<input />')
      .attr("name", "project")
      .val(theProject.id)
      .appendTo(form);
  $('<input />')
      .attr("name", "format")
      .val(format)
      .appendTo(form);

  document.body.appendChild(form);

  window.open("about:blank", "openrefine-export");
  form.submit();

  document.body.removeChild(form);
};

WikibaseExporterMenuBar.checkSchemaAndExport = function (format) {
  var onSaved = function (callback) {
    WikibaseExporterMenuBar.exportTo(format);
  };
  if (!SchemaAlignment.isSetUp()) {
    SchemaAlignment.launch(null);
  } else if (SchemaAlignment._hasUnsavedChanges) {
    SchemaAlignment._save(onSaved);
  } else {
    onSaved();
  }
};

//extend the column header menu
$(function () {

  ExtensionBar.MenuItems.push(
      {
        "id": "reconcile",
        "label": $.i18n('wikibase-extension/menu-label'),
        "submenu": [
          {
            id: "wikidata/select-instance",
            label: $.i18n('wikibase-extension/select-wikibase-instance'),
            click: function () {
              WikibaseDialog.launch()
            }
          },
          {
            id: "wikidata/edit-schema",
            label: $.i18n('wikibase-extension/edit-wikibase-schema'),
            click: function () {
              SchemaAlignment.launch(false);
            }
          },
          {
            id: "wikidata/manage-account",
            label: $.i18n('wikibase-extension/manage-wikibase-account'),
            click: function () {
              ManageAccountDialog.checkAndLaunch();
            }
          },
          {},
          {
            id: "wikidata/import-schema",
            label: $.i18n('wikibase-extension/import-wikibase-schema'),
            click: function () {
              ImportSchemaDialog.launch();
            }
          },
          {
            id: "wikidata/export-schema",
            label: $.i18n('wikibase-extension/export-schema'),
            click: function () {
              WikibaseExporterMenuBar.checkSchemaAndExport("wikibase-schema");
            }
          },
          {},
          {
            id: "wikidata/perform-edits",
            label: $.i18n('wikibase-extension/perform-edits-on-wikibase'),
            click: function () {
              PerformEditsDialog.checkAndLaunch();
            }
          },
          {
            id: "wikidata/export-qs",
            label: $.i18n('wikibase-extension/export-to-qs'),
            click: function () {
              WikibaseExporterMenuBar.checkSchemaAndExport("quickstatements");
            }
          }
        ]
      }
  );
});

