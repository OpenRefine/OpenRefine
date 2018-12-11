package com.google.refine.commands.browsing;


import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.clustering.binning.KeyerFactory;
import com.google.refine.clustering.knn.DistanceFactory;
import com.google.refine.commands.Command;

public class GetClusteringFunctionsAndDistancesCommand extends Command {
	final static Logger logger = LoggerFactory.getLogger("get-clustering-functions-and-distances_command");
	
	private static class FunctionsAndDistancesResponse {
		@JsonProperty("distances")
		public List<String> distances = DistanceFactory.getDistanceNames();
		
		@JsonProperty("keyers")
		public List<String> keyers = KeyerFactory.getKeyerNames();
	}

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	respondJSON(response, new FunctionsAndDistancesResponse());
    }
}
