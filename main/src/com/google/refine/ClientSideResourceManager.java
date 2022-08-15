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

package com.google.refine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.simile.butterfly.ButterflyModule;
import edu.mit.simile.butterfly.MountPoint;

public class ClientSideResourceManager {

    final static Logger logger = LoggerFactory.getLogger("refine_clientSideResourceManager");

    static public class QualifiedPath {

        public ButterflyModule module;
        public String path;
        public String fullPath;
    }

    static public class ClientSideResourceBundle {

        final protected Set<String> _pathSet = new HashSet<String>();
        final protected List<QualifiedPath> _pathList = new ArrayList<QualifiedPath>();
    }

    final static protected Map<String, ClientSideResourceBundle> s_bundles = new HashMap<String, ClientSideResourceBundle>();

    static public void addPaths(
            String bundleName,
            ButterflyModule module,
            String[] paths) {

        ClientSideResourceBundle bundle = s_bundles.get(bundleName);
        if (bundle == null) {
            bundle = new ClientSideResourceBundle();
            s_bundles.put(bundleName, bundle);
        }

        for (String path : paths) {
            String fullPath = resolve(module, path);
            if (fullPath == null) {
                logger.error("Failed to add paths to unmounted module " + module.getName());
                break;
            }
            if (!bundle._pathSet.contains(fullPath)) {
                QualifiedPath qualifiedPath = new QualifiedPath();
                qualifiedPath.module = module;
                qualifiedPath.path = path;
                qualifiedPath.fullPath = fullPath;

                bundle._pathSet.add(fullPath);
                bundle._pathList.add(qualifiedPath);
            }
        }
    }

    static public QualifiedPath[] getPaths(String bundleName) {
        ClientSideResourceBundle bundle = s_bundles.get(bundleName);
        if (bundle == null) {
            return new QualifiedPath[] {};
        } else {
            QualifiedPath[] paths = new QualifiedPath[bundle._pathList.size()];
            bundle._pathList.toArray(paths);
            return paths;
        }
    }

    static protected String resolve(ButterflyModule module, String path) {
        MountPoint mountPoint = module.getMountPoint();
        if (mountPoint != null) {
            String mountPointPath = mountPoint.getMountPoint();
            if (mountPointPath != null) {
                StringBuffer sb = new StringBuffer();

                boolean slashed = path.startsWith("/");
                char[] mountPointChars = mountPointPath.toCharArray();

                sb.append(mountPointChars, 0, slashed ? mountPointChars.length - 1 : mountPointChars.length);
                sb.append(path);

                return sb.toString();
            }
        }
        return null;
    }
}
