/**
 * A renderer for null cells
 */
class NullCellRenderer {
  render(rowIndex, cellIndex, cell, cellUI) {
    if (!cell || ("v" in cell && cell.v === null)) {
      var divContent = document.createElement('div');
      var nullSpan = document.createElement('span');
      nullSpan.className = 'data-table-null';
      nullSpan.innerHTML = 'null';
      divContent.appendChild(nullSpan);
      return divContent;
    } 
  }
}
