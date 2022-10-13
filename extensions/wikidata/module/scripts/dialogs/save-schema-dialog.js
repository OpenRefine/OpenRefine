var SaveSchemaDialog = {};

SaveSchemaDialog.launch = function() {
  var self = this;
  var frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/save-schema-dialog.html"));
  var elmts = this._elmts = DOM.bind(frame);

  this._elmts.dialogHeader.text($.i18n('wikibase-save-schema-dialog/dialog-header'));
  this._elmts.nameLabel.html($.i18n('wikibase-save-schema-dialog/schema-name'));
  this._elmts.existingTemplateLabel.text($.i18n('wikibase-save-schema-dialog/existing-schema-label'));
  this._elmts.manageButton.text($.i18n('wikibase-save-schema-dialog/manage-schemas'));
  this._elmts.cancelButton.text($.i18n('core-buttons/cancel'));
  this._elmts.saveButton.text($.i18n('wikibase-save-schema-dialog/save'));

  this._level = DialogSystem.showDialog(frame);

  var dismiss = function() {
    DialogSystem.dismissUntil(self._level - 1);
  };

  // populate the list of existing templates
  let wikibaseName = WikibaseManager.getSelectedWikibaseName();
  let templates = WikibaseTemplateManager.getTemplates(wikibaseName);
  if (templates.length === 0) {
     this._elmts.existingTemplateArea.hide();
  } else {
    $('<option></option>')
        .attr('selected', 'selected')
        .attr('value', '__placeholder__')
        .addClass('placeholder')
        .text($.i18n('wikibase-save-schema-dialog/select-schema'))
        .appendTo(elmts.templateSelect);
    templates.forEach(template =>
      $('<option></option>')
        .attr('value', template.name)
        .text(template.name)
        .appendTo(elmts.templateSelect));
  }

  elmts.templateSelect.on('change', function(e) {
     SaveSchemaDialog._elmts.nameInput.val($(this).val());
  });

  elmts.nameInput.focus();
  elmts.nameInput.on('change', function(e) {
     SaveSchemaDialog._elmts.templateSelect.val('__placeholder__');
  });

  elmts.cancelButton.on('click', function() {
     dismiss();
  });
 
  elmts.manageButton.on('click', function() {
    new SchemaManagementDialog();
  });

  elmts.form.on('submit',function(e) {
    e.preventDefault();
    let wikibaseName = WikibaseManager.getSelectedWikibaseName();
    let templateName = elmts.nameInput.val();
    if (templateName.trim().length === 0) {
      alert($.i18n('wikibase-save-schema-dialog/empty-name'));
      return;
    }
    let schema = SchemaAlignment.getJSON();
    WikibaseTemplateManager.addTemplate(wikibaseName, templateName, schema);
    WikibaseTemplateManager.saveTemplates();
    SchemaAlignment.updateAvailableTemplates();
    dismiss();
  });
};

