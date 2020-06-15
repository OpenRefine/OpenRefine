/**
 * Hebrew (עברית) language functions
 */
( function ( $ ) {
	'use strict';

	$.i18n.languages.he = $.extend( {}, $.i18n.languages[ 'default' ], {
		convertGrammar: function ( word, form ) {
			switch ( form ) {
				case 'prefixed':
				case 'תחילית': // the same word in Hebrew
					// Duplicate prefixed "Waw", but only if it's not already double
					if ( word.slice( 0, 1 ) === 'ו' && word.slice( 0, 2 ) !== 'וו' ) {
						word = 'ו' + word;
					}

					// Remove the "He" if prefixed
					if ( word.slice( 0, 1 ) === 'ה' ) {
						word = word.slice( 1 );
					}

					// Add a hyphen (maqaf) before numbers and non-Hebrew letters
					if ( word.slice( 0, 1 ) < 'א' || word.slice( 0, 1 ) > 'ת' ) {
						word = '־' + word;
					}
			}

			return word;
		}
	} );
}( jQuery ) );
