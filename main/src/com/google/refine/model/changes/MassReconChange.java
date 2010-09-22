package com.google.refine.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.history.Change;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;
import com.google.refine.util.Pool;

public class MassReconChange implements Change {
    final protected Map<Long, Recon> _newRecons;
    final protected Map<Long, Recon> _oldRecons;
    
    public MassReconChange(Map<Long, Recon> newRecons, Map<Long, Recon> oldRecons) {
        _newRecons = newRecons;
        _oldRecons = oldRecons;
    }
    
    public void apply(Project project) {
        switchRecons(project, _newRecons);
    }
    
    public void revert(Project project) {
        switchRecons(project, _oldRecons);
    }
    
    protected void switchRecons(Project project, Map<Long, Recon> reconMap) {
        synchronized (project) {
            for (int r = 0; r < project.rows.size(); r++) {
                Row row = project.rows.get(r);
                
                for (int c = 0; c < row.cells.size(); c++) {
                    Cell cell = row.cells.get(c);
                    if (cell != null && cell.recon != null) {
                        Recon recon = cell.recon;
                        
                        if (reconMap.containsKey(recon.id)) {
                            row.setCell(c, new Cell(cell.value, reconMap.get(recon.id)));
                        }
                    }
                }
            }
        }
    }

    public void save(Writer writer, Properties options) throws IOException {
        writeRecons(writer, options, _oldRecons, "oldReconCount");
        writeRecons(writer, options, _newRecons, "newReconCount");
        writer.write("/ec/\n"); // end of change marker
    }
    
    protected void writeRecons(Writer writer, Properties options, Map<Long, Recon> recons, String key) throws IOException {
        writer.write(key + "="); writer.write(Integer.toString(recons.size())); writer.write('\n');
        for (Recon recon : recons.values()) {
            Pool pool = (Pool) options.get("pool");
            pool.poolReconCandidates(recon);

            JSONWriter jsonWriter = new JSONWriter(writer);
            try {
                recon.write(jsonWriter, options);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            writer.write("\n");
        }
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        Map<Long, Recon> oldRecons = new HashMap<Long, Recon>();
        Map<Long, Recon> newRecons = new HashMap<Long, Recon>();
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);
            
            if ("oldReconCount".equals(field)) {
                loadRecons(reader, pool, oldRecons, value);
            } else if ("newReconCount".equals(field)) {
                loadRecons(reader, pool, newRecons, value);
            }
        }
        
        MassReconChange change = new MassReconChange(newRecons, oldRecons);
        
        return change;
    }
    
    static protected void loadRecons(LineNumberReader reader, Pool pool, Map<Long, Recon> recons, String countString) throws Exception {
        int count = Integer.parseInt(countString);
        
        for (int i = 0; i < count; i++) {
            String line = reader.readLine();
            Recon recon = Recon.loadStreaming(line, pool);
            
            recons.put(recon.id, recon);
        }
    }
}

