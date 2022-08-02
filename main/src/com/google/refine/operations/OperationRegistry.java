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

package com.google.refine.operations;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.refine.model.AbstractOperation;

import edu.mit.simile.butterfly.ButterflyModule;

public abstract class OperationRegistry {

    static final public Map<String, List<Class<? extends AbstractOperation>>> s_opNameToClass = new HashMap<String, List<Class<? extends AbstractOperation>>>();

    static final public Map<Class<? extends AbstractOperation>, String> s_opClassToName = new HashMap<Class<? extends AbstractOperation>, String>();

    static public void registerOperation(ButterflyModule module, String name, Class<? extends AbstractOperation> klass) {
        String key = module.getName() + "/" + name;

        s_opClassToName.put(klass, key);

        List<Class<? extends AbstractOperation>> classes = s_opNameToClass.get(key);
        if (classes == null) {
            classes = new LinkedList<Class<? extends AbstractOperation>>();
            s_opNameToClass.put(key, classes);
        }
        classes.add(klass);
    }

    static public Class<? extends AbstractOperation> resolveOperationId(String op) {
        if (!op.contains("/")) {
            op = "core/" + op; // backward compatible
        }
        List<Class<? extends AbstractOperation>> classes = s_opNameToClass.get(op);
        if (classes != null && classes.size() > 0) {
            return classes.get(classes.size() - 1);
        }
        return null;
    }
}
