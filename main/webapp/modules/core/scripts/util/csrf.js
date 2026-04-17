CSRFUtil = {
    _tokenMaxAge: 30 * 60 * 1000 // half an hour in milliseconds (tokens live for 1 hour in the backend)
};


/**
 * Requests a CSRF token and calls the supplied callback.
 * @param {function(string)} onCSRF - The callback to call with the token.
 * @deprecated use CSRFUtil.getCSRF().then(onCSRF).
 * This is unused internally and can be removed after a suitable deprecation period for extensions.
 */
CSRFUtil.wrapCSRF = function(onCSRF) {
    let tokenAge = CSRFUtil._tokenMaxAge + 1;
    if (CSRFUtil._token && CSRFUtil._fetchedOn) {
        tokenAge = new Date() - CSRFUtil._fetchedOn;
    }
    if (tokenAge < CSRFUtil._tokenMaxAge) {
        onCSRF(CSRFUtil._token);
    } else {
        $.get(
            "command/core/get-csrf-token")
          .done(function(response) {
            CSRFUtil._token = response['token'];
            CSRFUtil._fetchedOn = new Date();
            onCSRF(CSRFUtil._token);
        });
    }
};

/**
 * Performs a POST request where an additional CSRF token is automatically attached.
 * @param {string} url - The URL to send the request to.
 * @param {Object|string} data - The data to send with the request.
 * @param {function(object)} success - The callback to call on success.
 * @param {string} dataType - The type of data expected in the response.
 * @param {function(object)} failCallback - The callback to call on failure.
 * @deprecated use CSRFUtil.post()
 * This is unused internally and can be removed after a suitable deprecation period for extensions.
 */
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

/**
 * Returns a Promise which provides a CSRF token when resolved, requesting it from the server, if needed
 * @returns {Promise} - A promise that resolves with the CSRF token
 */
CSRFUtil.getCSRF = function () {
    let tokenAge = CSRFUtil._tokenMaxAge + 1;
    if (CSRFUtil._token && CSRFUtil._fetchedOn) {
        tokenAge = new Date() - CSRFUtil._fetchedOn;
    }
    if (tokenAge < CSRFUtil._tokenMaxAge) {
        return $.when(CSRFUtil._token);
    } else {
        return $.get(
          "command/core/get-csrf-token"
        ).fail((jqXHR, textStatus, errorThrown) => {
            console.error(jqXHR, textStatus, errorThrown);
        }).then((response) => {
            CSRFUtil._token = response['token'];
            CSRFUtil._fetchedOn = new Date();
            return response['token'];
        });
    }
};

/**
 * Performs a POST request where an additional CSRF token is automatically attached.
 *
 * This accepts four forms for compatibility with the previous implementation and $.post():
 *   post(url), post(url, string), post(url, dataObj), post(settingsObj).
 *
 * @param {string|Object} urlOrSettings - The URL to send the request to or a settings object.
 * @param {Object|string|undefined} data - The data to send with the request if the first parameter is a URL
 * @returns {jQuery.jqXHR} The jQuery AJAX request object.
 */
CSRFUtil.post = function (urlOrSettings, data) {
    let settings = {};
    if (typeof urlOrSettings == 'string') {
        if (typeof data == 'string' || typeof data == 'object') {
            settings = {url: urlOrSettings, data: data}
        } else if (typeof data == 'undefined') {
            settings = {url: urlOrSettings}
        } else {
            throw new Error('Invalid arguments for CSRFUtil.postCSRF2: 2 arg form expects string or object for data');
        }
    } else if (typeof urlOrSettings == 'object') {
        settings = urlOrSettings;
    } else {
        throw new Error('Invalid arguments for CSRFUtil.postCSRF2: expected just url or url plus data string/object or just settings object');
    }

    // Return a Promise that resolves with the result of the POST request
    return CSRFUtil.getCSRF().then(function (token) {
        let fullData = settings.data || {};
        if (typeof fullData == 'string') {
            settings.data = fullData + "&" + $.param({csrf_token: token});
        } else {
            settings.data['csrf_token'] = token;
        }
        const defaults = {/*dataType: 'json',*/}; // TODO: When our API is fixed, we can default to JSON
        return $.post({...defaults, ...settings})
          .fail((jqXHR, textStatus, errorThrown) => {
              console.error(jqXHR, textStatus, errorThrown);
          });
    });

};
