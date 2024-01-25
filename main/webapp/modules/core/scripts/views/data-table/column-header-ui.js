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

function DataTableColumnHeaderUI(dataTableView, column, columnIndex, td) {
  this._dataTableView = dataTableView;
  this._column = column;
  this._columnIndex = columnIndex;
  this._td = td;

  this._render();
}

DataTableColumnHeaderUI._extenders = [];

/*
  To extend, call

  DataTableColumnHeaderUI.extendMenu(function(column, columnHeaderUI, menu) {
    ...
    MenuSystem.appendTo(menu, path, newItems);
  });
 */
DataTableColumnHeaderUI.extendMenu = function(extender) {
  DataTableColumnHeaderUI._extenders.push(extender);
};

DataTableColumnHeaderUI.prototype.getColumn = function() {
  return this._column;
};

DataTableColumnHeaderUI.prototype._render = function() {
  var self = this;
  var td = $(this._td);

  td.html(DOM.loadHTML("core", "scripts/views/data-table/column-header.html"));
  var elmts = DOM.bind(td);

  elmts.nameContainer.text(this._column.name);
  elmts.dropdownMenu.on('click',function() {
    self._createMenuForColumnHeader(this);
  });
  
  if ("reconStats" in this._column) {
    var stats = this._column.reconStats;
    if (stats.nonBlanks > 0) {
      var newPercent = Math.ceil(100 * stats.newTopics / stats.nonBlanks);
      var matchPercent = Math.ceil(100 * stats.matchedTopics / stats.nonBlanks);
      var unreconciledPercent = Math.ceil(100 * (stats.nonBlanks - stats.matchedTopics - stats.newTopics) / stats.nonBlanks);
      var title = $.i18n('core-views/recon-stats', matchPercent, newPercent, unreconciledPercent);

      var whole = $('<div>')
      .addClass("column-header-recon-stats-bar")
      .attr("title", title)
      .appendTo(elmts.reconStatsContainer.show());

      $('<div>')
      .addClass("column-header-recon-stats-blanks")
      .width(Math.round((stats.newTopics + stats.matchedTopics) * 100 / stats.nonBlanks) + "%")
      .appendTo(whole);

      $('<div>')
      .addClass("column-header-recon-stats-matched")
      .width(Math.round(stats.matchedTopics * 100 / stats.nonBlanks) + "%")
      .appendTo(whole);
    }
  }
  if("sourceReconConfig" in this._column) {
    if(this._column.sourceReconConfig.service){
     var service = ReconciliationManager.getServiceFromUrl(this._column.sourceReconConfig.service);
     var serviceName;
     if(service) {
       serviceName=service.name;
     }
     if(serviceName){
      elmts.nameContainer.attr("title",$.i18n('core-views/data-extended-from',service.name));
      }
    }
  }
};

DataTableColumnHeaderUI.prototype._createMenuForColumnHeader = function(elmt) {
  var self = this;
  var menu = [
    {
      id: "core/facet",
      label: $.i18n('core-views/facet'),
      width: "170px",
      submenu: []
    },
    {},
    {
      id: "core/edit-cells",
      label: $.i18n('core-views/edit-cells'),
      width: "170px",
      submenu: []
    },
    {
      id: "core/edit-column",
      label: $.i18n('core-views/edit-column'),
      submenu: []
    },
    {
      id: "core/transpose",
      label: $.i18n('core-views/transpose'),
      submenu: []
    },
    {},
    (
      this._dataTableView._getSortingCriterionForColumn(this._column.name) === null ?
        {
          id: "core/sort",
          "label": $.i18n('core-views/sort'),
          "click": function() {
            self._showSortingCriterion(null, self._dataTableView._getSortingCriteriaCount() > 0);
          }
        } :
        {
          id: "core/sort",
          label: $.i18n('core-views/sort'),
          submenu: this.createSortingMenu()
        }
    ),
    {
      id: "core/view",
      label: $.i18n('core-views/view'),
      tooltip: $.i18n('core-views/collapse-expand'),
      submenu: [
        {
          label: $.i18n('core-views/collapse-this'),
          click: function() {
            self._dataTableView._collapsedColumnNames[self._column.name] = true;
            self._dataTableView.render();
          }
        },
        {
          label: $.i18n('core-views/collapse-other'),
          click: function() {
            var collapsedColumnNames = {};
            for (var i = 0; i < theProject.columnModel.columns.length; i++) {
              if (i != self._columnIndex) {
                collapsedColumnNames[theProject.columnModel.columns[i].name] = true;
              }
            }
            self._dataTableView._collapsedColumnNames = collapsedColumnNames;
            self._dataTableView.render();
          }
        },
        {
          label: $.i18n('core-views/collapse-left'),
          click: function() {
            for (var i = 0; i < self._columnIndex; i++) {
              self._dataTableView._collapsedColumnNames[theProject.columnModel.columns[i].name] = true;
            }
            self._dataTableView.render();
          }
        },
        {
          label: $.i18n('core-views/collapse-right'),
          click: function() {
            for (var i = self._columnIndex + 1; i < theProject.columnModel.columns.length; i++) {
              self._dataTableView._collapsedColumnNames[theProject.columnModel.columns[i].name] = true;
            }
            self._dataTableView.render();
          }
        },
        {
          label: $.i18n('core-views/expand-all'),
          /**
           * This function expands all the columns in the project
           */
          // CS427 Issue Link: https://github.com/OpenRefine/OpenRefine/issues/4067
          click: function() {
            self._dataTableView._collapsedColumnNames = [];
            self._dataTableView.render();
          }
        },
        {
          label: $.i18n('core-views/expand-left'),
          /**
           * This function expands the columns to the left of the selected column
           */
          // CS427 Issue Link: https://github.com/OpenRefine/OpenRefine/issues/4067
          click: function() {
            //by deleting these entries from collapsedColumnNames, they won't render on the dataTableView
            for (var i = 0; i < self._columnIndex; i++) {
              delete self._dataTableView._collapsedColumnNames[theProject.columnModel.columns[i].name];
            }
            self._dataTableView.render();
          }
        },
        {
          label: $.i18n('core-views/expand-right'),
          /**
           * This function expands the columns to the right of the selected column
           */
          // CS427 Issue Link: https://github.com/OpenRefine/OpenRefine/issues/4067
          click: function() {
            //by deleting these entries from collapsedColumnNames, they won't render on the dataTableView
            for (var i = self._columnIndex + 1; i < theProject.columnModel.columns.length; i++) {
              delete self._dataTableView._collapsedColumnNames[theProject.columnModel.columns[i].name];
            }
            self._dataTableView.render();
          }
        }
      ]
    },
    {},
    {
      id: "core/reconcile",
      label: $.i18n('core-views/reconcile'),
      tooltip: $.i18n('core-views/reconcile-tooltip'),
      width: "170px",
      submenu: []
    }
  ];

  for (var i = 0; i < DataTableColumnHeaderUI._extenders.length; i++) {
    DataTableColumnHeaderUI._extenders[i].call(null, this._column, this, menu);
  }

  MenuSystem.createAndShowStandardMenu(menu, elmt, { width: "120px", horizontal: false });
};

DataTableColumnHeaderUI.prototype.createSortingMenu = function() {
  var self = this;
  var criterion = this._dataTableView._getSortingCriterionForColumn(this._column.name);
  var criteriaCount = this._dataTableView._getSortingCriteriaCount();
  var hasOtherCriteria = (criterion === null) ? (criteriaCount > 0) : criteriaCount > 1;

  var items = [
    {
      "label": $.i18n('core-views/sort'),
      "click": function() {
        self._showSortingCriterion(criterion, hasOtherCriteria);
      }
    }
  ];

  if (criterion !== null) {
    items.push({
      "label": $.i18n('core-views/reverse'),
      "click": function() {
        criterion.reverse = !criterion.reverse;
        self._dataTableView._addSortingCriterion(criterion);
      }
    });
    items.push({
      "label": $.i18n('core-views/remove-sort'),
      "click": function() {
        self._dataTableView._removeSortingCriterionOfColumn(criterion.column);
      }
    });
  }

  return items;
};

DataTableColumnHeaderUI.prototype._showSortingCriterion = function(criterion, hasOtherCriteria) {
  criterion = criterion || {
    column: this._column.name,
    valueType: "string",
    caseSensitive: false,
    errorPosition: 1,
    blankPosition: 2
  };

  var self = this;
  var frame = $(DOM.loadHTML("core", "scripts/views/data-table/sorting-criterion-dialog.html"));
  var elmts = DOM.bind(frame);

  elmts.dialogHeader.text($.i18n('core-views/sort-by')+' ' + this._column.name);
  
  elmts.or_views_sortAs.text($.i18n('core-views/sort-cell'));
  elmts.or_views_positionBlank.text($.i18n('core-views/pos-blank'));
  
  elmts.or_views_text.text($.i18n('core-views/text'));
  elmts.or_views_caseSens.text($.i18n('core-views/case-sensitive'));
  elmts.or_views_numbers.text($.i18n('core-views/numbers'));
  elmts.or_views_dates.text($.i18n('core-views/dates'));
  elmts.or_views_booleans.text($.i18n('core-views/booleans'));
  elmts.or_views_dragDrop.text($.i18n('core-views/drag-drop'));
  elmts.directionForwardLabel.text($.i18n('core-views/forward'));
  elmts.directionReverseLabel.text($.i18n('core-views/reverse'));
  elmts.or_views_sortByCol.text($.i18n('core-views/sort-by-col'));
  elmts.okButton.html($.i18n('core-buttons/ok'));
  elmts.cancelButton.text($.i18n('core-buttons/cancel'));

  elmts.valueTypeOptions
  .find("input[type='radio'][value='" + criterion.valueType + "']")
  .prop('checked', true);

  var setValueType = function(valueType) {
    var forward = elmts.directionForwardLabel;
    var reverse = elmts.directionReverseLabel;
    if (valueType == "string") {
      forward.html("a - z");
      reverse.html("z - a");
    } else if (valueType == "number") {
      forward.html($.i18n('core-views/smallest-first'));
      reverse.html($.i18n('core-views/largest-first'));
    } else if (valueType == "date") {
      forward.html($.i18n('core-views/earliest-first'));
      reverse.html($.i18n('core-views/latest-first'));
    } else if (valueType == "boolean") {
      forward.html($.i18n('core-views/false-true'));
      reverse.html($.i18n('core-views/true-false'));
    }
  };
  elmts.valueTypeOptions
  .find("input[type='radio']")
  .on('change',function() {
    setValueType(this.value);
  });

  if (criterion.valueType == "string" && criterion.caseSensitive) {
    elmts.caseSensitiveCheckbox.prop('checked', true);
  }

  elmts.directionOptions
  .find("input[type='radio'][value='" + (criterion.reverse ? "reverse" : "forward") + "']")
  .prop('checked', true);

  if (hasOtherCriteria) {
    elmts.sortAloneContainer.show();
  }

  var validValuesHtml = '<li kind="value">'+$.i18n('core-views/valid-values')+'</li>';
  var blankValuesHtml = '<li kind="blank">'+$.i18n('core-views/blanks')+'</li>';
  var errorValuesHtml = '<li kind="error">'+$.i18n('core-views/errors')+'</li>';
  var positionsHtml;
  if (criterion.blankPosition < 0) {
    if (criterion.errorPosition > 0) {
      positionsHtml = [ blankValuesHtml, validValuesHtml, errorValuesHtml ];
    } else if (criterion.errorPosition < criterion.blankPosition) {
      positionsHtml = [ errorValuesHtml, blankValuesHtml, validValuesHtml ];
    } else {
      positionsHtml = [ blankValuesHtml, errorValuesHtml, validValuesHtml ];
    }
  } else {
    if (criterion.errorPosition < 0) {
      positionsHtml = [ errorValuesHtml, validValuesHtml, blankValuesHtml ];
    } else if (criterion.errorPosition < criterion.blankPosition) {
      positionsHtml = [ validValuesHtml, errorValuesHtml, blankValuesHtml  ];
    } else {
      positionsHtml = [ validValuesHtml, blankValuesHtml, errorValuesHtml ];
    }
  }
  elmts.blankErrorPositions.html(positionsHtml.join("")).sortable().disableSelection();

  var level = DialogSystem.showDialog(frame);
  var dismiss = function() { DialogSystem.dismissLevel(level - 1); };

  setValueType(criterion.valueType); 

  elmts.cancelButton.on('click',dismiss);
  elmts.okButton.on('click',function() {
    var criterion2 = {
        column: self._column.name,
        valueType: elmts.valueTypeOptions.find("input[type='radio']:checked")[0].value,
        reverse: elmts.directionOptions.find("input[type='radio']:checked")[0].value == "reverse"
    };

    var valuePosition, blankPosition, errorPosition;
    elmts.blankErrorPositions.find("li").each(function(index, elmt) {
      var kind = this.getAttribute("kind");
      if (kind == "value") {
        valuePosition = index;
      } else if (kind == "blank") {
        blankPosition = index;
      } else if (kind == "error") {
        errorPosition = index;
      }
    });
    criterion2.blankPosition = blankPosition - valuePosition;
    criterion2.errorPosition = errorPosition - valuePosition;

    if (criterion2.valueType == "string") {
      criterion2.caseSensitive = elmts.caseSensitiveCheckbox[0].checked;
    }

    self._dataTableView._addSortingCriterion(
        criterion2, elmts.sortAloneContainer.find("input")[0].checked);

    dismiss();
  });
};
