/*
* Derived from Portfolio filterable.js by Joel Sutherland
*
* Copyright (C) 2009 Joel Sutherland.
* Liscenced under the MIT liscense
*
* Modified for OpenRefine by Simone Povoscania
* Modified for OpenRefine by Lucero Garcia
*/

(function($) {
        $.fn.filterListSearch = function(text) {
            text = text.trim();
            settings = {
                    animationSpeed: 500,
                    show: { height: 'show', opacity: 'show' },
                    hide: { height: 'hide', opacity: 'hide' },
                    useAll: true,
                    projectSelector: '#projectTags a',
                    selectedTagClass: 'current',
                    allTag: 'all'
            }
            var listElement = $(this);
            console.log("list element", listElement)
            /* FILTER: trigger filter */
            listElement.trigger("filter", text);

            $(settings.tagSelector).removeClass('current');
            $(this).addClass('current');
            /* FILTER: select a text and filter */
            listElement.bind("filter", function( e, text ) {
                console.log("filtering", )
                console.log("tres",listElement.tr)
                // search in columns and rows, if contains show == true, else none
            });

            /*listElement.bind("filter", function( e, text ){
                console.log(element)
                console.log("binding if contains : ", text)
                if(settings.useAll){
                    $(settings.projectSelector).removeClass(settings.selectedTagClass);
                    console.log("adding class to", settings.projectSelector + '[href="' + text + '"]')
                    $(settings.projectSelector + '[href="' + text + '"]').addClass(settings.selectedTagClass);
                }
                if(text == '' || text.length <1){
                    $(this).trigger("show");
                }else{
                    $(this).trigger("show", [ '.' + text ] );
                    $(this).trigger("hide", [ ':not(.' + text + ')' ] );
                }
            });*/

            /* SHOW: show a single class
            $(this).bind("show", function( e, selectorToShow ){
                    listElement.children(selectorToShow).animate(settings.show, settings.animationSpeed);
            });*/

            /* SHOW: hide a single class
            $(this).bind("hide", function( e, selectorToHide ){
                    listElement.children(selectorToHide).animate(settings.hide, settings.animationSpeed * 0.6);
            });*/

            return this;
        };
})(jQuery);