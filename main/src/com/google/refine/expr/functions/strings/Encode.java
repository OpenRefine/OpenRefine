
package com.google.refine.expr.functions.strings;

import com.google.common.io.BaseEncoding;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

import java.util.Properties;

public class Encode implements Function {

    /**
     * Encodes a string using a given encoding. Encodings include Base16, Base32Hex, Base32, Base64, and Base64Url.
     *
     * @param args
     *            Arguments to the function.
     * @return The encoded string.
     */
    @Override
    public Object call(Properties bindings, Object[] args) {
        // check if args are valid
        if (args.length == 2) {
            // check if args are strings
            if (args[0] instanceof String && args[1] instanceof String) {
                String encoding = (String) args[1];
                String string = (String) args[0];
                // check if encoding is valid
                switch (encoding) {
                    case "base16":
                        return BaseEncoding.base16().encode(string.getBytes());
                    case "base32hex":
                        return BaseEncoding.base32Hex().encode(string.getBytes());
                    case "base32":
                        return BaseEncoding.base32().encode(string.getBytes());
                    case "base64":
                        return BaseEncoding.base64().encode(string.getBytes());
                    case "base64url":
                        return BaseEncoding.base64Url().encode(string.getBytes());
                    default:
                        // encoding);
                        return new EvalError(EvalErrorMessage.unknown_encoding(ControlFunctionRegistry.getFunctionName(this), encoding));
                }
            }
        }
        // encoding.");
        return new EvalError(EvalErrorMessage.expects_one_string_and_encoding(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_encode();
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
