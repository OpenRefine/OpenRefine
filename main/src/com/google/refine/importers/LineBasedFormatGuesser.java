package com.google.refine.importers;

import java.io.File;

import com.google.refine.importing.FormatGuesser;

public class LineBasedFormatGuesser implements FormatGuesser {

    @Override
    public String guess(File file, String encoding, String seedFormat) {
        SeparatorBasedImporter.Separator sep = SeparatorBasedImporter.guessSeparator(file, encoding);
        if (sep != null) {
            return "text/line-based/*sv";
        }
        int[] widths = FixedWidthImporter.guessColumnWidths(file, encoding);
        if (widths != null && widths.length > 0) {
            return "text/line-based/fixed-width";
        }
        return null;
    }
}
