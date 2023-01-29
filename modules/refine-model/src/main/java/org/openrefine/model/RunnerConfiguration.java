
package org.openrefine.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object supplying configuration parameters to a datamodel instance.
 * 
 */
public abstract class RunnerConfiguration {

    protected final Logger logger = LoggerFactory.getLogger(RunnerConfiguration.class);

    public static final RunnerConfiguration empty = new RunnerConfigurationImpl();

    /**
     * Retrieves a configuration parameter as a string.
     * 
     * @param key
     *            the key of the configuration parameter
     * @param defaultValue
     *            the default value to return if the configuration parameter was not supplied
     */
    public abstract String getParameter(String key, String defaultValue);

    /**
     * Retrieves a configuration parameter as an integer.
     * 
     * @param key
     *            the key of the configuration parameter
     * @param defaultValue
     *            the default value to return if the configuration parameter was not supplied
     */
    public int getIntParameter(String key, int defaultValue) {
        String stringValue = getParameter(key, Integer.toString(defaultValue));
        try {
            return Integer.valueOf(stringValue);
        } catch (NumberFormatException e) {
            logger.warn(String.format(
                    "Invalid parameter value '%s' for configuration setting %s, falling back to default value %d",
                    stringValue, key, defaultValue));
            return defaultValue;
        }
    }

    /**
     * Retrieves a configuration parameter as a long.
     * 
     * @param key
     *            the key of the configuration parameter
     * @param defaultValue
     *            the default value to return if the configuration parameter was not supplied
     */
    public long getLongParameter(String key, long defaultValue) {
        String stringValue = getParameter(key, Long.toString(defaultValue));
        try {
            return Long.valueOf(stringValue);
        } catch (NumberFormatException e) {
            logger.warn(String.format(
                    "Invalid parameter value '%s' for configuration setting %s, falling back to default value %d",
                    stringValue, key, defaultValue));
            return defaultValue;
        }
    }
}
