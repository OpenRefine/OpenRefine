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
            $('<span> </span>').appendTo(divContent);
            $('<a href="javascript:{}">mark as new</a>')
                .addClass("data-table-recon-action")
                .appendTo(divContent).click(function(evt) {
                    self._doMarkAsNew();
                });
            
            if (this._dataTableView._showRecon && "c" in r && r.c.length > 0) {
                var candidates = r.c;
                var ul = $('<ul></ul>').addClass("data-table-recon-candidates").appendTo(divContent);
                var renderCandidate = function(candidate, index) {
                    var li = $('<li></li>').appendTo(ul);
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
                    $('<a href="javascript:{}">match</a>')
                        .addClass("data-table-recon-action")
                        .appendTo(li).click(function(evt) {
                            self._doSetAsMatch(candidate.id);
                        });
                };
                
                for (var i = 0; i < candidates.length; i++) {
                    renderCandidate(candidates[i], i);
                }
            }
        }
    }
};

DataTableCellUI.prototype._doRematch = function() {
    this._doJudgment("discard");
};

DataTableCellUI.prototype._doMarkAsNew = function() {
    this._doJudgment("new");
};

DataTableCellUI.prototype._doSetAsMatch = function(candidateID) {
    this._doJudgment("match", { candidate : candidateID });
};

DataTableCellUI.prototype._doJudgment = function(judgment, params) {
    params = params || {};
    params.row = this._rowIndex;
    params.cell = this._cellIndex;
    params.judgment = judgment;
    this.doPostThenUpdate("judge-one-cell", params);
};

DataTableCellUI.prototype.createUpdateFunction = function(onBefore) {
    var self = this;
    return function(data) {
        if (data.code == "ok") {
            var onDone = function() {
                self._cell = data.cell;
                self._render();
                ui.historyWidget.update();
            };
        } else {
            var onDone = function() {
                ui.processWidget.update();
            }
        }
        
        if (onBefore) {
            onBefore(onDone);
        } else {
            onDone();
        }
    };
};

DataTableCellUI.prototype.doPostThenUpdate = function(command, params) {
    params.project = theProject.id;
    $.post(
        "/command/" + command + "?" + $.param(params),
        null,
        this.createUpdateFunction(),
        "json"
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
