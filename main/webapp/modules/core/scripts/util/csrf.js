CSRFUtil = {};

// Requests a CSRF token and calls the supplied callback
// with the token
CSRFUtil.wrapCSRF = function(onCSRF) {
    $.get(
        "command/core/get-csrf-token",
        {},
        function(response) {
            onCSRF(response['token']);
        },
        "json"
    );
};

// Performs a POST request where an additional CSRF token
// is supplied in the POST data. The arguments match those
// of $.post().
CSRFUtil.postCSRF = function(url, data, success, dataType, failCallback) {
    return CSRFUtil.wrapCSRF(function(token) {
        var fullData = data || {};
        if (typeof fullData == 'string') {
            fullData = fullData + "&" + $.param({csrf_token: token});
        } else {
            fullData['csrf_token'] = token;
        }
        var req = $.post(url, fullData, success, dataType);
        if (failCallback !== undefined) {
            req.fail(failCallback);
        }
    });
};
