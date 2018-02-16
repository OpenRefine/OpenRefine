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
public class TypeInferringException extends Exception {

    /**
     * Creates a new instance of <code>TypeInferringException</code> without
     * detail message.
     */
    public TypeInferringException() {
    }

    /**
     * Constructs an instance of <code>TypeInferringException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public TypeInferringException(String msg) {
        super(msg);
    }
}
