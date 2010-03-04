var Scripting = {
    languages: [ "gel", "jython", "clojure" ]
};

Scripting.parse = function(expression) {
    var colon = expression.indexOf(":");
    if (colon > 0) {
        var l = expression.substring(0, colon);
        for (var i = 0; i < Scripting.languages.length; i++) {
            if (l == Scripting.languages[i]) {
                return {
                    language: l,
                    expression: expression.substring(colon + 1)
                };
            }
        }
    }
    
    return {
        language: "gel",
        expression: expression
    };
};