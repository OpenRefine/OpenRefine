I18NUtil = {};

/*
 * Initialize i18n and load message translation file for
 * the given module.
 * @public
 * @param {string} module The module name to load the translation file for.
 */
I18NUtil.init = function (module) {
    /*
     * Note that the browser language is only used to show a warning if the server
     * replies with another. The language is instead picked form the `userLang` preference.
     */
    var lang = (navigator.language).split("-")[0];
    var dictionary = "";

    $.ajax({
        url: "command/core/load-language?",
        type: "POST",
        async: false,
        data: {
            module: module
        },
        success: function (data) {
            dictionary = data['dictionary'];
            var langFromServer = data['lang'];
            if (lang !== langFromServer) {
                console.warn('Language \'' + lang + '\' missing translation. Defaulting to \''+langFromServer+'\'.');
                lang = langFromServer;
            }
        }
    }).fail(function( jqXhr, textStatus, errorThrown ) {
        var errorMessage = $.i18n('core-index/prefs-loading-failed');
        if(errorMessage != "" && errorMessage != 'core-index/prefs-loading-failed') {
            alert(errorMessage);
        } else {
            alert( textStatus + ':' + errorThrown );
        }
    });
    $.i18n().load(dictionary, lang);
    $('html').attr('lang', lang.replace('_', '-'));
    if (module === 'core') {
      $.i18n({locale: lang});
      // TODO: This should be globally accessible, but this is OK for our current needs
      Refine.userLang = lang;
    }
}
