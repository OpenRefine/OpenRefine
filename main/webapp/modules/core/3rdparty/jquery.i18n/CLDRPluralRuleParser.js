/**
 * cldrpluralparser.js
 * A parser engine for CLDR plural rules.
 *
 * Copyright 2012-2014 Santhosh Thottingal and other contributors
 * Released under the MIT license
 * http://opensource.org/licenses/MIT
 *
 * @source https://github.com/santhoshtr/CLDRPluralRuleParser
 * @author Santhosh Thottingal <santhosh.thottingal@gmail.com>
 * @author Timo Tijhof
 * @author Amir Aharoni
 */

/**
 * Evaluates a plural rule in CLDR syntax for a number
 * @param {string} rule
 * @param {integer} number
 * @return {boolean} true if evaluation passed, false if evaluation failed.
 */

// UMD returnExports https://github.com/umdjs/umd/blob/master/returnExports.js
(function(root, factory) {
	if (typeof define === 'function' && define.amd) {
		// AMD. Register as an anonymous module.
		define(factory);
	} else if (typeof exports === 'object') {
		// Node. Does not work with strict CommonJS, but
		// only CommonJS-like environments that support module.exports,
		// like Node.
		module.exports = factory();
	} else {
		// Browser globals (root is window)
		root.pluralRuleParser = factory();
	}
}(this, function() {

function pluralRuleParser(rule, number) {
	'use strict';

	/*
	Syntax: see http://unicode.org/reports/tr35/#Language_Plural_Rules
	-----------------------------------------------------------------
	condition     = and_condition ('or' and_condition)*
		('@integer' samples)?
		('@decimal' samples)?
	and_condition = relation ('and' relation)*
	relation      = is_relation | in_relation | within_relation
	is_relation   = expr 'is' ('not')? value
	in_relation   = expr (('not')? 'in' | '=' | '!=') range_list
	within_relation = expr ('not')? 'within' range_list
	expr          = operand (('mod' | '%') value)?
	operand       = 'n' | 'i' | 'f' | 't' | 'v' | 'w'
	range_list    = (range | value) (',' range_list)*
	value         = digit+
	digit         = 0|1|2|3|4|5|6|7|8|9
	range         = value'..'value
	samples       = sampleRange (',' sampleRange)* (',' ('…'|'...'))?
	sampleRange   = decimalValue '~' decimalValue
	decimalValue  = value ('.' value)?
	*/

	// We don't evaluate the samples section of the rule. Ignore it.
	rule = rule.split('@')[0].replace(/^\s*/, '').replace(/\s*$/, '');

	if (!rule.length) {
		// Empty rule or 'other' rule.
		return true;
	}

	// Indicates the current position in the rule as we parse through it.
	// Shared among all parsing functions below.
	var pos = 0,
		operand,
		expression,
		relation,
		result,
		whitespace = makeRegexParser(/^\s+/),
		value = makeRegexParser(/^\d+/),
		_n_ = makeStringParser('n'),
		_i_ = makeStringParser('i'),
		_f_ = makeStringParser('f'),
		_t_ = makeStringParser('t'),
		_v_ = makeStringParser('v'),
		_w_ = makeStringParser('w'),
		_is_ = makeStringParser('is'),
		_isnot_ = makeStringParser('is not'),
		_isnot_sign_ = makeStringParser('!='),
		_equal_ = makeStringParser('='),
		_mod_ = makeStringParser('mod'),
		_percent_ = makeStringParser('%'),
		_not_ = makeStringParser('not'),
		_in_ = makeStringParser('in'),
		_within_ = makeStringParser('within'),
		_range_ = makeStringParser('..'),
		_comma_ = makeStringParser(','),
		_or_ = makeStringParser('or'),
		_and_ = makeStringParser('and');

	function debug() {
		// console.log.apply(console, arguments);
	}

	debug('pluralRuleParser', rule, number);

	// Try parsers until one works, if none work return null
	function choice(parserSyntax) {
		return function() {
			var i, result;

			for (i = 0; i < parserSyntax.length; i++) {
				result = parserSyntax[i]();

				if (result !== null) {
					return result;
				}
			}

			return null;
		};
	}

	// Try several parserSyntax-es in a row.
	// All must succeed; otherwise, return null.
	// This is the only eager one.
	function sequence(parserSyntax) {
		var i, parserRes,
			originalPos = pos,
			result = [];

		for (i = 0; i < parserSyntax.length; i++) {
			parserRes = parserSyntax[i]();

			if (parserRes === null) {
				pos = originalPos;

				return null;
			}

			result.push(parserRes);
		}

		return result;
	}

	// Run the same parser over and over until it fails.
	// Must succeed a minimum of n times; otherwise, return null.
	function nOrMore(n, p) {
		return function() {
			var originalPos = pos,
				result = [],
				parsed = p();

			while (parsed !== null) {
				result.push(parsed);
				parsed = p();
			}

			if (result.length < n) {
				pos = originalPos;

				return null;
			}

			return result;
		};
	}

	// Helpers - just make parserSyntax out of simpler JS builtin types
	function makeStringParser(s) {
		var len = s.length;

		return function() {
			var result = null;

			if (rule.substr(pos, len) === s) {
				result = s;
				pos += len;
			}

			return result;
		};
	}

	function makeRegexParser(regex) {
		return function() {
			var matches = rule.substr(pos).match(regex);

			if (matches === null) {
				return null;
			}

			pos += matches[0].length;

			return matches[0];
		};
	}

	/**
	 * Integer digits of n.
	 */
	function i() {
		var result = _i_();

		if (result === null) {
			debug(' -- failed i', parseInt(number, 10));

			return result;
		}

		result = parseInt(number, 10);
		debug(' -- passed i ', result);

		return result;
	}

	/**
	 * Absolute value of the source number (integer and decimals).
	 */
	function n() {
		var result = _n_();

		if (result === null) {
			debug(' -- failed n ', number);

			return result;
		}

		result = parseFloat(number, 10);
		debug(' -- passed n ', result);

		return result;
	}

	/**
	 * Visible fractional digits in n, with trailing zeros.
	 */
	function f() {
		var result = _f_();

		if (result === null) {
			debug(' -- failed f ', number);

			return result;
		}

		result = (number + '.').split('.')[1] || 0;
		debug(' -- passed f ', result);

		return result;
	}

	/**
	 * Visible fractional digits in n, without trailing zeros.
	 */
	function t() {
		var result = _t_();

		if (result === null) {
			debug(' -- failed t ', number);

			return result;
		}

		result = (number + '.').split('.')[1].replace(/0$/, '') || 0;
		debug(' -- passed t ', result);

		return result;
	}

	/**
	 * Number of visible fraction digits in n, with trailing zeros.
	 */
	function v() {
		var result = _v_();

		if (result === null) {
			debug(' -- failed v ', number);

			return result;
		}

		result = (number + '.').split('.')[1].length || 0;
		debug(' -- passed v ', result);

		return result;
	}

	/**
	 * Number of visible fraction digits in n, without trailing zeros.
	 */
	function w() {
		var result = _w_();

		if (result === null) {
			debug(' -- failed w ', number);

			return result;
		}

		result = (number + '.').split('.')[1].replace(/0$/, '').length || 0;
		debug(' -- passed w ', result);

		return result;
	}

	// operand       = 'n' | 'i' | 'f' | 't' | 'v' | 'w'
	operand = choice([n, i, f, t, v, w]);

	// expr          = operand (('mod' | '%') value)?
	expression = choice([mod, operand]);

	function mod() {
		var result = sequence(
			[operand, whitespace, choice([_mod_, _percent_]), whitespace, value]
		);

		if (result === null) {
			debug(' -- failed mod');

			return null;
		}

		debug(' -- passed ' + parseInt(result[0], 10) + ' ' + result[2] + ' ' + parseInt(result[4], 10));

		return parseFloat(result[0]) % parseInt(result[4], 10);
	}

	function not() {
		var result = sequence([whitespace, _not_]);

		if (result === null) {
			debug(' -- failed not');

			return null;
		}

		return result[1];
	}

	// is_relation   = expr 'is' ('not')? value
	function is() {
		var result = sequence([expression, whitespace, choice([_is_]), whitespace, value]);

		if (result !== null) {
			debug(' -- passed is : ' + result[0] + ' == ' + parseInt(result[4], 10));

			return result[0] === parseInt(result[4], 10);
		}

		debug(' -- failed is');

		return null;
	}

	// is_relation   = expr 'is' ('not')? value
	function isnot() {
		var result = sequence(
			[expression, whitespace, choice([_isnot_, _isnot_sign_]), whitespace, value]
		);

		if (result !== null) {
			debug(' -- passed isnot: ' + result[0] + ' != ' + parseInt(result[4], 10));

			return result[0] !== parseInt(result[4], 10);
		}

		debug(' -- failed isnot');

		return null;
	}

	function not_in() {
		var i, range_list,
			result = sequence([expression, whitespace, _isnot_sign_, whitespace, rangeList]);

		if (result !== null) {
			debug(' -- passed not_in: ' + result[0] + ' != ' + result[4]);
			range_list = result[4];

			for (i = 0; i < range_list.length; i++) {
				if (parseInt(range_list[i], 10) === parseInt(result[0], 10)) {
					return false;
				}
			}

			return true;
		}

		debug(' -- failed not_in');

		return null;
	}

	// range_list    = (range | value) (',' range_list)*
	function rangeList() {
		var result = sequence([choice([range, value]), nOrMore(0, rangeTail)]),
			resultList = [];

		if (result !== null) {
			resultList = resultList.concat(result[0]);

			if (result[1][0]) {
				resultList = resultList.concat(result[1][0]);
			}

			return resultList;
		}

		debug(' -- failed rangeList');

		return null;
	}

	function rangeTail() {
		// ',' range_list
		var result = sequence([_comma_, rangeList]);

		if (result !== null) {
			return result[1];
		}

		debug(' -- failed rangeTail');

		return null;
	}

	// range         = value'..'value
	function range() {
		var i, array, left, right,
			result = sequence([value, _range_, value]);

		if (result !== null) {
			debug(' -- passed range');

			array = [];
			left = parseInt(result[0], 10);
			right = parseInt(result[2], 10);

			for (i = left; i <= right; i++) {
				array.push(i);
			}

			return array;
		}

		debug(' -- failed range');

		return null;
	}

	function _in() {
		var result, range_list, i;

		// in_relation   = expr ('not')? 'in' range_list
		result = sequence(
			[expression, nOrMore(0, not), whitespace, choice([_in_, _equal_]), whitespace, rangeList]
		);

		if (result !== null) {
			debug(' -- passed _in:' + result);

			range_list = result[5];

			for (i = 0; i < range_list.length; i++) {
				if (parseInt(range_list[i], 10) === parseFloat(result[0])) {
					return (result[1][0] !== 'not');
				}
			}

			return (result[1][0] === 'not');
		}

		debug(' -- failed _in ');

		return null;
	}

	/**
	 * The difference between "in" and "within" is that
	 * "in" only includes integers in the specified range,
	 * while "within" includes all values.
	 */
	function within() {
		var range_list, result;

		// within_relation = expr ('not')? 'within' range_list
		result = sequence(
			[expression, nOrMore(0, not), whitespace, _within_, whitespace, rangeList]
		);

		if (result !== null) {
			debug(' -- passed within');

			range_list = result[5];

			if ((result[0] >= parseInt(range_list[0], 10)) &&
				(result[0] < parseInt(range_list[range_list.length - 1], 10))) {

				return (result[1][0] !== 'not');
			}

			return (result[1][0] === 'not');
		}

		debug(' -- failed within ');

		return null;
	}

	// relation      = is_relation | in_relation | within_relation
	relation = choice([is, not_in, isnot, _in, within]);

	// and_condition = relation ('and' relation)*
	function and() {
		var i,
			result = sequence([relation, nOrMore(0, andTail)]);

		if (result) {
			if (!result[0]) {
				return false;
			}

			for (i = 0; i < result[1].length; i++) {
				if (!result[1][i]) {
					return false;
				}
			}

			return true;
		}

		debug(' -- failed and');

		return null;
	}

	// ('and' relation)*
	function andTail() {
		var result = sequence([whitespace, _and_, whitespace, relation]);

		if (result !== null) {
			debug(' -- passed andTail' + result);

			return result[3];
		}

		debug(' -- failed andTail');

		return null;

	}
	//  ('or' and_condition)*
	function orTail() {
		var result = sequence([whitespace, _or_, whitespace, and]);

		if (result !== null) {
			debug(' -- passed orTail: ' + result[3]);

			return result[3];
		}

		debug(' -- failed orTail');

		return null;
	}

	// condition     = and_condition ('or' and_condition)*
	function condition() {
		var i,
			result = sequence([and, nOrMore(0, orTail)]);

		if (result) {
			for (i = 0; i < result[1].length; i++) {
				if (result[1][i]) {
					return true;
				}
			}

			return result[0];
		}

		return false;
	}

	result = condition();

	/**
	 * For success, the pos must have gotten to the end of the rule
	 * and returned a non-null.
	 * n.b. This is part of language infrastructure,
	 * so we do not throw an internationalizable message.
	 */
	if (result === null) {
		throw new Error('Parse error at position ' + pos.toString() + ' for rule: ' + rule);
	}

	if (pos !== rule.length) {
		debug('Warning: Rule not parsed completely. Parser stopped at ' + rule.substr(0, pos) + ' for rule: ' + rule);
	}

	return result;
}

return pluralRuleParser;

}));
