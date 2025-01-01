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

import java.text.Collator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;

/**
 * An abstract syntax tree node encapsulating an operator call, such as "+".
 */
public class OperatorCallExpr extends GrelExpr {

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
                        if (n2 == 0) {
                            if (n1 == 0) {
                                return Double.NaN;
                            } else {
                                return n1 > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                            }
                        }
                        // TODO: This will throw on divide by zero - return Double.Infinity / -Infinity instead?
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
                } else if (args[0] instanceof String && args[1] instanceof String) {
                    String s1 = (String) args[0];
                    String s2 = (String) args[1];
                    Collator collator = Collator.getInstance();
                    collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
//                    collator.setStrength(Collator.SECONDARY);

                    if (">".equals(_op)) {
                        return collator.compare(s1, s2) > 0;
                    } else if (">=".equals(_op)) {
                        return collator.compare(s1, s2) >= 0;
                    } else if ("<".equals(_op)) {
                        return collator.compare(s1, s2) < 0;
                    } else if ("<=".equals(_op)) {
                        return collator.compare(s1, s2) <= 0;
                    } else if ("==".equals(_op)) {
                        return collator.compare(s1, s2) == 0;
                    } else if ("!=".equals(_op)) {
                        return collator.compare(s1, s2) != 0;
                    }
                }

                if (args[0] instanceof String || args[1] instanceof String) {
                    String s1 = args[0] instanceof String ? (String) args[0] : args[0].toString();
                    String s2 = args[1] instanceof String ? (String) args[1] : args[1].toString();

                    if ("+".equals(_op)) {
                        return s1 + s2;
                    }
                }

                if (args[0] instanceof Comparable && args[1] instanceof Comparable
                        && (args[0].getClass().isAssignableFrom(args[1].getClass()) ||
                                args[1].getClass().isAssignableFrom(args[0].getClass()))) {
                    Comparable s1 = (Comparable) args[0];
                    Comparable s2 = (Comparable) args[1];

                    if (">".equals(_op)) {
                        return s1.compareTo(s2) > 0;
                    } else if (">=".equals(_op)) {
                        return s1.compareTo(s2) >= 0;
                    } else if ("<".equals(_op)) {
                        return s1.compareTo(s2) < 0;
                    } else if ("<=".equals(_op)) {
                        return s1.compareTo(s2) <= 0;
                    } else if ("==".equals(_op)) {
                        return s1.compareTo(s2) == 0;
                    } else if ("!=".equals(_op)) {
                        return s1.compareTo(s2) != 0;
                    }
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
    public final Optional<Set<String>> getColumnDependencies(Optional<String> baseColumn) {
        Set<String> dependencies = new HashSet<>();
        for (Evaluable ev : _args) {
            Optional<Set<String>> deps = ev.getColumnDependencies(baseColumn);
            if (deps.isEmpty()) {
                return Optional.empty();
            }
            dependencies.addAll(deps.get());
        }
        return Optional.of(dependencies);
    }

    @Override
    public Optional<Evaluable> renameColumnDependencies(Map<String, String> substitutions) {
        Evaluable[] translatedArgs = new Evaluable[_args.length];
        for (int i = 0; i != _args.length; i++) {
            Optional<Evaluable> translatedArg = _args[i].renameColumnDependencies(substitutions);
            if (translatedArg.isEmpty()) {
                return Optional.empty();
            }
            translatedArgs[i] = translatedArg.get();
        }
        return Optional.of(new OperatorCallExpr(translatedArgs, _op));
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(_args);
        result = prime * result + Objects.hash(_op);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OperatorCallExpr other = (OperatorCallExpr) obj;
        return Arrays.equals(_args, other._args) && Objects.equals(_op, other._op);
    }
}
