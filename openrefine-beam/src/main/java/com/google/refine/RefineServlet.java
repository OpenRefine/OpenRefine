/*
 * 
 * Copyright 2010, Google Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package com.google.refine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefineServlet {
  static private String ASSIGNED_VERSION = "2.8";

  static public String VERSION = "";
  static public String REVISION = "";
  static public String FULL_VERSION = "";
  static public String FULLNAME = "OpenRefine ";


  static public final String AGENT_ID = "/en/google_refine"; // TODO: Unused? Freebase ID

  static final long serialVersionUID = 2386057901503517403L;

  static private final String JAVAX_SERVLET_CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";
  static final private Map<String, String> classMappingsCache = new HashMap<>();
  static final private Map<String, Class<?>> classCache = new HashMap<>();
  static final private List<ClassMapping> classMappings = new ArrayList<>();
  
  static private class ClassMapping {
    final String from;
    final String to;

    ClassMapping(String from, String to) {
      this.from = from;
      this.to = to;
    }
  }

  static public Class<?> getClass(String className) throws ClassNotFoundException {
    String toClassName = classMappingsCache.get(className);
    if (toClassName == null) {
      toClassName = className;

      for (ClassMapping m : classMappings) {
        if (m.from.endsWith("*")) {
          if (toClassName.startsWith(m.from.substring(0, m.from.length() - 1))) {
            toClassName = m.to + toClassName.substring(m.from.length() - 1);
          }
        } else {
          if (m.from.equals(toClassName)) {
            toClassName = m.to;
          }
        }
      }

      classMappingsCache.put(className, toClassName);
    }

    Class<?> klass = classCache.get(toClassName);
    if (klass == null) {
      klass = Class.forName(toClassName);
      classCache.put(toClassName, klass);
    }
    return klass;
  }

}
