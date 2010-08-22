DataTableColumnHeaderUI.extendMenu(function(column, columnHeaderUI, menu) {
    var doReconcile = function() {
        new ReconDialog(column);
    };

    var doReconDiscardJudgments = function() {
        Gridworks.postCoreProcess(
            "recon-discard-judgments",
            { columnName: column.name },
            null,
            { cellsChanged: true, columnStatsChanged: true }
        );
    };

    var doReconMatchBestCandidates = function() {
        Gridworks.postCoreProcess(
            "recon-match-best-candidates",
            { columnName: column.name },
            null,
            { cellsChanged: true, columnStatsChanged: true }
        );
    };

    var doReconMarkNewTopics = function(shareNewTopics) {
        Gridworks.postCoreProcess(
            "recon-mark-new-topics",
            { columnName: column.name, shareNewTopics: shareNewTopics },
            null,
            { cellsChanged: true, columnStatsChanged: true }
        );
    };

    var doSearchToMatch = function() {
        var frame = DialogSystem.createDialog();
        frame.width("400px");

        var header = $('<div></div>').addClass("dialog-header").text("Search for Match").appendTo(frame);
        var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
        var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

        $('<p></p>').text("Search Freebase for a topic to match all filtered cells:").appendTo(body);

        var input = $('<input />').appendTo($('<p></p>').appendTo(body));

        input.suggest({}).bind("fb-select", function(e, data) {
            var query = {
                "id" : data.id,
                "type" : []
            };
            var baseUrl = "http://api.freebase.com/api/service/mqlread";
            var url = baseUrl + "?" + $.param({ query: JSON.stringify({ query: query }) }) + "&callback=?";

            $.getJSON(
                url,
                null,
                function(o) {
                    var types = "result" in o ? o.result.type : [];

                    Gridworks.postCoreProcess(
                        "recon-match-specific-topic-to-cells",
                        {
                            columnName: column.name,
                            topicID: data.id,
                            topicGUID: data.guid,
                            topicName: data.name,
                            types: types.join(",")
                        },
                        null,
                        { cellsChanged: true, columnStatsChanged: true }
                    );

                    DialogSystem.dismissUntil(level - 1);
                }
            );
        });

        $('<button></button>').text("Cancel").click(function() {
            DialogSystem.dismissUntil(level - 1);
        }).appendTo(footer);

        var level = DialogSystem.showDialog(frame);
        input.focus().data("suggest").textchange();
    };
    
    MenuSystem.appendTo(menu, [ "core/reconcile" ], [
        {
            label: "Start Reconciling ...",
            tooltip: "Reconcile text in this column with topics on Freebase",
            click: doReconcile
        },
        {},
        {
            label: "Facets",
            submenu: [
                {
                    label: "By Judgment",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.judgment",
                                "omitError" : true
                            },
                            {
                                "scroll" : false
                            }
                        );
                    }
                },
                {},
                {
                    label: "Best Candidate's Relevance Score",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.best.score",
                                "mode" : "range"
                            },
                            {
                            }
                        );
                    }
                },
                {
                    label: "Best Candidate's Type Match",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.features.typeMatch",
                                "omitError" : true
                            },
                            {
                                "scroll" : false
                            }
                        );
                    }
                },
                {
                    label: "Best Candidate's Name Match",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.features.nameMatch",
                                "omitError" : true
                            },
                            {
                                "scroll" : false
                            }
                        );
                    }
                },
                {},
                {
                    label: "Best Candidate's Name Edit Distance",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.features.nameLevenshtein",
                                "mode" : "range"
                            },
                            {
                            }
                        );
                    }
                },
                {
                    label: "Best Candidate's Name Word Similarity",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.features.nameWordDistance",
                                "mode" : "range"
                            },
                            {
                            }
                        );
                    }
                },
                {},
                {
                    label: "Best Candidate's Types",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.best.type",
                                "omitError" : true
                            }
                        );
                    }
                }
            ]
        },
        {
            label: "QA Facets",
            submenu: [
                {
                    label: "QA Results",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name + " QA Results",
                                "columnName" : column.name, 
                                "expression" : "cell.recon.features.qaResult"
                            }
                        );
                    }
                },
                {
                    label: "Judgment Actions",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name + " Judgment Actions",
                                "columnName" : column.name, 
                                "expression" : "cell.recon.judgmentAction"
                            }
                        );
                    }
                },
                {
                    label: "Judgment History Entries",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name + " History Entries",
                                "columnName" : column.name, 
                                "expression" : "cell.recon.judgmentHistoryEntry"
                            }
                        );
                    }
                }
            ]
        },
        {
            label: "Actions",
            submenu: [
                {
                    label: "Match Each Cell to Its Best Candidate",
                    tooltip: "Match each cell to its best candidate in this column for all current filtered rows",
                    click: doReconMatchBestCandidates
                },
                {
                    label: "Create a New Topic for Each Cell",
                    tooltip: "Mark to create one new topic for each cell in this column for all current filtered rows",
                    click: function() {
                        doReconMarkNewTopics(false);
                    }
                },
                {},
                {
                    label: "Create One New Topic for Similar Cells",
                    tooltip: "Mark to create one new topic for each group of similar cells in this column for all current filtered rows",
                    click: function() {
                        doReconMarkNewTopics(true);
                    }
                },
                {
                    label: "Match All Filtered Cells to ...",
                    tooltip: "Search for a topic to match all filtered cells to",
                    click: doSearchToMatch
                },
                {},
                {
                    label: "Discard Reconciliation Judgments",
                    tooltip: "Discard reconciliaton judgments in this column for all current filtered rows",
                    click: doReconDiscardJudgments
                }
            ]
        }
    ]);
});