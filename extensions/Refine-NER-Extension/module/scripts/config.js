/* Expose configuration */
var NERExtension = {};
NERExtension.commandPath = "/command/named-entity-recognition/";
NERExtension.servicesPath = NERExtension.commandPath + "services";

// Register a dummy reconciliation service that will be used to display named entities
ReconciliationManager.registerService({
  name: "NamedEntity",
  url: "NamedEntity",
  // By setting the URL to "{{id}}",
  // this whole string will be replaced with the actual URL
  view: { url: "{{id}}" },
});
