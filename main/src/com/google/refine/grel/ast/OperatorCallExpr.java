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

package com.google.refine.grel.ast;

import java.util.Properties;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;

/**
 * An abstract syntax tree node encapsulating an operator call, such as "+".
 */
public class OperatorCallExpr implements Evaluable {

    final protected Evaluable[] _args;
    final protected String _op;

    public OperatorCallExpr(Evaluable[] args, String op) {
        _args = args;
        _op = op;
    }

    @Override
    public Object evaluate(Properties bindings) {
        Object[] args = new Object[_args.length];
        for (int i = 0; i < _args.length; i++) {
            Object v = _args[i].evaluate(bindings);
            if (ExpressionUtils.isError(v)) {
                return v;
            }
            args[i] = v;
        }

        if (args.length == 2) {
            if (args[0] != null && args[1] != null) {
                if (isIntegral(args[0]) && isIntegral(args[1])) {
                    long n1 = ((Number) args[0]).longValue();
                    long n2 = ((Number) args[1]).longValue();

                    if ("+".equals(_op)) {
                        return n1 + n2;
                    } else if ("-".equals(_op)) {
                        return n1 - n2;
                    } else if ("*".equals(_op)) {
                        return n1 * n2;
                    } else if ("/".equals(_op)) {
                        if (n2 == 0 && n1 == 0) {
                            return Double.NaN;
                        }
                        return n1 / n2;
                    } else if ("%".equals(_op)) {
                        return n1 % n2;
                    } else if (">".equals(_op)) {
                        return n1 > n2;
                    } else if (">=".equals(_op)) {
                        return n1 >= n2;
                    } else if ("<".equals(_op)) {
                        return n1 < n2;
                    } else if ("<=".equals(_op)) {
                        return n1 <= n2;
                    } else if ("==".equals(_op)) {
                        return n1 == n2;
                    } else if ("!=".equals(_op)) {
                        return n1 != n2;
                    }
                } else if (args[0] instanceof Number && args[1] instanceof Number) {
                    double n1 = ((Number) args[0]).doubleValue();
                    double n2 = ((Number) args[1]).doubleValue();

                    if ("+".equals(_op)) {
                        return n1 + n2;
                    } else if ("-".equals(_op)) {
                        return n1 - n2;
                    } else if ("*".equals(_op)) {
                        return n1 * n2;
                    } else if ("/".equals(_op)) {
                        if (n2 == 0 && n1 == 0) {
                            return Double.NaN;
                        }
                        return n1 / n2;
                    } else if ("%".equals(_op)) {
                        return n1 % n2;
                    } else if (">".equals(_op)) {
                        return n1 > n2;
                    } else if (">=".equals(_op)) {
                        return n1 >= n2;
                    } else if ("<".equals(_op)) {
                        return n1 < n2;
                    } else if ("<=".equals(_op)) {
                        return n1 <= n2;
                    } else if ("==".equals(_op)) {
                        return n1 == n2;
                    } else if ("!=".equals(_op)) {
                        return n1 != n2;
                    }
                }

                if ("+".equals(_op)) {
                    return args[0].toString() + args[1].toString();
                }
            }

            if ("==".equals(_op)) {
                if (args[0] != null) {
                    return args[0].equals(args[1]);
                } else {
                    return args[1] == null;
                }
            } else if ("!=".equals(_op)) {
                if (args[0] != null) {
                    return !args[0].equals(args[1]);
                } else {
                    return args[1] != null;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        for (Evaluable ev : _args) {
            if (sb.length() > 0) {
                sb.append(' ');
                sb.append(_op);
                sb.append(' ');
            }
            sb.append(ev.toString());
        }

        return sb.toString();
    }

    private boolean isIntegral(Object n) {
        return n instanceof Long || n instanceof Integer;
    }
}
