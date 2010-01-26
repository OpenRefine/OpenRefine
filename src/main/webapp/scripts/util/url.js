URL = {};

URL.getParameters = function() {
    var r = {};
    
    var params = window.location.search;
    if (params.length > 1) {
        params = params.substr(1).split("&");
        $.each(params, function() {
            pair = this.split("=");
            r[pair[0]] = unescape(pair[1]);
        });
    }
    
    return r;
};