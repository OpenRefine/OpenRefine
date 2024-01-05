I18NUtil = {};

/*
   Initialize i18n and load message translation file from the server.

   Note that the language is set by the 'userLang' user preference setting.  You can change that by
   clicking on 'Language Settings' on the landing page.
*/
I18NUtil.init = function (module) {
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
