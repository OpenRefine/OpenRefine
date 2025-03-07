
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

OperationIconRegistry.setIcon('core/text-transform', 'images/operations/transform.svg');
// OperationIconRegistry.setIcon('core/mass-edit', undefined);
OperationIconRegistry.setIcon('core/multivalued-cell-join', 'images/operations/join-rows.svg');
OperationIconRegistry.setIcon('core/multivalued-cell-split', 'images/operations/split-rows.svg');
OperationIconRegistry.setIcon('core/fill-down', 'images/operations/fill-down.svg');
OperationIconRegistry.setIcon('core/blank-down', 'images/operations/blank-down.svg');
OperationIconRegistry.setIcon('core/transpose-columns-into-rows', 'images/operations/transpose-into-rows.svg');
OperationIconRegistry.setIcon('core/transpose-rows-into-columns', 'images/operations/transpose-into-columns.svg');
OperationIconRegistry.setIcon('core/key-value-columnize', 'images/operations/key-value-columnize.svg');

OperationIconRegistry.setIcon('core/column-addition', 'images/operations/add-column.svg');
OperationIconRegistry.setIcon('core/column-removal', 'images/operations/delete.svg');
OperationIconRegistry.setIcon('core/column-multi-removal', 'images/operations/delete.svg');
OperationIconRegistry.setIcon('core/column-rename', 'images/operations/rename.svg');
// OperationIconRegistry.setIcon('core/column-move', undefined);
OperationIconRegistry.setIcon('core/column-split', 'images/operations/split-columns.svg');
OperationIconRegistry.setIcon('core/column-addition-by-fetching-urls', 'images/operations/fetch-urls.svg');

// OperationIconRegistry.setIcon('core/column-reorder', undefined);

OperationIconRegistry.setIcon('core/row-removal', 'images/operations/delete.svg');
// OperationIconRegistry.setIcon('core/row-star', undefined);
// OperationIconRegistry.setIcon('core/row-flag', undefined);
// OperationIconRegistry.setIcon('core/row-reorder', undefined);
// OperationIconRegistry.setIcon('core/row-addition', undefined);
// OperationIconRegistry.setIcon('core/row-duplicate-removal', undefined);

OperationIconRegistry.setIcon('core/recon', 'images/operations/reconcile.svg');
// OperationIconRegistry.setIcon('core/recon-mark-new-topics', undefined);
// OperationIconRegistry.setIcon('core/recon-match-best-candidates', undefined);
// OperationIconRegistry.setIcon('core/recon-discard-judgments', undefined);
// OperationIconRegistry.setIcon('core/recon-match-specific-topic-to-cells', undefined);
// OperationIconRegistry.setIcon('core/recon-judge-similar-cells', undefined);
// OperationIconRegistry.setIcon('core/recon-clear-similar-cells', undefined);
// OperationIconRegistry.setIcon('core/recon-copy-across-columns', undefined);
OperationIconRegistry.setIcon('core/extend-reconciled-data', 'images/operations/data-extension.svg');
// OperationIconRegistry.setIcon('core/recon-use-values-as-identifiers', undefined);

