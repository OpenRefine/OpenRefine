function DataTableCellUI(dataTableView, cell, rowIndex, cellIndex, td) {
    this._dataTableView = dataTableView;
    this._cell = cell;
    this._rowIndex = rowIndex;
    this._cellIndex = cellIndex;
    this._td = td;
    
    this._render();
}

DataTableCellUI.prototype._render = function() {
    var self = this;
    var cell = this._cell;
    
    var divContent = $('<div/>')
        .addClass("data-table-cell-content");
        
    var editLink = $('<a href="javascript:{}" />')
        .addClass("data-table-cell-edit")
        .text("edit")
        .appendTo(divContent)
        .click(function() { self._startEdit(this); });
        
    $(this._td).empty()
        .unbind()
        .mouseenter(function() { editLink.css("visibility", "visible"); })
        .mouseleave(function() { editLink.css("visibility", "hidden"); });
    
    if (!cell || ("v" in cell && cell.v === null)) {
        $('<span>').html("&nbsp;").appendTo(divContent);
    } else if ("e" in cell) {
        $('<span>').addClass("data-table-error").text(cell.e).appendTo(divContent);
    } else if (!("r" in cell) || !cell.r) {
        $('<span>').text(cell.v).appendTo(divContent);
    } else {
        var r = cell.r;
        if (r.j == "new") {
            $('<span>').text(cell.v + " (new topic) ").appendTo(divContent);
            
            $('<a href="javascript:{}"></a>')
                .text("re\u2011match")
                .addClass("data-table-recon-action")
                .appendTo(divContent).click(function(evt) {
                    self._doRematch();
                });
        } else if (r.j == "matched" && "m" in r && r.m != null) {
            var match = cell.r.m;
            $('<a></a>')
                .text(match.name)
                .attr("href", "http://www.freebase.com/view" + match.id)
                .attr("target", "_blank")
                .appendTo(divContent);
                
            $('<span> </span>').appendTo(divContent);
            $('<a href="javascript:{}"></a>')
                .text("re\u2011match")
                .addClass("data-table-recon-action")
                .appendTo(divContent)
                .click(function(evt) {
                    self._doRematch();
                });
        } else {
            $('<span>').text(cell.v).appendTo(divContent);
            
            if (this._dataTableView._showRecon) {
                var ul = $('<div></div>').addClass("data-table-recon-candidates").appendTo(divContent);
                if ("c" in r && r.c.length > 0) {
                    var candidates = r.c;
                    var renderCandidate = function(candidate, index) {
                        var li = $('<div></div>').addClass("data-table-recon-candidate").appendTo(ul);
                        
                        $('<a href="javascript:{}">&nbsp;</a>')
                            .addClass("data-table-recon-match-similar")
                            .attr("title", "Match this topic to this cell and other cells with the same content")
                            .appendTo(li).click(function(evt) {
                                self._doMatchTopicToSimilarCells(candidate);
                            });
                            
                        $('<a href="javascript:{}">&nbsp;</a>')
                            .addClass("data-table-recon-match")
                            .attr("title", "Match this topic to this cell")
                            .appendTo(li).click(function(evt) {
                                self._doMatchTopicToOneCell(candidate);
                            });
                            
                        $('<a></a>')
                            .addClass("data-table-recon-topic")
                            .attr("href", "http://www.freebase.com/view" + candidate.id)
                            .attr("target", "_blank")
                            .click(function(evt) {
                                self._previewCandidateTopic(candidate.id, this);
                                evt.preventDefault();
                                return false;
                            })
                            .text(candidate.name)
                            .appendTo(li);
                            
                        $('<span></span>').addClass("data-table-recon-score").text("(" + Math.round(candidate.score) + ")").appendTo(li);
                    };
                    
                    for (var i = 0; i < candidates.length; i++) {
                        renderCandidate(candidates[i], i);
                    }
                }
                
                var liNew = $('<div></div>').addClass("data-table-recon-candidate").appendTo(ul);
                $('<a href="javascript:{}">&nbsp;</a>')
                    .addClass("data-table-recon-match-similar")
                    .attr("title", "Create a new topic for this cell and other cells with the same content")
                    .appendTo(liNew).click(function(evt) {
                        self._doMatchNewTopicToSimilarCells();
                    });
                    
                $('<a href="javascript:{}">&nbsp;</a>')
                    .addClass("data-table-recon-match")
                    .attr("title", "Create a new topic for this cell")
                    .appendTo(liNew).click(function(evt) {
                        self._doMatchNewTopicToOneCell();
                    });
                    
                $('<span>').text("(New topic)").appendTo(liNew);
                
                $('<a href="javascript:{}"></a>')
                    .addClass("data-table-recon-search")
                    .click(function(evt) {
                        self._searchForMatch();
                        return false;
                    })
                    .text("search for match")
                    .appendTo($('<div>').appendTo(divContent));
            }
        }
    }
    
    divContent.appendTo(this._td);
};

DataTableCellUI.prototype._doRematch = function() {
    this._doJudgment("none");
};

DataTableCellUI.prototype._doMatchNewTopicToOneCell = function() {
    this._doJudgment("new");
};

DataTableCellUI.prototype._doMatchNewTopicToSimilarCells = function() {
    this._doJudgmentForSimilarCells("new", { shareNewTopics: true }, true);
};

DataTableCellUI.prototype._doMatchTopicToOneCell = function(candidate) {
    this._doJudgment("matched", {
        topicID : candidate.id,
        topicGUID: candidate.guid,
        topicName: candidate.name,
        score: candidate.score,
        types: candidate.types.join(",")
   });
};

DataTableCellUI.prototype._doMatchTopicToSimilarCells = function(candidate) {
    this._doJudgmentForSimilarCells("matched", {
        topicID : candidate.id,
        topicGUID: candidate.guid,
        topicName: candidate.name,
        score: candidate.score,
        types: candidate.types.join(",")
    }, true);
};

DataTableCellUI.prototype._doJudgment = function(judgment, params) {
    params = params || {};
    params.row = this._rowIndex;
    params.cell = this._cellIndex;
    params.judgment = judgment;
    this._postProcessOneCell("recon-judge-one-cell", params, true);
};

DataTableCellUI.prototype._doJudgmentForSimilarCells = function(judgment, params) {
    params = params || {};
    params.columnName = Gridworks.cellIndexToColumn(this._cellIndex).name;
    params.similarValue = this._cell.v;
    params.judgment = judgment;
    
    this._postProcessSeveralCells("recon-judge-similar-cells", params, true);
};

DataTableCellUI.prototype._searchForMatch = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("400px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Search for Match").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    var html = $(
        '<div class="grid-layout layout-normal layout-full"><table>' +
            '<tr><td colspan="2">Search Freebase for topic to match ' + this._cell.v + '</td></tr>' +
            '<tr>' +
                '<td><input bind="input" /></td>' +
                '<td><input type="checkbox" checked="true" bind="checkSimilar" /> Match other cells with same content</td>' +
            '</tr>' +
        '</table></div>'
    ).appendTo(body);
    
    var elmts = DOM.bind(html);
    
    var match = null;
    var commit = function() {
        if (match != null) {
            var query = {
                "id" : match.id,
                "type" : []
            };
            var baseUrl = "http://api.freebase.com/api/service/mqlread";
            var url = baseUrl + "?" + $.param({ query: JSON.stringify({ query: query }) }) + "&callback=?";
            
            $.getJSON(
                url,
                null,
                function(o) {
                    var types = "result" in o ? o.result.type : [];
                    var params = {
                        judgment: "matched",
                        topicID: match.id,
                        topicGUID: match.guid,
                        topicName: match.name,
                        types: $.map(types, function(elmt) { return elmt.id; }).join(",")
                    };
                    if (elmts.checkSimilar[0].checked) {
                        params.similarValue = self._cell.v;
                        params.columnName = Gridworks.cellIndexToColumn(self._cellIndex).name;

                        self._postProcessSeveralCells("recon-judge-similar-cells", params, true);
                    } else {
                        params.row = self._rowIndex;
                        params.cell = self._cellIndex;

                        self._postProcessOneCell("recon-judge-one-cell", params, true);
                    }

                    DialogSystem.dismissUntil(level - 1);
                },
                "jsonp"
            );
        }
    };
    
    $('<button></button>').text("Match").click(commit).appendTo(footer);
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    var level = DialogSystem.showDialog(frame);
    
    elmts.input
        .attr("value", this._cell.v)
        .suggest({})
        .bind("fb-select", function(e, data) {
            match = data;
            commit();
        })
        .focus()
        .data("suggest").textchange();
};

DataTableCellUI.prototype._postProcessOneCell = function(command, params, columnStatsChanged) {
    var self = this;

    Gridworks.postProcess(
        command, 
        params, 
        null,
        { columnStatsChanged: columnStatsChanged },
        {
            onDone: function(o) {
                self._cell = o.cell;
                self._render();
            }
        }
    );
};

DataTableCellUI.prototype._postProcessSeveralCells = function(command, params, columnStatsChanged) {
    Gridworks.postProcess(
        command, 
        params, 
        null,
        { cellsChanged: true, columnStatsChanged: columnStatsChanged }
    );
};

DataTableCellUI.prototype._previewCandidateTopic = function(id, elmt) {
    var url = "http://www.freebase.com/widget/topic" + id + '?mode=content&blocks=[{"block"%3A"image"}%2C{"block"%3A"full_info"}%2C{"block"%3A"article_props"}]';
    
    var fakeMenu = MenuSystem.createMenu();
    fakeMenu
        .width(700)
        .height(300)
        .css("background", "none")
        .css("border", "none");
    
    var iframe = $('<iframe></iframe>')
        .attr("width", "100%")
        .attr("height", "100%")
        .css("background", "none")
        .css("border", "none")
        .attr("src", url)
        .appendTo(fakeMenu);
    
    MenuSystem.showMenu(fakeMenu, function(){});
    MenuSystem.positionMenuLeftRight(fakeMenu, $(elmt));
};

DataTableCellUI.prototype._startEdit = function(elmt) {
    self = this;
    
    var originalContent = !this._cell || ("v" in this._cell && this._cell.v === null) ? "" : this._cell.v;
    
    var menu = MenuSystem.createMenu().addClass("data-table-cell-editor").width("400px");
    menu.html(
        '<table class="grid-layout layout-tighest layout-full data-table-cell-editor-layout">' +
            '<tr>' +
                '<td colspan="5">' +
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
                    '<select bind="typeSelect">' +
                        '<option value="text">text</option>' +
                        '<option value="number">number</option>' +
                        '<option value="boolean">boolean</option>' +
                        '<option value="date">date</option>' +
                    '</select>' +
                '</td>' +
                '<td width="1%">' +
                    '<input type="checkbox" bind="applyOthersCheckbox" />' +
                '</td>' +
                '<td>' +
                    'apply to other cells with<br/>' +
                    'same content <span class="data-table-cell-editor-key">(Ctrl-Enter)</span>' +
                '</td>' +
            '</tr>' +
        '</table>'
    );
    var elmts = DOM.bind(menu);
    
    MenuSystem.showMenu(menu, function(){});
    MenuSystem.positionMenuLeftRight(menu, $(this._td));
    
    var commit = function() {
        var type = elmts.typeSelect[0].value;
        var applyOthers = elmts.applyOthersCheckbox[0].checked;
        
        var text = elmts.textarea[0].value;
        var value = text;
        
        if (type == "number") {
            value = parseFloat(text);
            if (isNaN(value)) {
                alert("Not a valid number.");
                return;
            }
        } else if (type == "boolean") {
            value = ("true" == text);
        } else if (type == "date") {
            value = Date.parse(text);
            if (!value) {
                alert("Not a valid date.");
                return;
            }
            value = value.toString("yyyy-MM-ddTHH:mm:ssZ");
        }
        
        MenuSystem.dismissAll();
        
        if (applyOthers) {
            Gridworks.postProcess(
                "mass-edit",
                {},
                {
                    columnName: Gridworks.cellIndexToColumn(self._cellIndex).name,
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
            Gridworks.postProcess(
                "edit-one-cell", 
                {
                    row: self._rowIndex,
                    cell: self._cellIndex,
                    value: value,
                    type: type
                }, 
                null,
                {},
                {
                    onDone: function(o) {
                        self._cell = o.cell;
                        self._render();
                    }
                }
            );
        }
    };
    
    elmts.okButton.click(commit);
    elmts.textarea
        .text(originalContent)
        .keydown(function(evt) {
            if (!evt.shiftKey) {
                if (evt.keyCode == 13) {
                    if (evt.ctrlKey) {
                        elmts.applyOthersCheckbox[0].checked = true;
                    }
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