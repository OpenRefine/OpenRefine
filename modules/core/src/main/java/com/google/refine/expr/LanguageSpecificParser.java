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

package com.google.refine.expr;

import org.apache.commons.lang3.NotImplementedException;

/**
 * A parser for an expression language. It is registered in {@link MetaParser} with a language prefix to identify it.
 */
public interface LanguageSpecificParser {

    /**
     * @deprecated in favor of {@link #parse(String, String)}, which is the one implementations should override
     */
    @Deprecated(since = "3.9")
    public default Evaluable parse(String source) throws ParsingException {
        throw new NotImplementedException("The method parse(String source, String languagePrefix) should be overridden");
    }

    /**
     * Parse an expression.
     * 
     * @param source
     *            the source to be parsed
     * @param languagePrefix
     *            the prefix which identifies the language (which is the identifier with which this
     *            {@link LanguageSpecificParser} was registered)
     * @return a parsed expression
     * @throws ParsingException
     *             when the source contains any syntax error
     */
    default public Evaluable parse(String source, String languagePrefix) throws ParsingException {
        return parse(source);
    }
}
