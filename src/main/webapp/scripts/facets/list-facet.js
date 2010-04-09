function ListFacet(div, config, options, selection) {
    this._div = div;
    this._config = config;
    
    this._options = options || {};
    if (!("sort" in this._options)) {
        this._options.sort = "name";
    }
    
    this._selection = selection || [];
    this._blankChoice = null;
    this._errorChoice = null;
    
    this._data = null;
    
    this.render();
}

ListFacet.reconstruct = function(div, uiState) {
    return new ListFacet(div, uiState.c, uiState.o, uiState.s);
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
        selectError: this._errorChoice !== null && this._errorChoice.s
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
    
    this.render();
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

ListFacet.prototype.render = function() {
    var self = this;
    
    var scrollTop = 0;
    try {
        scrollTop = this._div[0].childNodes[1].scrollTop;
    } catch (e) {
    }
    var container = this._div.empty();
    
    var headerDiv = $('<div></div>').addClass("facet-title").appendTo(container);
    $('<span></span>').text(this._config.name).appendTo(headerDiv);
    
    var removeButton = $('<img>')
        .attr("src", "images/close.png")
        .attr("title", "Remove this facet")
        .addClass("facet-choice-link")
        .click(function() {
            self._remove();
        }).prependTo(headerDiv);
    
    var bodyDiv = $('<div></div>').addClass("facet-body");
    if (!("scroll" in this._options) || this._options.scroll) {
        bodyDiv.addClass("facet-body-scrollable");
    }
    
    if (!this._data) {
        $('<div>').text("Loading...").addClass("facet-body-message").appendTo(bodyDiv);
        bodyDiv.appendTo(container);
    } else if ("error" in this._data) {
        $('<div>').text(this._data.error).addClass("facet-body-message").appendTo(bodyDiv);
        bodyDiv.appendTo(container);
    } else {
        var selectionCount = this._selection.length +
              (this._blankChoice !== null && this._blankChoice.s ? 1 : 0) +
              (this._errorChoice !== null && this._errorChoice.s ? 1 : 0);
            
        if (selectionCount > 0) {
            var reset = function() {
                self._reset();
            };
            removeButton.after(
                $('<a href="javascript:{}"></a>').addClass("facet-choice-link").text("reset").click(reset)
            );
        }
        
        var renderEdit = this._config.expression == "value";
        var renderChoice = function(choice, customLabel) {
            var label = customLabel || choice.v.l;
            var count = choice.c;
            
            var choiceDiv = $('<div></div>').addClass("facet-choice").appendTo(bodyDiv);
            if (choice.s) {
                choiceDiv.addClass("facet-choice-selected");
            }
            
            var a = $('<a href="javascript:{}"></a>').addClass("facet-choice-label").text(label).appendTo(choiceDiv);
            $('<span></span>').addClass("facet-choice-count").text(count).appendTo(choiceDiv);
            
            var select = function() {
                self._select(choice, false);
            };
            var selectOnly = function() {
                self._select(choice, true);
            };
            var deselect = function() {
                self._deselect(choice);
            };
            
            if (choice.s) { // selected
                if (selectionCount > 1) {
                    // select only
                    a.click(selectOnly);
                } else {
                    // deselect
                    a.click(deselect);
                }
                
                // remove link
                $('<a href="javascript:{}"></a>').addClass("facet-choice-link").text("remove").click(deselect).prependTo(choiceDiv);
            } else if (selectionCount > 0) {
                a.click(selectOnly);
                
                // include link
                $('<a href="javascript:{}"></a>').addClass("facet-choice-link").text("include").click(select).prependTo(choiceDiv);
            } else {
                a.click(select);
            }
            
            if (renderEdit && customLabel === undefined) {
                // edit link
                var editLink = $('<a href="javascript:{}"></a>').addClass("facet-choice-edit").text("edit").click(function() {
                    self._editChoice(choice, choiceDiv);
                }).appendTo(choiceDiv);
                
                choiceDiv
                    .mouseenter(function() {
                        editLink.css("visibility", "visible");
                    })
                    .mouseleave(function() {
                        editLink.css("visibility", "hidden");
                    });
            }
        };
        
        var choices = this._data.choices;
        for (var i = 0; i < choices.length; i++) {
            renderChoice(choices[i]);
        }
        if (this._blankChoice !== null) {
            renderChoice(this._blankChoice, "(blank)");
        }
        if (this._errorChoice !== null) {
            renderChoice(this._errorChoice, "(error)");
        }
        
        bodyDiv.appendTo(container);
        bodyDiv[0].scrollTop = scrollTop;
        
        var footerDiv = $('<div></div>').addClass("facet-footer").appendTo(container);
        
        $('<span>').text(choices.length + " choices: ").appendTo(footerDiv);
        if (this._options.sort == "name") {
            $('<a href="javascript:{}"></a>').addClass("action").text("re-sort by count").click(function() {
                self._options.sort = "count";
                self._reSortChoices();
                self.render();
            }).appendTo(footerDiv);
        } else {
            $('<a href="javascript:{}"></a>').addClass("action").text("re-sort by name").click(function() {
                self._options.sort = "name";
                self._reSortChoices();
                self.render();
            }).appendTo(footerDiv);
        }
        
        if (this._config.expression == "value") {
            $('<span>').html(" &bull; ").appendTo(footerDiv);
            $('<a href="javascript:{}"></a>').addClass("action").text("cluster").click(function() {
                self._doEdit();
            }).appendTo(footerDiv);
        }
    }
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
    
    var originalContent = choice.v.v;
    var commit = function() {
        var text = elmts.textarea[0].value;
        
        MenuSystem.dismissAll();
        
        Gridworks.postProcess(
            "mass-edit",
            {},
            {
                columnName: self._config.columnName,
                expression: "value",
                edits: JSON.stringify([{
                    from: [ originalContent ],
                    to: text
                }])
            },
            {
                includeEngine: false, // we're really changing all rows, not just the visible ones
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
