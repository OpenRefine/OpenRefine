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
OperationIconRegistry.setIcon('core/mass-edit', 'images/operations/mass-edit.svg');
OperationIconRegistry.setIcon('core/multivalued-cell-join', 'images/operations/join-rows.svg');
OperationIconRegistry.setIcon('core/multivalued-cell-split', 'images/operations/split-rows.svg');
OperationIconRegistry.setIcon('core/fill-down', 'images/operations/fill-down.svg');
OperationIconRegistry.setIcon('core/blank-down', 'images/operations/blank-down.svg');
OperationIconRegistry.setIcon('core/transpose-columns-into-rows', 'images/operations/transpose-into-rows.svg');
OperationIconRegistry.setIcon('core/transpose-rows-into-columns', 'images/operations/transpose-into-columns.svg');
OperationIconRegistry.setIcon('core/key-value-columnize', 'images/operations/key-value-columnize.svg');

OperationIconRegistry.setIcon('core/column-addition', 'images/operations/add.svg');
OperationIconRegistry.setIcon('core/column-removal', 'images/operations/delete.svg');
OperationIconRegistry.setIcon('core/column-rename', 'images/operations/rename.svg');
OperationIconRegistry.setIcon('core/column-move', 'images/operations/column-move.svg');
OperationIconRegistry.setIcon('core/column-split', 'images/operations/split-columns.svg');
OperationIconRegistry.setIcon('core/column-addition-by-fetching-urls', 'images/operations/fetch-urls.svg');
OperationIconRegistry.setIcon('core/column-reorder', 'images/operations/column-reorder.svg');

OperationIconRegistry.setIcon('core/row-removal', 'images/operations/delete.svg');
OperationIconRegistry.setIcon('core/row-star', 'images/operations/row-star.svg');
OperationIconRegistry.setIcon('core/row-flag', 'images/operations/row-flag.svg');
OperationIconRegistry.setIcon('core/row-reorder', 'images/operations/row-reorder.svg');
OperationIconRegistry.setIcon('core/row-addition', 'images/operations/add.svg');
OperationIconRegistry.setIcon('core/row-duplicate-removal', 'images/operations/row-duplicate-removal.svg');
OperationIconRegistry.setIcon('core/row-keep-matched', 'images/operations/row-keep-matched.svg');

OperationIconRegistry.setIcon('core/recon', 'images/operations/reconcile.svg');
OperationIconRegistry.setIcon('core/recon-mark-new-topics', 'images/operations/recon-mark-new-topics.svg');
OperationIconRegistry.setIcon('core/recon-match-best-candidates', 'images/operations/recon-match-best-candidates.svg');
OperationIconRegistry.setIcon('core/recon-discard-judgments', 'images/operations/recon-discard-judgments.svg');
OperationIconRegistry.setIcon('core/recon-match-specific-topic-to-cells', 'images/operation/recon-match-specific-topic-to-cells.svg');
OperationIconRegistry.setIcon('core/recon-judge-similar-cells', 'images/operations/recon-match-specific-topic-to-cells.svg');
OperationIconRegistry.setIcon('core/recon-clear-similar-cells', 'images/operations/delete.svg');
OperationIconRegistry.setIcon('core/recon-copy-across-columns', 'images/operations/recon-copy-across-columns.svg');
OperationIconRegistry.setIcon('core/extend-reconciled-data', 'images/operations/data-extension.svg');
OperationIconRegistry.setIcon('core/recon-use-values-as-identifiers', 'images/operations/recon-use-values-as-identifiers.svg');

