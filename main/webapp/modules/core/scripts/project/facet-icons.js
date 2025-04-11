var FacetIconRegistry = {
  map: new Map()
};

FacetIconRegistry.setIcon = function(facetId, iconPath) {
  if (!iconPath.endsWith(".svg")) {
    throw new Error('Facet icon must be a path to a SVG file.');
  }
  // this will cause the image to load so that once it is added to the DOM it will already be in the browser cache
  var image = new Image();
  image.src = iconPath;

  this.map.set(facetId, iconPath);
}

FacetIconRegistry.getIcon = function(facetId) {
  return this.map.get(facetId);
}

FacetIconRegistry.setIcon('list', 'images/facets/list.svg');
FacetIconRegistry.setIcon('range', 'images/facets/range.svg');
FacetIconRegistry.setIcon('timerange', 'images/facets/timerange.svg');
FacetIconRegistry.setIcon('text', 'images/facets/text.svg');
FacetIconRegistry.setIcon('scatterplot', 'images/facets/scatterplot.svg');

