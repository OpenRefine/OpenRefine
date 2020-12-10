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
            try{
                listElement.trigger("filterSearch", text);
            }
            catch(e)
            {
                console.log(e)
            }

            // $(settings.tagSelector).removeClass('current');
            // $(this).addClass('current');
            /* FILTER: select a text and filter */
            listElement.bind("filterSearch", function( e, text ) {
                // get each project row
                listElement.each( function () {
                    $(this).removeClass(settings.selectedTagClass);
                    var row = this
                    // console.log("initial row", row)

                    // get each column of the metadata
                    var columns = $(this).find('td')
                    columns.each( function () {
                        // text inside the column
                        var textInColumn = this.textContent
                        // if the text in input is in the textcontent, row will be shown
                        if(textInColumn.contains(text))
                        {
                            // console.log("contains text")

                            if(!$(row).hasClass(settings.selectedTagClass))
                            {
                                // console.log("adding class",settings.selectedTagClass)
                                $(row).addClass(settings.selectedTagClass);
                            }
                        }
                    });
                    // console.log("final row:" , row)
                });
                console.log("final list element", listElement)
                $(this).trigger("filterSearchList",[ settings.selectedTagClass ]);
            });

            /* FILTERPORTFOLIO: pass in a class to show, all others will be hidden */
            listElement.bind("filterSearchList", function( e, classToShow ) {
                if( text == ''){
                        $(this).trigger("showSearch");
                }else{
                        $(this).trigger("showSearch", [ '.' + classToShow ] );
                        $(this).trigger("hideSearch", [ ':not(.' + classToShow + ')' ] );
                }
            });

            /* SHOW: show a single class*/
            $(this).bind("showSearch", function( e, selectorToShow ){
                    $("#tableBody").children(selectorToShow).animate(settings.show, settings.animationSpeed);
            });

            /* SHOW: hide a single class*/
            $(this).bind("hideSearch", function( e, selectorToHide ){
                    $("#tableBody").children(selectorToHide).animate(settings.hide, settings.animationSpeed * 0.6);
            });

            return this;
        };
})(jQuery);