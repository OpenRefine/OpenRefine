
/**
 * Adds a few tweaks to a suggest widget, mostly to indicate
 * the status of the validation.
 */
var fixSuggestInput = function(input) {
   input.on("fb-select", function(evt, data) {
       input.addClass('wbs-validated-input');
       input.trigger('blur');
   }).on("fb-textchange", function(evt, data) {
       input.removeClass('wbs-validated-input');
   }).on('blur',function() {
       setTimeout(function() {
        if(! input.hasClass('wbs-validated-input')) {
            input.addClass('wbs-unvalidated-input');
        }
       }, 100);
   }).on('focus',function() {
       input.removeClass('wbs-unvalidated-input');
   });
}

