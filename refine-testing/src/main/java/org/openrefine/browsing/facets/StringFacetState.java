
package org.openrefine.browsing.facets;

import java.util.Collections;
import java.util.Map;

public class StringFacetState implements FacetState, FacetResult {

    private static final long serialVersionUID = -2404479997670686923L;

    public Map<String, Long> occurences;

    public StringFacetState() {
        this(Collections.emptyMap());
    }

    public StringFacetState(Map<String, Long> occurences) {
        this.occurences = occurences;
    }

}
