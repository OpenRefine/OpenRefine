function ListFacet(div, config, options, selection) {
    this._div = div;
    this._config = config;
    if (!("invert" in this._config)) {
        this._config.invert = false;
    }
    
    this._options = options || {};
    if (!("sort" in this._options)) {
        this._options.sort = "name";
    }
    
    this._selection = selection || [];
    this._blankChoice = (config.selectBlank) ? { s : true, c : 0 } : null;
    this._errorChoice = (config.selectError) ? { s : true, c : 0 } : null;
    
    this._data = null;
    
    this._initializeUI();
    this._update();
}

ListFacet.reconstruct = function(div, uiState) {
    return new ListFacet(div, uiState.c, uiState.o, uiState.s);
};

ListFacet.prototype.dispose = function() {
};

ListFacet.prototype.reset = function() {
    this._selection = [];
    this._blankChoice = null;
    this._errorChoice = null;
};

ListFacet.prototype.getUIState = function() {
    var json = {
        c: this.getJSON(),
        o: this._options
    };
    
    json.s = json.c.selection;
    delete json.c.selection;
    
    return json;
};

ListFacet.prototype.getJSON = function() {
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

ListFacet.prototype.hasSelection = function() {
    return this._selection.length > 0 || 
        (this._blankChoice !== null && this._blankChoice.s) || 
        (this._errorChoice !== null && this._errorChoice.s);
};

ListFacet.prototype.updateState = function(data) {
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

ListFacet.prototype._reSortChoices = function() {
    this._data.choices.sort(this._options.sort == "name" ?
        function(a, b) {
            return a.v.l.localeCompare(b.v.l);
        } :
        function(a, b) {
            return b.c - a.c;
        }
    );
};

ListFacet.prototype._initializeUI = function() {
    var self = this;
    
    var facet_id = this._div.attr("id");
    
    this._div.empty().show().html(
        '<div class="facet-title">' +
            '<div class="grid-layout layout-tightest layout-full"><table><tr>' +
                '<td width="1%"><a href="javascript:{}" title="Remove this facet" class="facet-title-remove" bind="removeButton">&nbsp;</a></td>' +
                '<td>' +
                    '<a href="javascript:{}" class="facet-choice-link" bind="resetButton">reset</a>' +
                    '<a href="javascript:{}" class="facet-choice-link" bind="invertButton">invert</a>' +
                    '<a href="javascript:{}" class="facet-choice-link" bind="changeButton">change</a>' +
                    '<span bind="titleSpan"></span>' +
                '</td>' +
            '</tr></table></div>' +
        '</div>' +
        '<div class="facet-expression" bind="expressionDiv"></div>' +
        '<div class="facet-controls" bind="controlsDiv" style="display:none;"><div class="grid-layout layout-tightest layout-full">' +
            '<table><tr>' +
                '<td><span bind="choiceCountContainer"></span> <span bind="sortGroup">sorted by ' +
                    '<input bind="sortByNameLink" type="radio" id="' + facet_id + '-name-sort" name="radio" checked="checked" /><label for="' + facet_id + '-name-sort">name</label>' +
                    '<input bind="sortByCountLink" type="radio" id="' + facet_id + '-count-sort" name="radio" /><label for="' + facet_id + '-count-sort">count</label>' +
                '</span></td>' +
                '<td width="1%" nowrap=""><button bind="clusterLink">cluster</button></td>' +
            '</tr></table>' +
        '</div></div>' +
        '<div class="facet-body" bind="bodyDiv">' +
            '<div class="facet-body-inner" bind="bodyInnerDiv"></div>' +
        '</div>'
    );
    this._elmts = DOM.bind(this._div);
    
    this._elmts.titleSpan.text(this._config.name);
    this._elmts.changeButton.attr("title","Current Expression: " + this._config.expression).click(function() {
        self._elmts.expressionDiv.slideToggle(100);
    });
    this._elmts.expressionDiv.text(this._config.expression).hide().click(function() { self._editExpression(); });
    this._elmts.removeButton.click(function() { self._remove(); });
    this._elmts.resetButton.click(function() { self._reset(); });
    this._elmts.invertButton.click(function() { self._invert(); });

    this._elmts.sortByCountLink.click(function() {
        if (self._options.sort != "count") {
            self._options.sort = "count";
            self._reSortChoices();
            self._update(true);
        }
    });
    this._elmts.sortByNameLink.click(function() {
        if (self._options.sort != "name") {
            self._options.sort = "name";
            self._reSortChoices();
            self._update(true);
        }
    });
        
    this._elmts.sortGroup.buttonset();
    
    this._elmts.clusterLink.click(function() { self._doEdit(); }).button();
    if (this._config.expression != "value" && this._config.expression != "gel:value") {
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

ListFacet.prototype._update = function(resetScroll) {
    var self = this;
    
    var invert = this._config.invert;
    if (invert) {
        this._elmts.bodyInnerDiv.addClass("facet-mode-inverted");
        this._elmts.invertButton.addClass("facet-mode-inverted");
    } else {
        this._elmts.bodyInnerDiv.removeClass("facet-mode-inverted");
        this._elmts.invertButton.removeClass("facet-mode-inverted");
    }
    
    if (!this._data) {
        //this._elmts.statusDiv.hide();
        this._elmts.controlsDiv.hide();
        this._elmts.bodyInnerDiv.empty().append(
            $('<div>').text("Loading...").addClass("facet-body-message"));
            
        return;
    } else if ("error" in this._data) {
        //this._elmts.statusDiv.hide();
        this._elmts.controlsDiv.hide();
        this._elmts.bodyInnerDiv.empty().append(
            $('<div>').text(this._data.error).addClass("facet-body-message"));
        
        return;
    }
    
    var scrollTop = 0;
    if (!resetScroll) {
        try {
            scrollTop = this._elmts.bodyInnerDiv[0].scrollTop;
        } catch (e) {
        }
    }
    
    this._elmts.bodyInnerDiv.empty();
    //this._elmts.statusDiv.show();
    this._elmts.controlsDiv.show();
    
    var choices = this._data.choices;
    var selectionCount = this._selection.length +
          (this._blankChoice !== null && this._blankChoice.s ? 1 : 0) +
          (this._errorChoice !== null && this._errorChoice.s ? 1 : 0);
          
    this._elmts.choiceCountContainer.text(choices.length + " choices");
    if (selectionCount > 0) {
        this._elmts.resetButton.show();
        this._elmts.invertButton.show();
    } else {
        this._elmts.resetButton.hide();
        this._elmts.invertButton.hide();
    }
    
    if (this._options.sort == "name") {
        this._elmts.sortByNameLink.addClass("facet-mode-link-selected");
        this._elmts.sortByCountLink.removeClass("facet-mode-link-selected");
    } else {
        this._elmts.sortByNameLink.removeClass("facet-mode-link-selected");
        this._elmts.sortByCountLink.addClass("facet-mode-link-selected");
    }
    
    var html = [];
    var temp = $('<div>');
    var encodeHtml = function(s) {
        return temp.text(s).html();
    };
    
    var renderEdit = this._config.expression == "value";
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
                html.push('<a href="javascript:{}" class="facet-choice-link facet-choice-edit" style="visibility: hidden">edit</a>');
            }
            
            html.push('<a href="javascript:{}" class="facet-choice-label">' + encodeHtml(label) + '</a>');
            html.push('<span class="facet-choice-count">' + count + '</span>');
            
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
    this._elmts.bodyInnerDiv[0].scrollTop = scrollTop;
    
    var getChoice = function(elmt) {
        var index = parseInt(elmt.attr("choiceIndex"),10);
        if (index == -1) {
            return self._blankChoice;
        } else if (index == -2) {
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
        bodyInnerDiv.find('.facet-choice-label').click(function() {
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
        bodyInnerDiv.find('.facet-choice-edit').click(function() {
            var choice = findChoice($(this));
            self._editChoice(choice, $(this).closest('.facet-choice'));
        });
        
        bodyInnerDiv.find('.facet-choice').mouseenter(function() {
            $(this).find('.facet-choice-edit').css("visibility", "visible");
            
            var choice = getChoice($(this));
            if (!choice.s) {
                $(this).find('.facet-choice-toggle').css("visibility", "visible");
            }
        }).mouseleave(function() {
            $(this).find('.facet-choice-edit').css("visibility", "hidden");

            var choice = getChoice($(this));
            if (!choice.s) {
                $(this).find('.facet-choice-toggle').css("visibility", "hidden");
            }
        });
        
        bodyInnerDiv.find('.facet-choice-toggle').click(function() {
            var choice = findChoice($(this));
            if (choice.s) {
                deselect(choice);
            } else {
                select(choice);
            }
        });
    };
    window.setTimeout(wireEvents, 100);
};

ListFacet.prototype._doEdit = function() {
    new ClusteringDialog(this._config.columnName, this._config.expression);
};

ListFacet.prototype._editChoice = function(choice, choiceDiv) {
    var self = this;
    
    var menu = MenuSystem.createMenu().addClass("data-table-cell-editor").width("400px");
    menu.html(
        '<table class="data-table-cell-editor-layout">' +
            '<tr>' +
                '<td colspan="3">' +
                    '<textarea class="data-table-cell-editor-editor" bind="textarea" />' +
                '</td>' +
            '</tr>' +
            '<tr>' +
                '<td width="1%" align="center">' +
                    '<button bind="okButton">Apply</button><br/>' +
                    '<span class="data-table-cell-editor-key">Enter</span>' +
                '</td>' +
                '<td width="1%" align="center">' +
                    '<button bind="cancelButton">Cancel</button><br/>' +
                    '<span class="data-table-cell-editor-key">Esc</span>' +
                '</td>' +
                '<td>' +
                '</td>' +
            '</tr>' +
        '</table>'
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
        
        Gridworks.postProcess(
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
                        if (choice.v.v == originalContent) {
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
    
    elmts.okButton.click(commit);
    elmts.textarea
        .text(originalContent)
        .keydown(function(evt) {
            if (!evt.shiftKey) {
                if (evt.keyCode == 13) {
                    commit();
                } else if (evt.keyCode == 27) {
                    MenuSystem.dismissAll();
                }
            }
        })
        .select()
        .focus();
        
    elmts.cancelButton.click(function() {
        MenuSystem.dismissAll();
    });
};

ListFacet.prototype._select = function(choice, only) {
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

ListFacet.prototype._deselect = function(choice) {
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

ListFacet.prototype._reset = function() {
    this._selection = [];
    this._blankChoice = null;
    this._errorChoice = null;
    this._config.invert = false;
    
    this._updateRest();
};

ListFacet.prototype._invert = function() {
    this._config.invert = !this._config.invert;
    
    this._updateRest();
};

ListFacet.prototype._remove = function() {
    ui.browsingEngine.removeFacet(this);
    
    this._div = null;
    this._config = null;
    
    this._selection = null;
    this._blankChoice = null;
    this._errorChoice = null;
    this._data = null;
};

ListFacet.prototype._updateRest = function() {
    Gridworks.update({ engineChanged: true });
};

ListFacet.prototype._editExpression = function() {
    var self = this;
    var title = (this._config.columnName) ? 
            ("Edit Facet's Expression based on Column " + this._config.columnName) : 
            "Edit Facet's Expression";
    
    var column = Gridworks.columnNameToColumn(this._config.columnName);
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
                if (self._config.expression == "value" || self._config.expression == "gel:value") {
                    self._elmts.clusterLink.show();
                } else {
                    self._elmts.clusterLink.hide();
                }
                
                self.reset();
                self._updateRest();
            }
        }
    );
};