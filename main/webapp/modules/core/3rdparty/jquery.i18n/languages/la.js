/**
 * Latin (lingua Latina) language functions
 *
 * @author Santhosh Thottingal
 */

( function ( $ ) {
	'use strict';

	$.i18n.languages.la = $.extend( {}, $.i18n.languages[ 'default' ], {
		convertGrammar: function ( word, form ) {
			switch ( form ) {
				case 'genitive':
				// only a few declensions, and even for those mostly the singular only
					word = word.replace( /u[ms]$/i, 'i' ); // 2nd declension singular
					word = word.replace( /ommunia$/i, 'ommunium' ); // 3rd declension neuter plural (partly)
					word = word.replace( /a$/i, 'ae' ); // 1st declension singular
					word = word.replace( /libri$/i, 'librorum' ); // 2nd declension plural (partly)
					word = word.replace( /nuntii$/i, 'nuntiorum' ); // 2nd declension plural (partly)
					word = word.replace( /tio$/i, 'tionis' ); // 3rd declension singular (partly)
					word = word.replace( /ns$/i, 'ntis' );
					word = word.replace( /as$/i, 'atis' );
					word = word.replace( /es$/i, 'ei' ); // 5th declension singular
					break;
				case 'accusative':
				// only a few declensions, and even for those mostly the singular only
					word = word.replace( /u[ms]$/i, 'um' ); // 2nd declension singular
					word = word.replace( /ommunia$/i, 'am' ); // 3rd declension neuter plural (partly)
					word = word.replace( /a$/i, 'ommunia' ); // 1st declension singular
					word = word.replace( /libri$/i, 'libros' ); // 2nd declension plural (partly)
					word = word.replace( /nuntii$/i, 'nuntios' );// 2nd declension plural (partly)
					word = word.replace( /tio$/i, 'tionem' ); // 3rd declension singular (partly)
					word = word.replace( /ns$/i, 'ntem' );
					word = word.replace( /as$/i, 'atem' );
					word = word.replace( /es$/i, 'em' ); // 5th declension singular
					break;
				case 'ablative':
				// only a few declensions, and even for those mostly the singular only
					word = word.replace( /u[ms]$/i, 'o' ); // 2nd declension singular
					word = word.replace( /ommunia$/i, 'ommunibus' ); // 3rd declension neuter plural (partly)
					word = word.replace( /a$/i, 'a' ); // 1st declension singular
					word = word.replace( /libri$/i, 'libris' ); // 2nd declension plural (partly)
					word = word.replace( /nuntii$/i, 'nuntiis' ); // 2nd declension plural (partly)
					word = word.replace( /tio$/i, 'tione' ); // 3rd declension singular (partly)
					word = word.replace( /ns$/i, 'nte' );
					word = word.replace( /as$/i, 'ate' );
					word = word.replace( /es$/i, 'e' ); // 5th declension singular
					break;
			}

			return word;
		}
	} );
}( jQuery ) );
