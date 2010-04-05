package com.metaweb.gridworks.history;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.model.Project;

public class ChangeSequence implements Change {
    final protected Change[] _changes;
    
    public ChangeSequence(Change[] changes) {
        _changes = changes;
    }

    public void apply(Project project) {
        synchronized (project) {
            for (int i = 0; i < _changes.length; i++) {
                _changes[i].apply(project);
            }
        }
    }

    public void revert(Project project) {
        synchronized (project) {
            for (int i = _changes.length - 1; i >= 0 ; i--) {
                _changes[i].apply(project);
            }
        }
    }

    public void save(Writer writer, Properties options) throws IOException {
        writer.write("count="); writer.write(Integer.toString(_changes.length)); writer.write('\n');
        for (int i = 0; i < _changes.length; i++) {
            Change change = _changes[i];
            
            writer.write(change.getClass().getName()); writer.write('\n');
            
            change.save(writer, options);
        }
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader) throws Exception {
        String line = reader.readLine();
        if (line == null) line = "";
        int equal = line.indexOf('=');
        
        assert "count".equals(line.substring(0, equal));
        
        int count = Integer.parseInt(line.substring(equal + 1));
        Change[] changes = new Change[count];
        
        for (int i = 0; i < count; i++) {
            changes[i] = History.readOneChange(reader);
        }
        
        line = reader.readLine();
        assert "/ec/".equals(line);
        
        return new ChangeSequence(changes);
    }
}
