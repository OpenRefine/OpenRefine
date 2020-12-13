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
            text = text.trim().toLowerCase();
            settings = {
                    selectedTagClass: 'current',
            }
            var listElements = $(this);
            /* FILTER: select a text and filter */
            listElements.bind("filterSearch", function( e, text ) {
                // get each project row
                $(this).find("tr").filter(function() {
                        $(this).removeClass(settings.selectedTagClass);
                        return $(this).text().toLowerCase().contains(text)
                }).addClass(settings.selectedTagClass);
                $(this).trigger("filterSearchList",[ settings.selectedTagClass ]);
            });

            /* FILTERPORTFOLIO: pass in a class to show, all others will be hidden */
            listElements.bind("filterSearchList", function( e, classToShow ) {
                if( text == ''){
                        return
                }else{
                        $(this).trigger("show", [ '.' + classToShow ] );
                        $(this).trigger("hide", [ ':not(.' + classToShow + ')' ] );
                }
            });


            /* FILTER: trigger filter */
            listElements.trigger("filterSearch", text);
            return this;
        };
})(jQuery);
