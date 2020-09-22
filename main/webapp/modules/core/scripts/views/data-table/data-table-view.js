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

function DataTableView(div) {
  this._div = div;

  this._pageSize = 100;
  this._showRecon = true;
  this._collapsedColumnNames = {};
  this._sorting = { criteria: [] };
  this._columnHeaderUIs = [];
  this._shownulls = false;
  this._totalSize = this._pageSize;
  this._sizeRowFirst = 0;
  this._sizeRowsTotal = 0;
  this._sizeSinglePage = 0;
  this._scrollTop = 0;
  this._downwardDirection = true;
  this._pageStart = 0;
  this._headerTop = 0;
  this._screenSize = 0;
  this._bigScreenSetSize = 0;

  this._showRows(0);
  
  this._refocusPageInput = false;
}

DataTableView._extenders = [];

/*
  To extend, do something like this

  DataTableView.extendMenu(function(dataTableView, menu) {
      ...
    MenuSystem.appendTo(menu, [ "core/view" ], {
      "label": "Test",
      "click": function() {
          alert("Test");
      } 
    });
  });
*/
DataTableView.extendMenu = function(extender) {
  DataTableView._extenders.push(extender);
};

DataTableView.prototype.getSorting = function() {
  return this._sorting;
};

DataTableView.prototype.resize = function() {
  var topHeight =
    this._div.find(".viewpanel-header").outerHeight(true);
  var tableContainerIntendedHeight = this._div.innerHeight() - topHeight;
  
  var tableContainer = this._div.find(".data-table-container").css("display", "block");
  var tableContainerVPadding = tableContainer.outerHeight(true) - tableContainer.height();
  tableContainer.height((tableContainerIntendedHeight - tableContainerVPadding) + "px");
};

DataTableView.prototype.update = function(onDone) {
  this._showRows(0, onDone);
};

DataTableView.prototype.render = function() {
  var self = this;

  var oldTableDiv = this._div.find(".data-table-container");
  var scrollLeft = (oldTableDiv.length > 0) ? oldTableDiv[0].scrollLeft : 0;

  var html = $(
    '<div class="viewpanel-header">' +
      '<div class="viewpanel-rowrecord" bind="rowRecordControls">'+$.i18n('core-views/show-as')+': ' +
        '<span bind="modeSelectors"></span>' + 
      '</div>' +
      '<div class="viewpanel-sorting" bind="sortingControls"></div>' +
    '</div>' +
    '<div bind="dataTableContainer" class="data-table-container">' +
      '<table class="data-table">'+
        '<thead bind="tableHeader" class="data-table-header">'+
        '</thead>'+
        '<tbody bind="table" class="data-table">'+
        '</tbody>'+
      '</table>' +
    '</div>'
  );
  var elmts = DOM.bind(html);
  this._div.empty().append(html);

  ui.summaryBar.updateResultCount();

  var renderBrowsingModeLink = function(label, value) {
    var a = $('<a href="javascript:{}"></a>')
    .addClass("viewPanel-browsingModes-mode")
    .text(label)
    .appendTo(elmts.modeSelectors);

    if (value == ui.browsingEngine.getMode()) {
      a.addClass("selected");
    } else {
      a.addClass("action").click(function(evt) {
        ui.browsingEngine.setMode(value);
      });
    }
  };
  renderBrowsingModeLink($.i18n('core-views/rows'), "row-based");
  renderBrowsingModeLink($.i18n('core-views/records'), "record-based");

  if (this._sorting.criteria.length > 0) {
    this._renderSortingControls(elmts.sortingControls);
  }

  this._renderDataTables(elmts.table[0], elmts.tableHeader[0]);

  // show/hide null values in cells
  $(".data-table-null").toggle(self._shownulls);

  this.resize();
  
  elmts.dataTableContainer[0].scrollLeft = scrollLeft;
};

DataTableView.prototype._renderSortingControls = function(sortingControls) {
  var self = this;

  $('<a href="javascript:{}"></a>')
  .addClass("action")
  .text($.i18n('core-views/sort') + " ")
  .append($('<img>').attr("src", "../images/down-arrow.png"))
  .appendTo(sortingControls)
  .click(function() {
    self._createSortingMenu(this);
  });
};

DataTableView.prototype._checkPaginationSize = function(gridPageSize, defaultGridPageSize) {
  var self = this;
  var newGridPageSize = [];
  
  if(gridPageSize == null || typeof gridPageSize != "object") return defaultGridPageSize;

  for (var i = 0; i < gridPageSize.length; i++) {
    if(typeof gridPageSize[i] == "number" && gridPageSize[i] > 0 && gridPageSize[i] < 10000)
      newGridPageSize.push(gridPageSize[i]);
  }

  if(newGridPageSize.length < 2) return defaultGridPageSize;
  
  var distinctValueFilter = (value, index, selfArray) => (selfArray.indexOf(value) == index);
  newGridPageSize.filter(distinctValueFilter);
  
  newGridPageSize.sort((a, b) => (a - b));
  
  return newGridPageSize;
};

DataTableView.prototype._renderDataTables = function(table, tableHeader) {
  var self = this;

  var columns = theProject.columnModel.columns;
  var columnGroups = theProject.columnModel.columnGroups;

  /*------------------------------------------------------------
   *  Column Group Headers
   *------------------------------------------------------------
   */

  var renderColumnKeys = function(keys) {
    if (keys.length > 0) {
      var tr = tableHeader.insertRow(-1);
      $(tr.appendChild(document.createElement("th"))).attr('colspan', '3'); // star, flag, row index

      for (var c = 0; c < columns.length; c++) {
        var column = columns[c];
        var th = tr.appendChild(document.createElement("th"));
        if (self._collapsedColumnNames.hasOwnProperty(column.name)) {
          $(th).html('&nbsp;');
        } else {
          for (var k = 0; k < keys.length; k++) {
            // if a node is a key in the tree-based data (JSON/XML/etc), then also display a dropdown arrow (non-functional currently)
            // See https://github.com/OpenRefine/OpenRefine/blob/master/main/src/com/google/refine/model/ColumnGroup.java
            // and https://github.com/OpenRefine/OpenRefine/tree/master/main/src/com/google/refine/importers/tree
            if (c == keys[k]) {
              $('<img />').attr("src", "../images/down-arrow.png").appendTo(th);
              break;
            }
          }
        }
      }
    }
  };

  var renderColumnGroups = function(groups, keys) {
    var nextLayer = [];

    if (groups.length > 0) {
      var tr = tableHeader.insertRow(-1);
      $(tr.appendChild(document.createElement("th"))).attr('colspan', '3'); // star, flag, row index

      for (var c = 0; c < columns.length; c++) {
        var foundGroup = false;
        var columnGroup;

        for (var g = 0; g < groups.length; g++) {
          columnGroup = groups[g];
          if (columnGroup.startColumnIndex == c) {
            foundGroup = true;
            break;
          }
        }

        var th = tr.appendChild(document.createElement("th"));
        if (foundGroup) {
          th.setAttribute("colspan", columnGroup.columnSpan);
          th.style.background = "#FF6A00";

          if (columnGroup.keyColumnIndex >= 0) {
            keys.push(columnGroup.keyColumnIndex);
          }

          c += (columnGroup.columnSpan - 1);

          if ("subgroups" in columnGroup) {
            nextLayer = nextLayer.concat(columnGroup.subgroups);
          }
        }
      }
    }

    renderColumnKeys(keys);

    if (nextLayer.length > 0) {
      renderColumnGroups(nextLayer, []);
    }
  };

  if (columnGroups.length > 0) {
    renderColumnGroups(
        columnGroups, 
        [ theProject.columnModel.keyCellIndex ]
    );
  }    

  /*------------------------------------------------------------
   *  Column Headers with Menus
   *------------------------------------------------------------
   */

  var trHead = tableHeader.insertRow(-1);
  DOM.bind(
      $(trHead.appendChild(document.createElement("th")))
      .attr("colspan", "3")
      .addClass("column-header")
      .html(
        '<div class="column-header-title">' +
          '<a class="column-header-menu" bind="dropdownMenu"></a><span class="column-header-name">'+$.i18n('core-views/all')+'</span>' +
        '</div>'
      )
  ).dropdownMenu.click(function() {
    self._createMenuForAllColumns(this);
  });
  this._columnHeaderUIs = [];
  var createColumnHeader = function(column, index) {
    var th = trHead.appendChild(document.createElement("th"));
    $(th).addClass("column-header").attr('title', column.name);
    if (self._collapsedColumnNames.hasOwnProperty(column.name)) {
      $(th).html("&nbsp;").click(function(evt) {
        delete self._collapsedColumnNames[column.name];
        self.render();
      });
    } else {
      var columnHeaderUI = new DataTableColumnHeaderUI(self, column, index, th);
      self._columnHeaderUIs.push(columnHeaderUI);
    }
  };

  for (var i = 0; i < columns.length; i++) {
    createColumnHeader(columns[i], i);
  }

  /*------------------------------------------------------------
   *  Data Cells
   *------------------------------------------------------------
   */

  var onClickStar = function(evt) {
    var selectedRow = evt.currentTarget.data;
    var newStarred = !selectedRow.row.starred;
    Refine.postCoreProcess(
      "annotate-one-row",
      { row: selectedRow.row.i, starred: newStarred },
      null,
      {},
      {
        onDone: function(o) {
          selectedRow.row.starred = newStarred;
          renderRow(selectedRow.tr, selectedRow.r, selectedRow.row, selectedRow.even);
        }
      },
      "json"
    );
  };
  var onClickFlag = function(evt) {
    var selectedRow = evt.currentTarget.data;
    var newFlagged = !selectedRow.row.flagged;
    Refine.postCoreProcess(
      "annotate-one-row",
      { row: selectedRow.row.i, flagged: newFlagged },
      null,
      {},
      {
        onDone: function(o) {
          selectedRow.row.flagged = newFlagged;
          renderRow(selectedRow.tr, selectedRow.r, selectedRow.row, selectedRow.even);
        }
      },
      "json"
    );
  };

  var renderRowTemplate = function() {
    var tr = document.createElement('tr');
    
    var tdStar = tr.insertCell(-1);
    $('<a href="javascript:{}">&nbsp;</a>')
    .addClass("data-table-star-off") // off by default
    .appendTo(tdStar);

    var tdFlag = tr.insertCell(-1);
    $('<a href="javascript:{}">&nbsp;</a>')
    .addClass("data-table-flag-off") // off by default
    .appendTo(tdFlag);

    var tdIndex = tr.insertCell(-1);
    var div = document.createElement('div');
    tdIndex.appendChild(div);

    for (var i = 0; i < columns.length; i++) {
      tr.insertCell(-1);
    }

    return tr;
  };

  var rowTemplate = renderRowTemplate();

  var renderRow = function(tr, r, row, even) {
    var cellsRow = row.cells;

    tr.cells[0].data = {tr: tr, r: r, row: row, even: even};
    tr.cells[1].data = {tr: tr, r: r, row: row, even: even};

    tr.cells[0].addEventListener('click', onClickStar);
    if (row.starred) {
      var a = tr.cells[0].querySelector('a');
      a.classList.remove("data-table-star-off");
      a.classList.add("data-table-star-on");
    } else {
      var a = tr.cells[0].querySelector('a');
      a.classList.add("data-table-star-off");
      a.classList.remove("data-table-star-on");
    }

    tr.cells[1].addEventListener('click', onClickFlag);
    if (row.flagged) {
      var a = tr.cells[1].querySelector('a');
      a.classList.remove("data-table-flag-off");
      a.classList.add("data-table-flag-on");
    } else {
      var a = tr.cells[1].querySelector('a');
      a.classList.add("data-table-flag-off");
      a.classList.remove("data-table-flag-on");
    }

    var tdIndex = tr.cells[2];
    var div = tdIndex.querySelector('div');
    if (theProject.rowModel.mode == "record-based") {
      if ("j" in row) {
        $(tr).addClass("record");
        div.innerHTML = (row.j + 1) + '.';
      } else {
        div.innerHTML = '\u00A0';
      }
    } else {
      div.innerHTML = (row.i + 1) + '.';
    }

    $(tr).addClass(even ? "even" : "odd");

    for (var i = 0; i < columns.length; i++) {
      var column = columns[i];
      var td = tr.cells[i + 3];
      if (self._collapsedColumnNames.hasOwnProperty(column.name)) {
        td.innerHTML = "&nbsp;";
      } else {
        var cell = (column.cellIndex < cellsRow.length) ? cellsRow[column.cellIndex] : null;
        // TODO: Update cell rather than replacing
        new DataTableCellUI(self, cell, row.i, column.cellIndex, td);
      }
    }
  };

  window.loadRows = function(start, top) {
    var rows = theProject.rowModel.rows;
    var even = (start % 2) ? false : true;
    for (var r = 0; r < rows.length; r++) {
      var row = rows[r];
      var tr = rowTemplate.cloneNode(true);
      var tbody = document.querySelector('.data-table tbody');
      if(top) {
        tbody.insertBefore(tr, tbody.children[r + 1]);
      } else {
        tbody.appendChild(tr);
      }
      if (theProject.rowModel.mode == "row-based" || "j" in row) {
        even = !even;
      }
      renderRow(tr, r, row, even);
    }
  }
  loadRows();

  var total = 0, row;
  for (var i = 1; i < table.rows.length; i++) {
      row = table.rows[i];
      total += row.offsetHeight;
  }
  this._sizeRowFirst = total / this._pageSize;
  this._sizeRowsTotal = this._sizeRowFirst * theProject.rowModel.filtered;
  this._sizeSinglePage = this._sizeRowFirst * this._pageSize;
  document.querySelector('.data-table tbody').insertRow(0).setAttribute('class', 'first-row');
  this._headerTop = $('thead').offset().top + $('thead').height();
  this._pageStart = 0;
  this._totalSize = this._pageSize;
  this._screenSize = Math.ceil((window.innerHeight - this._headerTop) / this._sizeRowFirst);
  this._bigScreenSetSize = this._pageSize * Math.round((2 * this._screenSize) / this._pageSize);
  this._adjustNextSetClasses();

  window.prevOperationSet = false;
  var scrollEvent = function(positionNextSet, positionLastElement, positionPrevSet, positionFirstElement, evt) {
    if(!prevOperationSet) {
      if(self._downwardDirection) {
        if((positionNextSet.top >= 0 && positionNextSet.bottom <= window.innerHeight) || (positionLastElement.top < window.innerHeight && positionLastElement.top > 0)) {
          self._onBottomTable(table.parentNode.parentNode, evt);
          prevOperationSet = true;
        }
      }
      else if(!self._downwardDirection) {
        if(positionPrevSet.top >= 0 && positionPrevSet.bottom <= window.innerHeight || (positionFirstElement.bottom > 0 && positionFirstElement.bottom < window.innerHeight)) {
          self._onTopTable(table.parentNode.parentNode, evt);
          prevOperationSet = true;
        }
      }
    }
  };

  var onScroll = function(evt) {
    self._downwardDirection = self._scrollTop < $(this).scrollTop();
    try {
      var nextSet = document.querySelectorAll('.load-next-set');
      var prevSet = nextSet[0];
      nextSet = nextSet[nextSet.length - 1];
      var positionNextSet = nextSet.getBoundingClientRect();
      var positionPrevSet = prevSet.getBoundingClientRect();
    } catch (err) {
      var positionNextSet = {top: undefined, bottom: undefined};
      var positionPrevSet = {top: undefined, bottom: undefined};
    }
    var lastElement = document.querySelector('.last-row');
    var positionLastElement = lastElement.getBoundingClientRect();
    var firstElement = document.querySelector('.first-row');
    var positionFirstElement = firstElement.getBoundingClientRect();

    scrollEvent(positionNextSet, positionLastElement, positionPrevSet, positionFirstElement, evt);

    if((positionLastElement.top <= 0 && positionLastElement.bottom >= 0) || (positionFirstElement.top < self._headerTop + 1 && positionFirstElement.bottom >= window.innerHeight)) {
      clearTimeout($.data(this, 'scrollTimer'));
      $.data(this, 'scrollTimer', setTimeout(function() {
        self.getPageNumberScrolling(self._scrollTop, table);
        prevOperationSet = false;
      }, 250));
    }

    self._scrollTop = $(this).scrollTop();
  }

  $(table.parentNode.parentNode).on('scroll', onScroll);
}; // end _renderDataTables

DataTableView.prototype.getPageNumberScrolling = function(scrollPosition, table) {
  // Loading sign
  if(document.querySelector('div#body').classList.contains('hide-left-panel'))
    var width = 0.5 * window.innerWidth;
  else var width = 150 + 0.5 * window.innerWidth;
  var loadingImg = document.createElement('img');
  loadingImg.setAttribute('src', 'images/large-spinner.gif');
  loadingImg.style.zIndex = '10';
  loadingImg.style.top = '55%';
  loadingImg.style.left = width + 'px';
  loadingImg.style.position = 'fixed';
  table.appendChild(loadingImg);

  var goto = Math.floor(scrollPosition / this._sizeSinglePage);
  this._onChangeGotoScrolling(scrollPosition, goto, table, loadingImg);
};

DataTableView.prototype._removeUpperRows = function(start) {
  if (theProject.rowModel.mode == "record-based") {
    if($('.data-table tbody tr.record').length > 2 * this._pageSize) {
      $('.data-table tbody tr').slice(1, $('.data-table tbody tr.record').eq(this._pageSize).index()).remove();
      }
  } else if($('.data-table tbody tr').length > 2 * this._pageSize) {
    if(this._pageSize > this._screenSize) {
      $('.data-table tbody tr').slice(1, Math.max(0, $('.data-table tbody tr').length - 2 * this._pageSize)).remove();
    } else {
      this._pageStart = Math.max(this._pageStart, this._totalSize - this._bigScreenSetSize);
      var sliceIndex = $('.data-table tbody tr').length - this._bigScreenSetSize;
      $('.data-table tbody tr').slice(1, Math.max(0, sliceIndex)).remove();
    }
  }
};

DataTableView.prototype._removeLowerRows = function(start) {
  if (theProject.rowModel.mode == "record-based") {
    $('.data-table tbody tr').slice($('.data-table tbody tr.record').eq(2 * this._pageSize).index(), $('.data-table tbody tr').length).remove();
  } else {
    if(this._pageSize > this._screenSize) {
      $('.data-table tbody tr').slice(2 * this._pageSize + 1, $('.data-table tbody tr').length).remove();
    } else {
      this._totalSize = this._pageStart + this._bigScreenSetSize;
      $('.data-table tbody tr').slice(this._totalSize + 1).remove();
    }
  }
};

DataTableView.prototype._adjustNextSetClasses = function(start, top) {
  if(!top) {
    // Deletion of upper rows that are not visible anymore
    this._pageStart = start - this._pageSize;
    this._removeUpperRows(start);
  } else {
    // Deletion of lower rows that are not visible anymore
    this._pageStart = start;
    this._removeLowerRows(start);
  }

  var heightToAddTop = (this._pageStart) * this._sizeRowFirst;
  var heightToAddBottom = Math.max(0, this._sizeRowsTotal - this._totalSize * this._sizeRowFirst);
  this._addHeights(heightToAddTop, heightToAddBottom);
};

DataTableView.prototype._addHeights = function(heightToAddTop, heightToAddBottom, table, loadingImg) {
  $('.data-table tbody tr:first').css('height', heightToAddTop);

  document.querySelector('.data-table').insertRow();
  $('.data-table tbody tr:last').css('height', heightToAddBottom);
  $('.data-table tbody tr:last').addClass('last-row');
  if(theProject.rowModel.rows.length >= this._pageSize) {
    if (theProject.rowModel.mode == "record-based") {
      $('.data-table tbody tr.record').eq(-1 * (this._pageSize / 2 + 1)).addClass('load-next-set');
      $('.data-table tbody tr.record').eq(this._pageSize / 2 - 1).addClass('load-next-set');
    } else {
      $('.data-table tbody tr').eq(-1 * (this._pageSize / 2 + 2)).addClass('load-next-set');
      $('.data-table tbody tr').eq(this._pageSize / 2).addClass('load-next-set');
    }
  }
  if(table !== undefined) table.removeChild(loadingImg);
  setTimeout(function() { prevOperationSet = false; }, 250);
};

DataTableView.prototype._adjustNextSetClassesSpeed = function(start, table, loadingImg) {
  var heightToAddTop = Math.max(0, start * this._sizeRowFirst);
  var heightToAddBottom = Math.max(0, this._sizeRowsTotal - this._totalSize * this._sizeRowFirst);

  $('.data-table tbody tr').slice(1, $('.data-table tbody tr').length - theProject.rowModel.rows.length).remove();
  this._pageStart = this._totalSize - this._pageSize;

  this._addHeights(heightToAddTop, heightToAddBottom, table, loadingImg);
};

DataTableView.prototype._showRows = function(start, onDone) {
  var self = this;
  Refine.fetchRows(start, this._pageSize, function() {
    self.render();

    while(self._totalSize < self._screenSize) {
      self._showRowsBottom(start + self._pageSize, onDone);
    }

    if (onDone) {
      onDone();
    }
  }, this._sorting);
};

DataTableView.prototype._showRowsBottom = function(start, onDone) {
  var self = this;

  this._totalSize = start + this._pageSize;

  Refine.fetchRows(start, this._pageSize, function() {
    $('.last-row').remove();

    loadRows(start);
    self._adjustNextSetClasses(start);

    if (onDone) {
      onDone();
    }
  }, this._sorting);
};

DataTableView.prototype._showRowsTop = function(start, limit, onDone) {
  var self = this;

  this._totalSize = start + 2 * this._pageSize;

  Refine.fetchRows(start, limit, function() {
    $('.last-row').remove();

    loadRows(start, true);
    self._adjustNextSetClasses(start, true);

    if (onDone) {
      onDone();
    }
  }, this._sorting);
};

DataTableView.prototype._showRowsSpeed = function(table, start, loadingImg, onDone) {
  var self = this;

  this._totalSize = start +  this._pageSize;

  Refine.fetchRows(start, this._pageSize, function() {
    $('.last-row').remove();

    loadRows(start);
    self._adjustNextSetClassesSpeed(start, table, loadingImg);

    while(self._totalSize - self._pageStart < self._screenSize) {
      self._showRowsBottom(start + self._pageSize, onDone);
    }

    if (onDone) {
      onDone();
    }
  }, this._sorting);
};

DataTableView.prototype._onChangeGotoScrolling = function(scrollPosition, gotoPageNumber, table, loadingImg, elmt, evt) {
  var row = scrollPosition / this._sizeRowFirst;
  row -= this._pageSize / 2.5;
  this._showRowsSpeed(table, Math.floor(row), loadingImg);
};

DataTableView.prototype._onBottomTable = function(elmt, evt) {
  if(this._totalSize < theProject.rowModel.filtered) {
    this._showRowsBottom(this._totalSize);
  }
};

DataTableView.prototype._onTopTable = function(elmt, evt) {
  if(this._pageStart - this._pageSize >= 0) {
    this._showRowsTop(this._pageStart - this._pageSize, this._pageSize);
  } else if(this._pageStart) {
    this._showRowsTop(0, this._pageStart);
  }
};

DataTableView.prototype._getSortingCriteriaCount = function() {
  return this._sorting.criteria.length;
};

DataTableView.prototype._sortedByColumn = function(columnName) {
  for (var i = 0; i < this._sorting.criteria.length; i++) {
    if (this._sorting.criteria[i].column == columnName) {
      return true;
    }
  }
  return false;
};

DataTableView.prototype._getSortingCriterionForColumn = function(columnName) {
  for (var i = 0; i < this._sorting.criteria.length; i++) {
    if (this._sorting.criteria[i].column == columnName) {
      return this._sorting.criteria[i];
    }
  }
  return null;
};

DataTableView.prototype._removeSortingCriterionOfColumn = function(columnName) {
  for (var i = 0; i < this._sorting.criteria.length; i++) {
    if (this._sorting.criteria[i].column == columnName) {
      this._sorting.criteria.splice(i, 1);
      break;
    }
  }
  this.update();
};

DataTableView.prototype._addSortingCriterion = function(criterion, alone) {
  if (alone) {
    this._sorting.criteria = [];
  } else {
    for (var i = 0; i < this._sorting.criteria.length; i++) {
      if (this._sorting.criteria[i].column == criterion.column) {
        this._sorting.criteria[i] = criterion;
        this.update();
        return;
      }
    }
  }
  this._sorting.criteria.push(criterion);
  this.update();
};

/** below can be move to seperate file **/
  var doTextTransformPrompt = function() {
    var frame = $(
        DOM.loadHTML("core", "scripts/views/data-table/text-transform-dialog.html")
        .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));

    var elmts = DOM.bind(frame);
    elmts.or_views_errorOn.text($.i18n('core-views/on-error'));
    elmts.or_views_keepOr.text($.i18n('core-views/keep-or'));
    elmts.or_views_setBlank.text($.i18n('core-views/set-blank'));
    elmts.or_views_storeErr.text($.i18n('core-views/store-err'));
    elmts.or_views_reTrans.text($.i18n('core-views/re-trans'));
    elmts.or_views_timesChang.text($.i18n('core-views/times-chang'));
    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.text($.i18n('core-buttons/cancel'));    

    var level = DialogSystem.showDialog(frame);
    var dismiss = function() { DialogSystem.dismissUntil(level - 1); };

    elmts.cancelButton.click(dismiss);
    elmts.okButton.click(function() {
        new ExpressionColumnDialog(
                previewWidget.getExpression(true),
                $('input[name="text-transform-dialog-onerror-choice"]:checked')[0].value,
                elmts.repeatCheckbox[0].checked,
                elmts.repeatCountInput[0].value
        );
    });
    
    var previewWidget = new ExpressionPreviewDialog.Widget(
      elmts,
      -1,
      [],
      [],
      null
    );
    previewWidget._prepareUpdate = function(params) {
      params.repeat = elmts.repeatCheckbox[0].checked;
      params.repeatCount = elmts.repeatCountInput[0].value;
    };
  };
  /** above can be move to seperate file **/
  
DataTableView.prototype._createMenuForAllColumns = function(elmt) {
  var self = this;
  var menu = [
        {
            label: $.i18n('core-views/transform'),
            id: "core/facets",
            width: "200px",
            click: function() {
                   doTextTransformPrompt();
            }
        },
    {},
    {
      label: $.i18n('core-views/facet'),
      id: "core/facets",
      width: "200px",
      submenu: [
        {
          label: $.i18n('core-views/facet-star'),
          id: "core/facet-by-star",
          click: function() {
            ui.browsingEngine.addFacet(
              "list", 
              {
                "name" : $.i18n('core-views/starred-rows'),
                "columnName" : "", 
                "expression" : "row.starred"
              },
              {
                "scroll" : false
              }
            );
          }
        },
        {
          label: $.i18n('core-views/facet-flag'),
          id: "core/facet-by-flag",
          click: function() {
            ui.browsingEngine.addFacet(
              "list", 
              {
                "name" : $.i18n('core-views/flagged-rows'),
                "columnName" : "", 
                "expression" : "row.flagged"
              },
              {
                "scroll" : false
              }
            );
          }
        },
        {
          label: $.i18n('core-views/facet-blank'),
          id: "core/facet-by-blank",
          click: function() {
            ui.browsingEngine.addFacet(
              "list", 
              {
                "name" : $.i18n('core-views/blank-rows'),
                "columnName" : "", 
                "expression" : "(filter(row.columnNames,cn,isNonBlank(cells[cn].value)).length()==0).toString()"
              },
              {
                "scroll" : false
              }
            );
          }
        },
        {
          label: $.i18n('core-views/blank-values'),
          id: "core/blank-values",
          click: function() {
            ui.browsingEngine.addFacet(
                "list",
                {
                  "name" : $.i18n('core-views/blank-values'),
                  "columnName" : "",
                  "expression" : "filter(row.columnNames,cn,isBlank(cells[cn].value))"
                },
                {
                  "scroll" : false
                }
            );
          }
        },
        {
          label: $.i18n('core-views/blank-records'),
          id: "core/blank-records",
          click: function() {
            ui.browsingEngine.addFacet(
                "list",
                {
                  "name" : $.i18n('core-views/blank-records'),
                  "columnName" : "",
                  "expression" : "filter(row.columnNames,cn,isBlank(if(row.record.fromRowIndex==row.index,row.record.cells[cn].value.join(\"\"),true)))"
                },
                {
                  "scroll" : false
                }
            );
          }
        },
        {
          label: $.i18n('core-views/non-blank-values'),
          id: "core/non-blank-values",
          click: function() {
            ui.browsingEngine.addFacet(
              "list", 
              {
                "name" : $.i18n('core-views/non-blank-values'),
                "columnName" : "", 
                "expression" : "filter(row.columnNames,cn,isNonBlank(cells[cn].value))"
              },
              {
                "scroll" : false
              }
            );
          }
        },
        {
          label: $.i18n('core-views/non-blank-records'),
          id: "core/non-blank-records",
          click: function() {
            ui.browsingEngine.addFacet(
              "list", 
              {
                "name" : $.i18n('core-views/non-blank-records'),
                "columnName" : "", 
                "expression" : "filter(row.columnNames,cn,isNonBlank(if(row.record.fromRowIndex==row.index,row.record.cells[cn].value.join(\"\"),null)))"
              },
              {
                "scroll" : false
              }
            );
          }
        }
      ]
    },
    {},
    {
      label: $.i18n('core-views/edit-rows'),
      id: "core/edit-rows",
      width: "200px",
      submenu: [
        {
          label: $.i18n('core-views/star-rows'),
          id: "core/star-rows",
          click: function() {
            Refine.postCoreProcess("annotate-rows", { "starred" : "true" }, null, { rowMetadataChanged: true });
          }
        },
        {
          label: $.i18n('core-views/unstar-rows'),
          id: "core/unstar-rows",
          click: function() {
            Refine.postCoreProcess("annotate-rows", { "starred" : "false" }, null, { rowMetadataChanged: true });
          }
        },
        {},
        {
          label: $.i18n('core-views/flag-rows'),
          id: "core/flag-rows",
          click: function() {
            Refine.postCoreProcess("annotate-rows", { "flagged" : "true" }, null, { rowMetadataChanged: true });
          }
        },
        {
          label: $.i18n('core-views/unflag-rows'),
          id: "core/unflag-rows",
          click: function() {
            Refine.postCoreProcess("annotate-rows", { "flagged" : "false" }, null, { rowMetadataChanged: true });
          }
        },
        {},
        {
          label: $.i18n('core-views/remove-matching'),
          id: "core/remove-rows",
          click: function() {
            Refine.postCoreProcess("remove-rows", {}, null, { rowMetadataChanged: true });
          }
        }
      ]
    },
    {
      label: $.i18n('core-views/edit-col'),
      id: "core/edit-columns",
      width: "200px",
      submenu: [
        {
          label: $.i18n('core-views/reorder-remove')+"...",
          id: "core/reorder-columns",
          click: function() {
            new ColumnReorderingDialog();
          }
        },
        {},
        {
          label: $.i18n('core-views/fill-down'),
          id: "core/fill-down",
          click: doAllFillDown
        },
        {
          label: $.i18n('core-views/blank-down'),
          id: "core/blank-down",
          click: doAllBlankDown
        }
      ]
    },
    {},
    {
      label: $.i18n('core-views/view'),
      id: "core/view",
      width: "200px",
      submenu: [
        {
          label: $.i18n('core-views/collapse-all'),
          id: "core/collapse-all-columns",
          click: function() {
            for (var i = 0; i < theProject.columnModel.columns.length; i++) {
              self._collapsedColumnNames[theProject.columnModel.columns[i].name] = true;
            }
            self.render();
          }
        },
        {
          label: $.i18n('core-views/expand-all'),
          id: "core/expand-all-columns",
          click: function() {
            self._collapsedColumnNames = [];
            self.render();
          }
        },
        {
          label: $.i18n('core-views/display-null'),
          id: "core/display-null",
          click: function() {
            $(".data-table-null").toggle();
            self._shownulls = !(self._shownulls);
          }
        }
      ]
    }
  ];

  for (var i = 0; i < DataTableView._extenders.length; i++) {
    DataTableView._extenders[i].call(null, this, menu);
  }

  MenuSystem.createAndShowStandardMenu(menu, elmt, { width: "120px", horizontal: false });
};

DataTableView.prototype._createSortingMenu = function(elmt) {
  var self = this;
  var items = [
    {
      "label" : $.i18n('core-views/remove-sort'),
      "click" : function() {
        self._sorting.criteria = [];
        self.update();
      }
    },
    {
      "label" : $.i18n('core-views/reorder-perma'),
      "click" : function() {
        Refine.postCoreProcess(
          "reorder-rows",
          null,
          {
            "sorting" : JSON.stringify(self._sorting),
            "mode" : ui.browsingEngine.getMode()
          },
          { rowMetadataChanged: true },
          {
            onDone: function() {
              self._sorting.criteria = [];
            }
          }
        );
      }
    },
    {}
  ];

  var getColumnHeaderUI = function(columnName) {
    for (var i = 0; i < self._columnHeaderUIs.length; i++) {
      var columnHeaderUI = self._columnHeaderUIs[i];
      if (columnHeaderUI.getColumn().name == columnName) {
        return columnHeaderUI;
      }
    }
    return null;
  };
  var createSubmenu = function(criterion) {
    var columnHeaderUI = getColumnHeaderUI(criterion.column);
    if (columnHeaderUI !== null) {
      items.push({
        "label" : $.i18n('core-views/by')+" " + criterion.column,
        "submenu" : columnHeaderUI.createSortingMenu()
      });
    }
  };
  for (var i = 0; i < this._sorting.criteria.length; i++) {
    createSubmenu(this._sorting.criteria[i]);
  }

  MenuSystem.createAndShowStandardMenu(items, elmt, { horizontal: false });
};

var doAllFillDown = function() {
  doFillDown(theProject.columnModel.columns.length - 1);
};

var doFillDown = function(colIndex) {
  if (colIndex >= 0) {
    Refine.postCoreProcess(
        "fill-down",
        {
          columnName: theProject.columnModel.columns[colIndex].name
        },
        null,
        {modelsChanged: true},
        {
          onDone: function() {
            doFillDown(--colIndex);
          }
        }
    );
  }
};

var doAllBlankDown = function() {
  doBlankDown(0);
};

var doBlankDown = function(colIndex) {
  if (colIndex < theProject.columnModel.columns.length) {
    Refine.postCoreProcess(
        "blank-down",
        {
          columnName: theProject.columnModel.columns[colIndex].name
        },
        null,
        { modelsChanged: true },
        {
          onDone: function() {
            doBlankDown(++colIndex);
          }
        }
    );
  }
};


DataTableView.prototype._updateCell = function(rowIndex, cellIndex, cell) {
  var rows = theProject.rowModel.rows;
  for (var r = 0; r < rows.length; r++) {
    var row = rows[r];
    if (row.i === rowIndex) {
      while (cellIndex >= row.cells.length) {
        row.cells.push(null);
      }
      row.cells[cellIndex] = cell;
      break;
    }
  }
};

DataTableView.sampleVisibleRows = function(column) {
  var rowIndices = [];
  var values = [];

  var rows = theProject.rowModel.rows;
  for (var r = 0; r < rows.length; r++) {
    var row = rows[r];

    rowIndices.push(row.i);

    var v = null;
    if (column && column.cellIndex < row.cells.length) {
      var cell = row.cells[column.cellIndex];
      if (cell !== null) {
        v = cell.v;
      }
    }
    values.push(v);
  }

  return {
    rowIndices: rowIndices,
    values: values
  };
};

DataTableView.promptExpressionOnVisibleRows = function(column, title, expression, onDone) {
  var o = DataTableView.sampleVisibleRows(column);

  var self = this;
  new ExpressionPreviewDialog(
    title,
    column.cellIndex, 
    o.rowIndices, 
    o.values,
    expression,
    onDone
  );
};
