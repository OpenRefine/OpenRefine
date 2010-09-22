package com.google.refine.expr;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import clojure.lang.IFn;

import com.google.refine.gel.Parser;

abstract public class MetaParser {
    static public class LanguageInfo {
        final public String                 name;
        final public LanguageSpecificParser parser;
        final public String                 defaultExpression;
        
        LanguageInfo(String name, LanguageSpecificParser parser, String defaultExpression) {
            this.name = name;
            this.parser = parser;
            this.defaultExpression = defaultExpression;
        }
    }
    
    static protected Map<String, LanguageInfo> s_languages;
    static {
        s_languages = new HashMap<String, LanguageInfo>();
        
        registerLanguageParser("gel", "Gridworks Expression Language (GEL)", new LanguageSpecificParser() {
            
            @Override
            public Evaluable parse(String s) throws ParsingException {
                return parseGEL(s);
            }
        }, "value");
        
        registerLanguageParser("clojure", "Clojure", new LanguageSpecificParser() {
            
            @Override
            public Evaluable parse(String s) throws ParsingException {
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
        }, "value");
    }
    
    static public void registerLanguageParser(String languagePrefix, String name, LanguageSpecificParser parser, String defaultExpression) {
        s_languages.put(languagePrefix, new LanguageInfo(name, parser, defaultExpression));
    }
    
    static public LanguageInfo getLanguageInfo(String languagePrefix) {
        return s_languages.get(languagePrefix.toLowerCase());
    }
    
    static public Set<String> getLanguagePrefixes() {
        return s_languages.keySet();
    }
    
    /**
     * Parse an expression that might have a language prefix into an Evaluable.
     * Expressions without valid prefixes or without any prefix are assumed to be
     * GEL expressions.
     * 
     * @param s
     * @return
     * @throws ParsingException
     */
    static public Evaluable parse(String s) throws ParsingException {
        String language = "gel";
        
        int colon = s.indexOf(':');
        if (colon >= 0) {
            language = s.substring(0, colon);
        }
        
        LanguageInfo info = s_languages.get(language.toLowerCase());
        if (info != null) {
            return info.parser.parse(s.substring(colon + 1));
        } else {
            return parseGEL(s);
        }
    }
    
    static protected Evaluable parseGEL(String s) throws ParsingException {
        Parser parser = new Parser(s);
        
        return parser.getExpression();
    }
}
