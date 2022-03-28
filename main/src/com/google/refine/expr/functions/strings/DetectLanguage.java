package com.google.refine.expr.functions.strings;

import com.google.common.base.Optional;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.util.LangDetectUtils;
import com.optimaize.langdetect.i18n.LdLocale;

import java.io.IOException;
import java.util.Properties;

public class DetectLanguage implements Function {
    /**
     * Detects the language of the given string and provides the language code.
     * @param bindings bindings
     * @param args arguments
     * @return the language code of the string
     */
    @Override
    public Object call(Properties bindings, Object[] args) {
        if(args.length == 1) {
            Object obj = args[0]; // get the first argument
            if(obj instanceof String) { // if it is a string
                String text = (String) obj; // get the string
                if(text.length() > 0) { // if the string is not empty
                    try { // try to detect the language
                        Optional<LdLocale> lang = LangDetectUtils.detect(text); // detect the language
                        if(lang.isPresent()) { // if the language is detected
                            return lang.get().getLanguage(); // return the language code
                        } else { // if the language is not detected
                            return new EvalError("Language detection failed"); // return an error
                        }
                    } catch (IOException e) { // if the language detection failed
                        e.printStackTrace(); // print the stack trace
                    }
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " requires one argument");
    }

    @Override
    public String getDescription() {
        return "Detects the language of the given string and provides the language code.";
    }

    @Override
    public String getParams() {
        return "string obj";
    }

    @Override
    public String getReturns() {
        return "string";
    }
}
