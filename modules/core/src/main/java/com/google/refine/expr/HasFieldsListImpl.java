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

import java.util.ArrayList;
import java.util.Properties;

public class HasFieldsListImpl extends ArrayList<HasFields> implements HasFieldsList {

    private static final long serialVersionUID = -8635194387420305802L;

    @Override
    public Object getField(String name, Properties bindings) {
        int c = size();
        if (c > 0 && get(0) != null && get(0).fieldAlsoHasFields(name)) {
            HasFieldsListImpl l = new HasFieldsListImpl();
            for (int i = 0; i < size(); i++) {
                HasFields o = this.get(i);
                l.add(i, o == null ? null : (HasFields) o.getField(name, bindings));
            }
            return l;
        } else {
            Object[] r = new Object[this.size()];
            for (int i = 0; i < r.length; i++) {
                HasFields o = this.get(i);
                r[i] = o == null ? null : o.getField(name, bindings);
            }
            return r;
        }
    }

    @Override
    public int length() {
        return size();
    }

    @Override
    public boolean fieldAlsoHasFields(String name) {
        int c = size();
        return (c > 0 && get(0).fieldAlsoHasFields(name));
    }

    @Override
    public HasFieldsList getSubList(int fromIndex, int toIndex) {
        HasFieldsListImpl subList = new HasFieldsListImpl();
        subList.addAll(this.subList(fromIndex, toIndex));

        return subList;
    }
}
