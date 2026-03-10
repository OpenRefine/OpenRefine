/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.grel;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.expr.Evaluable;

/**
 * Interface of GREL controls such as if, forEach, forNonBlank, with. A control can decide which part of the code to
 * execute and can affect the environment bindings. Functions, on the other hand, can't do either.
 * <p>
 * TODO: Add extension name either here or to registration?
 */
public interface Control {

    public Object call(Properties bindings, Evaluable[] args);

    public String checkArguments(Evaluable[] args);

    /**
     * Get a brief description of what the Control does to be used in the Help tab of the Expression Editor.
     * <p>
     * TODO: This is not currently localized, but should be.
     *
     * @return a brief description of what the function does
     */
    @JsonProperty("description")
    default String getDescription() {
        // Fallback for a pre-2018 Control which implements Jsonizable
        return Function.getStringFromJsonizable(this, "description");
    }

    /**
     * Get a string describing the parameters for this Control to be used in the Help tab of the Expression Editor. This
     * is not currently structured in any way but should be a human-readable description of the parameters for this
     * function.
     * <p>
     * TODO: Adding structured parameter info would help support smart editing.
     *
     * @return a string describing the parameters for this function
     */
    @JsonProperty("params")
    @JsonInclude(Include.NON_EMPTY)
    default String getParams() {
        // Fallback for a pre-2018 Control which implements Jsonizable
        return Function.getStringFromJsonizable(this, "params");
    }

    /**
     * Get a string describing the return type for this Control, to be used in the Help tab of the Expression Editor.
     * <p>
     * TODO: This is currently just a string, but in the future we may want to add more structure here to allow for
     * better automation of handling return values (e.g. automatically generating output display based on return type,
     * etc.).
     *
     * @return a string describing the return type for this function
     */
    @JsonProperty("returns")
    default String getReturns() {
        // Fallback for a pre-2018 Control which implements Jsonizable
        return Function.getStringFromJsonizable(this, "returns");
    }
}
