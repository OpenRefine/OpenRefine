package com.metaweb.gridworks.commands.edit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.ProjectMetadata;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.importers.ExcelImporter;
import com.metaweb.gridworks.importers.Importer;
import com.metaweb.gridworks.importers.TsvCsvImporter;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.ParsingUtilities;
import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

public class CreateProjectCommand extends Command {
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		try {
			Properties options = parseUrlParameters(request);
			Project project = new Project();
			
			internalImport(request, project, options);

			ProjectMetadata pm = new ProjectMetadata();
			pm.setName(options.getProperty("project-name"));
			pm.setPassword(options.getProperty("project-password"));
			ProjectManager.singleton.registerProject(project, pm);

			project.columnModel.update();
			
			redirect(response, "/project.html?project=" + project.id);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected Properties parseUrlParameters(HttpServletRequest request) {
		Properties options = new Properties();
		
		String query = request.getQueryString();
		if (query != null) {
			if (query.startsWith("?")) {
				query = query.substring(1);
			}
			
			String[] pairs = query.split("&");
			for (String pairString : pairs) {
				int equal = pairString.indexOf('=');
				String name = equal >= 0 ? pairString.substring(0, equal) : "";
				String value = equal >= 0 ? ParsingUtilities.decode(pairString.substring(equal + 1)) : "";
				
				options.put(name, value);
			}
		}
		return options;
	}
	
	protected void internalImport(
		HttpServletRequest	request,
		Project				project,
		Properties			options
	) throws Exception {
		MultipartParser parser = null;
		try {
			parser = new MultipartParser(request, 20 * 1024 * 1024);
		} catch (Exception e) {
			// silent
		}
		
		if (parser != null) {
			Part part = null;
			while ((part = parser.readNextPart()) != null) {
	            
				if (part.isFile()) {
					FilePart filePart = (FilePart) part;
					Importer importer = guessImporter(
							options, null, filePart.getFileName());
					
					if (importer.takesReader()) {
						Reader reader = new InputStreamReader(filePart.getInputStream());
						try {
							importer.read(reader, project, options);
						} finally {
							reader.close();
						}
					} else {
						InputStream inputStream = filePart.getInputStream();
						try {
							importer.read(inputStream, project, options);
						} finally {
							inputStream.close();
						}
					}
				} else if (part.isParam()) {
					ParamPart paramPart = (ParamPart) part;
					String paramName = paramPart.getName();
					if (paramName.equals("raw-text")) {
						StringReader reader = new StringReader(paramPart.getStringValue());
						try {
							new TsvCsvImporter().read(reader, project, options);
						} finally {
							reader.close();
						}
					} else if (paramName.equals("url")) {
						String url = paramPart.getStringValue();
						if (url.length() > 0) {
							internalImportURL(request, project, options, url);
						}
					} else {
						options.put(paramName, paramPart.getStringValue());
					}
				}
			}
		}
	}
	
	protected void internalImportURL(
		HttpServletRequest	request,
		Project				project,
		Properties			options,
		String				urlString
	) throws Exception {
		URL url = new URL(urlString);
		URLConnection connection = null;
		
		try {
			connection = url.openConnection();
			connection.setConnectTimeout(5000);
			connection.connect();
		} catch (Exception e) {
			throw new Exception("Cannot connect to " + urlString, e);
		}
		
        InputStream inputStream = null;
        try {
			inputStream = connection.getInputStream();
        } catch (Exception e) {
			throw new Exception("Cannot retrieve content from " + url, e);
        }
        
        try {
        	Importer importer = guessImporter(
    			options, 
    			connection.getContentType(),
    			url.getPath()
			);
        
			if (importer.takesReader()) {
				String encoding = connection.getContentEncoding();
				
				Reader reader = new InputStreamReader(
					inputStream, (encoding == null) ? "ISO-8859-1" : encoding);
							
				importer.read(reader, project, options);
			} else {
				importer.read(inputStream, project, options);
			}
        } finally {
			inputStream.close();
        }
	}
	
	protected Importer guessImporter(
			Properties options, String contentType, String fileName) {
		
		if (contentType != null) {
			contentType = contentType.toLowerCase().trim();
			
			if ("application/msexcel".equals(contentType) ||
		        "application/x-msexcel".equals(contentType) ||
		        "application/x-ms-excel".equals(contentType) ||
		        "application/vnd.ms-excel".equals(contentType) ||
		        "application/x-excel".equals(contentType) ||
		        "application/xls".equals(contentType) ||
		        "application/x-xls".equals(contentType)) {
				
				return new ExcelImporter(); 
			}
		} else if (fileName != null) {
			fileName = fileName.toLowerCase();
			if (fileName.endsWith(".xls")) { // Note: we can't handle .xlsx yet
				return new ExcelImporter(); 
			}
		}
		
		return new TsvCsvImporter();
	}

}
