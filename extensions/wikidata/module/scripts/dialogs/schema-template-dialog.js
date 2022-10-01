function SchemaTemplateDialog() {
  this.launch();
}

SchemaTemplateDialog.prototype.launch = function () {
  const frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/schema-template-dialog.html"));
  const elmts = this.elmts = DOM.bind(frame);
  elmts.dialogHeader.text($.i18n("wikibase-schema-template/dialog-header"));
  elmts.closeButton.text($.i18n("wikibase-schema/close-button"));
  elmts.importLabel.text($.i18n("wikibase-schema-template/import"));

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
      "command/wikidata/validate-wikibase-schema-template",
      { template: text },
      function(data) {
        // success
        const parsedTemplate = JSON.parse(text);
        WikibaseTemplateManager.addTemplate(self.activeWikibase, parsedTemplate.name, parsedTemplate.schema);
        WikibaseTemplateManager.saveTemplates();
        elmts.fileInput.val('');
        self.populateDialog();
      },
      "json",
      function(error) {
        elmts.errorField.text($.i18n('wikibase-schema-template/invalid-template'));
      }
    );

  });
};

SchemaTemplateDialog.prototype.populateDialog = function () {
  this.elmts.templateList.empty();
  let templates = WikibaseTemplateManager.getTemplates(this.activeWikibase);

  for (let template of templates) {
    let templateName = template.name;

    const templateItemDOM = $(DOM.loadHTML("wikidata", "scripts/dialogs/schema-template-item.html"));
    let _elmts = DOM.bind(templateItemDOM);
    _elmts.templateName.text(templateName);

    _elmts.renameTemplate.text($.i18n('wikibase-schema-template/rename'));
    _elmts.downloadTemplate.text($.i18n('wikibase-schema-template/export'));
    _elmts.deleteTemplate.text($.i18n('wikibase-schema-template/delete'));

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

SchemaTemplateDialog.prototype.deleteTemplate = function (wikibaseName, templateName) {
  WikibaseTemplateManager.deleteTemplate(wikibaseName, templateName);
  WikibaseTemplateManager.saveTemplates();
  this.populateDialog();
};

SchemaTemplateDialog.prototype.renameTemplate = function (wikibaseName, templateName) {
  let newName = prompt($.i18n('wikibase-schema-template/enter-new-template-name'), templateName);
  if (newName && newName.trim()) {
    WikibaseTemplateManager.renameTemplate(wikibaseName, templateName, newName.trim());
    WikibaseTemplateManager.saveTemplates();
    this.populateDialog();
  }
};

;
