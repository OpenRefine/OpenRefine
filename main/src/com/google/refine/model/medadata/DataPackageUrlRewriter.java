package com.google.refine.model.medadata;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.refine.importing.UrlRewriter;


public class DataPackageUrlRewriter implements UrlRewriter {
    private static String METADATA_FILENAME = "datapackage.json";
    
    @Override
    public List<Result> rewrite(String url) throws MalformedURLException, IOException {
        List<Result> listResult = new ArrayList<Result>();
        
        if (!filter(url))
            return listResult;
        
//        listResult.add(new Result(url, "json", true));
        
        DataPackageMetaData meta = new DataPackageMetaData();
        meta.loadFromStream(new URL(url).openStream());
//        meta.writeToFile(new File(destDir, "datapackage.json"));
        // XXX: metadata::import Import the data files. May neet to involve the Progress Bar here.
        for (String path : meta.getResources()) {
            String fileURL = getBaseURL(url) + "/" + path;
            listResult.add(new Result(fileURL, "text/line-based/*sv", true));  // XXX: metadata::import csv for now,Get the format from json
//            FileUtils.copyURLToFile(fileURL, File);
        }
        
        return listResult;
    }

    @Override
    public boolean filter(String url) {
        return url.endsWith(METADATA_FILENAME);
    }
    
    private String getBaseURL(String url) {
        return url.replaceFirst(METADATA_FILENAME, "");
    }

}
