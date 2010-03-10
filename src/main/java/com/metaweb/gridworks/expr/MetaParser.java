package com.metaweb.gridworks.expr;

import java.io.StringReader;
import java.util.Properties;

import clojure.lang.IFn;

import com.metaweb.gridworks.gel.Parser;

abstract public class MetaParser {
    static public Evaluable parse(String s) throws ParsingException {
        String language = "gel";
        
        int colon = s.indexOf(':');
        if (colon >= 0) {
            language = s.substring(0, colon);
        }
        
        if ("jython".equalsIgnoreCase(language)) {
            return parseJython(s.substring(colon + 1));
        } else if ("clojure".equalsIgnoreCase(language)) {
            return parseClojure(s.substring(colon + 1));
        } else if ("gel".equalsIgnoreCase(language)) {
            return parseGEL(s.substring(colon + 1));
        } else {
            return parseGEL(s);
        }
    }
    
    static protected Evaluable parseGEL(String s) throws ParsingException {
        Parser parser = new Parser(s);
        
        return parser.getExpression();
    }
    
    static protected Evaluable parseJython(String s) throws ParsingException {
        return new JythonEvaluable(s);
    }
    
    static protected Evaluable parseClojure(String s) throws ParsingException {
        try {
            IFn fn = (IFn) clojure.lang.Compiler.load(new StringReader(
                "(fn [value cell cells row rowIndex] " + s + ")"
            ));
            
            return new Evaluable() {
                private IFn _fn;
                
                public Evaluable init(IFn fn) {
                    _fn = fn;
                    return this;
                }
                
                public Object evaluate(Properties bindings) {
                    try {
                        return _fn.invoke(
                            bindings.get("value"),
                            bindings.get("cell"),
                            bindings.get("cells"),
                            bindings.get("row"),
                            bindings.get("rowIndex")
                        );
                    } catch (Exception e) {
                        return new EvalError(e.getMessage());
                    }
                }
            }.init(fn);
        } catch (Exception e) {
            throw new ParsingException(e.getMessage());
        }
    }
}
