
package com.google.refine.extension.database;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import org.testng.annotations.Test;

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

    @Test
    public void testSetMaliciousHost() {
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDatabaseType("mysql");

        assertThrows(IllegalArgumentException.class,
                () -> config.setDatabaseHost("127.0.0.1:3306,(allowLoadLocalInfile=true,allowUrlInLocalInfile=true),127.0.0.1"));
    }
}
