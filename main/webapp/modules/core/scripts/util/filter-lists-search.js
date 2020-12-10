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
                    selectedTagClass: 'current',
            }
            var listElements = $(this);
            /* FILTER: trigger filter */
            listElements.trigger("filterSearch", text);
            /* FILTER: select a text and filter */
            listElements.bind("filterSearch", function( e, text ) {
                // get each project row
                listElements.each( function () {
                    $(this).removeClass(settings.selectedTagClass);
                    var row = this
                    // get each column of the metadata
                    var columns = $(this).find('td')
                    columns.each( function () {
                        // text inside the column
                        var textInColumn = this.textContent
                        // if the text in input is in the text of the column, row will be shown
                        if(textInColumn.contains(text))
                        {
                            if(!$(row).hasClass(settings.selectedTagClass))
                            {
                                $(row).addClass(settings.selectedTagClass);
                                return true;
                            }
                        }
                    });
                });
                $(this).trigger("filterSearchList",[ settings.selectedTagClass ]);
            });

            /* FILTERPORTFOLIO: pass in a class to show, all others will be hidden */
            listElements.bind("filterSearchList", function( e, classToShow ) {
                if( text == ''){
                        return
                        // $(this).trigger("showSearch");
                }else{
                        $(this).trigger("showSearch", [ '.' + classToShow ] );
                        $(this).trigger("hideSearch", [ ':not(.' + classToShow + ')' ] );
                }
            });

            /* SHOW: show a single class*/
            $(this).bind("showSearch", function( e, selectorToShow ){
                    $("#tableBody").children(selectorToShow).show()
            });

            /* SHOW: hide a single class*/
            $(this).bind("hideSearch", function( e, selectorToHide ){
                    $("#tableBody").children(selectorToHide).hide()
            });

            return this;
        };
})(jQuery);
