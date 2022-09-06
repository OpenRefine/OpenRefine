/*

Copyright 2010,2013 Google Inc. and other contributors
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

package com.google.refine.model.recon;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

import edu.mit.simile.butterfly.ButterflyModule;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "mode")
@JsonTypeIdResolver(ReconConfigResolver.class)
abstract public class ReconConfig {

    final static protected Logger LOGGER = LoggerFactory.getLogger("recon-config");

    static final public Map<String, List<Class<? extends ReconConfig>>> s_opNameToClass = new HashMap<String, List<Class<? extends ReconConfig>>>();

    static final public Map<Class<? extends ReconConfig>, String> s_opClassToName = new HashMap<Class<? extends ReconConfig>, String>();

    static public void registerReconConfig(ButterflyModule module, String name, Class<? extends ReconConfig> klass) {
        String key = module.getName() + "/" + name;

        s_opClassToName.put(klass, key);

        List<Class<? extends ReconConfig>> classes = s_opNameToClass.get(key);
        if (classes == null) {
            classes = new LinkedList<Class<? extends ReconConfig>>();
            s_opNameToClass.put(key, classes);
        }
        classes.add(klass);
    }

    static public Class<? extends ReconConfig> getClassFromMode(String mode) {
        // Backward compatibility
        if ("extend".equals(mode) || "strict".equals(mode)) {
            mode = "freebase/" + mode;
        } else if ("heuristic".equals(mode)) {
            mode = "core/standard-service"; // legacy
        } else if (!mode.contains("/")) {
            mode = "core/" + mode;
        }

        // TODO: This can fail silently if the Freebase extension is not installed.
        List<Class<? extends ReconConfig>> classes = s_opNameToClass.get(mode);
        if (classes != null && classes.size() > 0) {
            return classes.get(classes.size() - 1);
        }
        return null;
    }

    static public ReconConfig reconstruct(String json) throws Exception {
        return ParsingUtilities.mapper.readValue(json, ReconConfig.class);
    }

    abstract public int getBatchSize();

    abstract public String getBriefDescription(Project project, String columnName);

    abstract public ReconJob createJob(
            Project project,
            int rowIndex,
            Row row,
            String columnName,
            Cell cell);

    abstract public List<Recon> batchRecon(List<ReconJob> jobs, long historyEntryID);

    abstract public Recon createNewRecon(long historyEntryID);

    public void save(Writer writer) {
        try {
            ParsingUtilities.defaultWriter.writeValue(writer, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the identifier for the reconciliation mode, as serialized in JSON. This is the same identifier that was
     * used to register the registration mode. Jackson already adds the mode during serialization hence the JsonIgnore
     * here.
     */
    @JsonIgnore
    abstract public String getMode();
}
