package com.google.refine.io;

import java.io.IOException;


public class DirectoryNotFoundException extends IOException {
    private static final long serialVersionUID = 7605744699673955879L;

    public DirectoryNotFoundException(String message) { super(message); }
}
