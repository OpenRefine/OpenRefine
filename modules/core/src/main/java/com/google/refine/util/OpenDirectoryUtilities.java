
package com.google.refine.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class OpenDirectoryUtilities {

    public static void openDirectory(File dir) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            desktop.open(dir);
        } else /* if Mac */ {
            Runtime.getRuntime().exec(
                    "open .",
                    new String[] {},
                    dir);
        }
    }
}
