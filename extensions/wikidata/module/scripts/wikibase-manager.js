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

WikibaseManager.getSelectedWikibaseApiForEntityType = function (entityType) {
  let manifest = WikibaseManager.getSelectedWikibase();
  // version 1
  if (manifest.version.split('.')[0] === '1') {
    return manifest.wikibase.site_iri;
  } else { // version 2 or above
    let record = manifest.entity_types[entityType];
    let api = record === undefined ? undefined : record.mediawiki_api;
    return api === undefined ? manifest.mediawiki.api : api;
  }
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
 * for a given entity type, such as "https://wikidata.reconci.link/${lang}/api".
 *
 * Notice that there is a "${lang}" variable in the URL, which should
 * be replaced with the actual language code.
 */
WikibaseManager.getSelectedWikibaseReconEndpoint = function (entityType) {
  let manifest = WikibaseManager.getSelectedWikibase();
  // version 1
  if (manifest.version.split('.')[0] === '1') {
    if (entityType === 'item') {
      return manifest.reconciliation.endpoint;
    }
    return null;
  } else { // version 2 or above
    let record = manifest.entity_types[entityType];
    return record === undefined ? undefined : record.reconciliation_endpoint;
  }
};

WikibaseManager.getReconciliationEndpoints = function (manifest) {
  // version 1
  if (manifest.version.split('.')[0] === '1') {
    return [manifest.reconciliation.endpoint];
  } else { // version 2 or above
    return Object.keys(manifest.entity_types)
        .map(k => manifest.entity_types[k].reconciliation_endpoint)
        .filter(endpoint => endpoint != null);
  }
};


WikibaseManager.getSelectedWikibaseSiteIriForEntityType = function (entityType) {
  let manifest = WikibaseManager.getSelectedWikibase();
  // version 1
  if (manifest.version.split('.')[0] === '1') {
    return manifest.wikibase.site_iri;
  } else { // version 2 or above
    let record = manifest.entity_types[entityType];
    return record === undefined ? undefined : record.site_iri;
  }
};


WikibaseManager.getSelectedWikibaseAvailableEntityTypes = function () {
  let manifest = WikibaseManager.getSelectedWikibase();
  // version 1
  if (manifest.version.split('.')[0] === '1') {
    return ['item', 'property'];
  } else { // version 2 or above
    return Object.keys(manifest.entity_types);
  }
};


/**
 * TODO temporary function to be removed once we have proper support
 * for multiple entity types. This one just guesses which item-like
 * entity type we should let the user edit, based on the manifest.
 * - for Wikidata it returns 'item'
 * - for Commons it returns 'mediainfo'
 */
WikibaseManager.getSelectedWikibaseDefaultEntityType = function () {
  let manifest = WikibaseManager.getSelectedWikibase();
  // version 1
  if (manifest.version.split('.')[0] === '1') {
    return 'item';
  } else { // version 2 or above
    for (let entityType of ['item', 'property', 'mediainfo']) {
      if (manifest.entity_types[entityType] !== undefined &&
          manifest.entity_types[entityType].site_iri === manifest.wikibase.site_iri) {
        return entityType;
      }
    }
    return 'item'; // by default, as a fallback
  }
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

var wikibaseLogoURLCache = {
  data: {},
  exist: function (url) {
    return wikibaseLogoURLCache.data.hasOwnProperty(url);
  },
  get: function (url) {
    return wikibaseLogoURLCache.data[url];
  },
  set: function (url, cachedData) {
    wikibaseLogoURLCache.data[url] = cachedData.responseJSON.query.general.logo;
  }
};


// Retrives the wikibaseName instance site info, if wikibaseName is empty returns the selected wikibase site info
WikibaseManager.retrieveLogoUrlFromSiteInfo = function(onSuccess, onError, wikibaseName) {
  var params = {
    action: 'query',
    meta: 'siteinfo',
    format: 'json'
  };
  let wikibase = (wikibaseName) ? WikibaseManager.wikibases[wikibaseName] : WikibaseManager.getSelectedWikibase();
  const url = wikibase.mediawiki.api;
  return $.ajax({
    url: url,
    data: params,
    dataType: "jsonp",
    timeout: 1000,
    cache: true,
    beforeSend: function () {
      if (wikibaseLogoURLCache.exist(url)) {
        onSuccess(wikibaseLogoURLCache.get(url));
        return false;
      }
      return true;
    },
    complete: function (jqXHR, textStatus) {
      wikibaseLogoURLCache.set(url, jqXHR);
    },
    success: function(response) {
      onSuccess(response.query.general.logo);
    },
    error: function(xhr, status, error) {
      onError(xhr, status, error);
	},
  });
};

// Retrives the logo url of wikibaseName, if wikibaseName is empty returns the selected wikibase logo url
WikibaseManager.getSelectedWikibaseLogoURL = function(onDone, wikibaseName) {
  WikibaseManager.retrieveLogoUrlFromSiteInfo(function(data) {
    onDone(data);
  }, function(xhr, status, error) {
    onDone("extension/wikidata/images/Wikibase_logo.png");
  }, wikibaseName);
};

