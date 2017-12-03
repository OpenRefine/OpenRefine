package com.google.refine.model.medadata;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.refine.importing.UrlRewriter;


public class DataPackageUrlRewriter implements UrlRewriter {
    @Override
    public List<Result> rewrite(String url) throws MalformedURLException, IOException {
        List<Result> listResult = new ArrayList<Result>();
        
        if (!filter(url))
            return listResult;
        
        listResult.add(new Result(url, "json", true, MetadataFormat.DATAPACKAGE_METADATA.name()));
        
        DataPackageMetadata meta = new DataPackageMetadata();
        meta.loadFromStream(new URL(url).openStream());
        // Import the data files.  
        for (String path : meta.getResourcePaths()) {
            String fileURL = getBaseURL(url) + "/" + path;
            listResult.add(new Result(fileURL, 
                    "", // leave to guesser. "text/line-based/*sv"
                    true));  
        }
        
        return listResult;
    }

    @Override
    public boolean filter(String url) {
        return url.endsWith(DataPackageMetadata.DEFAULT_FILE_NAME);
    }
    
    private String getBaseURL(String url) {
        return url.replaceFirst(DataPackageMetadata.DEFAULT_FILE_NAME, "");
    }

}
