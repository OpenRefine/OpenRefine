package com.google.refine.model.metadata;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.frictionlessdata.datapackage.Package;
import io.frictionlessdata.datapackage.exceptions.DataPackageException;

/** 
 * This class contains some methods which is not included in the official "Data Package" repo for now.
 * Some methods can be removed after the official library provide the corresponding function.
 */
public class PackageExtension {
    private final static Logger logger = LoggerFactory.getLogger(PackageExtension.class);
    
    private static final int JSON_INDENT_FACTOR = 4;
    
    public static final String JSON_KEY_LAST_UPDATED = "last_updated";
    public static final String JSON_KEY_DESCRIPTION = "description";
    public static final String JSON_KEY_KEYWORKS = "keywords";
    public static final String JSON_KEY_TITLE = "title";
    public static final String JSON_KEY_HOMEPAGE = "homepage";
    public static final String JSON_KEY_IMAGE = "image"; 
    public static final String JSON_KEY_LICENSE = "license";
    public static final String JSON_KEY_VERSION = "version";
    
    public static String DATAPACKAGE_TEMPLATE_FILE = "schemas/datapackage-template.json";
    
    /**
     * Do the package since the final spec for the compression/bundle are not settled yet.
     * https://github.com/frictionlessdata/datapackage-js/issues/93
     * 
     * @param pkg Package 
     * @param dataByteArrayOutputStream  ByteArrayOutputStream
     * @param destOs OutputStream
     * @throws IOException 
     * @throws FileNotFoundException 
     * @see Package#saveZip(String outputFilePath) 
     */
    public static void saveZip(Package pkg, final ByteArrayOutputStream dataByteArrayOutputStream, final OutputStream destOs) throws FileNotFoundException, IOException {
                try(ZipOutputStream zos = new ZipOutputStream(destOs)){
                    // json file 
                    ZipEntry entry = new ZipEntry(DataPackageMetadata.DEFAULT_FILE_NAME); 
                    zos.putNextEntry(entry);
                    zos.write(pkg.getJson().toString(JSON_INDENT_FACTOR).getBytes());
                    zos.closeEntry();
                    // default data file to data.csv or given path(can only handle one file because files cannot be restored)
                    String path = (String) pkg.getResources().get(0).getPath();
                    entry = new ZipEntry(StringUtils.isBlank(path) ? "data.csv" : path);
                    zos.putNextEntry(entry);
                    zos.write(dataByteArrayOutputStream.toByteArray());
                    zos.closeEntry();
                }           
    }
    
    /**
     * To build a Package object from a template file contains empty metadata
     *  
     * @param templateFile
     */
    public static Package buildPackageFromTemplate() {
        try {
            ClassLoader classLoader = PackageExtension.class.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(DATAPACKAGE_TEMPLATE_FILE);
            return new Package(IOUtils.toString(inputStream), false);
        } catch (ValidationException e) {
            logger.error("validation failed", ExceptionUtils.getStackTrace(e));
        } catch (DataPackageException e) {
            logger.error("DataPackage Exception", ExceptionUtils.getStackTrace(e));
        } catch (IOException e) {
            logger.error("IOException when build package from template", ExceptionUtils.getStackTrace(e));
        }
        
        return null;
    }
}
