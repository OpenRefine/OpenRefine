package com.metaweb.util.signal;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/*
 * This class allows our own SignalHandler class to fail more gracefully 
 * in case the "sun.misc.Signal*" classes are not found in the current jvm.
 */
final class SignalHandlerWrapper implements SignalHandler {

    private transient final SignalHandler existingHandler;

    private transient final AbstractSignalHandler handler;

    SignalHandlerWrapper(final String signalName, final AbstractSignalHandler handler) {
        this.handler = handler;
        final Signal signal = new Signal(signalName);
        existingHandler = Signal.handle(signal, this);
    }

    public void handle(final Signal sig) {
        if (handler.handle(sig.getName()) && (existingHandler != null)) {
            existingHandler.handle(sig);
        }
    }

}