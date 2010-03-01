package com.metaweb.gridworks.gel;

import java.util.LinkedList;
import java.util.List;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ParsingException;
import com.metaweb.gridworks.gel.Scanner.NumberToken;
import com.metaweb.gridworks.gel.Scanner.Token;
import com.metaweb.gridworks.gel.Scanner.TokenType;
import com.metaweb.gridworks.gel.ast.ControlCallExpr;
import com.metaweb.gridworks.gel.ast.FieldAccessorExpr;
import com.metaweb.gridworks.gel.ast.FunctionCallExpr;
import com.metaweb.gridworks.gel.ast.LiteralExpr;
import com.metaweb.gridworks.gel.ast.OperatorCallExpr;
import com.metaweb.gridworks.gel.ast.VariableExpr;

public class Parser {
	protected Scanner 	_scanner;
	protected Token 	_token;
	protected Evaluable _root;
	
	public Parser(String s) throws ParsingException {
		this(s, 0, s.length());
	}
	
	public Parser(String s, int from, int to) throws ParsingException {
		_scanner = new Scanner(s, from, to);
		_token = _scanner.next();
		
		_root = parseExpression();
	}
	
	public Evaluable getExpression() {
		return _root;
	}
	
	protected void next() {
		_token = _scanner.next();
	}
	
	protected ParsingException makeException(String desc) {
		int index = _token != null ? _token.start : _scanner.getIndex();
		
		return new ParsingException("Parsing error at offset " + index + ": " + desc);
	}
	
	protected Evaluable parseExpression() throws ParsingException {
		Evaluable sub = parseSubExpression();
		
		while (_token != null && 
				_token.type == TokenType.Operator && 
				">=<==!=".indexOf(_token.text) >= 0) {
			
			String op = _token.text;
			
			next();
			
			Evaluable sub2 = parseSubExpression();
			
			sub = new OperatorCallExpr(new Evaluable[] { sub, sub2 }, op);
		}
		
		return sub;
	}
	
	protected Evaluable parseSubExpression() throws ParsingException {
		Evaluable sub = parseTerm();
		
		while (_token != null && 
				_token.type == TokenType.Operator && 
				"+-".indexOf(_token.text) >= 0) {
			
			String op = _token.text;
			
			next();
			
			Evaluable sub2 = parseSubExpression();
			
			sub = new OperatorCallExpr(new Evaluable[] { sub, sub2 }, op);
		}
		
		return sub;
	}
	
	protected Evaluable parseTerm() throws ParsingException {
		Evaluable factor = parseFactor();
		
		while (_token != null && 
				_token.type == TokenType.Operator && 
				"*/".indexOf(_token.text) >= 0) {
			
			String op = _token.text;
			
			next();
			
			Evaluable factor2 = parseFactor();
			
			factor = new OperatorCallExpr(new Evaluable[] { factor, factor2 }, op);
		}
		
		return factor;
	}
	
	protected Evaluable parseFactor() throws ParsingException {
		if (_token == null) {
			throw makeException("Expression ends too early");
		}
		
		Evaluable eval = null;
		
		if (_token.type == TokenType.String) {
			eval = new LiteralExpr(_token.text);
			next();
		} else if (_token.type == TokenType.Number) {
			eval = new LiteralExpr(((NumberToken)_token).value);
			next();
		} else if (_token.type == TokenType.Operator && _token.text.equals("-")) { // unary minus?
			next();
			
			if (_token != null && _token.type == TokenType.Number) {
				eval = new LiteralExpr(-((NumberToken)_token).value);
				next();
			} else {
				throw makeException("Bad negative number");
			}
		} else if (_token.type == TokenType.Identifier) {
			String text = _token.text;
			next();
			
			if (_token == null || _token.type != TokenType.Delimiter || !_token.text.equals("(")) {
				eval = new VariableExpr(text);
			} else {
				Function f = ControlFunctionRegistry.getFunction(text);
				Control c = ControlFunctionRegistry.getControl(text);
				if (f == null && c == null) {
					throw makeException("Unknown function or control named " + text);
				}
				
				next(); // swallow (
				
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
			next();
			
			eval = parseExpression();
			
			if (_token != null && _token.type == TokenType.Delimiter && _token.text.equals(")")) {
				next();
			} else {
				throw makeException("Missing )");
			}
		} else {
			throw makeException("Missing number, string, identifier, or parenthesized expression");
		}
		
		while (_token != null) {
			if (_token.type == TokenType.Operator && _token.text.equals(".")) {
				next(); // swallow .
				
				if (_token == null || _token.type != TokenType.Identifier) {
					throw makeException("Missing function name");
				}
				
				String identifier = _token.text;
				next();
				
				if (_token != null && _token.type == TokenType.Delimiter && _token.text.equals("(")) {
					next(); // swallow (
					
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
				next(); // swallow [
				
				List<Evaluable> args = parseExpressionList("]");
				args.add(0, eval);
				
				eval = new FunctionCallExpr(makeArray(args), ControlFunctionRegistry.getFunction("get"));
			} else {
				break;
			}
		}
		
		return eval;
	}
	
	protected List<Evaluable> parseExpressionList(String closingDelimiter) throws ParsingException {
		List<Evaluable> l = new LinkedList<Evaluable>();
		
		if (_token != null && 
			(_token.type != TokenType.Delimiter || !_token.text.equals(closingDelimiter))) {
			
			while (_token != null) {
				Evaluable eval = parseExpression();
				
				l.add(eval);
				
				if (_token != null && _token.type == TokenType.Delimiter && _token.text.equals(",")) {
					next(); // swallow comma, loop back for more
				} else {
					break;
				}
			}
		}
		
		if (_token != null && _token.type == TokenType.Delimiter && _token.text.equals(closingDelimiter)) {
			next(); // swallow closing delimiter
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
