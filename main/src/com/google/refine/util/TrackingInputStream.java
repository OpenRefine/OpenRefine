package com.google.refine.util;

import java.io.IOException;
import java.io.InputStream;

public class TrackingInputStream extends InputStream {
    final private InputStream is;
    protected long bytesRead;

    public TrackingInputStream(InputStream is) {
        this.is = is;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    @Override
    public int read() throws IOException {
        return (int) track(is.read());
    }

    @Override
    public int read(byte[] b) throws IOException {
        return (int) track(is.read(b));
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return (int) track(is.read(b, off, len));
    }

    @Override
    public long skip(long n) throws IOException {
        return track(is.skip(n));
    }

    @Override
    public void mark(int readlimit) {
        is.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        is.reset();
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }
    
    @Override
    public void close() throws IOException {
        is.close();
    }

    protected long track(long bytesRead) {
        if (bytesRead > 0) {
            this.bytesRead += bytesRead;
        }
        return bytesRead;
    }
}
