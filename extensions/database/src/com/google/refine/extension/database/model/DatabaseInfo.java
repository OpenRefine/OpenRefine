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

package com.google.refine.extension.database.model;

import java.util.ArrayList;
import java.util.List;

public class DatabaseInfo {

    private List<DatabaseTable> tables;
    private int dbMajorVersion;
    private int dbMinorVersion;
    private String dbProductVersion;
    private String dbProductName;

    private ArrayList<DatabaseColumn> columns;
    private List<DatabaseRow> rows;

    public DatabaseInfo() {
        // TODO Auto-generated constructor stub
    }

    public List<DatabaseTable> getTables() {
        return tables;
    }

    public void setTables(List<DatabaseTable> tables) {
        this.tables = tables;
    }

    public void setDatabaseMajorVersion(int dbMajorVersion) {
        this.dbMajorVersion = dbMajorVersion;

    }

    public void setDatabaseMinorVersion(int dbMinorVersion) {
        this.dbMinorVersion = dbMinorVersion;

    }

    public void setDatabaseProductVersion(String dbProductVersion) {
        this.dbProductVersion = dbProductVersion;

    }

    public void setDatabaseProductName(String dbProductName) {
        this.dbProductName = dbProductName;

    }

    public int getDbMajorVersion() {
        return dbMajorVersion;
    }

    public void setDbMajorVersion(int dbMajorVersion) {
        this.dbMajorVersion = dbMajorVersion;
    }

    public int getDbMinorVersion() {
        return dbMinorVersion;
    }

    public void setDbMinorVersion(int dbMinorVersion) {
        this.dbMinorVersion = dbMinorVersion;
    }

    public String getDbProductVersion() {
        return dbProductVersion;
    }

    public void setDbProductVersion(String dbProductVersion) {
        this.dbProductVersion = dbProductVersion;
    }

    public String getDbProductName() {
        return dbProductName;
    }

    public void setDbProductName(String dbProductName) {
        this.dbProductName = dbProductName;
    }

    public void setColumns(ArrayList<DatabaseColumn> columns) {
        this.columns = columns;

    }

    public void setRows(List<DatabaseRow> rows) {
        this.rows = rows;

    }

    public ArrayList<DatabaseColumn> getColumns() {
        return columns;
    }

    public List<DatabaseRow> getRows() {
        return rows;
    }

    @Override
    public String toString() {
        return "DatabaseInfo [tables=" + tables + ", dbMajorVersion=" + dbMajorVersion + ", dbMinorVersion="
                + dbMinorVersion + ", dbProductVersion=" + dbProductVersion + ", dbProductName=" + dbProductName + "]";
    }

}
