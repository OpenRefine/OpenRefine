package org.dtls.fairifier;

import java.net.URI;
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
    private String name;
    
    public void push(){
        if (!this.hasModel()){
            throw new IllegalArgumentException("Model of Resource object not set!");
        }else{
            this.out = this.getModelString(); 
        }
        
        try{
            URL url = new URL("ftp://"+ this.username +":"+ this.password +"@"+ this.host + File.separator + this.name +";type=i");
            URLConnection urlc = url.openConnection();
            OutputStream os = urlc.getOutputStream();
            os.write(this.out.getBytes());
        } catch (IOException ex){
            throw new IllegalArgumentException("Invalid FTP URL");
        }
    }
    
    public void setHost(URI hostLocation){
        this.host = hostLocation.toString();
    }
    
    public void setCredentials(String username, String password){
        this.password = password;
        this.username = username;
    }
    
    public void setFileName(String name){
        this.name = name;
    }
}
