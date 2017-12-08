/*
* Copyright (C) 2009 Joel Sutherland.
* Liscenced under the MIT liscense
*/

(function($) {
        $.fn.filterList = function(settings) {
                settings = $.extend(
                        {
                                animationSpeed: 500,
                                show: { height: 'show', opacity: 'show' },
                                hide: { height: 'hide', opacity: 'hide' },
                                useTags: true,
                                tagSelector: '#projectTags a',
                                selectedTagClass: 'current',
                                allTag: 'all'
                        }, settings);
                
                var listElement = $(this);
                
                /* FILTER: select a tag and filter */
                listElement.bind("filter", function( e, tagToShow ){
                        if(settings.useTags){
                                $(settings.tagSelector).removeClass(settings.selectedTagClass);
                                $(settings.tagSelector + '[href=' + tagToShow + ']').addClass(settings.selectedTagClass);
                        }
                        $(this).trigger("filterMyList", [ tagToShow.substr(1) ]);
                });
        
                /* FILTERPORTFOLIO: pass in a class to show, all others will be hidden */
                listElement.bind("filterMyList", function( e, classToShow ){
                        if(classToShow === settings.allTag){
                                $(this).trigger("show");
                        }else{
                                $(this).trigger("show", [ '.' + classToShow ] );
                                $(this).trigger("hide", [ ':not(.' + classToShow + ')' ] );
                        }
                });
                
                /* SHOW: show a single class*/
                $(this).bind("show", function( e, selectorToShow ){
                        listElement.children(selectorToShow).animate(settings.show, settings.animationSpeed);
                });
                
                /* SHOW: hide a single class*/
                $(this).bind("hide", function( e, selectorToHide ){
                        listElement.children(selectorToHide).animate(settings.hide, settings.animationSpeed * 0.6);     
                });
                
                /* ============ Setup Tags ====================*/
                if(settings.useTags){
                        $(settings.tagSelector).on("click", function(){
                                listElement.trigger("filter", [ $(this).attr('href') ]);
                                
                                $(settings.tagSelector).removeClass('current');
                                $(this).addClass('current');
                        });
                }
                
                return this;
        };
})(jQuery);