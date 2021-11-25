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

package com.google.refine.templating;

import java.util.ArrayList;
import java.util.List;

import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.ast.FieldAccessorExpr;
import com.google.refine.grel.ast.VariableExpr;

public class Parser {
    static public Template parse(String s) throws ParsingException {
        List<Fragment> fragments = new ArrayList<Fragment>();

        int start = 0, current = 0;
        while (current < s.length() - 1) {
            char c = s.charAt(current);
            char c2 = s.charAt(current + 1);
            if (c == '\\') {
                if (c2 == '\\' || c2 == '{' || c2 == '$') {
                    fragments.add(new StaticFragment(s.substring(start, current).concat(Character.toString(c2))));
                    start = current += 2;
                } else {
                    // Invalid escape - just leave it in the template
                    current += 1; 
                }
                continue;
            }

            if (c == '$' && c2 == '{') {
                int closeBrace = s.indexOf('}', current + 2);
                if (closeBrace > current + 1) {
                    String columnName = s.substring(current + 2, closeBrace);

                    if (current > start) {
                        fragments.add(new StaticFragment(s.substring(start, current)));
                    }
                    start = current = closeBrace + 1;

                    fragments.add(
                            new DynamicFragment(
                                    new FieldAccessorExpr(
                                            new FieldAccessorExpr(
                                                    new VariableExpr("cells"), 
                                                    columnName), 
                                    "value")));

                    continue;
                }
            } else if (c == '{' && c2 == '{') {
                int closeBrace = s.indexOf('}', current + 2);
                if (closeBrace > current + 1 && closeBrace < s.length() - 1 && s.charAt(closeBrace + 1) == '}') {
                    String expression = s.substring(current + 2, closeBrace);

                    if (current > start) {
                        fragments.add(new StaticFragment(s.substring(start, current)));
                    }
                    start = current = closeBrace + 2;

                    fragments.add(
                            new DynamicFragment(
                                    MetaParser.parse(expression)));

                    continue;
                }
            }

            current++;
        }

        if (start < s.length()) {
            fragments.add(new StaticFragment(s.substring(start)));
        }

        return new Template(fragments);
    }
}
