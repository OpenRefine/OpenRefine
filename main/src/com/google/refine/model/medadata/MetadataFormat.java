package com.google.refine.model.medadata;


/**
 * A list of supported metadata format 
 *
 */
public enum MetadataFormat {
    PROJECT_METADATA("PROJECT_METADATA"),
    OKF_METADATA("OKF_METADATA"),
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
