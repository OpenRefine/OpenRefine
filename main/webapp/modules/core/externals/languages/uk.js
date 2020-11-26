/**
 * Ukrainian (Українська) language functions
 */

( function ( $ ) {
	'use strict';

	$.i18n.languages.uk = $.extend( {}, $.i18n.languages[ 'default' ], {
		convertGrammar: function ( word, form ) {
			switch ( form ) {
				case 'genitive': // родовий відмінок
					if ( word.slice( -1 ) === 'ь' ) {
						word = word.slice( 0, -1 ) + 'я';
					} else if ( word.slice( -2 ) === 'ія' ) {
						word = word.slice( 0, -2 ) + 'ії';
					} else if ( word.slice( -2 ) === 'ка' ) {
						word = word.slice( 0, -2 ) + 'ки';
					} else if ( word.slice( -2 ) === 'ти' ) {
						word = word.slice( 0, -2 ) + 'тей';
					} else if ( word.slice( -2 ) === 'ды' ) {
						word = word.slice( 0, -2 ) + 'дов';
					} else if ( word.slice( -3 ) === 'ник' ) {
						word = word.slice( 0, -3 ) + 'ника';
					}

					break;
				case 'accusative': // знахідний відмінок
					if ( word.slice( -2 ) === 'ія' ) {
						word = word.slice( 0, -2 ) + 'ію';
					}

					break;
			}

			return word;
		}
	} );

}( jQuery ) );
