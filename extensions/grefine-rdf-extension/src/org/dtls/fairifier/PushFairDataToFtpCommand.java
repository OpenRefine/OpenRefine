package org.dtls.fairifier;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.google.refine.commands.Command;
import java.net.URL;

/**
 * 
 * @author Shamanou van Leeuwen
 * @date 13-12-2016
 *
 */

public class PushFairDataToFtpCommand extends Command{
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) 
            throws ServletException, IOException {
        String data = req.getParameter("fair_rdf");
        PushFairDataToResourceAdapter adapter = new PushFairDataToResourceAdapter();
        URL host = new URL(req.getParameter("location"));
        adapter.setResource(
                new FtpResource(host, req.getParameter("username"), 
                        req.getParameter("password"), 
                        req.getParameter("name")
                )
        );
        adapter.push();
    }
}
