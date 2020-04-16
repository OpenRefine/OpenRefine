var ImportSchemaDialog = {};

ImportSchemaDialog.launch = function() {
  var self = this;
  var frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/import-schema-dialog.html"));
  var elmts = this._elmts = DOM.bind(frame);

  this._elmts.dialogHeader.text($.i18n('import-wikibase-schema/dialog-header'));
  this._elmts.fileLabel.html($.i18n('import-wikibase-schema/file-label'));
  this._elmts.schemaLabel.text($.i18n('import-wikibase-schema/schema-label'));
  this._elmts.cancelButton.text($.i18n('core-project/cancel'));
  this._elmts.importButton.text($.i18n('import-wikibase-schema/import'));

  this._level = DialogSystem.showDialog(frame);

  var dismiss = function() {
    DialogSystem.dismissUntil(self._level - 1);
  };


  frame.find('.cancel-button').click(function() {
     dismiss();
  });

  elmts.fileInput.on("change", function(evt) {
     var file = evt.target.files[0];
     var freader = new FileReader();
     freader.onload = function(evt) {
        elmts.schemaTextarea.val(evt.target.result);
        elmts.schemaTextarea.hide();
        elmts.schemaLabel.hide();
     }
     freader.readAsText(file);
  });

  elmts.importButton.click(function() {
    var schema = null;
    try {
       schema = JSON.parse(elmts.schemaTextarea.val());
    } catch(e) {
       elmts.invalidSchema.text($.i18n('import-wikibase-schema/invalid-schema'));
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
            SchemaAlignmentDialog._discardChanges();
            dismiss();
        },
        onError: function(e) {
            elmts.invalidSchema.text($.i18n('import-wikibase-schema/invalid-schema'));
        },
        }
    );
  });
};

