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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.RefineServlet;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;

/**
 * A serializable pool of ReconCandidates indexed by ID.
 *
 */
public class Pool {

    @JsonProperty("recons")
    final protected Map<String, Recon> recons = new HashMap<String, Recon>();

    // This is only for backward compatibility while loading old project files
    final protected Map<String, ReconCandidate> candidates = new HashMap<String, ReconCandidate>();

    private void pool(ReconCandidate candidate) {
        candidates.put(candidate.id, candidate);
    }

    public void pool(Recon recon) {
        recons.put(Long.toString(recon.id), recon);
        poolReconCandidates(recon);
    }

    public void poolReconCandidates(Recon recon) {
        if (recon.match != null) {
            pool(recon.match);
        }
        if (recon.candidates != null) {
            for (ReconCandidate candidate : recon.candidates) {
                pool(candidate);
            }
        }
    }

    public Recon getRecon(String id) {
        return recons.get(id);
    }

    public ReconCandidate getReconCandidate(String topicID) {
        return candidates.get(topicID);
    }

    public void save(OutputStream out) throws IOException {
        Writer writer = new OutputStreamWriter(out, "UTF-8");
        try {
            save(writer);
        } finally {
            writer.flush();
        }
    }

    public void save(Writer writer) throws IOException {
        writer.write(RefineServlet.VERSION);
        writer.write('\n');

        Collection<Recon> recons2 = recons.values();
        writer.write("reconCount=" + recons2.size());
        writer.write('\n');

        for (Recon recon : recons2) {
            ParsingUtilities.saveWriter.writeValue(writer, recon);
            writer.write('\n');
        }
    }

    public void load(InputStream is) throws Exception {
        load(new InputStreamReader(is, "UTF-8"));
    }

    public void load(Reader reader) throws Exception {
        LineNumberReader reader2 = new LineNumberReader(reader);

        /* String version = */ reader2.readLine();

        String line;
        while ((line = reader2.readLine()) != null) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);

            if ("reconCandidateCount".equals(field)) {
                int count = Integer.parseInt(value);

                for (int i = 0; i < count; i++) {
                    line = reader2.readLine();
                    if (line != null) {
                        ReconCandidate candidate = ReconCandidate.loadStreaming(line);
                        if (candidate != null) {
                            // pool for backward compatibility
                            pool(candidate);
                        }
                    }
                }
            } else if ("reconCount".equals(field)) {
                int count = Integer.parseInt(value);

                for (int i = 0; i < count; i++) {
                    line = reader2.readLine();
                    if (line != null) {
                        Recon recon = Recon.loadStreaming(line);
                        if (recon != null) {
                            pool(recon);
                        }
                    }
                }
            }
        }
    }
}
