/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.commands.browsing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.browsing.facets.ScatterplotFacet.ScatterplotFacetConfig;
import org.openrefine.browsing.facets.ScatterplotFacetResult;
import org.openrefine.browsing.facets.ScatterplotPainter;
import org.openrefine.browsing.util.ScatterplotFacetState;
import org.openrefine.commands.Command;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.util.ParsingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetScatterplotCommand extends Command {

    final static Logger logger = LoggerFactory.getLogger("get-scatterplot_command");
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            long start = System.currentTimeMillis();
            
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            ScatterplotFacetConfig conf = ParsingUtilities.mapper.readValue(
            		request.getParameter("plotter"),
            		ScatterplotFacetConfig.class);
            
            response.setHeader("Content-Type", "image/png");
            
            ServletOutputStream sos = null;
            
            try {
                sos = response.getOutputStream();
                draw(sos, project, engine, conf);
            } finally {
                sos.close();
            }
            
            logger.trace("Drawn scatterplot in {} ms", Long.toString(System.currentTimeMillis() - start));
        } catch (Exception e) {
            e.printStackTrace();
            respondException(response, e);
        }
    }

    public void draw(OutputStream output, Project project, Engine engine, ScatterplotFacetConfig o) throws IOException {
        GridState grid = project.getCurrentGridState();
        
        // Compute a modified Engine which includes the facet in last position
        EngineConfig origEngineConfig = engine.getConfig();
        int scatterplotFacetPosition = origEngineConfig.getFacetConfigs().size();
        List<FacetConfig> newFacetConfigs = new ArrayList<>(origEngineConfig.getFacetConfigs());
        newFacetConfigs.add(o);
        EngineConfig newEngineConfig = new EngineConfig(newFacetConfigs, engine.getMode());
        Engine newEngine = new Engine(grid, newEngineConfig);
        
        ScatterplotFacetResult scatterplotFacetResult = (ScatterplotFacetResult) newEngine.getFacetResults().get(scatterplotFacetPosition);
        ScatterplotFacetState facetState = scatterplotFacetResult.getFacetState();
        
        if (facetState.getValuesCount() > 0) {
            ScatterplotPainter drawer = new ScatterplotPainter(
                scatterplotFacetResult, 
                o.size, o.dim_x, o.dim_y, o.rotation, o.dot,
                o.getColor(), o.getBaseColor()
            );
            
            drawer.drawPoints(facetState);
            
            ImageIO.write(drawer.getImage(), "png", output);
        } else {
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR), "png", output);
        }
        
    }
    
}
