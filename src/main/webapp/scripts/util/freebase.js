var Freebase = {};

Freebase.mqlread = function(query, options, onDone) {
    var params = {};
    var queryEnv = {
        "query": query
    };
    
    if (options) {
        for (var n in options) {
            var v = options[n];
            if (typeof v != "string") {
                v = JSON.stringify(v);
            }
            
            queryEnv[n] = v;
        }
    }
    
    params.query = JSON.stringify(queryEnv);
    
    $.getJSON(
        "http://api.freebase.com/api/service/mqlread?" + $.param(params) + "&callback=?",
        null,
        onDone,
        "jsonp"
    );
};