
package com.google.refine.util;

import java.util.regex.Pattern;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HttpClientTests {

    @Test
    public void fromHostsToPattern() {
        String strPattern1 = "localhost|127.0.0.1";
        Pattern pattern1 = HttpClient.fromHostsToPattern(strPattern1);
        Assert.assertTrue(pattern1.matcher("localhost").matches());
        Assert.assertFalse(pattern1.matcher("host.domain.org").matches());

        String strPattern2 = "*.domain.org";
        Pattern pattern2 = HttpClient.fromHostsToPattern(strPattern2);
        Assert.assertFalse(pattern2.matcher("localhost").matches());
        Assert.assertTrue(pattern2.matcher("host.domain.org").matches());

        String strPattern3 = "*.domain.org|*.any.*|myhosts.*";
        Pattern pattern3 = HttpClient.fromHostsToPattern(strPattern3);
        Assert.assertFalse(pattern3.matcher("localhost").matches());
        Assert.assertTrue(pattern3.matcher("host.domain.org").matches());
        Assert.assertTrue(pattern3.matcher("random.domain.any.com").matches());
        Assert.assertTrue(pattern3.matcher("myhosts.mydomain.mine").matches());
    }
}
