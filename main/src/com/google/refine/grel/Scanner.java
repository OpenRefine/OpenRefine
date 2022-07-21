/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.grel;

public class Scanner {

    static public enum TokenType {
        Error, Delimiter, Operator, Identifier, Number, String, Regex
    }

    static public class Token {

        final public int start;
        final public int end;
        final public TokenType type;
        final public String text;

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

        final public Number value;

        public NumberToken(int start, int end, String text, Number value) {
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

    protected String _text; // input text to tokenize
    protected int _index; // index of the next character to process
    protected int _limit; // process up to this index

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
     * The regexPossible flag is used by the parser to hint the scanner what to do when it encounters a slash. Since the
     * divide operator / and the opening delimiter of a regex literal are the same, but divide operators and regex
     * literals can't occur at the same place in an expression, this flag is a cheap way to distinguish the two without
     * having to look ahead.
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
            long value = 0;

            while (_index < _limit && Character.isDigit(c = _text.charAt(_index))) {
                value = value * 10 + (c - '0');
                _index++;
            }

            if (_index < _limit && (c == '.' || c == 'e')) {
                double value2 = value;
                if (c == '.') {
                    _index++;

                    double division = 1;
                    while (_index < _limit && Character.isDigit(c = _text.charAt(_index))) {
                        value2 = value2 * 10 + (c - '0');
                        division *= 10;
                        _index++;
                    }

                    value2 /= division;
                }

                // TODO: support exponent e notation

                return new NumberToken(
                        start,
                        _index,
                        _text.substring(start, _index),
                        value2);
            } else {
                return new NumberToken(
                        start,
                        _index,
                        _text.substring(start, _index),
                        value);
            }
        } else if (c == '"' || c == '\'') {
            /*
             * String Literal
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
                            sb.toString());
                } else if (c == '\\') {
                    _index++; // skip escaping marker
                    if (_index < _limit) {
                        char c2 = _text.charAt(_index);
                        if (c2 == 't') {
                            sb.append('\t');
                        } else if (c2 == 'n') {
                            sb.append('\n');
                        } else if (c2 == 'r') {
                            sb.append('\r');
                        } else if (c2 == '\\') {
                            sb.append('\\');
                        } else {
                            sb.append(c2);
                        }
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
                    _text.substring(start, _index));
        } else if (c == '/' && regexPossible) {
            /*
             * Regex literal
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
                            caseInsensitive);
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
        } else if ("+-*/.%".indexOf(c) >= 0) { // operator
            _index++;

            return new Token(
                    start,
                    _index,
                    TokenType.Operator,
                    _text.substring(start, _index));
        } else if ("()[],".indexOf(c) >= 0) { // delimiter
            _index++;

            return new Token(
                    start,
                    _index,
                    TokenType.Delimiter,
                    _text.substring(start, _index));
        } else if (c == '!' && _index < _limit - 1 && _text.charAt(_index + 1) == '=') {
            _index += 2;
            return new Token(
                    start,
                    _index,
                    TokenType.Operator,
                    _text.substring(start, _index));
        } else if (c == '<') {
            if (_index < _limit - 1 &&
                    (_text.charAt(_index + 1) == '=' ||
                            _text.charAt(_index + 1) == '>')) {

                _index += 2;
                return new Token(
                        start,
                        _index,
                        TokenType.Operator,
                        _text.substring(start, _index));
            } else {
                _index++;
                return new Token(
                        start,
                        _index,
                        TokenType.Operator,
                        _text.substring(start, _index));
            }
        } else if (">=".indexOf(c) >= 0) { // operator
            if (_index < _limit - 1 && _text.charAt(_index + 1) == '=') {
                _index += 2;
                return new Token(
                        start,
                        _index,
                        TokenType.Operator,
                        _text.substring(start, _index));
            } else {
                _index++;
                return new Token(
                        start,
                        _index,
                        TokenType.Operator,
                        _text.substring(start, _index));
            }
        } else {
            _index++;
            detail = "Unrecognized symbol";
        }

        return new ErrorToken(
                start,
                _index,
                _text.substring(start, _index),
                detail);
    }
}
