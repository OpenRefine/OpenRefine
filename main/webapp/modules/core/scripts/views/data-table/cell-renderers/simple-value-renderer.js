
/**
 * A renderer for cells without reconciliation data,
 * just storing a basic value (string, number…)
 */
class SimpleValueCellRenderer {
  render(rowIndex, cellIndex, cell) {
    if (!("r" in cell) || !cell.r) {
      var divContent = document.createElement('div');
      if (typeof cell.v !== "string" || "t" in cell) {
        if (typeof cell.v == "number") {
          divContent.classList.add('data-table-cell-content-numeric');
        }
        var nonstringSpan = document.createElement('span');
        nonstringSpan.className = 'data-table-value-nonstring';
        nonstringSpan.textContent = cell.v;
        divContent.appendChild(nonstringSpan);
      } else {
        var arr = cell.v.split(" ");
        var spanArr = [];
        for (var i=0; i<arr.length; i++) {
          if (URLUtil.looksLikeUrl(arr[i])) {
            if (spanArr.length != 0) {
              var span = document.createElement('span');
              span.textContent = spanArr.join(" ");
              divContent.appendChild(span).appendChild(document.createTextNode('\u00A0'));
              spanArr = [];
            }
            var [realURL, extra] = (() => {
              var parts = arr[i].split('\n');
              return parts.length > 1 ? [parts[0], parts.slice(1).map((s) => ("\n" + s))] : [parts[0], []];
            })();

            var url = document.createElement('a');
            url.textContent = realURL;
            url.setAttribute('href', realURL);
            url.setAttribute('target', '_blank');
            if (i === arr.length-1){
              divContent.appendChild(url)
            } else {
              divContent.appendChild(url).appendChild(document.createTextNode('\u00A0'));
            }
            if (extra.length > 0) {
              for (var j=0; j<extra.length; j++) {
                spanArr.push(extra[j]);
              }
            }
          } else {
            spanArr.push(arr[i]);
          }
        }
        if (spanArr.length != 0) {
          var span = document.createElement('span');
          span.textContent = spanArr.join(" ");
          divContent.appendChild(span);
        }
      } 
      return divContent;
    } 
  }
}

