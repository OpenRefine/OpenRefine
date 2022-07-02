const WikibaseDialog = {};

WikibaseDialog.launch = function () {
  const frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/wikibase-dialog.html"));
  const elmts = this.elmts = DOM.bind(frame);
  elmts.dialogHeader.text($.i18n("wikibase-management/dialog-header"));
  elmts.explainSelectWikibase.text($.i18n("wikibase-management/explain-select-wikibase"));
  elmts.closeButton.text($.i18n("wikibase-management/close"));
  elmts.addButton.text($.i18n("wikibase-management/add-wikibase"));

  WikibaseDialog.populateDialog();

  let level = DialogSystem.showDialog(frame);

  elmts.closeButton.on('click',function () {
    DialogSystem.dismissUntil(level - 1);
  });

  elmts.addButton.on('click',function () {
    WikibaseDialog.addWikibaseManifest();
  });
};

WikibaseDialog.populateDialog = function () {
  let wikibases = WikibaseManager.getAllWikibaseManifests();
  let selectedWikibase =  WikibaseManager.getSelectedWikibaseName().toLowerCase();

  wikibases.sort((a, b) => {
    let ret;
    let aName = a.mediawiki.name;
    let bName = b.mediawiki.name;
    let aSelected = aName.toLowerCase() === selectedWikibase;
    let bSelected = bName.toLowerCase() === selectedWikibase;
    // ascending order
    // compare selected, then by name
    if (aSelected) {
      ret = -1;
    } else if (bSelected) {
      ret = 1;
    } else {
      if (aName < bName) {
        ret = -1;
      } else if (aName > bName) {
        ret = 1;
      } else {
        ret = 0;
      }
    }
    return ret;
  });

  WikibaseDialog.elmts.wikibaseList.empty();
  for (let manifest of wikibases) {
    let wikibaseName = manifest.mediawiki.name;

    let rootURL = manifest.mediawiki.root;

    const wikibase = $(DOM.loadHTML("wikidata", "scripts/dialogs/wikibase-item.html"));
    let _elmts = DOM.bind(wikibase);
    if (wikibaseName.toLowerCase() === selectedWikibase) {
      _elmts.wikibaseItem.addClass("selected");
    }
    _elmts.wikibaseItem.click(function(event) {
      WikibaseDialog.selectWikibase(wikibaseName )
    });
    _elmts.wikibaseImage.attr("alt",$.i18n('wikibase-account/logo-alt-text', wikibaseName));
    _elmts.wikibaseName.text(wikibaseName);
    _elmts.wikibaseUrl.text(rootURL);
    _elmts.deleteWikibase.text($.i18n('core-index/delete'));
    _elmts.deleteWikibase.click(function(event) {
      WikibaseDialog.removeWikibase(event, wikibaseName);
    });

    WikibaseDialog.elmts.wikibaseList.append(wikibase);

    WikibaseManager.getSelectedWikibaseLogoURL(function(data) {
      _elmts.wikibaseImage.attr("src",data);
    }, wikibaseName);
  }
};

WikibaseDialog.selectWikibase = function (wikibaseName) {
  if (wikibaseName !== WikibaseManager.getSelectedWikibaseName()) {
    WikibaseManager.selectWikibase(wikibaseName);
    WikibaseDialog.populateDialog();
    SchemaAlignment.onWikibaseChange();
  }
};

WikibaseDialog.removeWikibase = function (e, wikibaseName) {
  e.stopPropagation(); // must stop, otherwise the removed Wikibase will be selected
  WikibaseManager.removeWikibase(wikibaseName);
  WikibaseDialog.populateDialog();
};


WikibaseDialog.addWikibaseManifest = function () {
  const frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/add-wikibase-dialog.html"));
  const elmts = DOM.bind(frame);
  elmts.dialogHeader.text($.i18n("wikibase-addition/dialog-header"));
  elmts.explainAddManifest.text($.i18n("wikibase-addition/explain-add-manifest"));
  elmts.explainAddManifestViaURL.text($.i18n("wikibase-addition/explain-add-manifest-via-url"));
  elmts.explainPasteManifest.html($.i18n("wikibase-addition/explain-paste-manifest"));
  elmts.cancelButton.text($.i18n("wikibase-addition/cancel"));
  elmts.addButton.text($.i18n("wikibase-addition/add-wikibase"));
  elmts.invalidManifest.hide();

  let level = DialogSystem.showDialog(frame);

  elmts.cancelButton.on('click',function () {
    DialogSystem.dismissUntil(level - 1);
  });

  elmts.addButton.on('click',function () {
    let addManifest = function (manifest) {
      if (!WikibaseDialog.validateManifest(manifest)) {
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
      WikibaseDialog.populateDialog();
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

WikibaseDialog.validateManifest = function (manifest) {
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
