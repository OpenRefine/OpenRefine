from javax.servlet.http import HttpServlet

class increment(HttpServlet):
    def __init__(self):
        self.counter = 0
    def doGet(self, req, resp):
        self.counter += 1
        resp.outputStream.write(str(self.counter))
