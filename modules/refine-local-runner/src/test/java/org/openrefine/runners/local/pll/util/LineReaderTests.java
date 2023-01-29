
package org.openrefine.runners.local.pll.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.google.common.io.CountingInputStream;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LineReaderTests {

    Charset UTF8 = Charset.forName("UTF-8");

    protected InputStream asInputStream(String input) {
        return new ByteArrayInputStream(input.getBytes(UTF8));
    }

    @Test
    public void testNotBuffering() throws IOException {
        InputStream bais = asInputStream("hello\nworld");
        CountingInputStream countingInputStream = new CountingInputStream(bais);

        try (LineReader reader = new LineReader(countingInputStream, UTF8)) {
            String result = reader.readLine();
            Assert.assertEquals(result, "hello");
            Assert.assertEquals(countingInputStream.getCount(), 6);
        }
    }

    @Test
    public void testCarriageReturn() throws IOException {
        InputStream bais = asInputStream("hello\rworld");
        CountingInputStream countingInputStream = new CountingInputStream(bais);

        try (LineReader reader = new LineReader(countingInputStream, UTF8)) {
            String result = reader.readLine();
            Assert.assertEquals(result, "hello");
            Assert.assertEquals(countingInputStream.getCount(), 6);
        }
    }

    @Test
    public void testCarriageReturnAndNewline() throws IOException {
        InputStream bais = asInputStream("hello\r\nworld");
        CountingInputStream countingInputStream = new CountingInputStream(bais);

        try (LineReader reader = new LineReader(countingInputStream, UTF8)) {
            String result = reader.readLine();
            Assert.assertEquals(result, "hello");
            Assert.assertEquals(countingInputStream.getCount(), 7);
        }
    }

    @Test
    public void testReadAllLines() throws IOException {
        InputStream bais = asInputStream("hello\n\nworld");

        try (LineReader reader = new LineReader(bais, UTF8)) {
            String result = reader.readLine();
            Assert.assertEquals(result, "hello");
            result = reader.readLine();
            Assert.assertEquals(result, "");
            result = reader.readLine();
            Assert.assertEquals(result, "world");
            // here we detect the end of the file
            result = reader.readLine();
            Assert.assertNull(result);
            // and if we try to read more, we still get null
            result = reader.readLine();
            Assert.assertNull(result);
        }
    }

    @Test
    public void testTrailingNewLine() throws IOException {
        InputStream bais = asInputStream("hello\nworld\n");

        try (LineReader reader = new LineReader(bais, UTF8)) {
            String result = reader.readLine();
            Assert.assertEquals(result, "hello");
            result = reader.readLine();
            Assert.assertEquals(result, "world");
            result = reader.readLine();
            Assert.assertNull(result);
        }
    }

    @Test
    public void testUnicode() throws IOException {
        // the unicode '€' character is encoded on three bytes in UTF-8

        InputStream bais = asInputStream("h€llo\nworld");
        CountingInputStream countingInputStream = new CountingInputStream(bais);

        try (LineReader reader = new LineReader(countingInputStream, UTF8)) {
            String result = reader.readLine();
            Assert.assertEquals(result, "h€llo");
            Assert.assertEquals(countingInputStream.getCount(), 8);
        }
    }

    @Test
    public void testIgnoresIncompleteUnicode() throws IOException {
        // the unicode '€' character is encoded on three bytes in UTF-8

        InputStream bais = asInputStream("h€llo\nworld");
        // Read the first two bytes, which means the cursor is in the middle of the encoding of the € sign
        bais.read();
        bais.read();

        CountingInputStream countingInputStream = new CountingInputStream(bais);

        try (LineReader reader = new LineReader(countingInputStream, UTF8)) {
            String result = reader.readLine();
            Assert.assertTrue(result.endsWith("llo"));
            Assert.assertEquals(countingInputStream.getCount(), 6);
        }
    }

}
