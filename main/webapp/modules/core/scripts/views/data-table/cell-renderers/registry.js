
var CellRendererRegistry = {
  renderers: [
    {
      priority: 100,
      renderer: new NullCellRenderer()
    },
    {
      priority: 75,
      renderer: new ErrorCellRenderer()
    },
    {
      priority: 50,
      renderer: new SimpleValueCellRenderer()
    },
    {
      priority: 25,
      renderer: new ReconCellRenderer()
    }
  ]
};


/**
 * Method to be called by extensions to register a new cell
 * renderer.
 *
 * Cell renderers are executed in decreasing priority order.
 * See the array above to check when the native renderers are executed
 * and pick a priority accordingly.
 */
CellRendererRegistry.addRenderer = function(priority, renderer) {
  let record = {
    priority,
    renderer
  };
  this.renderers.push(record);
  // sort in decreasing order of priority.
  // (note: inserting an element in a sorted array can be done more efficiently,
  //  but here we do not expect more than a few insertions over the course of
  //  an execution, so it is not worth optimizing)
  Array.sort((a,b) => b.priority - a.priority);
}
