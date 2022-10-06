var PerformEditsDialog = {};

PerformEditsDialog.launch = function(logged_in_username, max_severity) {
  var self = this;
  var elmts = this._elmts;
  var frame = this.frame;

  if (this.missingSchema) {
    return;
  }

  this._level = DialogSystem.showDialog(frame);
  this._elmts.dialogHeader.text($.i18n('perform-wikibase-edits/dialog-header', WikibaseManager.getSelectedWikibaseName()));
  this._elmts.loggedInAs.text($.i18n('perform-wikibase-edits/logged-in-as'));
  this._elmts.editSummaryLabel.text($.i18n('perform-wikibase-edits/edit-summary-label'));
  this._elmts.editSummary.attr('placeholder', $.i18n('perform-wikibase-edits/edit-summary-placeholder'));
  this._elmts.maxlagLabel.text($.i18n('perform-wikibase-edits/maxlag-label'));
  this._elmts.maxlag.val(WikibaseManager.getSelectedWikibaseMaxlag());
  this._elmts.maxlag.attr('placeholder', WikibaseManager.getSelectedWikibaseMaxlag());
  this._elmts.explainMaxlag.html($.i18n('perform-wikibase-edits/explain-maxlag'));
  this._elmts.performEditsButton.text($.i18n('perform-wikibase-edits/perform-edits'));
  this._elmts.cancelButton.text($.i18n('perform-wikibase-edits/cancel'));

  var hiddenIframe = $('#hiddenIframe').contents();

  var dismiss = function() {
    DialogSystem.dismissUntil(self._level - 1);
  };


  var doFormSubmit = function() {
    hiddenIframe.find('body').append(elmts.performEditsForm.clone());
    var formCopy = hiddenIframe.find("#wikibase-perform-edits-form");
    formCopy.trigger('submit');

    if (elmts.editSummary.val().length === 0) {
      elmts.editSummary.trigger('focus');
      return;
    }

    if (elmts.maxlag.val().length === 0) {
      elmts.maxlag.val(WikibaseManager.getSelectedWikibaseMaxlag());
    }

    // validate maxlag
    if (!/^\+?[1-9]\d*$/.test(elmts.maxlag.val())) {
      elmts.maxlag.focus();
      alert($.i18n('perform-wikibase-edits/maxlag-validation'));
      return;
    }

    Refine.postProcess(
      "wikidata",
      "perform-wikibase-edits",
      {},
      {
        summary: elmts.editSummary.val(),
        maxlag: elmts.maxlag.val(),
        editGroupsUrlSchema: WikibaseManager.getSelectedWikibaseEditGroupsURLSchema(),
        tag: WikibaseManager.getSelectedWikibaseTagTemplate(),
        maxEditsPerMinute: WikibaseManager.getSelectedWikibaseMaxEditsPerMinute()
      },
      { includeEngine: true, cellsChanged: true, columnStatsChanged: true },
      { onDone: function() { dismiss(); } }
    );
  };

  elmts.loggedInUsername
   .text(logged_in_username)
   .attr('href', WikibaseManager.getSelectedWikibaseRoot() + 'User:' + logged_in_username);

  elmts.cancelButton.on('click',function() {
    dismiss();
  });

  this._elmts.editSummary.on('keypress',function (evt) {
    if (evt.which === 13) {
      doFormSubmit();
      evt.preventDefault();
    }
  });

  if (max_severity === 'CRITICAL') {
    elmts.performEditsButton.prop("disabled",true).addClass("button-disabled");
  } else {
    elmts.performEditsButton.on('click',function() {
      doFormSubmit();
    });
  }
};

PerformEditsDialog.updateEditCount = function(edit_count) {
  this._elmts.reviewYourEdits.html($.i18n('perform-wikibase-edits/review-your-edits',
      edit_count,
      WikibaseManager.getSelectedWikibaseMainPage(),
      WikibaseManager.getSelectedWikibaseName(),
  ));
};

PerformEditsDialog._updateWarnings = function(data) {
   var self = this;
   var warnings = data.warnings;
   var mainDiv = this._elmts.warningsArea;

   // clear everything
   mainDiv.empty();
   PerformEditsDialog.updateEditCount(data.edit_count);

   var table = $('<table></table>').appendTo(mainDiv);
   var onLocateRows = function() {
      DialogSystem.dismissUntil(self._level - 1);
   };
   for (var i = 0; i < warnings.length; i++) {
      var rendered = WarningsRenderer._renderWarning(warnings[i], onLocateRows);
      rendered.appendTo(table);
   }
};

PerformEditsDialog.checkAndLaunch = function () {
  this.frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/perform-edits-dialog.html"));
  this._elmts = DOM.bind(this.frame);
  this.missingSchema = false;

  var onSaved = function () {
    ManageAccountDialog.ensureLoggedIn(function (logged_in_username) {
      var discardWaiter = DialogSystem.showBusy($.i18n('perform-wikibase-edits/analyzing-edits'));
      Refine.postCSRF(
          "command/wikidata/preview-wikibase-schema?" + $.param({project: theProject.id}),
          {
             manifest: JSON.stringify(WikibaseManager.getSelectedWikibase()),
             engine: JSON.stringify(ui.browsingEngine.getJSON()),
             slow_mode: true
          },
          function (data) {
            discardWaiter();
            if (data['code'] !== 'error') {
              PerformEditsDialog._updateWarnings(data);
              PerformEditsDialog.launch(logged_in_username, data['max_severity']);
            } else {
              SchemaAlignment.launch();
            }
          },
          "json"
      );
    });
  };


  if (SchemaAlignment.isSetUp() && SchemaAlignment._hasUnsavedChanges) {
     SchemaAlignment._save(onSaved);
  } else {
     onSaved();
  }

};
