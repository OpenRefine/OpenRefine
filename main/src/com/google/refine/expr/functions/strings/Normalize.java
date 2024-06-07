package com.google.refine.expr.functions.strings;

import java.util.Properties;

import com.google.refine.clustering.binning.Keyer;
import com.google.refine.clustering.binning.NormalizeKeyer;
import com.google.refine.grel.Function;

public class Normalize implements Function {
    static Keyer normalizeKeyer = new NormalizeKeyer();

    @Override
    public Object call(Properties bindings, Object[] args) {
        // Check if the correct number of arguments are provided and they are not null
        if ((args.length == 1 && args[0] != null) || (args.length == 2 && args[0] != null && args[1] != null)) {
            Object o = args[0];
            String s = (o instanceof String) ? (String) o : o.toString();
            if (args.length == 2) {
                // If two arguments are provided, perform normalization with the specified form
                String form = args[1].toString().toUpperCase();
                return ((NormalizeKeyer) normalizeKeyer).keyWithForm(s, form);
            } else {
                // Perform default normalization (NFD) if only one argument is provided
                return normalizeKeyer.key(s);
            }
        }
        // Return null if input arguments are invalid or incomplete
        return null;
    }

    @Override
    public String getDescription() {
        return "Normalizes the string by removing diacritics, converting extended Western characters to their ASCII equivalents, and applying Unicode normalization forms.";
    }

    @Override
    public String getReturns() {
        return "string";
    }

    @Override
    public String getParams() {
        return "string s, [optional] string normalizationForm (NFC, NFD, NFKC, NFKD)";
    }
}
