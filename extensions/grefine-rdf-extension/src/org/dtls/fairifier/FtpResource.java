package org.dtls.fairifier;

import java.lang.IllegalArgumentException;
import java.net.URL;
import java.io.File;
import java.io.OutputStream;
import java.net.URLConnection;
import java.io.IOException;

/**
 * @author Shamanou van Leeuwen
 * @date 13-12-2016
 *
 */

public class FtpResource extends Resource{
    private String out;
    private String username;
    private String password;
    private String host;
    private String location;
    
    public FtpResource(URL hostLocation, String username, String password, String location){
        this.host = hostLocation.toString();
        this.password = password;
        this.username = username;
        this.location = location;
    }
    
    public void push(){
        if (!this.hasModel()){
            throw new IllegalArgumentException("Model of Resource object not set!");
        }else{
            this.out = this.getModelString(); 
        }
        
        try{
            URL url = new URL("ftp://"+ this.username +":"+ this.password +"@"+ this.host + File.separator + this.location +";type=i");
            URLConnection urlc = url.openConnection();
            OutputStream os = urlc.getOutputStream();
            os.write(this.out.getBytes());
        } catch (IOException ex){
            throw new IllegalArgumentException("Invalid FTP URL");
        }
    }
}
