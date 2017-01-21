package org.dtls.fairifier;

import java.lang.IllegalArgumentException;
import java.net.URL;
import java.io.File;
import java.io.OutputStream;
import java.net.URLConnection;
import java.io.IOException;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import java.io.ByteArrayInputStream;
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
    private String filename;
    
    public FtpResource(String host, String username, String password, String location, String filename){
        this.host = host;
        this.password = password;
        this.username = username;
        this.location = location;
        this.filename = filename;
    }
    
    public void push(){
        if (!this.hasModel()){
            throw new IllegalArgumentException("Data of Resource object not set!");
        }else{
            this.out = this.getModelString(); 
        }
        try{
            FTPClient ftp = new FTPClient();
            ftp.connect(this.host);
            ftp.login(this.username, this.password);
            ftp.changeWorkingDirectory(this.location);
            ftp.setFileTransferMode(ftp.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();//Switch to passive mode
            ftp.storeFile(this.filename, new ByteArrayInputStream(this.out.getBytes()));
            ftp.logout();
            ftp.disconnect();
        } catch (Exception ex){
            System.out.println(ex.getMessage());
        }
    }
}
