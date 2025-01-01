
var OperationIconRegistry = {
  map: new Map()
};

OperationIconRegistry.setIcon = function(operationId, iconPath) {
  this.map.set(operationId, iconPath);
}

OperationIconRegistry.getIcon = function(operationId) {
  return this.map.get(operationId);
}

