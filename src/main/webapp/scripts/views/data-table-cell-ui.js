function DataTableCellUI(dataTableView, cell, rowIndex, cellIndex, td) {
    this._dataTableView = dataTableView;
    this._cell = cell;
    this._rowIndex = rowIndex;
    this._cellIndex = cellIndex;
    this._td = td;
    
    this._render();
};

DataTableCellUI.prototype._render = function() {
    var self = this;
    var cell = this._cell;
    
    $(this._td).empty();
    var divContent = $('<div></div>').appendTo(this._td);
    
    if (cell == null || cell.v == null) {
        $(divContent).html("&nbsp;");
        // TODO: content editing UI
        return;
    }
    
    if (!("r" in cell) || cell.r == null) {
        $(divContent).html(cell.v);
    } else {
        var r = cell.r;
        if (r.j == "new") {
            $(divContent).html(cell.v + " (new topic)");
            
            $('<span> </span>').appendTo(divContent);
            $('<a href="javascript:{}">re-match</a>')
                .addClass("data-table-recon-action")
                .appendTo(divContent).click(function(evt) {
                    self._doRematch();
                });
        } else if (r.j == "matched" && "m" in r && r.m != null) {
            var match = cell.r.m;
            $('<a></a>')
                .attr("href", "http://www.freebase.com/view" + match.id)
                .attr("target", "_blank")
                .text(match.name)
                .appendTo(divContent);
                
            $('<span> </span>').appendTo(divContent);
            $('<a href="javascript:{}">re-match</a>')
                .addClass("data-table-recon-action")
                .appendTo(divContent).click(function(evt) {
                    self._doRematch();
                });
        } else {
            $(divContent).html(cell.v);
            
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
    params.columnName = Gridworks.cellIndexToColumn(this._cellIndex).headerLabel;
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
    
    $('<p></p>').text("Search Freebase for topic to match " + this._cell.v).appendTo(body);
    
    var input = $('<input />').attr("value", this._cell.v).appendTo($('<p></p>').appendTo(body));
    var match = null;
    input.suggest({}).bind("fb-select", function(e, data) {
        match = data;
    });
    
    var pSimilar = $('<p></p>').appendTo(body);
    var checkSimilar = $('<input type="checkbox" checked="true" />').appendTo(pSimilar);
    $('<span>').text(" Match other cells with the same content as well").appendTo(pSimilar);
    
    $('<button></button>').text("Match").click(function() {
        if (match != null) {
            var params = {
                judgment: "matched",
                topicID: match.id,
                topicGUID: match.guid,
                topicName: match.name,
                types: $.map(match.type, function(elmt) { return elmt.id; }).join(",")
            };
            if (checkSimilar[0].checked) {
                params.similarValue = self._cell.v;
                params.columnName = Gridworks.cellIndexToColumn(self._cellIndex).headerLabel;
                
                self._postProcessSeveralCells("recon-judge-similar-cells", params, true);
            } else {
                params.row = self._rowIndex;
                params.cell = self._cellIndex;
                
                self._postProcessOneCell("recon-judge-one-cell", params, true);
            }
        
            DialogSystem.dismissUntil(level - 1);
        }
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    var level = DialogSystem.showDialog(frame);
    input.focus().data("suggest").textchange();
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
                self._cell = data.cell;
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
