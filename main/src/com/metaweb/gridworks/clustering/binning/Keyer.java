package com.metaweb.gridworks.clustering.binning;


public abstract class Keyer {

    public String key(String s) {
        return this.key(s, (Object[]) null);
    }
    
    public abstract String key(String string, Object... params);
    
}
