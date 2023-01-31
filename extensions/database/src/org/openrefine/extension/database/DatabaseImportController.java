/*
 * Copyright (c) 2017, Tony Opara
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this 
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * 
 * Neither the name of Google nor the names of its contributors may be used to 
 * endorse or promote products derived from this software without specific 
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.openrefine.extension.database;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.RefineModel;
import org.openrefine.RefineServlet;
import org.openrefine.commands.HttpUtilities;
import org.openrefine.extension.database.model.DatabaseColumn;
import org.openrefine.extension.database.model.DatabaseQueryInfo;
import org.openrefine.importers.TabularParserHelper;
import org.openrefine.importing.ImportingController;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingManager;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.changes.GridCache;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.model.changes.LazyGridCache;
import org.openrefine.model.changes.LazyChangeDataStore;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DatabaseImportController implements ImportingController {

    private static final Logger logger = LoggerFactory.getLogger("DatabaseImportController");
    protected RefineServlet servlet;
    protected TabularParserHelper tabularParserHelper;
    public static int DEFAULT_PREVIEW_LIMIT = 100;
    public static String OPTIONS_KEY = "options";

    @Override
    public void init(RefineServlet servlet) {
        this.servlet = servlet;
        tabularParserHelper = new TabularParserHelper();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpUtilities.respond(response, "error", "GET not implemented");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("doPost Query String::{}", request.getQueryString());
        }
        response.setCharacterEncoding("UTF-8");
        Properties parameters = ParsingUtilities.parseUrlParameters(request);

        String subCommand = parameters.getProperty("subCommand");

        if (logger.isDebugEnabled()) {
            logger.info("doPost::subCommand::{}", subCommand);
        }

        if ("initialize-parser-ui".equals(subCommand)) {
            doInitializeParserUI(request, response, parameters);
        } else if ("parse-preview".equals(subCommand)) {
            try {

                doParsePreview(request, response, parameters);

            } catch (DatabaseServiceException e) {
                logger.error("doPost::DatabaseServiceException::{}", e);
                HttpUtilities.respond(response, "error", getDbServiceException(e));
            }
        } else if ("create-project".equals(subCommand)) {
            doCreateProject(request, response, parameters);
        } else {
            HttpUtilities.respond(response, "error", "No such sub command");
        }

    }

    private String getDbServiceException(Exception ex) {
        String message = "";
        if (ex instanceof DatabaseServiceException) {
            DatabaseServiceException dbEx = (DatabaseServiceException) ex;
            if (dbEx.isSqlException()) {
                message = message + dbEx.getSqlCode() + " " + dbEx.getSqlState();
            }
        }
        message = message + ex.getMessage();

        return message;
    }

    /**
     * 
     * @param request
     * @param response
     * @param parameters
     * @throws ServletException
     * @throws IOException
     */
    private void doInitializeParserUI(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("::doInitializeParserUI::");
        }

        ObjectNode result = ParsingUtilities.mapper.createObjectNode();
        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(result, "status", "ok");
        JSONUtilities.safePut(result, OPTIONS_KEY, options);

        JSONUtilities.safePut(options, "skipDataLines", 0);
        JSONUtilities.safePut(options, "storeBlankRows", true);
        JSONUtilities.safePut(options, "storeBlankCellsAsNulls", true);
        if (logger.isDebugEnabled()) {
            logger.debug("doInitializeParserUI:::{}", result.toString());
        }

        HttpUtilities.respond(response, result.toString());

    }

    /**
     * doParsePreview
     * 
     * @param request
     * @param response
     * @param parameters
     * @throws ServletException
     * @throws IOException
     * @throws DatabaseServiceException
     */
    private void doParsePreview(
            HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException, DatabaseServiceException {
        if (logger.isDebugEnabled()) {
            logger.debug("JobID::{}", parameters.getProperty("jobID"));
        }

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        DatabaseQueryInfo databaseQueryInfo = getQueryInfo(request);

        if (databaseQueryInfo == null) {
            HttpUtilities.respond(response, "error", "Invalid or missing Query Info");
        }

        job.updating = true;
        try {
            ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                    request.getParameter("options"));

            List<Exception> exceptions = new LinkedList<Exception>();

            parsePreview(
                    databaseQueryInfo,
                    job.metadata,
                    job,
                    DEFAULT_PREVIEW_LIMIT,
                    optionObj,
                    exceptions);
            Writer w = response.getWriter();
            JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
            try {
                writer.writeStartObject();
                if (exceptions.size() == 0) {
                    writer.writeStringField("status", "ok");
                } else {
                    writer.writeStringField("status", "error");
                    writer.writeStringField("message", getExceptionString(exceptions));
                }
                writer.writeEndObject();
            } catch (IOException e) {
                throw new ServletException(e);
            } finally {
                writer.flush();
                writer.close();
                w.flush();
                w.close();
            }

        } catch (IOException e) {
            throw new ServletException(e);
        } finally {
            job.touch();
            job.updating = false;
        }
    }

    private String getExceptionString(List<Exception> exceptions) {
        String ex = "";
        for (Exception e : exceptions) {
            ex = ex + e.getLocalizedMessage() + "\n";
        }
        // TODO Auto-generated method stub
        return ex;
    }

    /**
     * 
     * @param dbQueryInfo
     * @param project
     * @param metadata
     * @param job
     * @param limit
     * @param options
     * @param exceptions
     * @throws DatabaseServiceException
     */
    private void parsePreview(
            DatabaseQueryInfo dbQueryInfo,
            ProjectMetadata metadata,
            final ImportingJob job,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) throws DatabaseServiceException {

        DatabaseService databaseService = DatabaseService.get(dbQueryInfo.getDbConfig().getDatabaseType());
        String querySource = getQuerySource(dbQueryInfo);

        List<DatabaseColumn> columns = databaseService.getColumns(dbQueryInfo.getDbConfig(), dbQueryInfo.getQuery());

        setProgress(job, querySource, -1);

        JSONUtilities.safePut(options, "ignoreLines", 0); // number of blank lines at the beginning to ignore
        JSONUtilities.safePut(options, "headerLines", 1); // number of header lines

        try {
            Grid grid = tabularParserHelper.parseOneFile(
                    RefineModel.getRunner(),
                    metadata,
                    job, // fileSource
                    querySource, // archiveFileName
                    "",
                    new DBQueryResultImportReader(job, databaseService, querySource, columns, dbQueryInfo, 100),
                    limit, options);
            // this is just a preview so no changes will be applied to this project
            job.setProject(new Project(grid, new LazyChangeDataStore(), new LazyGridCache()));
        } catch (Exception e) {
            exceptions.add(e);
            e.printStackTrace();
        }

        setProgress(job, querySource, 100);

    }

    /**
     * doCreateProject
     * 
     * @param request
     * @param response
     * @param parameters
     */
    private void doCreateProject(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("DatabaseImportController::doCreateProject:::{}", parameters.getProperty("jobID"));
        }

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        final ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        final DatabaseQueryInfo databaseQueryInfo = getQueryInfo(request);
        if (databaseQueryInfo == null) {
            HttpUtilities.respond(response, "error", "Invalid or missing Query Info");
        }

        job.updating = true;
        try {
            final ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                    request.getParameter("options"));

            final List<Exception> exceptions = new LinkedList<Exception>();

            job.setState("creating-project");

            new Thread() {

                @Override
                public void run() {
                    ProjectMetadata pm = new ProjectMetadata();
                    pm.setName(JSONUtilities.getString(optionObj, "projectName", "Untitled"));
                    pm.setEncoding(JSONUtilities.getString(optionObj, "encoding", "UTF-8"));

                    try {
                        parseCreate(
                                databaseQueryInfo,
                                pm,
                                job,
                                -1,
                                optionObj,
                                exceptions);
                    } catch (DatabaseServiceException e) {
                        logger.error("DatabaseImportController::doCreateProject:::run{}", e);
                        // throw new RuntimeException("DatabaseServiceException::", e);
                    }

                    if (!job.canceled) {
                        if (exceptions.size() > 0) {
                            job.setError(exceptions);
                        } else {
                            ProjectManager.singleton.registerProject(job.getProject(), pm);
                            job.setState("created-project");
                            job.setProjectID(job.getProject().getId());
                            // logger.info("DatabaseImportController::doCreateProject:::run::projectID :{}",
                            // project.id);
                        }

                        job.touch();
                        job.updating = false;
                    }
                }
            }.start();

            HttpUtilities.respond(response, "ok", "done");
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    /**
     * @param dbQueryInfo
     * @param project
     * @param metadata
     * @param job
     * @param limit
     * @param options
     * @param exceptions
     * @throws DatabaseServiceException
     */
    private void parseCreate(
            DatabaseQueryInfo dbQueryInfo,
            ProjectMetadata metadata,
            final ImportingJob job,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) throws DatabaseServiceException {

        DatabaseService databaseService = DatabaseService.get(dbQueryInfo.getDbConfig().getDatabaseType());
        String querySource = getQuerySource(dbQueryInfo);

        List<DatabaseColumn> columns = databaseService.getColumns(dbQueryInfo.getDbConfig(), dbQueryInfo.getQuery());

        setProgress(job, querySource, -1);

        JSONUtilities.safePut(options, "ignoreLines", 0); // number of blank lines at the beginning to ignore
        JSONUtilities.safePut(options, "headerLines", 1); // number of header lines

        long startTime = System.currentTimeMillis();

        try {
            Grid grid = tabularParserHelper.parseOneFile(
                    RefineModel.getRunner(),
                    metadata,
                    job, // fileSource
                    querySource, // archiveFileName
                    "",
                    new DBQueryResultImportReader(job, databaseService, querySource, columns, dbQueryInfo, getCreateBatchSize()),
                    limit, options);
            long projectId = Project.generateID();
            ChangeDataStore dataStore = ProjectManager.singleton.getChangeDataStore(projectId);
            GridCache gridStore = ProjectManager.singleton.getGridCache(projectId);
            job.setProject(new Project(projectId, grid, dataStore, gridStore));
        } catch (Exception e) {
            exceptions.add(e);
        }

        long endTime = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("Execution Time: {}", endTime - startTime);
        }

        setProgress(job, querySource, 100);

    }

    private static int getCreateBatchSize() {
        String propBatchSize = DatabaseModuleImpl.getImportCreateBatchSize();
        int batchSize = 100;
        if (propBatchSize != null && !propBatchSize.isEmpty()) {
            try {
                batchSize = Integer.parseInt(propBatchSize);
            } catch (NumberFormatException nfe) {

            }
        }
        return batchSize;
    }

    /**
     * @param request
     * @return
     */
    private DatabaseQueryInfo getQueryInfo(HttpServletRequest request) {
        DatabaseConfiguration jdbcConfig = new DatabaseConfiguration();
        jdbcConfig.setConnectionName(request.getParameter("connectionName"));
        jdbcConfig.setDatabaseType(request.getParameter("databaseType"));
        jdbcConfig.setDatabaseHost(request.getParameter("databaseServer"));
        try {
            jdbcConfig.setDatabasePort(Integer.parseInt(request.getParameter("databasePort")));
        } catch (NumberFormatException nfE) {
            logger.error("getQueryInfo :: invalid database port ::{}", nfE);
        }
        jdbcConfig.setDatabaseUser(request.getParameter("databaseUser"));
        jdbcConfig.setDatabasePassword(request.getParameter("databasePassword"));
        jdbcConfig.setDatabaseName(request.getParameter("initialDatabase"));
        jdbcConfig.setDatabaseSchema(request.getParameter("initialSchema"));

        String query = request.getParameter("query");
        if (logger.isDebugEnabled()) {
            logger.debug("jdbcConfig::{}, query::{}", jdbcConfig, query);
        }
        if (jdbcConfig.getDatabaseHost() == null || jdbcConfig.getDatabaseName() == null
                || jdbcConfig.getDatabasePassword() == null || jdbcConfig.getDatabaseType() == null
                || jdbcConfig.getDatabaseUser() == null || query == null
                || (jdbcConfig.getDatabasePort() == 0 && !"SQLite".equalsIgnoreCase(jdbcConfig.getDatabaseType()))) {
            if (logger.isDebugEnabled()) {
                logger.debug("Missing Database Configuration::{}", jdbcConfig);
            }
            return null;
        }

        return new DatabaseQueryInfo(jdbcConfig, query);
    }

    private static String getQuerySource(DatabaseQueryInfo dbQueryInfo) {
        String dbType = dbQueryInfo.getDbConfig().getDatabaseType();
        return DatabaseService.get(dbType).getDatabaseUrl(dbQueryInfo.getDbConfig());
    }

    private static void setProgress(ImportingJob job, String querySource, int percent) {
        job.setProgress(percent, "Reading " + querySource);
    }
}
