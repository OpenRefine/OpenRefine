
package org.openrefine.model;

import java.util.Collections;
import java.util.Map;

/**
 * Supplies configuration parameters to a datamodel runner from a simple hash map.
 */
public class RunnerConfigurationImpl extends RunnerConfiguration {

    private final Map<String, String> map;

    public RunnerConfigurationImpl() {
        this.map = Collections.emptyMap();
    }

    public RunnerConfigurationImpl(Map<String, String> map) {
        this.map = map;
    }

    @Override
    public String getParameter(String key, String defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

}
