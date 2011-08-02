package com.google.refine.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;

import com.google.refine.importing.FormatGuesser;

public class TextFormatGuesser implements FormatGuesser {

    @Override
    public String guess(File file, String encoding, String seedFormat) {
        try {
            InputStream is = new FileInputStream(file);
            try {
                Reader reader = encoding != null ? new InputStreamReader(is, encoding) : new InputStreamReader(is);
                
                int totalBytes = 0;
                int bytes;
                int lineBreaks = 0;
                
                CharBuffer charBuffer = CharBuffer.allocate(4096);
                while (totalBytes < 64 * 1024 && (bytes = reader.read(charBuffer)) > 0) {
                    lineBreaks += countSubstrings(charBuffer.toString(), "\n");
                    
                    charBuffer.clear();
                    totalBytes += bytes;
                }
                
                if (lineBreaks > 3) {
                    return "text/line-based";
                }
            } finally {
                is.close();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static public int countSubstrings(String s, String sub) {
        int count = 0;
        int from = 0;
        while (from < s.length()) {
            int i = s.indexOf(sub, from);
            if (i < 0) {
                break;
            } else {
                from = i + sub.length();
                count++;
            }
        }
        return count;
    }
}
