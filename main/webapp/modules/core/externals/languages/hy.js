/**
 * Armenian (Հայերեն) language functions
 */

( function ( $ ) {
	'use strict';

	$.i18n.languages.hy = $.extend( {}, $.i18n.languages['default'], {
		convertGrammar: function ( word, form ) {
			if ( form === 'genitive' ) { // սեռական հոլով
				if ( word.substr( -1 ) === 'ա' ) {
					word = word.substr( 0, word.length - 1 ) + 'այի';
				} else if ( word.substr( -1 ) === 'ո' ) {
					word = word.substr( 0, word.length - 1 ) + 'ոյի';
				} else if ( word.substr( -4 ) === 'գիրք' ) {
					word = word.substr( 0, word.length - 4 ) + 'գրքի';
				} else {
					word = word + 'ի';
				}
			}

			return word;
		}
	} );
}( jQuery ) );
