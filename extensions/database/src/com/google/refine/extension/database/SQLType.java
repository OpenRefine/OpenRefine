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

import java.util.HashMap;
import java.util.Map;

public final class SQLType {

    private static final Map<DriverContainer, SQLType> jdbcDriverRegistry = new HashMap<DriverContainer, SQLType>();
    private final DriverContainer driverContainer;

    private SQLType(DriverContainer container) {
        this.driverContainer = container;
    }

    public static SQLType forName(String name) {
        for (SQLType sqlType : jdbcDriverRegistry.values()) {
            if (sqlType.getIdentifier().equalsIgnoreCase(name)) {
                return sqlType;
            }
        }
        return null;
    }

    public static SQLType registerSQLDriver(String identifier, String classpath) {
        return registerSQLDriver(identifier, classpath, true);
    }

    public static SQLType registerSQLDriver(String identifier, String classpath, boolean useJDBCManager) {
        DriverContainer driverContainer = new DriverContainer(identifier, classpath, useJDBCManager);
        if (!jdbcDriverRegistry.containsKey(driverContainer)) {
            SQLType newType = new SQLType(driverContainer);
            jdbcDriverRegistry.put(driverContainer, newType);
            return newType;
        }
        return null;
    }

    public String getClassPath() {
        return this.driverContainer.classpath;
    }

    public String getIdentifier() {
        return this.driverContainer.identifier;
    }

    public boolean usesJDBCManager() {
        return this.driverContainer.useJDBCManager;
    }

    private static class DriverContainer {

        public final String classpath;
        public final String identifier;
        public final boolean useJDBCManager;

        private DriverContainer(String identifier, String classpath, boolean useJDBCManager) {
            this.classpath = classpath;
            this.identifier = identifier;
            this.useJDBCManager = useJDBCManager;
        }

        public final boolean equals(Object obj) {
            return obj instanceof DriverContainer && ((DriverContainer) obj).classpath.equals(this.classpath)
                    && ((DriverContainer) obj).identifier.equals(this.identifier)
                    && ((DriverContainer) obj).useJDBCManager == this.useJDBCManager;
        }
    }
}
