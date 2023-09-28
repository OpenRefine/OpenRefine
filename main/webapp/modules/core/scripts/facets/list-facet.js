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

class ListFacet extends Facet {
  constructor(div, config, options, selection) {
    super(div, config, options);

    if (!("sort" in this._options)) {
      this._options.sort = "name";
    }

    this._selection = selection || config.selection || [];

    if (!("invert" in this._config)) {
      this._config.invert = false;
    }

    this._blankChoice = (config.selectBlank) ? { s : true, c : 0 } : null;
    this._errorChoice = (config.selectError) ? { s : true, c : 0 } : null;

    this._data = null;

    this._initialHeightSet = false;
    this._initializeUI();
    this._update();
  };

  reset() {
    this._selection = [];
    this._blankChoice = null;
    this._errorChoice = null;
  };

  getUIState() {
    var json = {
        c: this.getJSON(),
        o: this._options
    };

    json.s = json.c.selection;
    delete json.c.selection;

    return json;
  };

  getJSON() {
    var o = {
        type: "list",
        name: this._config.name,
        columnName: this._config.columnName,
        expression: this._config.expression,
        omitBlank: "omitBlank" in this._config ? this._config.omitBlank : false,
            omitError: "omitError" in this._config ? this._config.omitError : false,
                selection: [],
                selectBlank: this._blankChoice !== null && this._blankChoice.s,
                selectError: this._errorChoice !== null && this._errorChoice.s,
                invert: this._config.invert
    };
    for (var i = 0; i < this._selection.length; i++) {
      var choice = {
          v: cloneDeep(this._selection[i].v)
      };
      o.selection.push(choice);
    }
    return o;
  };

  hasSelection() {
    return this._selection.length > 0 || 
    (this._blankChoice !== null && this._blankChoice.s) || 
    (this._errorChoice !== null && this._errorChoice.s);
  };

  updateState(data) {
    this._data = data;

    if ("choices" in data) {
      var selection = [];
      var choices = data.choices;
      for (var i = 0; i < choices.length; i++) {
        var choice = choices[i];
        if (choice.s) {
          selection.push(choice);
        }
      }
      this._selection = selection;
      this._reSortChoices();

      this._blankChoice = data.blankChoice || null;
      this._errorChoice = data.errorChoice || null;
    }

    this._update();
  };

  checkInitialHeight() {
    if (this._elmts) {
      let innerList = this._elmts.bodyInnerDiv[0];
      if (!this._initialHeightSet && innerList.offsetHeight !== 0) {
        let innerHeight = innerList.offsetHeight;
        let defaultMaxHeight = 17 * 13;

        if (innerHeight > defaultMaxHeight) {
          this._elmts.bodyDiv.height(defaultMaxHeight + 'px');
        } else {
          this._elmts.bodyDiv.height((innerHeight + 1) + 'px');
        }
        this._initialHeightSet = true;
      }
    }
  }

  _reSortChoices() {
    this._data.choices.sort(this._options.sort === "name" ?
        function(a, b) {
      return a.v.l.toLowerCase().localeCompare(b.v.l.toLowerCase());
    } :
      function(a, b) {
      var c = b.c - a.c;
      return c !== 0 ? c : a.v.l.localeCompare(b.v.l);
    }
    );
  };

  _initializeUI() {
    var self = this;

    var facet_id = this._div.attr("id");

    this._div.empty().show().html(
      '<div class="facet-title" bind="facetTitle">' +
        '<div class="grid-layout layout-tightest layout-full"><table><tr>' +
          '<td width="1%">' +
            '<a href="javascript:{}" title="'+$.i18n('core-facets/remove-facet')+'" class="facet-title-remove" bind="removeButton">&nbsp;</a>' +
          '</td>' +
          '<td width="1%">' +
            '<a href="javascript:{}" title="'+$.i18n('core-facets/minimize-facet')+'" class="facet-title-minimize" bind="minimizeButton">&nbsp;</a>' +
          '</td>' +
          '<td>' +
            '<a href="javascript:{}" class="facet-choice-link" bind="resetButton">'+$.i18n('core-facets/reset')+'</a>' +
            '<a href="javascript:{}" class="facet-choice-link" bind="invertButton">'+$.i18n('core-facets/invert')+'</a>' +
            '<a href="javascript:{}" class="facet-choice-link" bind="changeButton">'+$.i18n('core-facets/change')+'</a>' +
            '<span bind="titleSpan"></span>' +
          '</td>' +
        '</tr></table></div>' +
      '</div>' +
      '<div class="facet-expression" bind="expressionDiv" title="'+$.i18n('core-facets/click-to-edit')+'"></div>' +
      '<div class="facet-controls" bind="controlsDiv" style="display:none;">' +
        '<a bind="choiceCountContainer" class="action" href="javascript:{}"></a> ' +
        '<span class="facet-controls-sortControls" bind="sortGroup">'+$.i18n('core-facets/sort-by')+': ' +
          '<a href="javascript:{}" bind="sortByNameLink">'+$.i18n('core-facets/name')+'</a> ' +
          '<a href="javascript:{}" bind="sortByCountLink">'+$.i18n('core-facets/count')+'</a>' +
        '</span>' +
        '<button bind="clusterLink" class="facet-controls-button button">'+$.i18n('core-facets/cluster')+'</button>' +
      '</div>' +
      '<div class="facet-body" bind="bodyDiv">' +
        '<div class="facet-body-inner" bind="bodyInnerDiv"></div>' +
      '</div>'
    );
    this._elmts = DOM.bind(this._div);

    this._elmts.titleSpan.text(this._config.name);
    this._elmts.changeButton.attr("title",$.i18n('core-facets/current-exp')+": " + this._config.expression).on('click',function() {
      self._elmts.expressionDiv.slideToggle(100, function() {
        if (self._elmts.expressionDiv.css("display") != "none") {
          self._editExpression();
        }
      });
    });
    
    this._elmts.expressionDiv.text(this._config.expression).hide().on('click',function() { self._editExpression(); });
    this._elmts.removeButton.on('click',function() { self._remove(); });
    this._elmts.minimizeButton.on('click',function() { self._minimize(); });
    this._elmts.resetButton.on('click',function() { self._reset(); });
    this._elmts.invertButton.on('click',function() { self._invert(); });

    this._elmts.choiceCountContainer.on('click',function() { self._copyChoices(); });
    this._elmts.sortByCountLink.on('click',function() {
      if (self._options.sort != "count") {
        self._options.sort = "count";
        self._reSortChoices();
        self._update(true);
      }
    });
    this._elmts.sortByNameLink.on('click',function() {
      if (self._options.sort != "name") {
        self._options.sort = "name";
        self._reSortChoices();
        self._update(true);
      }
    });

    this._elmts.clusterLink.on('click',function() { self._doEdit(); });
    if (this._config.expression != "value" && this._config.expression != "grel:value") {
      this._elmts.clusterLink.hide();
    }

    if (!("scroll" in this._options) || this._options.scroll) {
      this._elmts.bodyDiv.addClass("facet-body-scrollable");
      this._elmts.bodyDiv.resizable({
        minHeight: 30,
        handles: 's',
        stop: function(event, ui) {
          event.target.style.width = "auto"; // don't force the width
        }
      });
    }
  };

  _copyChoices() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.css({"min-width" : "600px"});

    var header = $('<div></div>').addClass("dialog-header").text($.i18n('core-facets/facet-choices')).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

    body.html('<textarea disabled wrap="off" bind="textarea" style="display: block; width: 100%; height: 400px;"></textarea>');
    var elmts = DOM.bind(body);

    $('<button class="button"></button>').text($.i18n('core-buttons/close')).on('click',function() {
      DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);

    var lines = [];
    for (var i = 0; i < this._data.choices.length; i++) {
      var choice = this._data.choices[i];
      lines.push(choice.v.l + "\t" + choice.c);
    }
    if (this._blankChoice) {
      lines.push("(blank)\t" + this._blankChoice.c);
    }
    if (this._errorChoice) {
      lines.push("(error)\t" + this._errorChoice.c);
    }

    var level = DialogSystem.showDialog(frame);

    var textarea = elmts.textarea[0];
    textarea.value = lines.join("\n");
    textarea.focus();
    textarea.select();
  };

  _update(resetScroll) {
    var self = this;

    var invert = this._config.invert;
    if (invert) {
      this._elmts.facetTitle.addClass("facet-title-inverted");
      this._elmts.bodyInnerDiv.addClass("facet-mode-inverted");
      this._elmts.invertButton.addClass("facet-mode-inverted");
    } else {
      this._elmts.facetTitle.removeClass("facet-title-inverted");
      this._elmts.bodyInnerDiv.removeClass("facet-mode-inverted");
      this._elmts.invertButton.removeClass("facet-mode-inverted");
    }

    if (!this._data) {
      //this._elmts.statusDiv.hide();
      this._elmts.controlsDiv.hide();
      this._elmts.bodyInnerDiv.empty().append(
          $('<div>').text($.i18n('core-facets/loading')).addClass("facet-body-message"));

      return;
    } else if ("error" in this._data) {
      //this._elmts.statusDiv.hide();
      this._elmts.controlsDiv.hide();

      if (this._data.error === "Too many choices") {
        this._elmts.bodyInnerDiv.empty();
        
        var messageDiv = $('<div>')
          .text($.i18n('core-facets/too-many-choices', this._data.choiceCount))
          .addClass("facet-body-message")
          .appendTo(this._elmts.bodyInnerDiv);
        $('<br>').appendTo(messageDiv);
        $('<a>')
        .text($.i18n('core-facets/set-choice-count'))
        .attr("href", "javascript:{}")
        .addClass("action")
        .addClass("secondary")
        .appendTo(messageDiv)
        .on('click',function() {
          self._setChoiceCountLimit(self._data.choiceCount);
        });
        
        this._renderBodyControls();
      } else {
        this._elmts.bodyInnerDiv.empty().append(
            $('<div>')
              .text(this._data.error)
              .addClass("facet-body-message"));
      }
      return;
    }

    var scrollTop = 0;
    if (!resetScroll) {
      try {
        scrollTop = this._elmts.bodyInnerDiv[0].scrollTop;
      } catch (e) {
      }
    }

    // FIXME: this is very slow for large numbers of choices (e.g. 18 seconds for 13K choices)
    this._elmts.bodyInnerDiv.empty();
    
    // None of the following alternatives are significantly faster
    
  //  this._elmts.bodyInnerDiv.innerHtml = '';
    
  //  this._elmts.bodyInnerDiv.detach();
  //  this._elmts.bodyInnerDiv.children().remove();
  //  this._elmts.bodyInnerDiv.appendTo('.facet-body');
    
  //  this._elmts.bodyInnerDiv.remove();
  //  this._elmts.bodyInnerDiv.html('<div class="facet-body-inner" bind="bodyInnerDiv"></div>');
  //  this._elmts.bodyInnerDiv.appendTo('.facet-body');

    //this._elmts.statusDiv.show();
    this._elmts.controlsDiv.show();

    var choices = this._data.choices;
    var selectionCount = this._selection.length +
    (this._blankChoice !== null && this._blankChoice.s ? 1 : 0) +
    (this._errorChoice !== null && this._errorChoice.s ? 1 : 0);

    this._elmts.choiceCountContainer.text($.i18n("core-facets/choice-count", choices.length));
    if (selectionCount > 0) {
      this._elmts.resetButton.show();
      this._elmts.invertButton.show();
    } else {
      this._elmts.resetButton.hide();
      this._elmts.invertButton.hide();
    }

    if (this._options.sort === "name") {
      this._elmts.sortByNameLink.removeClass("action").addClass("selected");
      this._elmts.sortByCountLink.removeClass("selected").addClass("action");
    } else {
      this._elmts.sortByNameLink.removeClass("selected").addClass("action");
      this._elmts.sortByCountLink.removeClass("action").addClass("selected");
    }

    var html = [];
    var temp = $('<div>');
    var encodeHtml = function(s) {
      return temp.text(s).html();
    };

    var renderEdit = this._config.expression === "value";
    var renderChoice = function(index, choice, customLabel) {
      var label = customLabel || choice.v.l;
      var count = choice.c;

      html.push('<div class="facet-choice' + (choice.s ? ' facet-choice-selected' : '') + '" choiceIndex="' + index + '">');

      // include/exclude link
      html.push(
        '<a href="javascript:{}" class="facet-choice-link facet-choice-toggle" ' +
          'style="visibility: ' + (choice.s ? 'visible' : 'hidden') + '">' + 
          (invert != choice.s ? 'exclude' : 'include') + 
        '</a>'
      );

      // edit link
      if (renderEdit) {
        html.push('<a href="javascript:{}" class="facet-choice-link facet-choice-edit" style="visibility: hidden">'+$.i18n('core-facets/edit')+'</a>');
      }

      html.push('<a href="javascript:{}" class="facet-choice-label">' + encodeHtml(label) + '</a>');
      html.push('<span class="facet-choice-count">' + (invert ? "-" : "") + count + '</span>');

      html.push('</div>');
    };
    for (var i = 0; i < choices.length; i++) {
      renderChoice(i, choices[i]);
    }
    if (this._blankChoice !== null) {
      renderChoice(-1, this._blankChoice, "(blank)");
    }
    if (this._errorChoice !== null) {
      renderChoice(-2, this._errorChoice, "(error)");
    }

    this._elmts.bodyInnerDiv.html(html.join(''));
    this._renderBodyControls();
    this._elmts.bodyInnerDiv[0].scrollTop = scrollTop;

    this.checkInitialHeight();

    var getChoice = function(elmt) {
      var index = parseInt(elmt.attr("choiceIndex"),10);
      if (index === -1) {
        return self._blankChoice;
      } else if (index === -2) {
        return self._errorChoice;
      } else {
        return choices[index];
      }
    };
    var findChoice = function(elmt) {
      return getChoice(elmt.closest('.facet-choice'));
    };
    var select = function(choice) {
      self._select(choice, false);
    };
    var selectOnly = function(choice) {
      self._select(choice, true);
    };
    var deselect = function(choice) {
      self._deselect(choice);
    };

    var wireEvents = function() {
      var bodyInnerDiv = self._elmts.bodyInnerDiv;
      bodyInnerDiv.off(); // remove all old handlers
      bodyInnerDiv.on('click', '.facet-choice-label', function(e) {
        e.preventDefault();
        var choice = findChoice($(this));
        if (choice.s) {
          if (selectionCount > 1) {
            selectOnly(choice);
          } else {
            deselect(choice);
          }
        } else if (selectionCount > 0) {
          selectOnly(choice);
        } else {
          select(choice);
        }
      });
      bodyInnerDiv.on('click', '.facet-choice-edit', function(e) {
        e.preventDefault();
        var choice = findChoice($(this));
        self._editChoice(choice, $(this).closest('.facet-choice'));
      });

      bodyInnerDiv.on('mouseenter mouseleave', '.facet-choice', function(e) {
        e.preventDefault();
        var visibility = 'visible';
        if (e.type === 'mouseleave') {
          visibility = 'hidden';
        }
        $(this).find('.facet-choice-edit').css("visibility", visibility);

        var choice = getChoice($(this));
        if (!choice.s) {
          $(this).find('.facet-choice-toggle').css("visibility", visibility);
        }
      });

      bodyInnerDiv.on('click', '.facet-choice-toggle', function(e) {
        e.preventDefault();
        var choice = findChoice($(this));
        if (choice.s) {
          deselect(choice);
        } else {
          select(choice);
        }
      });
    };
    window.setTimeout(wireEvents, 100);
  }; // end _update()

  _renderBodyControls() {
    var self = this;
    var bodyControls = $('<div>')
    .addClass("facet-body-controls")
    .appendTo(this._elmts.bodyInnerDiv);

    $('<a>')
    .text($.i18n('core-facets/facet-by-count'))
    .attr("href", "javascript:{}")
    .addClass("action")
    .addClass("secondary")
    .appendTo(bodyControls)
    .on('click',function() {
      ui.browsingEngine.addFacet(
        "range", 
        {
          "name" : self._config.columnName,
          "columnName" : self._config.columnName, 
          "expression" : self._getMetaExpression(),
          "mode" : "range"
        },
        {
        }
      );
    });
  };

  _getMetaExpression() {
    var r = Scripting.parse(this._config.expression);

    return r.language + ':facetCount(' + [
          r.expression,
          JSON.stringify(this._config.expression),
          JSON.stringify(this._config.columnName)
        ].join(', ') + ')';
  };

  _doEdit() {
    new ClusteringDialog(this._config.columnName, this._config.expression);
  };

  _editChoice(choice, choiceDiv) {
    var self = this;

    var menu = MenuSystem.createMenu().addClass("data-table-cell-editor").width("400px");
    menu.html(
        '<textarea class="data-table-cell-editor-editor" bind="textarea"></textarea>' +
        '<div id="data-table-cell-editor-actions">' +
          '<div class="data-table-cell-editor-action">' +
            '<button class="button" bind="okButton">'+$.i18n('core-buttons/apply')+'</button>' +
            '<div class="data-table-cell-editor-key">'+$.i18n('core-buttons/enter')+'</div>' +
          '</div>' +
          '<div class="data-table-cell-editor-action">' +
            '<button class="button" bind="cancelButton">'+$.i18n('core-buttons/cancel')+'</button>' +
            '<div class="data-table-cell-editor-key">'+$.i18n('core-buttons/esc')+'</div>' +
          '</div>' +
        '</div>'
    );
    var elmts = DOM.bind(menu);

    MenuSystem.showMenu(menu, function(){});
    MenuSystem.positionMenuLeftRight(menu, choiceDiv);

    var originalContent;
    if (choice === this._blankChoice) {
      originalContent = "(blank)";
    } else if (choice === this._errorChoice) {
      originalContent = "(error)";
    } else {
      originalContent = choice.v.v;
    }

    var commit = function() {
      var text = elmts.textarea[0].value;

      MenuSystem.dismissAll();

      var edit = { to : text };
      if (choice === self._blankChoice) {
        edit.fromBlank = true;
      } else if (choice === self._errorChoice) {
        edit.fromError = true;
      } else {
        edit.from = [ originalContent ];
      }

      Refine.postCoreProcess(
        "mass-edit",
        {},
        {
          columnName: self._config.columnName,
          expression: "value",
          edits: JSON.stringify([ edit ])
        },
        {
          // limit edits to rows constrained only by the other facets
          engineConfig: ui.browsingEngine.getJSON(false, self),
          cellsChanged: true
        },
        {
          onDone: function(o) {
            var selection = [];
            var gotSelection = false;
            for (var i = 0; i < self._selection.length; i++) {
              var choice = self._selection[i];
              if (choice.v.v === originalContent) {
                if (gotSelection) {
                  continue;
                }
                choice.v.v = text;
                gotSelection = true; // eliminate duplicated selections due to changing one selected choice to another
              }
              selection.push(choice);
            }
            self._selection = selection;
          }
        }
      );            
    };

    elmts.okButton.on('click',commit);
    elmts.textarea
    .text(originalContent)
    .on('keydown',function(evt) {
      if (!evt.shiftKey) {
        if (evt.key === "Enter") {
          commit();
        } else if (evt.key === "Escape") {
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

  _select(choice, only) {
    if (only) {
      this._selection = [];
      if (this._blankChoice !== null) {
        this._blankChoice.s = false;
      }
      if (this._errorChoice !== null) {
        this._errorChoice.s = false;
      }
    }

    choice.s = true;
    if (choice !== this._errorChoice && choice !== this._blankChoice) {
      this._selection.push(choice);
    }

    this._updateRest();
  };

  _deselect(choice) {
    if (choice === this._errorChoice || choice === this._blankChoice) {
      choice.s = false;
    } else {
      for (var i = this._selection.length - 1; i >= 0; i--) {
        if (this._selection[i] === choice) {
          this._selection.splice(i, 1);
          break;
        }
      }
    }
    this._updateRest();
  };

  _reset() {
    this._selection = [];
    this._blankChoice = null;
    this._errorChoice = null;
    this._config.invert = false;

    this._updateRest();
  };

  _invert() {
    this._config.invert = !this._config.invert;

    this._updateRest();
  };

  _updateRest() {
    Refine.update({ engineChanged: true });
  };

  _editExpression() {
    var self = this;
    var title = (this._config.columnName) ? 
        ($.i18n('core-facets/edit-based-col')+" " + this._config.columnName) : 
          $.i18n('core-facets/edit-facet-exp');

    var column = Refine.columnNameToColumn(this._config.columnName);
    var o = DataTableView.sampleVisibleRows(column);

    new ExpressionPreviewDialog(
      title,
      column ? column.cellIndex : -1, 
      o.rowIndices,
      o.values,
      this._config.expression, 
      function(expr) {
        if (expr != self._config.expression) {
          self._config.expression = expr;

          self._elmts.expressionDiv.text(self._config.expression);
          self._elmts.changeButton.attr("title", $.i18n('core-facets/current-exp')+": " + self._config.expression);
          if (self._config.expression === "value" || self._config.expression === "grel:value") {
            self._elmts.clusterLink.show();
          } else {
            self._elmts.clusterLink.hide();
          }

          self.reset();
          self._updateRest();
        }
        self._elmts.expressionDiv.hide();
      }
    );
  };

  _setChoiceCountLimit(choiceCount) {
    var limit = Math.ceil(choiceCount / 1000) * 1000;
    var s = window.prompt($.i18n('core-facets/set-max-choices'), limit);
    if (s) {
      var n = parseInt(s,10);

      if (!isNaN(n)) {
        var self = this;
        Refine.postCSRF(
          "command/core/set-preference",
          {
            name : "ui.browsing.listFacet.limit",
            value : JSON.stringify(n)
          },
          function(o) {
            if (o.code === "ok") {
              ui.browsingEngine.update();
            } else if (o.code === "error") {
              alert(o.message);
            }
          },
          "json"
        );      
      }
    }
  };
};

ListFacet.reconstruct = function(div, uiState) {
  return new ListFacet(div, uiState.c, uiState.o, uiState.s);
};

