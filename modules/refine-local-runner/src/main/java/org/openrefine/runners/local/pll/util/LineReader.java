
package org.openrefine.runners.local.pll.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.openrefine.runners.local.pll.TextFilePLL;

/**
 * A class which reads line by line from an {@link java.io.InputStream}, making sure it only reads the bytes it needs to
 * complete the current line.
 * <p>
 * This makes it possible to track at which byte offset each line starts, when used in combination with a
 * {@link com.google.common.io.CountingInputStream} (see {@link TextFilePLL}).
 * <p>
 * This cannot be achieved with {@link java.io.InputStreamReader} and {@link java.io.LineNumberReader}, because
 * {@link java.io.InputStreamReader} can read more bytes than it needs to output characters.
 * <p>
 * Like in {@link java.io.LineNumberReader}, a line is considered to be terminated by any one of a line feed ('\n'), a
 * carriage return ('\r'), or a carriage return followed immediately by a line feed.
 * <p>
 * TODO: this class is much less efficient than LineNumberReader. We should investigate why.
 * 
 * @author Antonin Delpeuch
 */
public class LineReader implements Closeable {

    protected final InputStream inputStream;
    protected final Charset charset;

    /**
     * Constructs a reader with the supplied input stream and charset.
     * 
     * @param inputStream
     * @param charset
     */
    public LineReader(InputStream inputStream, Charset charset) {
        this.inputStream = inputStream;
        this.charset = charset;
    }

    /**
     * Reads a single line from the input stream, not reading more characters beyond the line being read.
     * 
     * @return the line read or null if the input stream was exhausted
     * @throws IOException
     */
    public String readLine() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int currentByte = inputStream.read();
        while (currentByte >= 0) {
            if (currentByte == '\n') {
                currentByte = -2;
            } else if (currentByte == '\r') {
                // Swallow the next '\n' character if it immediately follows this one
                inputStream.mark(2);
                int nextByte = inputStream.read();
                if (nextByte != '\n') {
                    // if the next character was not '\n', put the stream back where it was
                    inputStream.reset();
                }
                currentByte = -2;
            } else {
                baos.write(currentByte);
                currentByte = inputStream.read();
            }
        }
        if (baos.size() > 0 || currentByte == -2) {
            return baos.toString(charset);
        } else {
            return null;
        }
    }

    /**
     * Closes the underlying input stream.
     */
    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
