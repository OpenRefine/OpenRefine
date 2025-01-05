
var OperationIconRegistry = {
  map: new Map()
};

OperationIconRegistry.setIcon = function(operationId, iconPath) {
  if (!iconPath.endsWith(".svg")) {
    throw new Error('Operation icon must be a path to a SVG file.');
  }
  // this will cause the image to load so that once it is added to the DOM it will already be in the browser cache
  var image = new Image();
  image.src = iconPath;

  this.map.set(operationId, iconPath);
}

OperationIconRegistry.getIcon = function(operationId) {
  return this.map.get(operationId);
}

