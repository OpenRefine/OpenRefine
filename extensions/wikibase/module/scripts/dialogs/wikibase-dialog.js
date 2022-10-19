function WikibaseDialog() {
  this.launch();
}

WikibaseDialog.activeWikibase = undefined;
WikibaseDialog.selectedWikibase = undefined;

WikibaseDialog.prototype.launch = function () {
  const frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/wikibase-dialog.html"));
  const elmts = this.elmts = DOM.bind(frame);
  elmts.dialogHeader.text($.i18n("wikibase-management/dialog-header"));
  elmts.explainSelectWikibase.text($.i18n("wikibase-management/explain-select-wikibase"));
  elmts.okButton.text($.i18n("wikibase-management/ok"));
  elmts.cancelButton.text($.i18n("wikibase-management/cancel"));
  elmts.addButton.text($.i18n("wikibase-management/add-wikibase"));
  elmts.discoverManifestsButton.text($.i18n("wikibase-management/discover-manifests"));

  this.activeWikibase =  WikibaseManager.getSelectedWikibaseName();
  this.selectedWikibase =  this.activeWikibase;
  this.populateDialog();

  let level = DialogSystem.showDialog(frame);

  elmts.cancelButton.on('click',function () {
    DialogSystem.dismissUntil(level - 1);
  });
  elmts.addButton.on('click',() => {
    this.addWikibaseManifest();
  });
  elmts.okButton.on('click',() => {
    this.selectWikibase(this.selectedWikibase);
    DialogSystem.dismissUntil(level - 1);
  });
};

WikibaseDialog.prototype.populateDialog = function () {
  let wikibases = WikibaseManager.getAllWikibaseManifests();

  this.elmts.wikibaseList.empty();
  for (let manifest of wikibases) {
    let wikibaseName = manifest.mediawiki.name;

    let rootURL = manifest.mediawiki.root;

    const wikibase = $(DOM.loadHTML("wikidata", "scripts/dialogs/wikibase-item.html"));
    let _elmts = DOM.bind(wikibase);
    _elmts.wikibaseSelect.value = wikibaseName;
    _elmts.wikibaseSelect.id = wikibaseName+'Select';

    if (wikibaseName === this.activeWikibase) {
      _elmts.wikibaseItem.addClass("active");
    }

    if (wikibaseName === this.selectedWikibase) {
      _elmts.wikibaseSelect.prop("checked",true);
      _elmts.wikibaseItem.addClass("selected");
    }
    _elmts.wikibaseItem.on('click', (event) => {
      if (wikibaseName !== this.selectedWikibase) {
        this.selectedWikibase = wikibaseName;
        this.populateDialog();
      }
    });
    _elmts.wikibaseImage.attr("alt",$.i18n('wikibase-account/logo-alt-text', wikibaseName));
    _elmts.wikibaseName.text(wikibaseName);
    _elmts.wikibaseUrl.text(rootURL);
    _elmts.deleteWikibase.text($.i18n('core-index/delete'));
    _elmts.deleteWikibase.addClass('wikibase-dialog-selector-delete');
    _elmts.deleteWikibase.on('click', (event) => {
      this.removeWikibase(event, wikibaseName);
    });

    this.elmts.wikibaseList.append(wikibase);

    WikibaseManager.getSelectedWikibaseLogoURL(function(data) {
      _elmts.wikibaseImage.attr("src",data);
    }, wikibaseName);
  }
};

WikibaseDialog.prototype.selectWikibase = function (wikibaseName) {
  if (wikibaseName !== WikibaseManager.getSelectedWikibaseName()) {
    WikibaseManager.selectWikibase(wikibaseName);
    this.activeWikibase =  this.selectedWikibase;
    this.populateDialog();
    SchemaAlignment.onWikibaseChange();
  }
};

WikibaseDialog.prototype.removeWikibase = function (e, wikibaseName) {
  e.stopPropagation(); // must stop, otherwise the removed Wikibase will be selected
  WikibaseManager.removeWikibase(wikibaseName);
  this.populateDialog();
};


WikibaseDialog.prototype.addWikibaseManifest = function () {
  const frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/add-wikibase-dialog.html"));
  const elmts = DOM.bind(frame);
  elmts.dialogHeader.text($.i18n("wikibase-addition/dialog-header"));
  elmts.explainAddManifest.text($.i18n("wikibase-addition/explain-add-manifest"));
  elmts.explainAddManifestViaURL.text($.i18n("wikibase-addition/explain-add-manifest-via-url"));
  elmts.manifestURLInput.attr("aria-label",$.i18n("wikibase-addition/manifest-url-input"));
  elmts.explainPasteManifest.html($.i18n("wikibase-addition/explain-paste-manifest"));
  elmts.manifestTextarea.attr("aria-label",$.i18n("wikibase-addition/manifest-paste-input"));
  elmts.cancelButton.text($.i18n("wikibase-addition/cancel"));
  elmts.addButton.text($.i18n("wikibase-addition/add-wikibase"));
  elmts.invalidManifest.hide();

  let level = DialogSystem.showDialog(frame);

  elmts.cancelButton.on('click',function () {
    DialogSystem.dismissUntil(level - 1);
  });

  elmts.addButton.on('click', () => {
    let addManifest = (manifest) => {
      if (!this.validateManifest(manifest)) {
        return;
      }

      WikibaseManager.addWikibase(manifest);

      // pre-register reconciliation services mentioned by this manifest
      for (let reconEndpoint of WikibaseManager.getReconciliationEndpoints(manifest)) {
        let lang = $.i18n('core-recon/wd-recon-lang');
        let endpoint = reconEndpoint.replace("${lang}", lang);
        ReconciliationManager.getOrRegisterServiceFromUrl(endpoint, function () {}, true);
      }

      DialogSystem.dismissUntil(level - 1);
      this.populateDialog();
    };

    let manifestURL = jQueryTrim(elmts.manifestURLInput.val());
    if (manifestURL.length) {
      WikibaseManager.fetchManifestFromURL(manifestURL, addManifest);
    } else {
      try {
        let manifest = JSON.parse(elmts.manifestTextarea.val());
        addManifest(manifest);
      } catch (e) {
        console.error(e);
        elmts.invalidManifest.show();
        elmts.invalidManifest.text($.i18n(e.toString()));
      }
    }
  });
};

WikibaseDialog.prototype.validateManifest = function (manifest) {
  if (!WikibaseDialog.ajv) {
    WikibaseDialog.ajv = new Ajv();
    WikibaseDialog.validateWikibaseManifestV1 = WikibaseDialog.ajv.compile(WikibaseManifestSchemaV1);
    WikibaseDialog.validateWikibaseManifestV2 = WikibaseDialog.ajv.compile(WikibaseManifestSchemaV2);
  }
  let majorVersion;
  if(manifest.version) {
    majorVersion = manifest.version.split('.')[0];
    if (!(majorVersion >= 1 && majorVersion <= 2)) {
      alert($.i18n('wikibase-addition/version-error', manifest.version));
      return false;
    }
  }

  if (majorVersion == 1) {
    if (WikibaseDialog.validateWikibaseManifestV1(manifest)) {
       return true;
    } else {
      let errMsg = WikibaseDialog.ajv.errorsText(WikibaseDialog.validateWikibaseManifestV1.errors, {
        dataVar: "manifest"
      });
      alert(errMsg);
      return false;
    }
  } else {
    if (WikibaseDialog.validateWikibaseManifestV2(manifest)) {
      return true;
    } else {
      let errMsg = WikibaseDialog.ajv.errorsText(WikibaseDialog.validateWikibaseManifestV2.errors, {
        dataVar: "manifest"
      });
      alert(errMsg);
      return false;
    }
  }
};
