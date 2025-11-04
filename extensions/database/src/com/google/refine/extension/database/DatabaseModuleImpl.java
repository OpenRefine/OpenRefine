/*
 * Copyright (c) 2017, Tony Opara
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this 
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * 
 * Neither the name of Google nor the names of its contributors may be used to 
 * endorse or promote products derived from this software without specific 
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.refine.extension.database;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.servlet.ServletConfig;

import edu.mit.simile.butterfly.ButterflyModuleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseModuleImpl extends ButterflyModuleImpl {

    private static final Logger logger = LoggerFactory.getLogger("DatabaseModuleImpl");

    public static DatabaseModuleImpl instance;

    public static Properties extensionProperties;

    static final int DEFAULT_CREATE_BATCH_SIZE = 100;
    static final int DEFAULT_PREVIEW_BATCH_SIZE = 100;

    @Override
    public void init(ServletConfig config)
            throws Exception {
        // TODO Auto-generated method stub
        super.init(config);

        readModuleProperty();

        // Set the singleton.
        instance = this;

        logger.trace("Database Extension module initialization completed");
    }

    public static int getCreateBatchSize() {
        return getBatchSize("create.batchSize", DEFAULT_CREATE_BATCH_SIZE);
    }

    public static int getPreviewBatchSize() {
        return getBatchSize("preview.batchSize", DEFAULT_PREVIEW_BATCH_SIZE);
    }

    private static int getBatchSize(String propertyName, int defaultValue) {
        if (extensionProperties == null) {
            return defaultValue;
        }

        String propBatchSize = extensionProperties.getProperty(propertyName);
        if (propBatchSize == null || propBatchSize.isEmpty()) {
            return defaultValue;
        }

        try {
            int batchSize = Integer.parseInt(propBatchSize);

            if (batchSize > 0) {
                return batchSize;
            }
        } catch (NumberFormatException nfe) {
            logger.warn("Error parsing {} property as Integer ({})", propertyName, propBatchSize);
        }

        return defaultValue;
    }

    /**
     * @deprecated use {@link #getCreateBatchSize()} instead.
     */
    @Deprecated(since = "3.9")
    public static String getImportCreateBatchSize() {
        return String.valueOf(getCreateBatchSize());
    }

    /**
     * @deprecated use {@link #getPreviewBatchSize()} instead.
     */
    @Deprecated(since = "3.9")
    public static String getImportPreviewBatchSize() {
        return String.valueOf(getPreviewBatchSize());
    }

    private void readModuleProperty() {
        // The module path
        File f = getPath();
        if (logger.isDebugEnabled()) {
            logger.debug("Module getPath(): {}", f.getPath());
        }

        // Load our custom properties.
        File modFile = new File(f, "MOD-INF");
        if (logger.isDebugEnabled()) {
            logger.debug("Module File: {}", modFile.getPath());
        }

        if (modFile.exists()) {

            extensionProperties = loadProperties(new File(modFile, "dbextension.properties"));

        }

    }

    private Properties loadProperties(File propFile) {
        Properties ps = new Properties();
        try {
            if (propFile.exists()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Loading Extension properties ({})", propFile);
                }

                try (final BufferedInputStream stream = new BufferedInputStream(new FileInputStream(propFile))) {
                    ps = new Properties();
                    ps.load(stream);
                }

            }
        } catch (Exception e) {
            logger.error("Error loading Database properties", e);
            /*
             * During an exception reading 'dbextension.properties' (security or no permissions for example) the try
             * with resources will autoclose the stream for us. And then we log the exception.
             */
        }
        return ps;
    }

}
