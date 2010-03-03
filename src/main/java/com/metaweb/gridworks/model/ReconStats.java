package com.metaweb.gridworks.model;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Recon.Judgment;

public class ReconStats implements Serializable, Jsonizable {
    private static final long serialVersionUID = -4831409797104437854L;

    final public int    nonBlanks;
    final public int    newTopics;
    final public int    matchedTopics;
    
    public ReconStats(int nonBlanks, int newTopics, int matchedTopics) {
        this.nonBlanks = nonBlanks;
        this.newTopics = newTopics;
        this.matchedTopics = matchedTopics;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("nonBlanks"); writer.value(nonBlanks);
        writer.key("newTopics"); writer.value(newTopics);
        writer.key("matchedTopics"); writer.value(matchedTopics);
        writer.endObject();
    }
    
    static public ReconStats create(Project project, int cellIndex) {
        int nonBlanks = 0;
        int newTopics = 0;
        int matchedTopics = 0;
        
        for (Row row : project.rows) {
            Cell cell = row.getCell(cellIndex);
            if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                nonBlanks++;
                
                if (cell.recon != null) {
                    if (cell.recon.judgment == Judgment.New) {
                        newTopics++;
                    } else if (cell.recon.judgment == Judgment.Matched) {
                        matchedTopics++;
                    }
                }
            }
        }
        
        return new ReconStats(nonBlanks, newTopics, matchedTopics);
    }
}
