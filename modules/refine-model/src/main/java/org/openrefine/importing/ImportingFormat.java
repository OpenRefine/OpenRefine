
package org.openrefine.importing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ImportingFormat {

    @JsonProperty("id")
    final public String id;
    @JsonProperty("label")
    final public String label;
    @JsonProperty("download")
    final public boolean download;
    @JsonProperty("uiClass")
    final public String uiClass;
    @JsonIgnore
    final public ImportingParser parser;

    ImportingFormat(
            String id,
            String label,
            boolean download,
            String uiClass,
            ImportingParser parser) {
        this.id = id;
        this.label = label;
        this.download = download;
        this.uiClass = uiClass;
        this.parser = parser;
    }
}
