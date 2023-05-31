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

Refine.FixedWidthParserUI = function(controller, jobID, job, format, config,
    dataContainerElmt, progressContainerElmt, optionContainerElmt) {

  this._controller = controller;
  this._jobID = jobID;
  this._job = job;
  this._format = format;
  this._config = config;

  this._dataContainer = dataContainerElmt;
  this._progressContainer = progressContainerElmt;
  this._optionContainer = optionContainerElmt;

  this._timerID = null;
  this._initialize();
  this.updatePreview();
};
Refine.DefaultImportingController.parserUIs.FixedWidthParserUI = Refine.FixedWidthParserUI;

Refine.FixedWidthParserUI.prototype.dispose = function() {
  if (this._timerID !== null) {
    window.clearTimeout(this._timerID);
    this._timerID = null;
  }
};

Refine.FixedWidthParserUI.prototype.confirmReadyToCreateProject = function() {
  return true; // always ready
};

Refine.FixedWidthParserUI.prototype.getOptions = function() {
  var options = {
    encoding: jQueryTrim(this._optionContainerElmts.encodingInput[0].value),
    columnWidths: this._getColumnWidths()
  };

  var columnNames = jQueryTrim(this._optionContainerElmts.columnNamesInput[0].value).replace(/,\s+/g, ',').split(',');
  if (columnNames.length > 0 && columnNames[0].length > 0) {
    options.columnNames = columnNames;
  }

  var parseIntDefault = function(s, def) {
    try {
      var n = parseInt(s,10);
      if (!isNaN(n)) {
        return n;
      }
    } catch (e) {
      // Ignore
    }
    return def;
  };
  if (this._optionContainerElmts.ignoreCheckbox[0].checked) {
    options.ignoreLines = parseIntDefault(this._optionContainerElmts.ignoreInput[0].value, -1);
  } else {
    options.ignoreLines = -1;
  }
  if (this._optionContainerElmts.headerLinesCheckbox[0].checked) {
    options.headerLines = parseIntDefault(this._optionContainerElmts.headerLinesInput[0].value, 0);
  } else {
    options.headerLines = 0;
  }
  if (this._optionContainerElmts.skipCheckbox[0].checked) {
    options.skipDataLines = parseIntDefault(this._optionContainerElmts.skipInput[0].value, 0);
  } else {
    options.skipDataLines = 0;
  }
  if (this._optionContainerElmts.limitCheckbox[0].checked) {
    options.limit = parseIntDefault(this._optionContainerElmts.limitInput[0].value, -1);
  } else {
    options.limit = -1;
  }

  options.guessCellValueTypes = this._optionContainerElmts.guessCellValueTypesCheckbox[0].checked;

  options.storeBlankRows = this._optionContainerElmts.storeBlankRowsCheckbox[0].checked;
  options.storeBlankCellsAsNulls = this._optionContainerElmts.storeBlankCellsAsNullsCheckbox[0].checked;
  options.includeFileSources = this._optionContainerElmts.includeFileSourcesCheckbox[0].checked;
  options.includeArchiveFile = this._optionContainerElmts.includeFileSourcesCheckbox[0].checked;
  options.includeArchiveFileName = this._optionContainerElmts.includeArchiveFileCheckbox[0].checked;

  options.disableAutoPreview = this._optionContainerElmts.disableAutoPreviewCheckbox[0].checked;

  return options;
};

Refine.FixedWidthParserUI.prototype._initialize = function() {
  var self = this;

  this._optionContainer.off().empty().html(
      DOM.loadHTML("core", "scripts/index/parser-interfaces/fixed-width-parser-ui.html"));
  this._optionContainerElmts = DOM.bind(this._optionContainer);
  this._optionContainerElmts.previewButton.on('click',function() { self.updatePreview(); });

  this._optionContainerElmts.previewButton.html($.i18n('core-buttons/update-preview'));
  $('#or-disable-auto-preview').text($.i18n('core-index-parser/disable-auto-preview'));
  $('#or-import-encoding').html($.i18n('core-index-import/char-encoding'));
  $('#or-import-columnWidth').text($.i18n('core-index-import/column-widths'));
  $('#or-import-columnNames').text($.i18n('core-index-import/column-names'));
  $('#or-import-comma').text($.i18n('core-index-import/comma-separated'));
  $('#or-import-optional').text($.i18n('core-index-import/optional-separated'));
  
  $('#or-import-ignore').text($.i18n('core-index-parser/ignore-first'));
  $('#or-import-lines').text($.i18n('core-index-parser/lines-beg'));
  $('#or-import-parse').text($.i18n('core-index-parser/parse-next'));
  $('#or-import-header').text($.i18n('core-index-parser/lines-header'));
  $('#or-import-discard').text($.i18n('core-index-parser/discard-initial'));
  $('#or-import-rows').text($.i18n('core-index-parser/rows-data'));
  $('#or-import-load').text($.i18n('core-index-parser/load-at-most'));
  $('#or-import-rows2').text($.i18n('core-index-parser/rows-data'));
  $('#or-import-parseCell').html($.i18n('core-index-parser/parse-cell'));
  $('#or-import-blank').text($.i18n('core-index-parser/store-blank'));
  $('#or-import-null').text($.i18n('core-index-parser/store-nulls'));
  $('#or-import-source').html($.i18n('core-index-parser/store-source'));
  $('#or-import-archive').html($.i18n('core-index-parser/store-archive'));

  
  this._optionContainerElmts.encodingInput
    .val(this._config.encoding || '')
    .on('click',function() {
      Encoding.selectEncoding($(this), function() {
        self.updatePreview();
      });
    });

  this._optionContainerElmts.columnWidthsInput[0].value = this._config.columnWidths.join(',');
  if ('columnNames' in this._config) {
    this._optionContainerElmts.columnNamesInput[0].value = this._config.columnNames.join(',');
  }

  if (this._config.ignoreLines > 0) {
    this._optionContainerElmts.ignoreCheckbox.prop('checked', true);
    this._optionContainerElmts.ignoreInput[0].value = this._config.ignoreLines.toString();
  }
  if (this._config.headerLines > 0) {
    this._optionContainerElmts.headerLinesCheckbox.prop('checked', true);
    this._optionContainerElmts.headerLinesInput[0].value = this._config.headerLines.toString();
  }
  if (this._config.limit > 0) {
    this._optionContainerElmts.limitCheckbox.prop('checked', true);
    this._optionContainerElmts.limitInput[0].value = this._config.limit.toString();
  }
  if (this._config.skipDataLines > 0) {
    this._optionContainerElmts.skipCheckbox.prop('checked', true);
    this._optionContainerElmts.skipInput.value[0].value = this._config.skipDataLines.toString();
  }
  if (this._config.storeBlankRows) {
    this._optionContainerElmts.storeBlankRowsCheckbox.prop('checked', true);
  }

  if (this._config.guessCellValueTypes) {
    this._optionContainerElmts.guessCellValueTypesCheckbox.prop('checked', true);
  }

  if (this._config.storeBlankCellsAsNulls) {
    this._optionContainerElmts.storeBlankCellsAsNullsCheckbox.prop('checked', true);
  }
  if (this._config.includeFileSources) {
    this._optionContainerElmts.includeFileSourcesCheckbox.prop('checked', true);
  }
  if (this._config.includeArchiveFileName) {
    this._optionContainerElmts.includeArchiveFileCheckbox.prop('checked', true);
  }

  if (this._config.disableAutoPreview) {
    this._optionContainerElmts.disableAutoPreviewCheckbox.prop('checked', true);
  }

  // If disableAutoPreviewCheckbox is not checked, we will schedule an automatic update
  var onChange = function() {
    if (!self._optionContainerElmts.disableAutoPreviewCheckbox[0].checked)
    {
        self._scheduleUpdatePreview();
    }
  };
  this._optionContainer.find("input").on("change", onChange);
  this._optionContainer.find("select").on("change", onChange);
};

Refine.FixedWidthParserUI.prototype._getColumnWidths = function() {
  var newColumnWidths = [];
  var a = jQueryTrim(this._optionContainerElmts.columnWidthsInput[0].value).replace(/,\s+/g, ',').split(',');
  for (var i = 0; i < a.length; i++) {
    var n = parseInt(a[i],10);
    if (!isNaN(n)) {
      newColumnWidths.push(n);
    }
  }
  return newColumnWidths;
};

Refine.FixedWidthParserUI.prototype._scheduleUpdatePreview = function() {
  if (this._timerID !== null) {
    window.clearTimeout(this._timerID);
    this._timerID = null;
  }

  var self = this;
  this._timerID = window.setTimeout(function() {
    self._timerID = null;
    self.updatePreview();
  }, 500); // 0.5 second
};

Refine.FixedWidthParserUI.prototype.updatePreview = function() {
  var self = this;

  this._progressContainer.show();

  var options = this.getOptions();
  // for preview, we need exact text, so it's easier to show where the columns are split
  options.guessCellValueTypes = false;

  this._controller.updateFormatAndOptions(options, function(result) {
    if (result.status == "ok") {
      self._controller.getPreviewData(function(projectData) {
        new Refine.FixedWidthPreviewTable(
            self,
            options,
            projectData,
            self._dataContainer
        );
        self._progressContainer.hide();
      }, 20);
    }
  });
};

Refine.FixedWidthPreviewTable = function(parserUI, config, projectData, elmt) {
  this._parserUI = parserUI;
  this._config = config;
  this._projectData = projectData;
  this._elmt = elmt;
  this._render();
};

Refine.FixedWidthPreviewTable.prototype._render = function() {
  var scrollTop = this._elmt[0].scrollTop;
  var scrollLeft = this._elmt[0].scrollLeft;

  this._elmt.off().empty();

  var self = this;
  var container = $('<div>')
  .addClass('fixed-width-preview-container')
  .appendTo(this._elmt);
  var table = $('<table>')
  .addClass("data-table")
  .addClass("fixed-width-preview-data-table")
  .appendTo(container)[0];

  var columns = this._projectData.columnModel.columns;
  var columnWidths = [].concat(this._config.columnWidths);
  var includeFileSources = this._config.includeFileSources;

  var addCell = function(tr) {
    var index = tr.cells.length;
    var td = tr.insertCell(index);
    td.className = (index % 2 === 0) ? 'even' : 'odd';
    return td;
  };

  /*------------------------------------------------------------
   *  Column Headers
   *------------------------------------------------------------
   */

  var trHead = table.insertRow(table.rows.length);
  $(addCell(trHead)).addClass("column-header").html('&nbsp;'); // index

  var createColumnHeader = function(column, index) {
    var name = column.name;
    var index2 = index - (includeFileSources ? 1 : 0);
    if (index2 >= 0 && index2 < columnWidths.length) {
      name = name.slice(0, columnWidths[index2]);
    }
    $(addCell(trHead))
    .addClass("column-header")
    .text(name)
    .attr('title', column.name);
  };
  for (var i = 0; i < columns.length; i++) {
    createColumnHeader(columns[i], i);
  }

  /*------------------------------------------------------------
   *  Data Rows of Cells
   *------------------------------------------------------------
   */

  var rows = this._projectData.rowModel.rows;
  var renderRow = function(tr, r, row) {
    var tdIndex = addCell(tr);
    $('<div></div>').html((row.i + 1) + ".").appendTo(tdIndex);

    var cells = row.cells;
    for (var i = 0; i < columns.length; i++) {
      var column = columns[i];
      var td = addCell(tr);
      var divContent = $('<div/>').addClass("data-table-cell-content").appendTo(td);

      var cell = (column.cellIndex < cells.length) ? cells[column.cellIndex] : null;
      if (!cell || ("v" in cell && cell.v === null)) {
        $('<span>').html("&nbsp;").appendTo(divContent);
      } else if ("e" in cell) {
        $('<span>').addClass("data-table-error").text(cell.e).appendTo(divContent);
      } else if (!("r" in cell) || !cell.r) {
        if (typeof cell.v !== "string") {
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
          $('<span>').text(cell.v.replace(/\s/g, ' ')).appendTo(divContent);
        }
      }
    }
  };

  for (var r = 0; r < rows.length; r++) {
    var row = rows[r];
    renderRow(table.insertRow(table.rows.length), r, row);
  }

  /*
   * Measure font metrics.
   */
  var pixelOffset = $(trHead.cells[includeFileSources ? 2 : 1]).position().left;
  var testString = '01234567890123456789012345678901234567890123456789';
  var testDiv = $('<div>')
  .css('position', 'absolute')
  .css('top', '-100px')
  .text(testString)
  .appendTo(container);
  var pixelsPerChar = testDiv.width() / testString.length;
  testDiv.remove();

  /*
   * Render column separators
   */
  var columnSeparators = [];
  var columnCharIndexes = [];
  var positionColumnSeparator = function(outer, charIndex) {
    outer.css('left',
        Math.round(pixelOffset + charIndex * pixelsPerChar - DOM.getHPaddings(outer) / 2) + 'px');
  };
  var computeCharIndex = function(evt) {
    var offset = evt.pageX - container.offset().left;
    return Math.round((offset - pixelOffset) / pixelsPerChar);
  };
  var updatePreview = function() {
    columnCharIndexes.sort(function(a, b) { return a - b; });

    var newColumnWidths = [];
    for (var i = 0; i < columnCharIndexes.length; i++) {
      var charIndex = columnCharIndexes[i];
      var columnWidth = (i === 0) ? charIndex : (charIndex - columnCharIndexes[i - 1]);
      if (columnWidth > 0) {
        newColumnWidths.push(columnWidth);
      }
    }

    self._parserUI._optionContainerElmts.columnWidthsInput[0].value = newColumnWidths.join(',');
    self._parserUI.updatePreview();
  };

  var newSeparator = $('<div>')
  .addClass('fixed-width-preview-column-separator-outer')
  .append($('<div>').addClass('fixed-width-preview-column-separator-inner'))
  .appendTo(container);

  var createColumnSeparator = function(charIndex, index) {
    columnCharIndexes[index] = charIndex;

    var outer = $('<div>')
    .addClass('fixed-width-preview-column-separator-outer')
    .appendTo(container);
    var inner = $('<div>')
    .addClass('fixed-width-preview-column-separator-inner')
    .appendTo(outer);
    var close = $('<div>').appendTo(inner);

    positionColumnSeparator(outer, charIndex);

    outer.on('mouseover',function() {
      newSeparator.hide();
    })
    .on('mouseout',function() {
      newSeparator.show();
    })
    .on('mousedown',function() {
      var mouseMove = function(evt) {
        var newCharIndex = computeCharIndex(evt);
        positionColumnSeparator(outer, newCharIndex);

        evt.preventDefault();
        evt.stopPropagation();
        return false;
      };
      var mouseUp = function(evt) {
        container.off('mousemove', mouseMove);
        container.off('mouseup', mouseUp);

        var newCharIndex = computeCharIndex(evt);
        positionColumnSeparator(outer, newCharIndex);

        columnCharIndexes[index] = newCharIndex;
        updatePreview();

        evt.preventDefault();
        evt.stopPropagation();
        return false;
      };
      container.on('mousemove', mouseMove);
      container.on('mouseup', mouseUp);
    });

    close.on('click',function() {
      columnCharIndexes[index] = index > 0 ? columnCharIndexes[index - 1] : 0;
      updatePreview();
    });
  };

  var charOffset = 0;
  for (var i = 0; i < columnWidths.length; i++) {
    var columnWidth = columnWidths[i];
    createColumnSeparator(charOffset + columnWidth, i);
    charOffset += columnWidth;
  }

  container
  .on('mouseout',function(evt) {
    newSeparator.hide();
  })
  .on('mousemove',function(evt) {
    var offset = evt.pageX - container.offset().left;
    var newCharIndex = Math.round((offset - pixelOffset) / pixelsPerChar);
    positionColumnSeparator(newSeparator.show(), newCharIndex);
  });
  newSeparator.on('mousedown',function(evt) {
    var newCharIndex = computeCharIndex(evt);
    columnCharIndexes.push(newCharIndex);
    updatePreview();

    evt.preventDefault();
    evt.stopPropagation();
    return false;
  });

  this._elmt[0].scrollTop = scrollTop;
  this._elmt[0].scrollLeft = scrollLeft;
};
