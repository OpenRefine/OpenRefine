package org.dtls.fairifier;

import java.io.StringWriter;

/**
 * @author Shamanou van Leeuwen
 * @date 28-11-2016
 *
 */
public abstract class Resource {
    public String fairData = null;
    
    public void setFairData(String fairData){
        this.fairData = fairData;
    }
    
    protected boolean hasModel(){
        if (this.fairData == null) return false;
        return true;
    }
    
    protected String getModelString(){
        return this.fairData;
    }
    
    abstract void push();
}
