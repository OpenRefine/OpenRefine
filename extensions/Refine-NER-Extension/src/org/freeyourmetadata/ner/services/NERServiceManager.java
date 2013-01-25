package org.freeyourmetadata.ner.services;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import com.google.refine.util.JSONUtilities;

/**
 * Manager that reads and stores service configurations in JSON
 * @author Ruben Verborgh
 */
public class NERServiceManager {
    private final static Logger LOGGER = Logger.getLogger(NERServiceManager.class);
    
    private final TreeMap<String, NERService> services;
    private final File settingsFile;
    
    /**
     * Creates a new <tt>NERServiceManager</tt>
     * @param settingsFile JSON file to read and store settings (might not exist yet)
     * @throws IOException if the settings file cannot be read
     * @throws JSONException if the settings file contains invalid JSON
     * @throws ClassNotFoundException if a service cannot be instantiated
     */
    public NERServiceManager(final File settingsFile) throws IOException, JSONException, ClassNotFoundException {
        this.settingsFile = settingsFile;
        services = new TreeMap<String, NERService>();
        
        // First load the default settings,
        // so new services are automatically instantiated
        updateFrom(new InputStreamReader(getClass().getResourceAsStream("DefaultServices.json")));
        // Then, load the user's settings from the specified file (if it exists)
        if (settingsFile.exists())
            updateFrom(new FileReader(settingsFile));
    }
    
    /**
     * Returns whether the manager contains the specified service
     * @param serviceName The name of the service
     * @return <tt>true</tt> if the manager contains the service
     */
    public boolean hasService(final String serviceName) {
        return services.containsKey(serviceName);
    }
    
    /**
     * Adds the service to the manager
     * @param name The name of the service
     * @param service The service
     */
    public void addService(final String name, final NERService service) {
        services.put(name, service);
    }
    
    /**
     * Gets the specified service
     * @param name The name of the service
     * @return The service
     */
    public NERService getService(final String name) {
        if (!services.containsKey(name))
            throw new IllegalArgumentException("No service named " + name + " exists.");
        return services.get(name);
    }
    
    /**
     * Gets the service if it exists, or creates one otherwise and adds it to the manager
     * @param serviceName The name of the service
     * @param className The class name of the service to instantiate
     * @return The service
     * @throws ClassNotFoundException if the service cannot be instantiated
     */
    protected NERService getOrCreateService(final String serviceName, final String className) throws ClassNotFoundException {
        final NERService service;
        // Return the service if it exists
        if (hasService(serviceName)) {
            service = getService(serviceName);
        }
        // Create a new service otherwise
        else {
            // Create the service through reflection
            final Class<?> serviceClass = getClass().getClassLoader().loadClass(className);
            try {
                service = (NERService)serviceClass.newInstance();
            }
            // We assume instantiation and access are possible
            catch (InstantiationException error) { throw new RuntimeException(error); }
            catch (IllegalAccessException error) { throw new RuntimeException(error); }
            
            // Add the newly created service
            addService(serviceName, service);
        }
        return service;
    }
    
    /**
     * Gets the names of all services in the manager
     * @return The services names
     */
    public String[] getServiceNames() {
        return services.keySet().toArray(new String[services.size()]);
    }
    
    /**
     * Saves the configuration to the settings file
     * @throws IOException if the file cannot be written
     */
    public void save() throws IOException {
        final FileWriter writer = new FileWriter(settingsFile);
        writeTo(new JSONWriter(writer));
        writer.close();
    }
    
    /**
     * Writes the configuration to the specified writer
     * @param output The writer
     */
    public void writeTo(final JSONWriter output) {
        try {
            /* Array of services */
            output.array();
            for (final String serviceName : getServiceNames()) {
                final NERService service = getService(serviceName);
                /* Service object */
                output.object();
                {
                    output.key("name");
                    output.value(serviceName);
                    output.key("class");
                    output.value(service.getClass().getName());
                    output.key("configured");
                    output.value(service.isConfigured());
                    output.key("documentation");
                    output.value(service.getDocumentationUri());
                    
                    /* Service settings object */
                    output.key("settings");
                    output.object();
                    for(final String propertyName : service.getPropertyNames()) {
                        output.key(propertyName);
                        output.value(service.getProperty(propertyName));
                    }
                    output.endObject();
                }
                output.endObject();
            }
            output.endArray();
        }
        catch (JSONException e) { /* does not happen */ }
    }
    
    /**
     * Updates the manager's configuration from the JSON array
     * @param serviceValues array of service settings
     * @throws JSONException if the objects in the array are in the wrong format
     */
    @SuppressWarnings("unchecked")
    public void updateFrom(final JSONArray serviceValues) throws JSONException {
        /* Array of services */
        final Object[] services = JSONUtilities.toArray((JSONArray)serviceValues);
        for (final Object value : services) {
            /* Service object */
            if (!(value instanceof JSONObject))
                throw new IllegalArgumentException("Value should be an array of JSON objects.");
            final JSONObject serviceValue = (JSONObject)value;
            try {
                final NERService service = getOrCreateService(serviceValue.getString("name"),
                                                              serviceValue.getString("class"));
                /* Service settings object */
                if (serviceValue.has("settings")) {
                    final JSONObject settings = serviceValue.getJSONObject("settings");
                    final Iterator<String> settingNames = settings.keys();
                    while (settingNames.hasNext()) {
                        final String settingName = settingNames.next();
                        service.setProperty(settingName, settings.getString(settingName));
                    }
                }
            }
            catch (ClassNotFoundException e) {
                LOGGER.error(String.format("Could not find NER service with class %s.",
                                           serviceValue.getString("class")));
            }
        }
    }
    
    /**
     * Updates the manager's configuration from the reader
     * @param serviceValuesReader reader of service settings
     * @throws JSONException if the JSON in the reader is in the wrong format
     * @throws ClassNotFoundException if a service cannot be instantiated
     */
    public void updateFrom(final Reader serviceValuesReader) throws JSONException, ClassNotFoundException {
        final JSONTokener tokener = new JSONTokener(serviceValuesReader);
        updateFrom((JSONArray)tokener.nextValue());
        try {
            serviceValuesReader.close();
        }
        catch (IOException e) {}
    }
}
