/*

Copyright 2011, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

Refine.PreviewTable = function(projectData, elmt) {
  this._projectData = projectData;
  this._elmt = elmt;
  this._render();
};

Refine.PreviewTable.prototype._render = function() {
  var self = this;
  var table = $('<table>').addClass("data-table").appendTo(this._elmt)[0];

  var columns = this._projectData.columnModel.columns;

  /*------------------------------------------------------------
   *  Column Headers
   *------------------------------------------------------------
   */

  var trHead = table.insertRow(table.rows.length);
  $(trHead.insertCell(0)).addClass("column-header").html('&nbsp;'); // index

  var createColumnHeader = function(column) {
    $(trHead.insertCell(trHead.cells.length))
    .addClass("column-header")
    .text(column.name);
  };
  for (var i = 0; i < columns.length; i++) {
    createColumnHeader(columns[i], i);
  }

  /*------------------------------------------------------------
   *  Data Cells
   *------------------------------------------------------------
   */

  var rows = this._projectData.rowModel.rows;
  var renderRow = function(tr, r, row, even) {
    $(tr).addClass(even ? "even" : "odd");

    var cells = row.cells;
    var tdIndex = tr.insertCell(tr.cells.length);
    $('<div></div>').html((row.i + 1) + ".").appendTo(tdIndex);

    for (var i = 0; i < columns.length; i++) {
      var column = columns[i];
      var td = tr.insertCell(tr.cells.length);
      var divContent = $('<div/>').addClass("data-table-cell-content").appendTo(td);

      var cell = (column.cellIndex < cells.length) ? cells[column.cellIndex] : null;
      if (!cell || ("v" in cell && cell.v === null)) {
        $('<span>').html("&nbsp;").appendTo(divContent);
      } else if ("e" in cell) {
        $('<span>').addClass("data-table-error").text(cell.e).appendTo(divContent);
      } else {
        if ("r" in cell && cell.ri !== null) {
          $('<a>')
          .attr("href", "#") // we don't have access to the reconciliation data here
          .text(cell.v)
          .appendTo(divContent);
        } else if (typeof cell.v !== "string") {
          if (typeof cell.v == "number") {
            divContent.addClass("data-table-cell-content-numeric");
          }
          $('<span>')
          .addClass("data-table-value-nonstring")
          .text(cell.v)
          .appendTo(divContent);
        } else if (URLUtil.looksLikeUrl(cell.v)) {
          $('<a>')
          .text(cell.v)
          .attr("href", cell.v)
          .attr("target", "_blank")
          .appendTo(divContent);
        } else {
          $('<span>').text(cell.v).appendTo(divContent);
        }
      }
    }
  };

  var even = true;
  for (var r = 0; r < rows.length; r++) {
    var row = rows[r];
    var tr = table.insertRow(table.rows.length);
    even = !even;
    renderRow(tr, r, row, even);
  }    
};
