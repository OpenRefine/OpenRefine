/**
 * 
 */
package com.metaweb.gridworks.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.ReconStats;
import com.metaweb.gridworks.model.recon.ReconConfig;
import com.metaweb.gridworks.util.ParsingUtilities;

public class ReconChange extends MassCellChange {
    final protected ReconConfig _newReconConfig;
    protected ReconStats _newReconStats;
    
    protected ReconConfig _oldReconConfig;
    protected ReconStats _oldReconStats;
    
    public ReconChange(
        List<CellChange>    cellChanges,
        String              commonColumnName,
        ReconConfig         newReconConfig,
        ReconStats          newReconStats // can be null
    ) {
        super(cellChanges, commonColumnName, false);
        _newReconConfig = newReconConfig;
        _newReconStats = newReconStats;
    }
    
    public ReconChange(
        CellChange[]    cellChanges,
        String          commonColumnName,
        ReconConfig     newReconConfig,
        ReconStats      newReconStats // can be null
    ) {
        super(cellChanges, commonColumnName, false);
        _newReconConfig = newReconConfig;
        _newReconStats = newReconStats;
    }
    
    public ReconChange(
        CellChange         cellChange,
        String             commonColumnName,
        ReconConfig     newReconConfig,
        ReconStats        newReconStats // can be null
    ) {
        super(cellChange, commonColumnName, false);
        _newReconConfig = newReconConfig;
        _newReconStats = newReconStats;
    }
    
    @Override
    public void apply(Project project) {
        synchronized (project) {
            super.apply(project);
            
            Column column = project.columnModel.getColumnByName(_commonColumnName);
            
            if (_newReconStats == null) {
                _newReconStats = ReconStats.create(project, column.getCellIndex());
            }
            
            _oldReconConfig = column.getReconConfig();
            _oldReconStats = column.getReconStats();
            
            column.setReconConfig(_newReconConfig);
            column.setReconStats(_newReconStats);
            
            column.clearPrecomputes();
        }
    }
    
    @Override
    public void revert(Project project) {
        synchronized (project) {
            super.revert(project);
            
            Column column = project.columnModel.getColumnByName(_commonColumnName);
            column.setReconConfig(_oldReconConfig);
            column.setReconStats(_oldReconStats);
            
            column.clearPrecomputes();
        }
    }
    
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("newReconConfig="); 
        if (_newReconConfig != null) {
            _newReconConfig.save(writer); 
        }
        writer.write('\n');
        
        writer.write("newReconStats=");
        if (_newReconStats != null) {
            _newReconStats.save(writer);
        }
        writer.write('\n');
        
        writer.write("oldReconConfig=");
        if (_oldReconConfig != null) {
            _oldReconConfig.save(writer); 
        }
        writer.write('\n');
        
        writer.write("oldReconStats=");
        if (_oldReconStats != null) {
            _oldReconStats.save(writer); 
        }
        writer.write('\n');
        
        super.save(writer, options);
    }
    
    static public Change load(LineNumberReader reader) throws Exception {
        ReconConfig newReconConfig = null;
        ReconStats newReconStats = null;
        ReconConfig oldReconConfig = null;
        ReconStats oldReconStats = null;
        
        String commonColumnName = null;
        CellChange[] cellChanges = null;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);
            
            if ("newReconConfig".equals(field)) {
                if (value.length() > 0) {
                    newReconConfig = ReconConfig.reconstruct(ParsingUtilities.evaluateJsonStringToObject(value));
                }
            } else if ("newReconStats".equals(field)) {
                if (value.length() > 0) {
                    newReconStats = ReconStats.load(ParsingUtilities.evaluateJsonStringToObject(value));
                }
            } else if ("oldReconConfig".equals(field)) {
                if (value.length() > 0) {
                    oldReconConfig = ReconConfig.reconstruct(ParsingUtilities.evaluateJsonStringToObject(value));
                }
            } else if ("oldReconStats".equals(field)) {
                if (value.length() > 0) {
                    oldReconStats = ReconStats.load(ParsingUtilities.evaluateJsonStringToObject(value));
                }
            } else if ("commonColumnName".equals(field)) {
                commonColumnName = value;
            } else if ("cellChangeCount".equals(field)) {
                int cellChangeCount = Integer.parseInt(value);
                
                cellChanges = new CellChange[cellChangeCount];
                for (int i = 0; i < cellChangeCount; i++) {
                    cellChanges[i] = CellChange.load(reader);
                }
            }
        }
        
        ReconChange change = new ReconChange(
                cellChanges, commonColumnName, newReconConfig, newReconStats);
        
        change._oldReconConfig = oldReconConfig;
        change._oldReconStats = oldReconStats;
        
        return change;
    }
}