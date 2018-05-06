
/**
 * Adds a few tweaks to a suggest widget, mostly to indicate
 * the status of the validation.
 */
var fixSuggestInput = function(input) {
   input.bind("fb-select", function(evt, data) {
       input.addClass('wbs-validated-input');
       input.blur();
   }).bind("fb-textchange", function(evt, data) {
       input.removeClass('wbs-validated-input');
   }).blur(function() {
       setTimeout(function() {
        if(! input.hasClass('wbs-validated-input')) {
            input.addClass('wbs-unvalidated-input');
        }
       }, 100);
   }).focus(function() {
       input.removeClass('wbs-unvalidated-input');
   });
}

