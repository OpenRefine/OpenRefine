package com.metaweb.gridworks.gel;

public class Scanner {
    static public enum TokenType {
        Error,
        Delimiter,
        Operator,
        Identifier,
        Number,
        String,
        Regex
    }
    
    static public class Token {
        final public int        start;
        final public int        end;
        final public TokenType  type;
        final public String     text;
        
        Token(int start, int end, TokenType type, String text) {
            this.start = start;
            this.end = end;
            this.type = type;
            this.text = text;
        }
    }
    
    static public class ErrorToken extends Token {
        final public String detail; // error detail
        
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
    
    static public class RegexToken extends Token {
        final public boolean caseInsensitive;
        
        public RegexToken(int start, int end, String text, boolean caseInsensitive) {
            super(start, end, TokenType.Regex, text);
            this.caseInsensitive = caseInsensitive;
        }
    }
    
    protected String     _text;  // input text to tokenize
    protected int        _index; // index of the next character to process
    protected int        _limit; // process up to this index
    
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
    
    /**
     * The regexPossible flag is used by the parser to hint the scanner what to do
     * when it encounters a slash. Since the divide operator / and the opening 
     * delimiter of a regex literal are the same, but divide operators and regex
     * literals can't occur at the same place in an expression, this flag is a cheap
     * way to distinguish the two without having to look ahead.
     * 
     * @param regexPossible
     * @return
     */
    public Token next(boolean regexPossible) {
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
            
        } else if (Character.isLetter(c) || c == '_') { // identifier
            while (_index < _limit) {
            	char c1 = _text.charAt(_index);
            	if (c1 == '_' || Character.isLetterOrDigit(c1)) {
            		_index++;
            	} else {
            		break;
            	}
            }
            
            return new Token(
                start, 
                _index, 
                TokenType.Identifier, 
                _text.substring(start, _index)
            );
        } else if (c == '/' && regexPossible) {
            /*
             *  Regex literal
             */
            StringBuffer sb = new StringBuffer();
            
            _index++; // skip opening delimiter
            
            while (_index < _limit) {
                c = _text.charAt(_index);
                if (c == '/') {
                    _index++; // skip closing delimiter
                    
                    boolean caseInsensitive = false;
                    if (_index < _limit && _text.charAt(_index) == 'i') {
                        caseInsensitive = true;
                        _index++;
                    }
                    
                    return new RegexToken(
                        start, 
                        _index, 
                        sb.toString(),
                        caseInsensitive
                    );
                } else if (c == '\\') {
                    sb.append(c);
                    
                    _index++; // skip escaping marker
                    if (_index < _limit) {
                        sb.append(_text.charAt(_index));
                    }
                } else {
                    sb.append(c);
                }
                _index++;
            }
            
            detail = "Regex not properly closed";
            // fall through
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
