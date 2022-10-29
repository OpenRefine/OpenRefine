
package com.google.refine.expr.functions.strings;

import com.google.common.base.Optional;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;
import com.google.refine.util.DetectLanguageUtils;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class DetectLanguage implements Function {

    /**
     * Detects the language of the given string and provides the language code.
     * 
     * @param bindings
     *            bindings
     * @param args
     *            arguments
     * @return the language code of the string
     */
    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] instanceof String) {
            String text = (String) args[0];
            if (text.length() > 0) {
                try {
                    Optional<LdLocale> lang = DetectLanguageUtils.detect(text);
                    if (lang.isPresent()) {
                        return lang.get().getLanguage();
                    } else {
                        return new EvalError(EvalErrorMessage.language_detect_failed(ControlFunctionRegistry.getFunctionName(this)));
                    }
                } catch (IOException e) {
                    return new EvalError(EvalErrorMessage.failed(ControlFunctionRegistry.getFunctionName(this), e.getMessage()));
                }
            }
        }
        return new EvalError(EvalErrorMessage.expects_one_string(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_detect_language();
    }

    @Override
    public String getParams() {
        return "string s";
    }

    @Override
    public String getReturns() {
        return "string";
    }
}
