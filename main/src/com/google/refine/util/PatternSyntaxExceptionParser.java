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

package com.google.refine.util;

import java.util.regex.PatternSyntaxException;

public class PatternSyntaxExceptionParser {
    private final PatternSyntaxException exception;

    public PatternSyntaxExceptionParser(PatternSyntaxException e) {
        this.exception = e;
    }

    public String getUserMessage() {
        StringBuffer sb = new StringBuffer();
        String desc = exception.getDescription();
        switch(desc)
        {
            case "Unclosed character class":
            //Need these errors to be more human readable
            //Possibly include html for formatting
            //Update tests first with user friendly errors
                sb.append("The regular expression is missing a closing ']' character.");
                break;
            case "Unmatched closing ')'":
                sb.append("The regular expression is missing a opening '(' character.");
                break;
            case "Unclosed group":
                sb.append("The regular expression is missing a closing ')' character.");
                break;
            default:
                // If no special handling in place fall back on method
                // as used in java.util.regex.PatternSyntaxException
                sb.append(desc);
        }
        return sb.toString();
    }
        /* Error messages from java.util.regex.Pattern
        "Unclosed character class"
        "Unmatched closing ')'"
        "Unexpected internal error"
        "Dangling meta character '" + ((char)ch) + "'"
        "\\k is not followed by '<' for named capturing group"
        "(named capturing group <"+ name+"> does not exist"
        "Illegal/unsupported escape sequence"
        "Bad class syntax"
        "Unclosed character class"
        "Illegal character range"
        "Unexpected character '"+((char)ch)+"'"
        "Unclosed character family"
        "Empty character family"
        "Unknown Unicode property {name=<" + name + ">, "+ "value=<" + value + ">}"
        "Unknown character script name {" + name + "}"
        "Unknown character block name {" + name + "}"
        "Unknown character property name {" + name + "}"
        "named capturing group has 0 length name"
        "named capturing group is missing trailing '>'"
        "Named capturing group <" + name + "> is already defined"
        "Look-behind group does not have " + "an obvious maximum length"
        "Unknown look-behind group"
        "Unknown group type"
        "Unknown inline modifier"
        "Internal logic error"
        "Unclosed counted closure"
        "Illegal repetition range"
        "Illegal repetition"
        "Illegal control escape sequence"
        "Illegal octal escape sequence"
        "Hexadecimal codepoint is too big"
        "Unclosed hexadecimal escape sequence"
        "Illegal hexadecimal escape sequence"
        "Illegal Unicode escape sequence"
        */


}
