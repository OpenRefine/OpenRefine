/*

Copyright 2010,2011. Google Inc.
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

package org.openrefine.grel;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.openrefine.expr.Evaluable;
import org.openrefine.expr.LanguageSpecificParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.grel.Scanner.NumberToken;
import org.openrefine.grel.Scanner.RegexToken;
import org.openrefine.grel.Scanner.Token;
import org.openrefine.grel.Scanner.TokenType;
import org.openrefine.grel.ast.ArrayExpr;
import org.openrefine.grel.ast.ControlCallExpr;
import org.openrefine.grel.ast.FieldAccessorExpr;
import org.openrefine.grel.ast.FunctionCallExpr;
import org.openrefine.grel.ast.GrelExpr;
import org.openrefine.grel.ast.LiteralExpr;
import org.openrefine.grel.ast.OperatorCallExpr;
import org.openrefine.grel.ast.VariableExpr;

public class Parser {
    
    static public LanguageSpecificParser grelParser = new LanguageSpecificParser() {
	        
	        @Override
	        public Evaluable parse(String source, String languagePrefix) throws ParsingException {
	        	Parser parser = new Parser(source, languagePrefix);
	            return parser.getExpression();
	        }
	};
	
    protected Scanner   _scanner;
    protected Token     _token;
    protected GrelExpr  _root;
    protected String    _languagePrefix;

    public Parser(String s, String languagePrefix) throws ParsingException {
        this(s, languagePrefix, 0, s.length());
    }

    public Parser(String s, String languagePrefix, int from, int to) throws ParsingException {
        _scanner = new Scanner(s, from, to);
        _token = _scanner.next(true);
        _languagePrefix = languagePrefix;

        _root = parseExpression();
    }

    public Evaluable getExpression() {
        return new GrelEvaluable(_root, _languagePrefix);
    }

    protected void next(boolean regexPossible) {
        _token = _scanner.next(regexPossible);
    }

    protected ParsingException makeException(String desc) {
        int index = _token != null ? _token.start : _scanner.getIndex();

        return new ParsingException("Parsing error at offset " + index + ": " + desc);
    }

    /**
     *  <expression> := <sub-expression>
     *                | <expression> [ "<" "<=" ">" ">=" "==" "!=" ] <sub-expression>
     */
    protected GrelExpr parseExpression() throws ParsingException {
        GrelExpr sub = parseSubExpression();

        while (_token != null &&
                _token.type == TokenType.Operator &&
                ">=<==!=".indexOf(_token.text) >= 0) {

            String op = _token.text;

            next(true);

            GrelExpr sub2 = parseSubExpression();

            sub = new OperatorCallExpr(new GrelExpr[] { sub, sub2 }, op);
        }

        return sub;
    }

    /**
     *  <sub-expression> := <term>
     *                    | <sub-expression> [ "+" "-" ] <term>
     */
    protected GrelExpr parseSubExpression() throws ParsingException {
        GrelExpr sub = parseTerm();

        while (_token != null &&
                _token.type == TokenType.Operator &&
                "+-".indexOf(_token.text) >= 0) {

            String op = _token.text;

            next(true);

            GrelExpr sub2 = parseTerm();

            sub = new OperatorCallExpr(new GrelExpr[] { sub, sub2 }, op);
        }

        return sub;
    }

    /**
     *  <term> := <factor>
     *          | <term> [ "*" "/" "%" ] <factor>
     */
    protected GrelExpr parseTerm() throws ParsingException {
        GrelExpr factor = parseFactor();

        while (_token != null &&
                _token.type == TokenType.Operator &&
                "*/%".indexOf(_token.text) >= 0) {

            String op = _token.text;

            next(true);

            GrelExpr factor2 = parseFactor();

            factor = new OperatorCallExpr(new GrelExpr[] { factor, factor2 }, op);
        }

        return factor;
    }

    /**
     *  <term> := <term-start> ( <path-segment> )*
     *  <term-start> :=
     *      <string> | <number> | - <number> | <regex> | <identifier> |
     *      <identifier> ( <expression-list> )
     *
     *  <path-segment> := "[" <expression-list> "]"
     *                  | "." <identifier>
     *                  | "." <identifier> "(" <expression-list> ")"
     *
     */
    protected GrelExpr parseFactor() throws ParsingException {
        if (_token == null) {
            throw makeException("Expecting something more at end of expression");
        }

        GrelExpr eval = null;

        if (_token.type == TokenType.String) {
            eval = new LiteralExpr(_token.text);
            next(false);
        } else if (_token.type == TokenType.Regex) {
            RegexToken t = (RegexToken) _token;

            try {
                Pattern pattern = Pattern.compile(_token.text, t.caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
                eval = new LiteralExpr(pattern);
                next(false);
            } catch (Exception e) {
                throw makeException("Bad regular expression (" + e.getMessage() + ")");
            }
        } else if (_token.type == TokenType.Number) {
            eval = new LiteralExpr(((NumberToken)_token).value);
            next(false);
        } else if (_token.type == TokenType.Operator && _token.text.equals("-")) { // unary minus?
            next(true);

            if (_token != null && _token.type == TokenType.Number) {
                Number n = ((NumberToken)_token).value;

                eval = new LiteralExpr(n instanceof Long ? -n.longValue() : -n.doubleValue());

                next(false);
            } else {
                throw makeException("Bad negative number");
            }
        } else if (_token.type == TokenType.Identifier) {
            String text = _token.text;
            next(false);

            if (_token == null || _token.type != TokenType.Delimiter || !_token.text.equals("(")) {
                eval = "null".equals(text) ? new LiteralExpr(null) : new VariableExpr(text);
            } else if( "PI".equals(text) ) {
                eval = new LiteralExpr(Math.PI);
                next(false);
            } else {
                Function f = ControlFunctionRegistry.getFunction(text);
                Control c = ControlFunctionRegistry.getControl(text);
                if (f == null && c == null) {
                    throw makeException("Unknown function or control named " + text);
                }

                next(true); // swallow (

                List<GrelExpr> args = parseExpressionList(")");

                if (c != null) {
                    GrelExpr[] argsA = makeArray(args);
                    String errorMessage = c.checkArguments(argsA);
                    if (errorMessage != null) {
                        throw makeException(errorMessage);
                    }
                    eval = new ControlCallExpr(argsA, c);
                } else {
                    eval = new FunctionCallExpr(makeArray(args), f, text);
                }
            }
        } else if (_token.type == TokenType.Delimiter && _token.text.equals("(")) {
            next(true);

            eval = parseExpression();

            if (_token != null && _token.type == TokenType.Delimiter && _token.text.equals(")")) {
                next(false);
            } else {
                throw makeException("Missing )");
            }
        } else if (_token.type == TokenType.Delimiter && _token.text.equals("[")) { // [ ... ] array
            next(true); // swallow [

            List<GrelExpr> args = parseExpressionList("]");

            eval = new ArrayExpr(makeArray(args));
        } else {
            throw makeException("Missing number, string, identifier, regex, or parenthesized expression");
        }

        while (_token != null) {
            if (_token.type == TokenType.Operator && _token.text.equals(".")) {
                next(false); // swallow .

                if (_token == null || _token.type != TokenType.Identifier) {
                    throw makeException("Missing function name");
                }

                String identifier = _token.text;
                next(false);

                if (_token != null && _token.type == TokenType.Delimiter && _token.text.equals("(")) {
                    next(true); // swallow (

                    Function f = ControlFunctionRegistry.getFunction(identifier);
                    if (f == null) {
                        throw makeException("Unknown function " + identifier);
                    }

                    List<GrelExpr> args = parseExpressionList(")");
                    args.add(0, eval);

                    eval = new FunctionCallExpr(makeArray(args), f, identifier);
                } else {
                    eval = new FieldAccessorExpr(eval, identifier);
                }
            } else if (_token.type == TokenType.Delimiter && _token.text.equals("[")) {
                next(true); // swallow [

                List<GrelExpr> args = parseExpressionList("]");
                args.add(0, eval);

                eval = new FunctionCallExpr(makeArray(args), ControlFunctionRegistry.getFunction("get"), "get");
            } else {
                break;
            }
        }

        return eval;
    }

    /**
     *  <expression-list> := <empty>
     *                     | <expression> ( "," <expression> )*
     *
     */
    protected List<GrelExpr> parseExpressionList(String closingDelimiter) throws ParsingException {
        List<GrelExpr> l = new LinkedList<GrelExpr>();

        if (_token != null &&
            (_token.type != TokenType.Delimiter || !_token.text.equals(closingDelimiter))) {

            while (_token != null) {
                GrelExpr eval = parseExpression();

                l.add(eval);

                if (_token != null && _token.type == TokenType.Delimiter && _token.text.equals(",")) {
                    next(true); // swallow comma, loop back for more
                } else {
                    break;
                }
            }
        }

        if (_token != null && _token.type == TokenType.Delimiter && _token.text.equals(closingDelimiter)) {
            next(false); // swallow closing delimiter
        } else {
            throw makeException("Missing " + closingDelimiter);
        }

        return l;
    }

    protected GrelExpr[] makeArray(List<GrelExpr> l) {
        GrelExpr[] a = new GrelExpr[l.size()];
        l.toArray(a);

        return a;
    }
}
