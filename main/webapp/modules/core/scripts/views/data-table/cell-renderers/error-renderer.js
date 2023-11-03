/**
 * A cell renderer for cells which contain evaluation errors
 */
class ErrorCellRenderer {
  render(rowIndex, cellIndex, cell, cellUI) {
    if ("e" in cell) {
      var divContent = document.createElement('div');
      var errorSpan = document.createElement('span');
      errorSpan.className = 'data-table-error';
      errorSpan.textContent = cell.e;
      divContent.appendChild(errorSpan);
      return divContent;
    }
  }
}

