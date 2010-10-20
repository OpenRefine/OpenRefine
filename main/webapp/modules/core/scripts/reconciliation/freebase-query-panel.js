function ReconFreebaseQueryPanel(column, service, container) {
    this._column = column;
    this._service = service;
    this._container = container;
    
    this._constructUI();
}

ReconFreebaseQueryPanel.prototype.activate = function() {
    this._panel.show();
};

ReconFreebaseQueryPanel.prototype.deactivate = function() {
    this._panel.hide();
};

ReconFreebaseQueryPanel.prototype.dispose = function() {
    this._panel.remove();
    this._panel = null;
    
    this._column = null;
    this._service = null;
    this._container = null;
};

ReconFreebaseQueryPanel.prototype._constructUI = function() {
    var self = this;
    this._panel = $(DOM.loadHTML("core", "scripts/reconciliation/freebase-query-panel.html")).appendTo(this._container);
    this._elmts = DOM.bind(this._panel);
    this._wireEvents();
};

ReconFreebaseQueryPanel.prototype._wireEvents = function() {
    var self = this;
    this._elmts.strictNamespaceInput
        .suggest({ type: '/type/namespace' })
        .bind("fb-select", function(e, data) {
            self._panel.find('input[name="recon-dialog-strict-choice"][value="key"]').attr("checked", "true");
            self._panel.find('input[name="recon-dialog-strict-namespace-choice"][value="other"]').attr("checked", "true");
        });
};

ReconFreebaseQueryPanel.prototype.start = function() {
    var bodyParams;
    
    var match = $('input[name="recon-dialog-strict-choice"]:checked')[0].value;
    if (match == "key") {
        var namespaceChoice = $('input[name="recon-dialog-strict-namespace-choice"]:checked')[0];
        var namespace;
        
        if (namespaceChoice.value == "other") {
            var suggest = this._elmts.strictNamespaceInput.data("data.suggest");
            if (!suggest) {
                alert("Please specify a namespace.");
                return;
            }
            namespace = {
                id: suggest.id,
                name: suggest.name
            };
        } else {
            namespace = {
                id: namespaceChoice.value,
                name: namespaceChoice.getAttribute("nsName")
            };
        }
    
        bodyParams = {
            columnName: this._column.name,
            config: JSON.stringify({
                mode: "freebase/strict",
                match: "key",
                namespace: namespace
            })
        };
    } else if (match == "id") {
        bodyParams = {
            columnName: this._column.name,
            config: JSON.stringify({
                mode: "freebase/strict",
                match: "id"
            })
        };
    } else if (match == "guid") {
        bodyParams = {
            columnName: this._column.name,
            config: JSON.stringify({
                mode: "freebase/strict",
                match: "guid"
            })
        };
    }
    
    Refine.postCoreProcess(
        "reconcile",
        {},
        bodyParams,
        { cellsChanged: true, columnStatsChanged: true }
    );
};