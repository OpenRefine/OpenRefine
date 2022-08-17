var SaveNewTemplateDialog = {};

SaveNewTemplateDialog.launch = function() {
  var self = this;
  var frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/save-new-template-dialog.html"));
  var elmts = this._elmts = DOM.bind(frame);

  this._elmts.dialogHeader.text($.i18n('wikibase-save-new-template/dialog-header'));
  this._elmts.nameLabel.html($.i18n('wikibase-save-new-template/template-name'));
  this._elmts.existingTemplateLabel.text($.i18n('wikibase-save-new-template/existing-template-label'));
  this._elmts.cancelButton.text($.i18n('core-buttons/cancel'));
  this._elmts.saveButton.text($.i18n('wikibase-save-new-template/save'));

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
        .text($.i18n('wikibase-save-new-template/select-template'))
        .appendTo(elmts.templateSelect);
    templates.forEach(template =>
      $('<option></option>')
        .attr('value', template.name)
        .text(template.name)
        .appendTo(elmts.templateSelect));
  }

  elmts.templateSelect.on('change', function(e) {
     SaveNewTemplateDialog._elmts.nameInput.val($(this).val());
  });

  elmts.nameInput.focus();
  elmts.nameInput.on('change', function(e) {
     SaveNewTemplateDialog._elmts.templateSelect.val('__placeholder__');
  });

  elmts.cancelButton.on('click',function() {
     dismiss();
  });

  elmts.form.on('submit',function(e) {
    e.preventDefault();
    let wikibaseName = WikibaseManager.getSelectedWikibaseName();
    let templateName = elmts.nameInput.val();
    if (templateName.trim().length === 0) {
      alert($.i18n('wikibase-save-new-template/empty-name'));
      return;
    }
    let schema = SchemaAlignment.getJSON();
    WikibaseTemplateManager.addTemplate(wikibaseName, templateName, schema);
    WikibaseTemplateManager.saveTemplates();
    SchemaAlignment.updateAvailableTemplates();
    dismiss();
  });
};

