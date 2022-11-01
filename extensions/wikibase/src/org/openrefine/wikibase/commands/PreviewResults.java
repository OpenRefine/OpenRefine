
package org.openrefine.wikibase.commands;

import java.util.List;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.qa.QAWarning.Severity;
import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PreviewResults {

    protected List<QAWarning> warnings;
    protected Severity maxSeverity;
    protected int nbWarnings;
    protected int editCount;
    protected List<EntityEdit> editsPreview;

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
    public List<EntityEdit> getEditsPreview() {
        return editsPreview;
    }

    protected PreviewResults(
            List<QAWarning> warnings,
            Severity maxSeverity,
            int nbWarnings,
            int editCount,
            List<EntityEdit> firstEdits) {
        this.warnings = warnings;
        this.maxSeverity = maxSeverity;
        this.nbWarnings = nbWarnings;
        this.editCount = editCount;
        this.editsPreview = firstEdits;
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
