package com.metaweb.gridworks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.simile.butterfly.ButterflyModule;
import edu.mit.simile.butterfly.MountPoint;


public class ClientSideResourceManager {
    static public class ClientSideResourceBundle {
        final protected Set<String>     _pathSet = new HashSet<String>();
        final protected List<String>    _pathList = new ArrayList<String>();
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
            if (!bundle._pathSet.contains(fullPath)) {
                bundle._pathSet.add(fullPath);
                bundle._pathList.add(fullPath);
            }
        }
    }
    
    static public String[] getPaths(String bundleName) {
        ClientSideResourceBundle bundle = s_bundles.get(bundleName);
        if (bundle == null) {
            return new String[] {};
        } else {
            String[] paths = new String[bundle._pathList.size()];
            bundle._pathList.toArray(paths);
            return paths;
        }
    }
    
    static protected String resolve(ButterflyModule module, String path) {
        StringBuffer sb = new StringBuffer();
        
        MountPoint mountPoint = module.getMountPoint();
        
        boolean slashed = path.startsWith("/");
        char[] mountPointChars = mountPoint.getMountPoint().toCharArray();
        
        sb.append(mountPointChars, 0, slashed ? mountPointChars.length - 1 : mountPointChars.length);
        sb.append(path);
        
        return sb.toString();
    }
}
