
package com.google.refine.clustering;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;

import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.clustering.knn.DistanceFactory;
import com.google.refine.clustering.knn.VicinoDistance;
import com.google.refine.clustering.knn.kNNClusterer;
import com.google.refine.clustering.knn.kNNClusterer.kNNClustererConfig;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class ClusteringProcessTests extends RefineTest {

    @Test
    public void testRunToCompletion() throws Exception {
        Project project = createProject(
                new String[] { "column" },
                new Serializable[][] {
                        { "ab" },
                        { "abc" },
                        { "c" }
                });

        DistanceFactory.put("ppm", new VicinoDistance(new edu.mit.simile.vicino.distances.PPMDistance()));

        String configJson = "{\"type\":\"knn\",\"function\":\"PPM\","
                + "\"column\":\"column\",\"params\":{\"radius\":1,\"blocking-ngram-size\":2}}";

        kNNClustererConfig config = ParsingUtilities.mapper.readValue(configJson, kNNClustererConfig.class);
        kNNClusterer clusterer = config.apply(project);
        Engine engine = new Engine(project);

        ClusteringProcess process = new ClusteringProcess(clusterer, engine, "test");
        assertFalse(process.isImmediate());

        // Start the process using the project's ProcessManager
        project.getProcessManager().queueProcess(process);

        // Wait for completion (small data, should be fast)
        long deadline = System.currentTimeMillis() + 5000;
        while (!process.isDone() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        assertTrue(process.isCompleted());
        assertFalse(process.isFailed());
        assertEquals(process.getClusterer(), clusterer);
    }

    @Test
    public void testCancellation() throws Exception {
        Project project = createProject(
                new String[] { "column" },
                new Serializable[][] {
                        { "ab" },
                        { "abc" }
                });

        DistanceFactory.put("ppm", new VicinoDistance(new edu.mit.simile.vicino.distances.PPMDistance()));

        String configJson = "{\"type\":\"knn\",\"function\":\"PPM\","
                + "\"column\":\"column\",\"params\":{\"radius\":1,\"blocking-ngram-size\":2}}";

        kNNClustererConfig config = ParsingUtilities.mapper.readValue(configJson, kNNClustererConfig.class);
        kNNClusterer clusterer = config.apply(project);
        Engine engine = new Engine(project);

        ClusteringProcess process = new ClusteringProcess(clusterer, engine, "test");

        // Cancel before running
        process.cancel();
        assertTrue(clusterer.isCanceled());
    }
}
