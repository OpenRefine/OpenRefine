package com.metaweb.gridworks.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {

    private static final int DEFAULT_BUFFER_SIZE = 4 * 1024;

    public static long copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static long copy(InputStream input, File file) throws IOException {
        FileOutputStream output = new FileOutputStream(file);
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n = 0;
        try {
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
        } finally {
            try {
                output.close();
            } catch (IOException e) {}
            try {
                input.close();
            } catch (IOException e) {}
        }
        return count;
    }
    
}
