package com.google.refine.importers;

import java.io.File;

import com.google.refine.importing.FormatGuesser;

public class BinaryFormatGuesser implements FormatGuesser {

    @Override
    public String guess(File file, String encoding, String seedFormat) {

        // TODO: Guess based on sniffing magic numbers
        
        return null;
    }
}
