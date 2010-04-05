package com.metaweb.util.signal;

public abstract class SignalHandler {

    SignalHandlerWrapper _wrapper;
    
    public SignalHandler(String signalName) {
        try {
            _wrapper = new SignalHandlerWrapper(signalName, this);
        } catch (Throwable e) {
            throw new java.lang.RuntimeException("Signal handling facilities are not available in this JVM.");
        }
    }
        
    /**
     * The method that handles the signal this handler has been registered for.
     * If the method returns false or throws, the chain of invocation is stopped;
     * this includes the handlers the JVM already registered for those signals.
     */
    public abstract boolean handle(String signame);

}