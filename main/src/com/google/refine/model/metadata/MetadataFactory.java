package com.google.refine.model.metadata;

import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

import io.frictionlessdata.datapackage.Package;
import io.frictionlessdata.datapackage.Resource;
import io.frictionlessdata.datapackage.exceptions.DataPackageException;

public class MetadataFactory {
    private final static Logger logger = LoggerFactory.getLogger(MetadataFactory.class);
    
    /**
     * Build metadata based on the format
     * @param format
     * @return
     */
    public static IMetadata buildMetadata(MetadataFormat format) {
        IMetadata metadata = null;
        if (format == MetadataFormat.PROJECT_METADATA) {
            metadata = new ProjectMetadata(); 
        } else if (format == MetadataFormat.DATAPACKAGE_METADATA) {
            metadata = new DataPackageMetadata();
        }
        
        return metadata;
    }
    
    /**
     * build an empty Data Package Metadata
     * @return
     */
    public static DataPackageMetadata buildDataPackageMetadata() {
        return (DataPackageMetadata) buildMetadata(MetadataFormat.DATAPACKAGE_METADATA);
    }
    
    /**
     * Build an empty data package metadata, then populate the fields from the Project Metadata
     * @param project
     * @return
     */
    public static DataPackageMetadata buildDataPackageMetadata(Project project) {
        DataPackageMetadata dpm = buildDataPackageMetadata();
        ProjectMetadata pmd = project.getMetadata();
        Package pkg = dpm.getPackage();
        Resource resource = SchemaExtension.createResource(project.getMetadata().getName(),
                project.columnModel);
        try {
            pkg.addResource(resource);
            
            putValue(pkg, Package.JSON_KEY_NAME, pmd.getName());
            putValue(pkg, PackageExtension.JSON_KEY_LAST_UPDATED, ParsingUtilities.localDateToString(pmd.getModified()));
            putValue(pkg, PackageExtension.JSON_KEY_DESCRIPTION, pmd.getDescription());
            putValue(pkg, PackageExtension.JSON_KEY_TITLE, pmd.getTitle());
            putValue(pkg, PackageExtension.JSON_KEY_HOMEPAGE, pmd.getHomepage());
            putValue(pkg, PackageExtension.JSON_KEY_IMAGE, pmd.getImage());
            putValue(pkg, PackageExtension.JSON_KEY_LICENSE, pmd.getLicense());
            
            pkg.removeProperty(PackageExtension.JSON_KEY_KEYWORKS);
            pkg.addProperty(PackageExtension.JSON_KEY_KEYWORKS, JSONUtilities.arrayToJSONArray(pmd.getTags()));
        } catch (ValidationException | IOException | DataPackageException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
        
        return dpm;
    }
    
    private static void putValue(Package pkg, String key, String value) throws DataPackageException {
        if(pkg.getJson().has(key)) {
            pkg.removeProperty(key);
        }
        pkg.addProperty(key, value);
    }
}
