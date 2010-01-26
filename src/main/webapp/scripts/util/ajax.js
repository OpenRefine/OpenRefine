Ajax = {};

Ajax.chainGetJSON = function() {
    var a = arguments;
    var i = 0;
    var next = function() {
        if (i <= a.length - 3) {
            var url = a[i++];
            var data = a[i++];
            var callback = a[i++];
            
            $.getJSON(url, data, function(o) {
                callback(o);
                next();
            }, "json");
        } else if (i < a.length) {
            var finalCallback = a[i++];
            finalCallback();
        }
    };
    next();
};