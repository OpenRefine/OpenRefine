package org.openrefine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to retrieve the version number
 * of OpenRefine efficiently, and provide class aliasing
 * independently of Butterfly.
 * 
 * @author Antonin Delpeuch
 *
 */
public class RefineModel {
    public static String VERSION = "Spark";
	
    private static class ClassMapping {
        public final String from;
        public final String to;
        
        ClassMapping(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }
    
    private static final List<ClassMapping> classMappings = new ArrayList<ClassMapping>();
    
    /**
     * Add a mapping that determines how old class names can be updated to newer
     * class names. Such updates are desirable as the Java code changes from version
     * to version. If the "from" argument ends with *, then it's considered a prefix;
     * otherwise, it's an exact string match.
     * 
     * @param from
     * @param to
     */
    static public void registerClassMapping(String from, String to) {
        classMappings.add(new ClassMapping(from, to.endsWith("*") ? to.substring(0, to.length() - 1) : to));
    }
    
    static {
        registerClassMapping("com.metaweb.refine.*", "org.openrefine.*");
        registerClassMapping("com.google.gridworks.*", "org.openrefine.*");
        registerClassMapping("com.google.refine.*", "org.openrefine.*");
    }

	private static final Map<String, String> classMappingsCache  = new HashMap<String, String>();
	private static final Map<String, Class<?>> classCache  = new HashMap<String, Class<?>>();
	
	// TODO(dfhuynh): Temporary solution until we figure out why cross butterfly module class resolution
	// doesn't entirely work
	// NOTE(antonin): This was probably due to butterfly not respecting the class loading protocol:
	// https://github.com/OpenRefine/OpenRefine/pull/1889
	public static void cacheClass(Class<?> klass) {
		classCache.put(klass.getName(), klass);
	}
	
	public static Class<?> getClass(String className) throws ClassNotFoundException {
		String toClassName = classMappingsCache.get(className);
	    if (toClassName == null) {
	        toClassName = className;
	        
	        for (ClassMapping m : classMappings) {
	            if (m.from.endsWith("*")) {
	                if (toClassName.startsWith(m.from.substring(0, m.from.length() - 1))) {
	                    toClassName = m.to + toClassName.substring(m.from.length() - 1);
	                }
	            } else {
	                if (m.from.equals(toClassName)) {
	                    toClassName = m.to;
	                }
	            }
	        }
	        
	        classMappingsCache.put(className, toClassName);
	    }
	    
	    Class<?> klass = classCache.get(toClassName);
	    if (klass == null) {
	        klass = Class.forName(toClassName);
	        classCache.put(toClassName, klass);
	    }
	    return klass;
	}
}
