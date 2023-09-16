
package org.openrefine.wikibase.manifests;

public class ManifestException extends Exception {

    public ManifestException(String msg) {
        super(msg);
    }

    public ManifestException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
