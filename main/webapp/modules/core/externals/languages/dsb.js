/**
 * Lower Sorbian (Dolnoserbski) language functions
 */
( function ( $ ) {
	'use strict';

	$.i18n.languages.dsb = $.extend( {}, $.i18n.languages[ 'default' ], {
		convertGrammar: function ( word, form ) {
			switch ( form ) {
				case 'instrumental': // instrumental
					word = 'z ' + word;
					break;
				case 'lokatiw': // lokatiw
					word = 'wo ' + word;
					break;
			}

			return word;
		}
	} );

}( jQuery ) );
