/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.frictionlessdata.tableschema.exceptions;

/**
 *
 * @author pechorin
 */
public class InvalidCastException extends Exception {

    /**
     * Creates a new instance of <code>InvalidCastException</code> without
     * detail message.
     */
    public InvalidCastException() {
    }

    /**
     * Constructs an instance of <code>InvalidCastException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public InvalidCastException(String msg) {
        super(msg);
    }
}
