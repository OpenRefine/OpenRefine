
package org.openrefine.extension.database;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.extension.database.SimpleTextEncryptor;

public class SimpleTextEncryptorTest {

    @Test
    public void encrypt() {
        SimpleTextEncryptor textEncryptor = new SimpleTextEncryptor("WEWssa!@d445d");
        String password = "testpass";
        String encPass = textEncryptor.encrypt(password);
        Assert.assertNotNull(encPass);
        Assert.assertNotEquals(encPass, password);

    }

    @Test
    public void decrypt() {
        SimpleTextEncryptor textEncryptor = new SimpleTextEncryptor("OOEWssa!@d445d");
        String password = "testpass";
        String encPass = textEncryptor.encrypt(password);
        Assert.assertNotNull(encPass);
        Assert.assertNotEquals(encPass, password);
        String decPass = textEncryptor.decrypt(encPass);
        Assert.assertEquals(decPass, password);
    }
}
