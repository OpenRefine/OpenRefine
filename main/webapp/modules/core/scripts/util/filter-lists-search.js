/*
* Derived from Portfolio filterable.js by Joel Sutherland
*
* Copyright (C) 2009 Joel Sutherland.
* Licensed under the MIT license
*
* Modified for OpenRefine by Simone Povoscania
* Modified for OpenRefine by Lucero Garcia
*/

(function($) {
    $.fn.filterListSearch = function(text) {
        text = text.trim().toLowerCase();
        const settings = {
            selectedTagClass: 'current',
        }
        var listElements = $(this);
        /* FILTER: select a text and filter */
        listElements.bind("filterSearch", function( e, text ) {
            // get each project row
            var projects = $(this).find("tr");

            var projectsWithText = projects.filter(function() {
                $(this).removeClass(settings.selectedTagClass);
                return $(this).find(".searchable").text().toLowerCase().includes(text)
            });
            projectsWithText.addClass(settings.selectedTagClass);
            $(this).trigger("filterSearchList",[ settings.selectedTagClass ]);
            var message = $('#no-results-message');
            if(projectsWithText.length === 0) {
                message.show();
            } else if(message.is(':visible')) {
                message.hide();
            }
        });

        /* FILTERPORTFOLIO: pass in a class to show, all others will be hidden */
        listElements.bind("filterSearchList", function( e, classToShow ) {
            if( text == ''){
                return
            }else{
                $(this).find('tr:not(.' + classToShow+')').hide();
                $(this).find('tr.' + classToShow).show();
            }
        });

        /* FILTER: trigger filter */
        listElements.trigger("filterSearch", text);
        return this;
    };
})(jQuery);
