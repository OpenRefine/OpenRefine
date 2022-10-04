function SchemaManagementDialog() {
  this.launch();
}

SchemaManagementDialog.prototype.launch = function () {
  const frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/schema-management-dialog.html"));
  const elmts = this.elmts = DOM.bind(frame);
  elmts.dialogHeader.text($.i18n("wikibase-schema-management-dialog/dialog-header"));
  elmts.closeButton.text($.i18n("wikibase-schema/close-button"));
  elmts.importLabel.text($.i18n("wikibase-schema-management-dialog/import"));

  this.activeWikibase =  WikibaseManager.getSelectedWikibaseName();
  this.populateDialog();

  let level = DialogSystem.showDialog(frame);

  elmts.closeButton.on('click',function () {
    DialogSystem.dismissUntil(level - 1);
  });
  let self = this;
  elmts.fileInput.on('change', async function () {
    const file = event.target.files.item(0)
    const text = await file.text();
    
    elmts.errorField.empty();
    // validate JSON structure
    Refine.postCSRF(
      "command/wikidata/parse-wikibase-schema",
      { template: text },
      function(data) {
        // success
        const parsedTemplate = JSON.parse(text);
        if (data.object_type === 'template') {
          WikibaseTemplateManager.addTemplate(self.activeWikibase, parsedTemplate.name, parsedTemplate.schema);
        } else {
          // legacy case: importing a schema generated before OpenRefine 3.7.
          // We need to additionally prompt the user for a name.
          let name = prompt($.i18n('wikibase-schema-management-dialog/enter-new-schema-name'));
          if (name) {
             WikibaseTemplateManager.addTemplate(self.activeWikibase, name, parsedTemplate);
          }
        }
        WikibaseTemplateManager.saveTemplates();
        elmts.fileInput.val('');
        self.populateDialog();
      },
      "json",
      function(error) {
        elmts.errorField.text($.i18n('wikibase-schema-management-dialog/invalid-schema'));
      }
    );

  });
};

SchemaManagementDialog.prototype.populateDialog = function () {
  this.elmts.templateList.empty();
  let templates = WikibaseTemplateManager.getTemplates(this.activeWikibase);

  for (let template of templates) {
    let templateName = template.name;

    const templateItemDOM = $(DOM.loadHTML("wikidata", "scripts/dialogs/schema-list-item.html"));
    let _elmts = DOM.bind(templateItemDOM);
    _elmts.templateName.text(templateName);

    _elmts.renameTemplate.text($.i18n('wikibase-schema-management-dialog/rename'));
    _elmts.downloadTemplate.text($.i18n('wikibase-schema-management-dialog/export'));
    _elmts.deleteTemplate.text($.i18n('wikibase-schema-management-dialog/delete'));

    _elmts.deleteTemplate.on('click', (event) => {
      this.deleteTemplate(this.activeWikibase, templateName);
    });
    _elmts.renameTemplate.on('click', (event) => {
      this.renameTemplate(this.activeWikibase, templateName);
    });
    let dataUrl = 'data:application/json,'+encodeURI(JSON.stringify(template));
    let name = template.name.replace(/[^\p{Letter}0-9]/giu, '_')+'.json';
    _elmts.downloadTemplate
        .attr('download', name)
        .attr('href', dataUrl);

    this.elmts.templateList.append(templateItemDOM);
  }
};

SchemaManagementDialog.prototype.deleteTemplate = function (wikibaseName, templateName) {
  WikibaseTemplateManager.deleteTemplate(wikibaseName, templateName);
  WikibaseTemplateManager.saveTemplates();
  this.populateDialog();
};

SchemaManagementDialog.prototype.renameTemplate = function (wikibaseName, templateName) {
  let newName = prompt($.i18n('wikibase-schema-management-dialog/enter-new-schema-name'), templateName);
  if (newName && newName.trim()) {
    WikibaseTemplateManager.renameTemplate(wikibaseName, templateName, newName.trim());
    WikibaseTemplateManager.saveTemplates();
    this.populateDialog();
  }
};

