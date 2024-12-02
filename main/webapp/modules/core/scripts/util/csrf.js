CSRFUtil = {
    _tokenMaxAge: 30 * 60 * 1000 // half an hour in milliseconds (tokens live for 1 hour in the backend)
};


// Requests a CSRF token and calls the supplied callback
// with the token
CSRFUtil.wrapCSRF = function(onCSRF) {
    let tokenAge = CSRFUtil._tokenMaxAge + 1;
    if (CSRFUtil._token && CSRFUtil._fetchedOn) {
        tokenAge = new Date() - CSRFUtil._fetchedOn;
    }
    if (tokenAge < CSRFUtil._tokenMaxAge) {
        onCSRF(CSRFUtil._token);
    } else {
        $.get(
            "command/core/get-csrf-token",
            {},
            function(response) {
                CSRFUtil._token = response['token'];
                CSRFUtil._fetchedOn = new Date();
                onCSRF(CSRFUtil._token);
            },
            "json"
        );
    }
};

// Performs a POST request where an additional CSRF token
// is supplied in the POST data. The arguments match those
// of $.post().
CSRFUtil.postCSRF = function(url, data, success, dataType, failCallback) {
    CSRFUtil.wrapCSRF(function(token) {
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
