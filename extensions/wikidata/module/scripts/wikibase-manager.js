/**
 * Manages Wikibase instances.
 */
const WikibaseManager = {
  selected: "Wikidata",
  wikibases: {
    "Wikidata": WikidataManifestV1_0 // default one
  }
};

WikibaseManager.getSelectedWikibase = function () {
  return WikibaseManager.wikibases[WikibaseManager.selected];
};

WikibaseManager.getSelectedWikibaseRoot = function () {
  return WikibaseManager.getSelectedWikibase().mediawiki.root;
};

WikibaseManager.getSelectedWikibaseMainPage = function () {
  return WikibaseManager.getSelectedWikibase().mediawiki.main_page;
};

WikibaseManager.getSelectedWikibaseApi = function () {
  return WikibaseManager.getSelectedWikibase().mediawiki.api;
};

WikibaseManager.getSelectedWikibaseName = function () {
  return WikibaseManager.selected;
};

WikibaseManager.getSelectedWikibaseSiteIri = function () {
  return WikibaseManager.getSelectedWikibase().wikibase.site_iri;
};

WikibaseManager.getSelectedWikibaseMaxlag = function() {
  return WikibaseManager.getSelectedWikibase().wikibase.maxlag;
};

WikibaseManager.getSelectedWikibaseOAuth = function() {
  return WikibaseManager.getSelectedWikibase().oauth;
};

WikibaseManager.getSelectedWikibaseEditGroupsURLSchema = function() {
  let editgroups = WikibaseManager.getSelectedWikibase().editgroups;
  return editgroups ? editgroups.url_schema : null;
};

/**
 * Returns the default reconciliation service URL of the Wikibase,
 * such as "https://wdreconcile.toolforge.org/${lang}/api".
 *
 * Notice that there is a "${lang}" variable in the URL, which should
 * be replaced with the actual language code.
 */
WikibaseManager.getSelectedWikibaseReconEndpoint = function () {
  return WikibaseManager.getSelectedWikibase().reconciliation.endpoint;
};

WikibaseManager.selectWikibase = function (wikibaseName) {
  if (WikibaseManager.wikibases.hasOwnProperty(wikibaseName)) {
    WikibaseManager.selected = wikibaseName;
  }
};

WikibaseManager.getAllWikibases = function () {
  return WikibaseManager.wikibases;
};

WikibaseManager.addWikibase = function (manifest) {
  WikibaseManager.wikibases[manifest.mediawiki.name] = manifest;
  WikibaseManager.saveWikibases();
};

WikibaseManager.removeWikibase = function (wikibaseName) {
  delete WikibaseManager.wikibases[wikibaseName];
  WikibaseManager.saveWikibases();
};

WikibaseManager.saveWikibases = function () {
  let manifests = [];
  for (let wikibaseName in WikibaseManager.wikibases) {
    if (WikibaseManager.wikibases.hasOwnProperty(wikibaseName)) {
      manifests.push(WikibaseManager.wikibases[wikibaseName])
    }
  }

  Refine.wrapCSRF(function (token) {
    $.ajax({
      async: false,
      type: "POST",
      url: "command/core/set-preference?" + $.param({
        name: "wikibase.manifests"
      }),
      data: {
        "value": JSON.stringify(manifests),
        csrf_token: token
      },
      dataType: "json"
    });
  });
};

WikibaseManager.loadWikibases = function (onDone) {
  $.ajax({
    url: "command/core/get-preference?" + $.param({
      name: "wikibase.manifests"
    }),
    success: function (data) {
      if (data.value && data.value !== "null" && data.value !== "[]") {
        let manifests = JSON.parse(data.value);
        manifests.forEach(function (manifest) {
          if (manifest.custom && manifest.custom.url && manifest.custom.last_updated
              && ((Date.now() - new Date(manifest.custom.last_updated)) > 7 * 24 * 60 * 60 * 1000)) {
            // If the manifest was fetched via URL and hasn't been updated for a week,
            // fetch it again to keep track of the lasted version
            WikibaseManager.fetchManifestFromURL(manifest.custom.url, function (newManifest) {
              WikibaseManager.wikibases[newManifest.mediawiki.name] = newManifest;
              WikibaseManager.saveWikibases();
            }, function () {
              // fall back to the current one if failed to fetch the latest one
              WikibaseManager.wikibases[manifest.mediawiki.name] = manifest;
            }, true)
          } else {
            WikibaseManager.wikibases[manifest.mediawiki.name] = manifest;
          }
        });

        WikibaseManager.selected = WikibaseManager.selectDefaultWikibaseAccordingToSavedSchema();

        if (onDone) {
          onDone();
        }
      }
    },
    dataType: "json"
  });
};

WikibaseManager.selectDefaultWikibaseAccordingToSavedSchema = function () {
  let schema = theProject.overlayModels.wikibaseSchema || {};
  if (!schema.siteIri) {
    return "Wikidata";
  }

  for (let wikibaseName in WikibaseManager.wikibases) {
    if (WikibaseManager.wikibases.hasOwnProperty(wikibaseName)) {
      let wikibase = WikibaseManager.wikibases[wikibaseName];
      if (schema.siteIri === wikibase.wikibase.site_iri) {
        return wikibase.mediawiki.name;
      }
    }
  }

  return "Wikidata";
};

WikibaseManager.fetchManifestFromURL = function (manifestURL, onSuccess, onError, silent) {
  let dismissBusy = function() {};
  if (!silent) {
    dismissBusy = DialogSystem.showBusy($.i18n("wikibase-management/contact-service") + "...");
  }

  let _onSuccess = function (data) {
    // record custom information in the manifest
    data.custom = {};
    data.custom.url = manifestURL;
    data.custom.last_updated = Date.now();
    if (onSuccess) {
      onSuccess(data);
    }
  };

  // The manifest host must support CORS.
  $.ajax(manifestURL, {
    "dataType": "json",
    "timeout": 5000
  }).success(function (data) {
    dismissBusy();
    _onSuccess(data);
  }).error(function (jqXHR, textStatus, errorThrown) {
    dismissBusy();
    if (!silent) {
      alert($.i18n("wikibase-management/error-contact")+": " + textStatus + " : " + errorThrown + " - " + manifestURL);
    }
    if (onError) {
      onError();
    }
  });
};

