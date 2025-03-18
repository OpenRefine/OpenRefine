
package com.google.refine.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenDirectoryUtilities {

    final static Logger logger = LoggerFactory.getLogger("open-directory-utilities");

    public static void openDirectory(File dir) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            desktop.open(dir);
        } else {
            openDirectoryFallback(String.valueOf(dir));
        }
    }

    private static void openDirectoryFallback(String location) throws IOException {
        Runtime rt = Runtime.getRuntime();

        if (SystemUtils.IS_OS_WINDOWS) {
            rt.exec(new String[] { "rundll32 ", "url.dll,FileProtocolHandler ", location });
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            rt.exec(new String[] { "open ", location });
        } else if (SystemUtils.IS_OS_LINUX) {
            rt.exec(new String[] { "xdg-open", location });
        } else {
            logger.warn(String.format("Java Desktop class not supported on this platform. Please open %s in your native application",
                    location));
        }
    }
}
