package org.dtls.fairifier;

import java.lang.Exception;

public class LayerUnavailableException extends Exception {
    public LayerUnavailableException(String message){
        super(message);
    }
}
