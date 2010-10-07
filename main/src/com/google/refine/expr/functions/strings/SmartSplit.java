package com.google.refine.expr.functions.strings;

import java.io.IOException;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import au.com.bytecode.opencsv.CSVParser;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class SmartSplit implements Function {
    static protected CSVParser s_tabParser = new CSVParser(
        '\t',
        CSVParser.DEFAULT_QUOTE_CHARACTER,
        CSVParser.DEFAULT_ESCAPE_CHARACTER,
        CSVParser.DEFAULT_STRICT_QUOTES,
        CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
        false
    );
    static protected CSVParser s_commaParser = new CSVParser(
        ',',
        CSVParser.DEFAULT_QUOTE_CHARACTER,
        CSVParser.DEFAULT_ESCAPE_CHARACTER,
        CSVParser.DEFAULT_STRICT_QUOTES,
        CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
        false
    );    
    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 1 && args.length <= 2) {
            CSVParser parser = null;
            
            Object v = args[0];
            String s = v.toString();
            
            if (args.length > 1) {
                String sep = args[1].toString();
                parser = new CSVParser(
                    sep.charAt(0),
                    CSVParser.DEFAULT_QUOTE_CHARACTER,
                    CSVParser.DEFAULT_ESCAPE_CHARACTER,
                    CSVParser.DEFAULT_STRICT_QUOTES,
                    CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                    false
                );
            }
            
            if (parser == null) {
                int tab = s.indexOf('\t');
                if (tab >= 0) {
                    parser = s_tabParser;
                } else {
                    parser = s_commaParser;
                }
            }
            
            try {
                return parser.parseLine(s);
            } catch (IOException e) {
                return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " error: " + e.getMessage());
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 1 or 2 strings");
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the array of strings obtained by splitting s with separator sep. Handles quotes properly. Guesses tab or comma separator if \"sep\" is not given.");
        writer.key("params"); writer.value("string s, optional string sep");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
