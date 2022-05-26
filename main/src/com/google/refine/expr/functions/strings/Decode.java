package com.google.refine.expr.functions.strings;

import com.google.common.io.BaseEncoding;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

import java.util.Properties;

public class Decode implements Function {
    /**
     * Decodes a string using a given encoding.
     * Encodings include Base16, Base32Hex, Base32, Base64, and Base64Url.
     *
     * @param args Arguments to the function.
     * @return The decoded string.
     */
    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            if(args[0] instanceof String && args[1] instanceof String) {
                String encoding = (String) args[1];
                String string = (String) args[0];
                switch (encoding) {
                    case "base16":
                        return new String( BaseEncoding.base16().decode(string));
                    case "base32hex":
                        return new String( BaseEncoding.base32Hex().decode(string));
                    case "base32":
                        return new String( BaseEncoding.base32().decode(string));
                    case "base64":
                        return new String( BaseEncoding.base64().decode(string));
                    case "base64url":
                        return new String( BaseEncoding.base64Url().decode(string));
                    default:
                        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + ": Unknown encoding: " + encoding);
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + ": expects two arguments: a string and an encoding.");
    }

    @Override
    public String getDescription() {
        return "Decodes a string using the specified encoding. Encodings include Base16, Base32Hex, Base32, Base64, and Base64Url.";
    }

    @Override
    public String getParams() {
        return "string s, string encoding";
    }

    @Override
    public String getReturns() {
        return "string";
    }
}
