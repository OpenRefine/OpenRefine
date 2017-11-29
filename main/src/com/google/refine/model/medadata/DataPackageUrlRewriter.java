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
        
        listResult.add(new Result(url, "json", true, MetadataFormat.OKF_METADATA.name()));
        
        DataPackageMetaData meta = new DataPackageMetaData();
        meta.loadFromStream(new URL(url).openStream());
        // Import the data files.  
        for (String path : meta.getResources()) {
            String fileURL = getBaseURL(url) + "/" + path;
            listResult.add(new Result(fileURL, "text/line-based/*sv", true));  // XXX: metadata::import csv for now,Get the format from json
        }
        
        return listResult;
    }

    @Override
    public boolean filter(String url) {
        return url.endsWith(DataPackageMetaData.DEFAULT_FILE_NAME);
    }
    
    private String getBaseURL(String url) {
        return url.replaceFirst(DataPackageMetaData.DEFAULT_FILE_NAME, "");
    }

}
