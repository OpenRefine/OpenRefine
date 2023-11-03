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

package com.google.refine.grel;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ParsingException;
import com.google.refine.expr.functions.arrays.ArgsToArray;
import com.google.refine.grel.Scanner.NumberToken;
import com.google.refine.grel.Scanner.RegexToken;
import com.google.refine.grel.Scanner.Token;
import com.google.refine.grel.Scanner.TokenType;
import com.google.refine.grel.ast.ControlCallExpr;
import com.google.refine.grel.ast.FieldAccessorExpr;
import com.google.refine.grel.ast.FunctionCallExpr;
import com.google.refine.grel.ast.LiteralExpr;
import com.google.refine.grel.ast.OperatorCallExpr;
import com.google.refine.grel.ast.VariableExpr;

public class Parser {

    protected Scanner _scanner;
    protected Token _token;
    protected Evaluable _root;

    public Parser(String s) throws ParsingException {
        this(s, 0, s.length());
    }

    public Parser(String s, int from, int to) throws ParsingException {
        _scanner = new Scanner(s, from, to);
        _token = _scanner.next(true);

        _root = parseExpression();
    }

    public Evaluable getExpression() {
        return _root;
    }

    protected void next(boolean regexPossible) {
        _token = _scanner.next(regexPossible);
    }

    protected ParsingException makeException(String desc) {
        int index = _token != null ? _token.start : _scanner.getIndex();

        return new ParsingException("Parsing error at offset " + index + ": " + desc);
    }

    /*
     * <expression> := <sub-expression> | <expression> [ "<" "<=" ">" ">=" "==" "!=" ] <sub-expression>
     */
    protected Evaluable parseExpression() throws ParsingException {
        Evaluable sub = parseSubExpression();

        while (_token != null &&
                _token.type == TokenType.Operator &&
                ">=<==!=".indexOf(_token.text) >= 0) {

            String op = _token.text;

            next(true);

            Evaluable sub2 = parseSubExpression();

            sub = new OperatorCallExpr(new Evaluable[] { sub, sub2 }, op);
        }

        return sub;
    }

    /*
     * <sub-expression> := <term> | <sub-expression> [ "+" "-" ] <term>
     */
    protected Evaluable parseSubExpression() throws ParsingException {
        Evaluable sub = parseTerm();

        while (_token != null &&
                _token.type == TokenType.Operator &&
                "+-".indexOf(_token.text) >= 0) {

            String op = _token.text;

            next(true);

            Evaluable sub2 = parseTerm();

            sub = new OperatorCallExpr(new Evaluable[] { sub, sub2 }, op);
        }

        return sub;
    }

    /*
     * <term> := <factor> | <term> [ "*" "/" "%" ] <factor>
     */
    protected Evaluable parseTerm() throws ParsingException {
        Evaluable factor = parseFactor();

        while (_token != null &&
                _token.type == TokenType.Operator &&
                "*/%".indexOf(_token.text) >= 0) {

            String op = _token.text;

            next(true);

            Evaluable factor2 = parseFactor();

            factor = new OperatorCallExpr(new Evaluable[] { factor, factor2 }, op);
        }

        return factor;
    }

    /*
     * <term> := <term-start> ( <path-segment> )* <term-start> := <string> | <number> | - <number> | <regex> |
     * <identifier> | <identifier> ( <expression-list> )
     *
     * <path-segment> := "[" <expression-list> "]" | "." <identifier> | "." <identifier> "(" <expression-list> ")"
     *
     */
    protected Evaluable parseFactor() throws ParsingException {
        if (_token == null) {
            throw makeException("Expecting something more at end of expression");
        }

        Evaluable eval = null;

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
            eval = new LiteralExpr(((NumberToken) _token).value);
            next(false);
        } else if (_token.type == TokenType.Operator && _token.text.equals("-")) { // unary minus?
            next(true);

            if (_token != null && _token.type == TokenType.Number) {
                Number n = ((NumberToken) _token).value;

                if (n instanceof Long) {
                    eval = new LiteralExpr(-n.longValue());
                } else {
                    eval = new LiteralExpr(-n.doubleValue());
                }

                next(false);
            } else {
                throw makeException("Bad negative number");
            }
        } else if (_token.type == TokenType.Identifier) {
            String text = _token.text;
            next(false);

            if (_token == null || _token.type != TokenType.Delimiter || !_token.text.equals("(")) {
                eval = "null".equals(text) ? new LiteralExpr(null) : new VariableExpr(text);
            } else if ("PI".equals(text)) {
                eval = new LiteralExpr(Math.PI);
                next(false);
            } else {
                Function f = ControlFunctionRegistry.getFunction(text);
                Control c = ControlFunctionRegistry.getControl(text);
                if (f == null && c == null) {
                    throw makeException("Unknown function or control named " + text);
                }

                next(true); // swallow (

                List<Evaluable> args = parseExpressionList(")");

                if (c != null) {
                    Evaluable[] argsA = makeArray(args);
                    String errorMessage = c.checkArguments(argsA);
                    if (errorMessage != null) {
                        throw makeException(errorMessage);
                    }
                    eval = new ControlCallExpr(argsA, c);
                } else {
                    eval = new FunctionCallExpr(makeArray(args), f);
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

            List<Evaluable> args = parseExpressionList("]");

            eval = new FunctionCallExpr(makeArray(args), new ArgsToArray());
        } else {
            throw makeException("Missing number, string, identifier, regex, or parenthesized expression");
        }

        while (_token != null) {
            if (_token.type == TokenType.Error) {
                throw makeException("Unknown function or control named" + _token.text);
            } else if (_token.type == TokenType.Operator && _token.text.equals(".")) {
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

                    List<Evaluable> args = parseExpressionList(")");
                    args.add(0, eval);

                    eval = new FunctionCallExpr(makeArray(args), f);
                } else {
                    eval = new FieldAccessorExpr(eval, identifier);
                }
            } else if (_token.type == TokenType.Delimiter && _token.text.equals("[")) {
                next(true); // swallow [

                List<Evaluable> args = parseExpressionList("]");
                args.add(0, eval);

                eval = new FunctionCallExpr(makeArray(args), ControlFunctionRegistry.getFunction("get"));
            } else {
                break;
            }
        }

        return eval;
    }

    /*
     * <expression-list> := <empty> | <expression> ( "," <expression> )*
     *
     */
    protected List<Evaluable> parseExpressionList(String closingDelimiter) throws ParsingException {
        List<Evaluable> l = new LinkedList<Evaluable>();

        if (_token != null &&
                (_token.type != TokenType.Delimiter || !_token.text.equals(closingDelimiter))) {

            while (_token != null) {
                Evaluable eval = parseExpression();

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

    protected Evaluable[] makeArray(List<Evaluable> l) {
        Evaluable[] a = new Evaluable[l.size()];
        l.toArray(a);

        return a;
    }
}
