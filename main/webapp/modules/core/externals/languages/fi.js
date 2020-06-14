/**
 * Finnish (Suomi) language functions
 *
 * @author Santhosh Thottingal
 */

( function ( $ ) {
	'use strict';

	$.i18n.languages.fi = $.extend( {}, $.i18n.languages[ 'default' ], {
		convertGrammar: function ( word, form ) {
			// vowel harmony flag
			var aou = word.match( /[aou][^äöy]*$/i ),
				origWord = word;
			if ( word.match( /wiki$/i ) ) {
				aou = false;
			}

			// append i after final consonant
			if ( word.match( /[bcdfghjklmnpqrstvwxz]$/i ) ) {
				word += 'i';
			}

			switch ( form ) {
				case 'genitive':
					word += 'n';
					break;
				case 'elative':
					word += ( aou ? 'sta' : 'stä' );
					break;
				case 'partitive':
					word += ( aou ? 'a' : 'ä' );
					break;
				case 'illative':
				// Double the last letter and add 'n'
					word += word.slice( -1 ) + 'n';
					break;
				case 'inessive':
					word += ( aou ? 'ssa' : 'ssä' );
					break;
				default:
					word = origWord;
					break;
			}

			return word;
		}
	} );
}( jQuery ) );
