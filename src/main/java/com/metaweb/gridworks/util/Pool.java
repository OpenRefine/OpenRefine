package com.metaweb.gridworks.util;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Gridworks;
import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;

public class Pool implements Jsonizable {
    final protected Map<String, ReconCandidate> candidates = new HashMap<String, ReconCandidate>();
    final protected Map<String, Recon> recons = new HashMap<String, Recon>();
    
    public void pool(ReconCandidate candidate) {
        candidates.put(candidate.topicID, candidate);
    }
    
    public void pool(Recon recon) {
        recons.put(Long.toString(recon.id), recon);
        
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
        Writer writer = new OutputStreamWriter(out);
        try {
            save(writer);
        } finally {
            writer.flush();
        }
    }
    
    public void save(Writer writer) throws IOException {
        writer.write(Gridworks.getVersion()); writer.write('\n');

        Properties options = new Properties();
        options.setProperty("mode", "save");
        options.put("pool", this);
        
        Collection<ReconCandidate> candidates2 = candidates.values();
        writer.write("reconCandidateCount=" + candidates2.size()); writer.write('\n');
        
        for (ReconCandidate c : candidates2) {
            JSONWriter jsonWriter = new JSONWriter(writer);
            try {
                c.write(jsonWriter, options);
                
                writer.write('\n');
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        Collection<Recon> recons2 = recons.values();
        writer.write("reconCount=" + recons2.size()); writer.write('\n');
        
        for (Recon recon : recons2) {
            JSONWriter jsonWriter = new JSONWriter(writer);
            try {
                recon.write(jsonWriter, options);
                
                writer.write('\n');
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
                            pool(candidate);
                        }
                    }
                }
            } else if ("reconCount".equals(field)) {
                int count = Integer.parseInt(value);
                
                for (int i = 0; i < count; i++) {
                    line = reader2.readLine();
                    if (line != null) {
                        Recon recon = Recon.loadStreaming(line, this);
                        if (recon != null) {
                            pool(recon);
                        }
                    }
                }
            }
        }
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        
        writer.key("reconCandidates");
            writer.object();
            for (Entry<String, ReconCandidate> entry : candidates.entrySet()) {
                writer.key(entry.getKey());
                entry.getValue().write(writer, options);
            }
            writer.endObject();
        
        writer.key("recons");
            writer.object();
            for (Entry<String, Recon> entry : recons.entrySet()) {
                writer.key(entry.getKey().toString());
                entry.getValue().write(writer, options);
            }
            writer.endObject();
            
        writer.endObject();
    }
}
