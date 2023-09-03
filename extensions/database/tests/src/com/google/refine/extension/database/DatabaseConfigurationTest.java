package com.google.refine.extension.database;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class DatabaseConfigurationTest {

    @Test
    public void testToURI() {
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDatabaseType("mysql");
        config.setDatabaseHost("my.host");
        // maliciously crafted database name which attempts to enable local file reads for an exploit
        config.setDatabaseName("test?allowLoadLocalInfile=true#");

        String url = config.toURI().toString();
        // the database name is escaped, preventing the exploit
        assertEquals(url, "jdbc:mysql://my.host/test%3FallowLoadLocalInfile=true%23");
    }
}
