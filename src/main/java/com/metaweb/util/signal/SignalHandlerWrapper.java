package com.metaweb.util.signal;

/*
 * This class allows our own SignalHandler class to fail more gracefully 
 * in case the "sun.misc.Signal*" classes are not found in the current jvm.
 */
final class SignalHandlerWrapper implements sun.misc.SignalHandler {

    private final sun.misc.SignalHandler existingHandler;

    private final SignalHandler handler;

    SignalHandlerWrapper(String signalName, SignalHandler handler) {
        this.handler = handler;
        sun.misc.Signal signal = new sun.misc.Signal(signalName);
        existingHandler = sun.misc.Signal.handle(signal, this);
    }

    public void handle(sun.misc.Signal sig) {
        if (handler.handle(sig.getName()) && (existingHandler != null)) {
            existingHandler.handle(sig);
        }
    }

}