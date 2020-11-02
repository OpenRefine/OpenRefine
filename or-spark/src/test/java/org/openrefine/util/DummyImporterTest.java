
package org.openrefine.util;

import org.openrefine.importers.ImporterTest;

public class DummyImporterTest extends ImporterTest {
    /**
     * This class is here to prevent TestNG from running into a weird bug (trying to instantiate ImporterTest itself if
     * it has no subclass). This class should be deleted once we do not rely on PowerMock.
     */
}
