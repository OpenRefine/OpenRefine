/*

Copyright 2017, Owen Stephens
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
    * Neither the name of the copyright holder nor the names of its
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

package com.google.refine.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.RefineServlet;

abstract public class HttpHeadersSupport {

    static final protected Map<String, HttpHeaderInfo> s_headers = new HashMap<String, HttpHeaderInfo>();

    static public class HttpHeaderInfo {

        @JsonIgnore
        final public String name;
        @JsonProperty("header")
        final public String header;
        @JsonProperty("defaultValue")
        final public String defaultValue;

        HttpHeaderInfo(String header, String defaultValue) {
            this.name = header.toLowerCase();
            this.header = header;
            this.defaultValue = defaultValue;
        }
    }

    static {
        registerHttpHeader("User-Agent", RefineServlet.FULLNAME);
        registerHttpHeader("Accept", "*/*");
        registerHttpHeader("Authorization", "");
    }

    /**
     * @param header
     * @param defaultValue
     */
    static public void registerHttpHeader(String header, String defaultValue) {
        s_headers.put(header.toLowerCase(), new HttpHeaderInfo(header, defaultValue));
    }

    static public HttpHeaderInfo getHttpHeaderInfo(String header) {
        return s_headers.get(header.toLowerCase());
    }

    static public Set<String> getHttpHeaderLabels() {
        return s_headers.keySet();
    }
}
