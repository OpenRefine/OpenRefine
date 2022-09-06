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

package com.google.refine.expr.functions.strings;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;
import com.google.refine.model.Project;

public class Reinterpret implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2 || args.length == 3) {
            Object o1 = args[0];
            Object o2 = args[1];
            if (o1 != null && o2 != null && o2 instanceof String) {
                String str = (o1 instanceof String) ? (String) o1 : o1.toString();
                String decoder;
                String encoder;

                encoder = (String) o2;
                if (args.length == 2) {
                    Project project = (Project) bindings.get("project");
                    ProjectMetadata metadata = ProjectManager.singleton.getProjectMetadata(project.id);
                    decoder = metadata.getEncoding(); // can return "" for broken projects
                } else {
                    decoder = (String) args[2];
                }
                return reinterpret(str, decoder, encoder);
            }
        }
        // given target encoding and optional source encoding");
        return new EvalError(EvalErrorMessage.expects_string_to_reinterpret(ControlFunctionRegistry.getFunctionName(this)));
    }

    private Object reinterpret(String str, String decoder, String encoder) {
        String result = null;

        byte[] bytes;
        if (decoder == null || decoder.isEmpty()) {
            bytes = str.getBytes();
        } else {
            try {
                bytes = str.getBytes(decoder);
            } catch (UnsupportedEncodingException e) {
                // + "' is not available or recognized.");
                return new EvalError(EvalErrorMessage.unrecognized_source_encoding(ControlFunctionRegistry.getFunctionName(this), decoder));
            }
        }
        try {
            if (encoder == null || encoder.isEmpty()) {
                result = new String(bytes); // system default encoding
            } else {
                result = new String(bytes, encoder);
            }
        } catch (UnsupportedEncodingException e) {
            // is not available or recognized.");
            return new EvalError(EvalErrorMessage.unrecognized_target_encoding(ControlFunctionRegistry.getFunctionName(this), encoder));
        }

        return result;
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_reinterpret();
    }

    @Override
    public String getParams() {
        return "string s, string target encoding, string source encoding";
    }

    @Override
    public String getReturns() {
        return "string";
    }
}
