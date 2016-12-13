package org.dtls.fairifier;

import com.hp.hpl.jena.rdf.model.Model;
import java.io.StringWriter;

/**
 * @author Shamanou van Leeuwen
 * @date 28-11-2016
 *
 */
public abstract class Resource {
    protected Model fairData = null;
    public void setFairData(Model fairData){
        this.fairData = fairData;
    }
    
    protected boolean hasModel(){
        if (this.fairData == null) return true;
        return false;
    }
    
    protected String getModelString(){
        StringWriter out = new StringWriter();
        this.fairData.write(out, "TTL");
        return out.toString();
    }
    
    abstract void push();
}
