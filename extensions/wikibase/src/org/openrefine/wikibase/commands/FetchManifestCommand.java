
package org.openrefine.wikibase.commands;

import static org.openrefine.wikibase.commands.CommandUtilities.respondError;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.refine.commands.Command;

import org.openrefine.wikibase.utils.HttpClient;

/**
 * Proxies Wikibase manifests to allow the client to bypass CORS restrictions.
 */
public class FetchManifestCommand extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String url = request.getParameter("url");
        try {
            if (url == null) {
                respondError(response, "No URL provided.");
                return;
            }

            // fetch the contents at the url with a plain get request and return the response
            OkHttpClient client = HttpClient.getClient();
            Request req = new Request.Builder().url(url).build();
            Response res = client.newCall(req).execute();
            if (!res.isSuccessful()) {
                response.sendError(res.code(), res.message());
            }
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write(res.body().string());
            response.setStatus(200);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
