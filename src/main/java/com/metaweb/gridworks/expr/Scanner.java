package com.metaweb.gridworks.expr;

public class Scanner {
	static public enum TokenType {
		Error,
		Delimiter,
		Operator,
		Identifier,
		Number,
		String
	}
	
	static public class Token {
		final public int		start;
		final public int		end;
		final public TokenType	type;
		final public String		text;
		
		Token(int start, int end, TokenType type, String text) {
			this.start = start;
			this.end = end;
			this.type = type;
			this.text = text;
		}
	}
	
	static public class ErrorToken extends Token {
		final public String		detail; // error detail
		
		public ErrorToken(int start, int end, String text, String detail) {
			super(start, end, TokenType.Error, text);
			this.detail = detail;
		}
	}
	
	static public class NumberToken extends Token {
		final public double value;
		
		public NumberToken(int start, int end, String text, double value) {
			super(start, end, TokenType.Number, text);
			this.value = value;
		}
	}
	
	protected String 	_text;
	protected int		_index;
	protected int		_limit;
	
	public Scanner(String s) {
		this(s, 0, s.length());
	}
	
	public Scanner(String s, int from, int to) {
		_text = s;
		_index = from;
		_limit = to;
	}
	
	public int getIndex() {
		return _index;
	}
	
	public Token next() {
		// skip whitespace
		while (_index < _limit && Character.isWhitespace(_text.charAt(_index))) {
			_index++;
		}
		if (_index == _limit) {
			return null;
		}
		
		char c = _text.charAt(_index);
		int start = _index;
		String detail = null;
		
		if (Character.isDigit(c)) { // number literal
			double value = 0;
			
			while (_index < _limit && Character.isDigit(c = _text.charAt(_index))) {
				value = value * 10 + (c - '0');
				_index++;
			}
			
			if (_index < _limit && c == '.') {
				_index++;
				
				double division = 1;
				while (_index < _limit && Character.isDigit(c = _text.charAt(_index))) {
					value = value * 10 + (c - '0');
					division *= 10;
					_index++;
				}
				
				value /= division;
			}
			
			// TODO: support exponent e notation
			
			return new NumberToken(
				start, 
				_index, 
				_text.substring(start, _index),
				value
			);
		} else if (c == '"' || c == '\'') { 
			/*
			 *  String Literal
			 */
			
			StringBuffer sb = new StringBuffer();
			char delimiter = c;
			
			_index++; // skip opening delimiter
			
			while (_index < _limit) {
				c = _text.charAt(_index);
				if (c == delimiter) {
					_index++; // skip closing delimiter
					
					return new Token(
						start, 
						_index, 
						TokenType.String, 
						sb.toString()
					);
				} else if (c == '\\') {
					_index++; // skip escaping marker
					if (_index < _limit) {
						sb.append(_text.charAt(_index));
					}
				} else {
					sb.append(c);
				}
				_index++;
			}
			
			detail = "String not properly closed";
			// fall through
			
		} else if (Character.isLetter(c)) { // identifier
			while (_index < _limit && Character.isLetterOrDigit(_text.charAt(_index))) {
				_index++;
			}
			
			return new Token(
				start, 
				_index, 
				TokenType.Identifier, 
				_text.substring(start, _index)
			);
		} else if ("+-*/.".indexOf(c) >= 0) { // operator
			_index++;
			
			return new Token(
				start, 
				_index, 
				TokenType.Operator, 
				_text.substring(start, _index)
			);
		} else if ("()[],".indexOf(c) >= 0) { // delimiter
			_index++;
			
			return new Token(
				start, 
				_index, 
				TokenType.Delimiter, 
				_text.substring(start, _index)
			);
		} else if (c == '!' && _index < _limit - 1 && _text.charAt(_index + 1) == '=') {
			_index += 2;
			return new Token(
				start, 
				_index, 
				TokenType.Operator, 
				_text.substring(start, _index)
			);
		} else if (c == '<') {
			if (_index < _limit - 1 && 
					(_text.charAt(_index + 1) == '=' || 
					 _text.charAt(_index + 1) == '>')) {
				
				_index += 2;
				return new Token(
					start, 
					_index, 
					TokenType.Operator, 
					_text.substring(start, _index)
				);
			} else {
				_index++;
				return new Token(
					start, 
					_index, 
					TokenType.Operator, 
					_text.substring(start, _index)
				);
			}
		} else if (">=".indexOf(c) >= 0) { // operator
			if (_index < _limit - 1 && _text.charAt(_index + 1) == '=') {
				_index += 2;
				return new Token(
					start, 
					_index, 
					TokenType.Operator, 
					_text.substring(start, _index)
				);
			} else {
				_index++;
				return new Token(
					start, 
					_index, 
					TokenType.Operator, 
					_text.substring(start, _index)
				);
			}
		} else {
			_index++;
			detail = "Unrecognized symbol";
		}
		
		return new ErrorToken(
			start, 
			_index, 
			_text.substring(start, _index),
			detail
		);
	}
}
