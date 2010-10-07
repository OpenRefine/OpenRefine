var Scripting = {
};

Scripting.parse = function(expression) {
    var colon = expression.indexOf(":");
    if (colon > 0) {
        var l = expression.substring(0, colon);
        if (l == "gel") { // backward compatible
            l = "grel";
        }
        if (theProject.scripting.hasOwnProperty(l)) {
            return {
                language: l,
                expression: expression.substring(colon + 1)
            };
        }
    }
    
    return {
        language: "grel",
        expression: expression
    };
};