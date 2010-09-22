package com.google.refine.commands.browsing;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.browsing.Engine;
import com.google.refine.clustering.Clusterer;
import com.google.refine.clustering.binning.BinningClusterer;
import com.google.refine.clustering.knn.kNNClusterer;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;

public class ComputeClustersCommand extends Command {

    final static Logger logger = LoggerFactory.getLogger("compute-clusters_command");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            long start = System.currentTimeMillis();
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            JSONObject clusterer_conf = getJsonParameter(request,"clusterer");

            Clusterer clusterer = null;
            String type = clusterer_conf.has("type") ? clusterer_conf.getString("type") : "binning";
            
            if ("knn".equals(type)) {
                clusterer = new kNNClusterer();
            } else  {
                clusterer = new BinningClusterer();
            }
                
            clusterer.initializeFromJSON(project, clusterer_conf);
            
            clusterer.computeClusters(engine);
            
            respondJSON(response, clusterer);
            logger.info("computed clusters [{},{}] in {}ms", new Object[] { type, clusterer_conf.getString("function"), Long.toString(System.currentTimeMillis() - start) });
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
