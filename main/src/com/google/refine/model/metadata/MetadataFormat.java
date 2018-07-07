package com.google.refine.model.metadata;


/**
 * A list of supported metadata format 
 *
 */
public enum MetadataFormat {
    UNKNOWN("UNKNOWN"),
    PROJECT_METADATA("PROJECT_METADATA"),
    DATAPACKAGE_METADATA("DATAPACKAGE_METADATA"),
    CSVW_METADATA("CSVW_METADATA");
    
    private final String format;
    
    private MetadataFormat(final String format) {
        this.format = format;
    }
    
    @Override
    public String toString() {
        return format;
    }
}
