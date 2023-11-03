
var CellRendererRegistry = {
  renderers: [
    {
      name: 'null',
      renderer: new NullCellRenderer()
    },
    {
      name: 'error',
      renderer: new ErrorCellRenderer()
    },
    {
      name: 'simple-value',
      renderer: new SimpleValueCellRenderer()
    },
    {
      name: 'recon',
      renderer: new ReconCellRenderer()
    }
  ]
};


/**
 * Method to be called by extensions to register a new cell
 * renderer.
 *
 * Cell renderers are executed in order and we stop at the first one which
 * succeeds in rendering the cell. The following renderers are not executed at all.
 * See the array above to check when the native renderers are executed
 * and pick a location accordingly.
 *
 * The third argument can be used to specify before which renderer the 
 * new renderer should be executed. If it is not defined, the renderer will
 * be added at the very end of the queue.
 */
CellRendererRegistry.addRenderer = function(name, renderer, beforeRenderer) {
  let record = {
    name,
    renderer
  };
  if (beforeRenderer === undefined) {
    this.renderers.push(record);
  } else {
    let inserted = false;
    for (const [index, entry] of this.renderers.entries()) {
      if (entry.name === beforeRenderer) {
        this.renderers.splice(index, 0, record);
        inserted = true;
        break;
      }
    }
    if (!inserted) {
      throw new Error('Cell renderer "'+beforeRenderer+'" could not be found');
    }
  }
}
