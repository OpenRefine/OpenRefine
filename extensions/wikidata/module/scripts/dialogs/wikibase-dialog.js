const WikibaseDialog = {};

WikibaseDialog.launch = function () {
  const frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/wikibase-dialog.html"));
  const elmts = this.elmts = DOM.bind(frame);
  elmts.dialogHeader.text($.i18n("wikibase-management/dialog-header"));
  elmts.explainSelectWikibase.text($.i18n("wikibase-management/explain-select-wikibase"));
  elmts.currentSelectedWikibase.html($.i18n("wikibase-management/current-selected-wikibase",
      WikibaseManager.getSelectedWikibaseMainPage(), WikibaseManager.getSelectedWikibaseName()));
  elmts.closeButton.text($.i18n("wikibase-management/close"));
  elmts.addButton.text($.i18n("wikibase-management/add-wikibase"));

  WikibaseDialog.populateDialog();

  let level = DialogSystem.showDialog(frame);

  elmts.closeButton.click(function () {
    DialogSystem.dismissUntil(level - 1);
  });

  elmts.addButton.click(function () {
    WikibaseDialog.addWikibaseManifest();
  });
};

WikibaseDialog.populateDialog = function () {
  let wikibases = WikibaseManager.getAllWikibases();

  WikibaseDialog.elmts.wikibaseList.empty();
  for (let wikibaseName in wikibases) {
    WikibaseManager.getSelectedWikibaseLogoURL(function(data) {
      if (wikibases.hasOwnProperty(wikibaseName)) {
        let item = "<tr onclick=\"WikibaseDialog.selectWikibase('" + wikibaseName + "')\">";
        item += "<td class=\"wikibase-dialog-wikibase-logo\">" + "<img src=\""+ data + "\" alt=\"" + $.i18n('wikibase-account/logo-alt-text', wikibaseName) + "\"/>" + "</td>";
        item += "<td>" + wikibaseName + "</td>";
        if (wikibaseName.toLowerCase() === WikibaseManager.getSelectedWikibaseName().toLowerCase()) {
          item += "<td><a class=\"wikibase-dialog-selector-remove wikibase-selected\" onclick=\"void(0)\"></a></td>";
        } else {
          item += "<td><a class=\"wikibase-dialog-selector-remove\" onclick=\"WikibaseDialog.removeWikibase(event, '" + wikibaseName + "')\"></a></td>";
        }
        item += "</tr>";
        WikibaseDialog.elmts.wikibaseList.append(item);
      }
    }, wikibaseName);
  }
};

WikibaseDialog.selectWikibase = function (wikibaseName) {
  if (wikibaseName !== WikibaseManager.getSelectedWikibaseName()) {
    WikibaseManager.selectWikibase(wikibaseName);
    WikibaseDialog.elmts.currentSelectedWikibase.html($.i18n("wikibase-management/current-selected-wikibase",
        WikibaseManager.getSelectedWikibaseMainPage(), WikibaseManager.getSelectedWikibaseName()));
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

  elmts.cancelButton.click(function () {
    DialogSystem.dismissUntil(level - 1);
  });

  elmts.addButton.click(function () {
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

    let manifestURL = $.trim(elmts.manifestURLInput.val());
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

  if (manifest.version !== undefined && manifest.version.startsWith('1.')) {
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
