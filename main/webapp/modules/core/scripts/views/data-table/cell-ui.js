/*

Copyright 2010, Google Inc.
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

function DataTableCellUI(dataTableView, cell, rowIndex, cellIndex, td) {
  this._dataTableView = dataTableView;
  this._cell = cell;
  this._rowIndex = rowIndex;
  this._cellIndex = cellIndex;
  this._td = td;
  this._focusBeforeEdit;

  this._render();
}

var reconMatchSilimilarCellsByDefault = true;

DataTableCellUI.previewMatchedCells = true;

(function() {
   
   $.ajax({
     url: "command/core/get-preference?" + $.param({
        name: "cell-ui.previewMatchedCells"
     }),
    success: function(data) {
      if (data.value && data.value == "false") {
        DataTableCellUI.previewMatchedCells = false;
     }
   },
   dataType: "json",
  });
})();

DataTableCellUI.prototype._render = function() {
  var self = this;
  var cell = this._cell;

  var divContent = document.createElement('div');
  divContent.className = 'data-table-cell-content';

  var editLink = document.createElement('button');
  editLink.className = 'data-table-cell-edit';
  editLink.setAttribute('title', $.i18n('core-views/edit-cell'));
  editLink.addEventListener('click', function() {
    self._startEdit(this);
    self._focusBeforeEdit = editLink;
  });

  $(this._td).empty()
  .off()
  .on('mouseenter',function() { editLink.style.opacity = "1" })
  .on('mouseleave',function() { if (!$(editLink).is(":focus")) editLink.style.opacity = "0" })
  .on('focusin',function() { editLink.style.opacity = "1" })
  .on('focusout',function() { editLink.style.opacity = "0" });

  var renderedCell = undefined;
  for (let record of CellRendererRegistry.renderers) {
    try {
      renderedCell = record.renderer.render(this._rowIndex, this._cellIndex, cell, this);
    } catch (e) {
      continue;
    }
    if (renderedCell) {
      break;
    }
  }

  if (renderedCell) {
    divContent.appendChild(renderedCell);
  }
  divContent.appendChild(editLink).appendChild(document.createTextNode($.i18n('core-facets/edit')));

  this._td.appendChild(divContent);
};

DataTableCellUI.prototype._startEdit = function(elmt) {
  self = this;

  var originalContent = !this._cell || ("v" in this._cell && this._cell.v === null) ? "" : this._cell.v;

  var menu = MenuSystem.createMenu().addClass("data-table-cell-editor").width("400px");
  menu.html(DOM.loadHTML("core", "scripts/views/data-table/cell-editor.html"));
  var elmts = DOM.bind(menu);

  elmts.or_views_dataType.html($.i18n('core-views/data-type'));
  elmts.textarea.attr('aria-label',$.i18n('core-views/cell-content'));
  elmts.or_views_text.html($.i18n('core-views/text'));
  elmts.or_views_number.html($.i18n('core-views/number'));
  elmts.or_views_boolean.html($.i18n('core-views/boolean'));
  elmts.or_views_date.html($.i18n('core-views/date'));
  elmts.okButton.html($.i18n('core-buttons/apply'));
  elmts.or_views_enter.html($.i18n('core-buttons/enter'));
  elmts.okallButton.html($.i18n('core-buttons/apply-to-all'));
  elmts.or_views_ctrlEnter.html($.i18n('core-views/ctrl-enter'));
  elmts.cancelButton.html($.i18n('core-buttons/cancel'));
  elmts.or_views_esc.html($.i18n('core-buttons/esc'));

  var cellDataType = typeof originalContent === "string" ? "text" : typeof originalContent;
  cellDataType = (this._cell !== null && "t" in this._cell && this._cell.t !=  null) ? this._cell.t : cellDataType;
  elmts.typeSelect.val(cellDataType);

  elmts.typeSelect.on('change', function() {
    var newType = elmts.typeSelect.val();
    if (newType === "date") {
      elmts.cell_help_text.html($.i18n('core-views/cell-edit-date-help'));
      $(elmts.cell_help_text).show();
    } else {
      $(elmts.cell_help_text).hide();
    }
  });

  if (cellDataType === "date") {
    elmts.cell_help_text.html($.i18n('core-views/cell-edit-date-help'));
    $(elmts.cell_help_text).show();
  }

  MenuSystem.showMenu(menu, function(){});
  MenuSystem.positionMenuLeftRight(menu, $(this._td));

  var commit = function() {
    var type = elmts.typeSelect[0].value;

    var applyOthers = 0;
    if (this === elmts.okallButton[0]) {
      applyOthers = 1;
    }

    var text = elmts.textarea[0].value;
    var value = text;

    if (type == "number") {
      value = parseFloat(text);
      if (isNaN(value)) {
        alert($.i18n('core-views/not-valid-number'));
        return;
      }
    } else if (type == "boolean") {
      value = ("true" == text);
    } else if (type == "date") {
      value = Date.parse(text);
      if (!value) {
        alert($.i18n('core-views/not-valid-date'));
        return;
      }
      value = value.toString("yyyy-MM-ddTHH:mm:ssZ");
    }

    self._focusBeforeEdit.focus();
    MenuSystem.dismissAll();

    if (applyOthers) {
      Refine.postCoreProcess(
        "mass-edit",
        {},
        {
          columnName: Refine.cellIndexToColumn(self._cellIndex).name,
          expression: "value",
          edits: JSON.stringify([{
            from: [ originalContent ],
            to: value,
            type: type
          }])
        },
        { cellsChanged: true }
      );
    } else {
      Refine.postCoreProcess(
        "edit-one-cell", 
        {},
        {
          row: self._rowIndex,
          cell: self._cellIndex,
          value: value,
          type: type
        },
        {},
        {
          onDone: function(o) {
            if (o.cell.r) {
              o.cell.r = o.pool.recons[o.cell.r];
            }

            self._focusBeforeEdit.focus();
            MenuSystem.dismissAll();
            self._cell = o.cell;
            self._dataTableView._updateCell(self._rowIndex, self._cellIndex, self._cell);
            self._render();
          }
        }
      );
    }
  };

  elmts.okButton.on('click',commit);
  elmts.okallButton.on('click',commit);
  elmts.textarea
  .text(originalContent)
  .on('keydown',function(evt) {
    if (!evt.shiftKey || elmts.textarea.is(':focus')) {
      if (evt.key == "Enter") {
        if (evt.ctrlKey) {
          elmts.okallButton.trigger('click');
        } else {
          elmts.okButton.trigger('click');
        }
      } else if (evt.key == "Escape") {
        MenuSystem.dismissAll();
      }
    }
  })
  .trigger('select')
  .trigger('focus');

  setInitialHeightTextArea(elmts.textarea[0]);

  elmts.cancelButton.on('click',function() {
    MenuSystem.dismissAll();
  });
};
