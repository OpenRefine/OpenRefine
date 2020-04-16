package org.openrefine.wikidata.commands;

import java.util.List;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.qa.QAWarning.Severity;
import org.openrefine.wikidata.updates.ItemUpdate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PreviewResults {

    protected List<QAWarning> warnings;
    protected Severity maxSeverity;
    protected int nbWarnings;
    protected int editCount;
    protected List<ItemUpdate> editsPreview;
    
    @JsonProperty("warnings")
    public List<QAWarning> getWarnings() {
    	return warnings;
    }
    
    @JsonProperty("max_severity")
    public Severity getMaxSeverity() {
    	return maxSeverity;
    }
    
    @JsonProperty("nb_warnings")
    public int getNbWarnings() {
    	return nbWarnings;
    }
    
    @JsonProperty("edit_count")
    public int getEditCount() {
    	return editCount;
    }
    
    @JsonProperty("edits_preview")
    public List<ItemUpdate> getEditsPreview() {
    	return editsPreview;
    }
    
    protected PreviewResults(
            List<QAWarning> warnings,
            Severity maxSeverity,
            int nbWarnings,
            int editCount,
            List<ItemUpdate> editsPreview) {
        this.warnings = warnings;
        this.maxSeverity = maxSeverity;
        this.nbWarnings = nbWarnings;
        this.editCount = editCount;
        this.editsPreview = editsPreview;
    }
    
    @Override
    public String toString() {
    	try {
    		ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return super.toString();
		}
    }
}