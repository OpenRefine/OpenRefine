var StatsExtension = {};

DataTableColumnHeaderUI.extendMenu(function(column, columnHeaderUI, menu) {
    var doStatsDialog = function(response) {
        var dialog = $(DOM.loadHTML("refine-stats", "scripts/stats-dialog.html"));
    
        var elmts = DOM.bind(dialog);
        elmts.dialogHeader.text("Statistics for column \"" + column.name + "\"");

        if (response["count"]) { elmts.dialogCount.text(response["count"]); 
        if (response["sum"]) { elmts.dialogSum.text(response["sum"]); }}
        if (response["min"]) { elmts.dialogMin.text(response["min"]); }
        if (response["max"]) { elmts.dialogMax.text(response["max"]); }
        if (response["mean"]) { elmts.dialogMean.text(response["mean"]); }
        if (response["median"]) { elmts.dialogMedian.text(response["median"]); }
        if (response["mode"]) { elmts.dialogMode.text(response["mode"]); }
        if (response["stddev"]) { elmts.dialogStdDev.text(response["stddev"]); }
        if (response["variance"]) { elmts.dialogVariance.text(response["variance"]); }

        var level = DialogSystem.showDialog(dialog);

        elmts.okButton.click(function() {
            DialogSystem.dismissUntil(level - 1);
        }); 
    };

    var prepStatsDialog = function() { 
        params = { "column_name": column.name };
        body = {};
        updateOptions = {};
        callbacks = { 
            "onDone": function(response) {
                doStatsDialog(response);
            }
        }

        Refine.postProcess(
            "refine-stats",
            "summarize",
            params,
            body,
            updateOptions,
            callbacks
        );
    }

    MenuSystem.insertAfter(
        menu,
        [ "core/transpose" ], 
        [
        {},
        {
            id: "refine-stats/summarize",
            label: "Column statistics",
            click: prepStatsDialog 
        }
        ]
    );
});

