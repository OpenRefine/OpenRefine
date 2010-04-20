from javax.servlet.http import HttpServlet

class basic(HttpServlet):
    def doGet(self, req, resp):
        resp.getOutputStream().write("Basic text response")
