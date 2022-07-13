// Make sure the wikibase manifests are loaded before initializing the schema UI.
Refine.registerUpdateFunction(function(options) {
  WikibaseManager.loadWikibases(function () {
    SchemaAlignment.onProjectUpdate(options);
  });
});
