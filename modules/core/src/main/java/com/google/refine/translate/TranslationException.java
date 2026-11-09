
package com.google.refine.translate;

public class TranslationException extends Exception {

    private static final long serialVersionUID = -4878637706812657361L;

    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}
