package com.metaweb.gridworks.model.changes;

 import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.History;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.Pool;

public class MassChange implements Change {
    final protected List<? extends Change> _changes;
    final protected boolean                _updateRowContextDependencies;
    
    public MassChange(List<? extends Change> changes, boolean updateRowContextDependencies) {
        _changes = changes;
        _updateRowContextDependencies = updateRowContextDependencies;
    }
    
    public void apply(Project project) {
        synchronized (project) {
            for (Change change : _changes) {
                change.apply(project);
            }
            
            if (_updateRowContextDependencies) {
                project.recomputeRowContextDependencies();
            }
        }
    }

    public void revert(Project project) {
        synchronized (project) {
            for (Change change : _changes) {
                change.revert(project);
            }
            
            if (_updateRowContextDependencies) {
                project.recomputeRowContextDependencies();
            }
        }
    }
    
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("updateRowContextDependencies="); writer.write(Boolean.toString(_updateRowContextDependencies)); writer.write('\n');
        writer.write("changeCount="); writer.write(Integer.toString(_changes.size())); writer.write('\n');
        for (Change c : _changes) {
            History.writeOneChange(writer, c, options);
        }
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        boolean updateRowContextDependencies = false;
        List<Change> changes = null;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            
            if ("updateRowContextDependencies".equals(field)) {
                updateRowContextDependencies = Boolean.parseBoolean(line.substring(equal + 1));
            } else if ("changeCount".equals(field)) {
                int changeCount = Integer.parseInt(line.substring(equal + 1));
                
                changes = new ArrayList<Change>(changeCount);
                for (int i = 0; i < changeCount; i++) {
                    changes.add(History.readOneChange(reader, pool));
                }
            }
        }
        
        MassChange change = new MassChange(changes, updateRowContextDependencies);
        
        return change;
    }
}
