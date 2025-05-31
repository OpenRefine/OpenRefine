var WikibaseUI = {};

WikibaseUI.createEditingResultsFacet = function () {
    const columnName = "Wikibase editing results";

    const column = theProject.columnModel.columns.find(col => col.name === columnName);
    if (!column) {
        alert(`${columnName} column not found.`);
        return;
    }

    ui.browsingEngine.addFacet(
        "list",
        {
            "name": columnName,
            "columnName": columnName,
            "expression": "if(isError(value), 'failed edit', if(isBlank(value), 'no edit', 'successful edit'))"
        }
    );
};