var SchemaAlignment = {};

SchemaAlignment.autoAlign = function() {
    var protograph = {};
    
    var columns = theProject.columnModel.columns;
    
    var typedColumns = [];
    var candidates = [];
    
    for (var c = 0; c < columns.length; c++) {
        var column = columns[c];
        if ("reconConfig" in column && column.reconConfig != null) {
            typedColumns.push(column);
        }
        candidates.push({
            status: "unbound",
            index: c,
            column: column
        })
    }
    
    
};