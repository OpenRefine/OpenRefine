package com.google.refine.model.medadata;

public class MetadataFactory {
    public static IMetadata buildMetadata(MetadataFormat format) {
        IMetadata metadata = null;
        if (format == MetadataFormat.PROJECT_METADATA) {
            metadata = new ProjectMetadata(); 
        } else if (format == MetadataFormat.DATAPACKAGE_METADATA) {
            metadata = new DataPackageMetadata();
        }
        
        return metadata;
    }
}
