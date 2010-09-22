package com.google.refine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.simile.butterfly.ButterflyModule;
import edu.mit.simile.butterfly.MountPoint;


public class ClientSideResourceManager {
    final static Logger logger = LoggerFactory.getLogger("refine_clientSideResourceManager");
    
    static public class QualifiedPath {
        public ButterflyModule  module;
        public String           path;
        public String           fullPath;
    }
    static public class ClientSideResourceBundle {
        final protected Set<String>     _pathSet = new HashSet<String>();
        final protected List<QualifiedPath>      _pathList = new ArrayList<QualifiedPath>();
    }
    
    final static protected Map<String, ClientSideResourceBundle> s_bundles
        = new HashMap<String, ClientSideResourceBundle>();
    
    static public void addPaths(
        String bundleName, 
        ButterflyModule module, 
        String[] paths) {
        
        ClientSideResourceBundle bundle = s_bundles.get(bundleName);
        if (bundle == null) {
            bundle = new ClientSideResourceBundle();
            s_bundles.put(bundleName, bundle);
        }
        
        for (String path : paths) {
            String fullPath = resolve(module, path);
            if (fullPath == null) {
                logger.error("Failed to add paths to unmounted module " + module.getName());
                break;
            }
            if (!bundle._pathSet.contains(fullPath)) {
                QualifiedPath qualifiedPath = new QualifiedPath();
                qualifiedPath.module = module;
                qualifiedPath.path = path;
                qualifiedPath.fullPath = fullPath;
                
                bundle._pathSet.add(fullPath);
                bundle._pathList.add(qualifiedPath);
            }
        }
    }
    
    static public QualifiedPath[] getPaths(String bundleName) {
        ClientSideResourceBundle bundle = s_bundles.get(bundleName);
        if (bundle == null) {
            return new QualifiedPath[] {};
        } else {
            QualifiedPath[] paths = new QualifiedPath[bundle._pathList.size()];
            bundle._pathList.toArray(paths);
            return paths;
        }
    }
    
    static protected String resolve(ButterflyModule module, String path) {
        MountPoint mountPoint = module.getMountPoint();
        if (mountPoint != null) {
            String mountPointPath = mountPoint.getMountPoint();
            if (mountPointPath != null) {
                StringBuffer sb = new StringBuffer();
                
                boolean slashed = path.startsWith("/");
                char[] mountPointChars = mountPointPath.toCharArray();
                
                sb.append(mountPointChars, 0, slashed ? mountPointChars.length - 1 : mountPointChars.length);
                sb.append(path);
                
                return sb.toString();
            }
        }
        return null;
    }
}
